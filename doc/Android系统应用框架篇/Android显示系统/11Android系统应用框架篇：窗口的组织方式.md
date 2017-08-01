# Android系统应用框架篇：窗口的组织方式

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，爱好广泛，技术栈主要涉及以下几个方面
>
>- Android/Linux
>- Java/Kotlin/JVM
>- Python
>- JavaScript/React/ReactNative
>- DataStructure/Algorithm
>
>文章首发于[Github](https://github.com/guoxiaoxing)，后续也会同步在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与
[CSDN](http://blog.csdn.net/allenwells)等博客平台上。文章中如果有什么问题，欢迎发邮件与我交流，邮件可发至guoxiaoxingse@163.com。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

Android系统的窗口与Activity类似，也是以堆栈的方式组织在WindowManagerService之中，其中Z轴位置较低的窗口位于Z轴位置较高的窗口下面。

WindowManagerService之中定义了三个类：

- WindowState：实现了WindowManagerPolicy.WindowState接口，它描述了Android系统中的窗口信息。
- WindowToken：窗口令牌，它描述了窗口类型、可见性等属性，这些属性的一组窗口具有相同的令牌。
- AppWindowToken：继承于WindowToken，用来描述应用的Activity窗口。

WindowManagerService也定了几个变量来描述系统中的窗口。

```java
//每一个WindowToken都用来描述一个窗口，并以窗口的IBinder为键值
final HashMap<IBinder, WindowToken> mTokenMap =
        new HashMap<IBinder, WindowToken>();

//和mTokenMap保存一样的WindowToken对象，提供List是为了方便遍历WindowToken对象
final ArrayList<WindowToken> mTokenList = new ArrayList<WindowToken>();

//保存AppWindowToken对象，用来描述Activity窗口，以窗口在Z轴上的坐标从小到大排列。
final ArrayList<AppWindowToken> mAppTokens = new ArrayList<AppWindowToken>();

//保存WindowState，它用来描述窗口信息，以窗口在Z轴坐标从小到大排列。
final ArrayList<WindowState> mWindows = new ArrayList<WindowState>();
```
Activity组件在启动的过程中，ActivityManagerService会调用addAppToken()方法来为它添加一个AppWindowToken。

```java
public class WindowManagerService extends IWindowManager.Stub
        implements Watchdog.Monitor {
    
    public void addAppToken(int addPos, IApplicationToken token,
                int groupId, int requestedOrientation, boolean fullscreen) {
            if (!checkCallingPermission(android.Manifest.permission.MANAGE_APP_TOKENS,
                    "addAppToken()")) {
                throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
            }
            
            // Get the dispatching timeout here while we are not holding any locks so that it
            // can be cached by the AppWindowToken.  The timeout value is used later by the
            // input dispatcher in code that does hold locks.  If we did not cache the value
            // here we would run the chance of introducing a deadlock between the window manager
            // (which holds locks while updating the input dispatcher state) and the activity manager
            // (which holds locks while querying the application token).
            long inputDispatchingTimeoutNanos;
            try {
                inputDispatchingTimeoutNanos = token.getKeyDispatchingTimeout() * 1000000L;
            } catch (RemoteException ex) {
                Slog.w(TAG, "Could not get dispatching timeout.", ex);
                inputDispatchingTimeoutNanos = DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS;
            }
    
            synchronized(mWindowMap) {
                //调用findAppWindowToken查找mTokenMap里是否已经存在一个AppWindowToken对象
                AppWindowToken wtoken = findAppWindowToken(token.asBinder());
                if (wtoken != null) {
                    Slog.w(TAG, "Attempted to add existing app token: " + token);
                    return;
                }
                wtoken = new AppWindowToken(token);
                wtoken.inputDispatchingTimeoutNanos = inputDispatchingTimeoutNanos;
                wtoken.groupId = groupId;
                wtoken.appFullscreen = fullscreen;
                wtoken.requestedOrientation = requestedOrientation;
                mAppTokens.add(addPos, wtoken);
                if (localLOGV) Slog.v(TAG, "Adding new app token: " + wtoken);
                mTokenMap.put(token.asBinder(), wtoken);
                mTokenList.add(wtoken);
    
                // Application tokens start out hidden.
                wtoken.hidden = true;
                wtoken.hiddenRequested = true;
    
                //dump();
            }
        }    
}
```

我们先来看看这个函数的参数：

```
int addPos：描述该Activity组件在堆栈里的位置，这个位置也是AppWindowToken在列表中的位置。
IApplicationToken token：指向用来描述Activity组件的ActivityRecord对象。
```
这个函数的逻辑也很简单，先是调用findAppWindowToken()方法查找mTokenMap里是否已经存在一个AppWindowToken对象，如果没有则创建一个AppWindowToken对象
并添加到mTokenMap里。