# Android系统应用框架篇：WIndowManagerService窗口的组织方式

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。


<font face="黑体">我是黑体字</font>
<font face="微软雅黑">我是微软雅黑</font>
<font face="STCAIYUN">我是华文彩云</font>
<font color=#0099ff size=7 face="黑体">color=#0099ff size=72 face="黑体"</font>
<font color=#00ffff size=72>color=#00ffff</font>
<font color=gray size=72>color=gray</font>


<table><tr><td bgcolor=orange>背景色是：orange</td></tr></table>


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
这个函数的逻辑也很简单，先是调用findAppWindowToken()方法查找mTokenMap里是否已经存在一个AppWindowToken对象，如果没有则利用传递进来的参数封装了一
个AppWindowToken对象并把它添加到了我们上面描述的WindowManagerService的三个成员变量mAppTokens、mTokenMap与mTokenList中。


移动AppToken

```java
public class WindowManagerService extends IWindowManager.Stub
        implements Watchdog.Monitor {
    
    public void moveAppToken(int index, IBinder token) {
        //1 移动AppToken需要android.Manifest.permission.MANAGE_APP_TOKENS权限
        if (!checkCallingPermission(android.Manifest.permission.MANAGE_APP_TOKENS,
                "moveAppToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }

        synchronized(mWindowMap) {
            if (DEBUG_REORDER) Slog.v(TAG, "Initial app tokens:");
            if (DEBUG_REORDER) dumpAppTokensLocked();
            //2 根据token查找出AppWindowToken对象
            final AppWindowToken wtoken = findAppWindowToken(token);
            //3 删除AppWindowToken对象
            if (wtoken == null || !mAppTokens.remove(wtoken)) {
                Slog.w(TAG, "Attempting to reorder token that doesn't exist: "
                      + token + " (" + wtoken + ")");
                return;
            }
            //4 在指定位置添加AppWindowToken对象
            mAppTokens.add(index, wtoken);
            if (DEBUG_REORDER) Slog.v(TAG, "Moved " + token + " to " + index + ":");
            if (DEBUG_REORDER) dumpAppTokensLocked();

            final long origId = Binder.clearCallingIdentity();
            if (DEBUG_REORDER) Slog.v(TAG, "Removing windows in " + token + ":");
            if (DEBUG_REORDER) dumpWindowsLocked();
            //5 将AppWindowToken对应的WindowState对象删除
            if (tmpRemoveAppWindowsLocked(wtoken)) {
                if (DEBUG_REORDER) Slog.v(TAG, "Adding windows back in:");
                if (DEBUG_REORDER) dumpWindowsLocked();
                //6 将AppWindowToken对应的WindowState对象插入到合适的位置
                reAddAppWindowsLocked(findWindowOffsetLocked(index), wtoken);
                if (DEBUG_REORDER) Slog.v(TAG, "Final window list:");
                if (DEBUG_REORDER) dumpWindowsLocked();
                updateFocusedWindowLocked(UPDATE_FOCUS_WILL_PLACE_SURFACES);
                mLayoutNeeded = true;
                performLayoutAndPlaceSurfacesLocked();
            }
            Binder.restoreCallingIdentity(origId);
        }
    }
}
```

移动WindowState到合适的位置

```java
public class WindowManagerService extends IWindowManager.Stub
        implements Watchdog.Monitor {
    
     // 参数tokenPos描述的是AppWindowToken在mAppTokens里的位置索引
     private int findWindowOffsetLocked(int tokenPos) {
            final int NW = mWindows.size();
    
            if (tokenPos >= mAppTokens.size()) {
                //依次遍历，找到与mAppTokens列表最上面一个AppWindowToken对象所对应的Z轴
                //位置最大的饿一个WindowState对象在mWindows中的位置，然后将其+1.
                int i = NW;
                while (i > 0) {
                    i--;
                    WindowState win = mWindows.get(i);
                    if (win.getAppToken() != null) {
                        return i+1;
                    }
                }
            }
    
            while (tokenPos > 0) {
                // Find the first app token below the new position that has
                // a window displayed.
                final AppWindowToken wtoken = mAppTokens.get(tokenPos-1);
                if (DEBUG_REORDER) Slog.v(TAG, "Looking for lower windows @ "
                        + tokenPos + " -- " + wtoken.token);
                if (wtoken.sendingToBottom) {
                    if (DEBUG_REORDER) Slog.v(TAG,
                            "Skipping token -- currently sending to bottom");
                    tokenPos--;
                    continue;
                }
                int i = wtoken.windows.size();
                while (i > 0) {
                    i--;
                    WindowState win = wtoken.windows.get(i);
                    int j = win.mChildWindows.size();
                    while (j > 0) {
                        j--;
                        WindowState cwin = win.mChildWindows.get(j);
                        if (cwin.mSubLayer >= 0) {
                            for (int pos=NW-1; pos>=0; pos--) {
                                if (mWindows.get(pos) == cwin) {
                                    if (DEBUG_REORDER) Slog.v(TAG,
                                            "Found child win @" + (pos+1));
                                    return pos+1;
                                }
                            }
                        }
                    }
                    for (int pos=NW-1; pos>=0; pos--) {
                        if (mWindows.get(pos) == win) {
                            if (DEBUG_REORDER) Slog.v(TAG, "Found win @" + (pos+1));
                            return pos+1;
                        }
                    }
                }
                tokenPos--;
            }
    
            return 0;
        }
}
```

该函数的目的在于找到与AppWindowToken对应的WindowState在WindowState堆栈里的起始偏移位置，该堆栈由成员变量mWindows来描述。通过
该起始偏移位置将WindowState插入的堆栈mWindows中。

我们分两种情况来讨论：

1 tokenPos >= mAppTokens.size()

tokenPos应该指向mAppTokens其中的某个位置，之所以会出现这种情况，说明tokenPos指向的AppWindowToken对象的Z轴位置要大于mAppTokens里任何一个AppWindowToken对象的Z轴位置。
