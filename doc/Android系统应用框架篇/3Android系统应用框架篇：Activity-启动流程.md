# Android系统应用框架篇：Activity-启动流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流
的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去
star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时
的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，
更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

关于Activity


Launcher.startActivitySafely(Intent intent, Object tag)

```java
/**
 * Default launcher application.
 */
public final class Launcher extends Activity
        implements View.OnClickListener, OnLongClickListener, LauncherModel.Callbacks, AllAppsView.Watcher {
        
    void startActivitySafely(Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            //调用Activity的startActivity()方法
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag="+ tag + " intent=" + intent, e);
        }
    }
    
}
```

我们来分析一下这段代码，当我们在手机上点击一个应用的图标时，上述代码就会运行。intent会先添加Intent.FLAG_ACTIVITY_NEW_TASK，保证新运行的应用
的Activity都建立在一个新的任务栈中。然后再去调用Activity的startActivity()方法。

那么应用的图标是怎么和这个应用的Lancher Activity联系起来的呢？

系统在启动的时候会启动PackageManagerService（包管理服务），所有的应用都是通过它安装的，PackageManagerService会对应用的AndroidManifest.xml
进行解析，从而得到应用里所有的组件信息。Lanucher组件在启动过程中就会去向PackageManagerService查询包含以下信息的Activity组件。

```xml
<intent-filter>
        <action android:name="android.intent.action.MAIN" />

        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
```

并为每一个包含该信息的Activity组件创建一个快捷图标，由此两者便建立了联系。关于Android应用的安装和启动流程，我们后续还有详细的文章做分析。


Activity.startActivity(Intent intent)

```java
public class Activity extends ContextThemeWrapper
        implements LayoutInflater.Factory,
        Window.Callback, KeyEvent.Callback,
        OnCreateContextMenuListener, ComponentCallbacks {
        
        @Override
        public void startActivity(Intent intent) {
            startActivityForResult(intent, -1);
        }

}
```
startActivity()会去调用startActivityForResult(intent, -1)，-1表示不需要知道Activity的执行结果。


Activity.startActivity(Intent intent)

```java
public class Activity extends ContextThemeWrapper
        implements LayoutInflater.Factory,
        Window.Callback, KeyEvent.Callback,
        OnCreateContextMenuListener, ComponentCallbacks {
        
    public void startActivityForResult(Intent intent, int requestCode) {
        if (mParent == null) {
            Instrumentation.ActivityResult ar =
                mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this,
                    intent, requestCode);
            if (ar != null) {
                mMainThread.sendActivityResult(
                    mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                    ar.getResultData());
            }
            if (requestCode >= 0) {
                // If this start is requesting a result, we can avoid making
                // the activity visible until the result is received.  Setting
                // this code during onCreate(Bundle savedInstanceState) or onResume() will keep the
                // activity hidden during this time, to avoid flickering.
                // This can only be done when a result is requested because
                // that guarantees we will get information back when the
                // activity is finished, no matter what happens to it.
                mStartedActivity = true;
            }
        } else {
            mParent.startActivityFromChild(this, intent, requestCode);
        }
    }
}
```
该方法牵扯到了Activity内部的两个成员变量：

```
Intrumentation：用来监控应用与系统之间的交互操作。
```
Activity的启动也是一个应用与系统之间的交互操作，startActivityForResult()会去调用mInstrumentation.execStartActivity()来代为执行Activity的启动操
作，这样Intrumentation便可以去监控Activity的整个启动过程。

```
ActivityThread：用来描述一个应用进程。
```
每当系统启动一个应用进程时，都会启动一个ActivityThread实例，该实例保存在Activity的mThread变量中。


Instrumentation.execStartActivity(
                        Context who, IBinder contextThread, IBinder token, Activity target,
                        Intent intent, int requestCode)

```java
/**
 * Base class for implementing application instrumentation code.  When running
 * with instrumentation turned on, this class will be instantiated for you
 * before any of the application code, allowing you to monitor all of the
 * interaction the system has with the application.  An Instrumentation
 * implementation is described to the system through an AndroidManifest.xml's
 * &lt;instrumentation&gt; tag.
 */
public class Instrumentation {

    public ActivityResult execStartActivity(
        Context who, IBinder contextThread, IBinder token, Activity target,
        Intent intent, int requestCode) {
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        if (mActivityMonitors != null) {
            synchronized (mSync) {
                final int N = mActivityMonitors.size();
                for (int i=0; i<N; i++) {
                    final ActivityMonitor am = mActivityMonitors.get(i);
                    if (am.match(who, null, intent)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            return requestCode >= 0 ? am.getResult() : null;
                        }
                        break;
                    }
                }
            }
        }
        try {
            int result = ActivityManagerNative.getDefault()
                .startActivity(whoThread, intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()),
                        null, 0, token, target != null ? target.mEmbeddedID : null,
                        requestCode, false, false);
            checkStartActivityResult(result, intent);
        } catch (RemoteException e) {
        }
        return null;
    }
    
}
```

我们再来看看传递给mInstrumentation.execStartActivity()方法的6个参数：

- Context who：被启动Activity的Context。
- IBinder contextThread：被启动Activity的主进程，由mMainThread.getApplicationThread()获取的一个类型为ApplicationThread
的Binder本地对象，该对象最终传递给了ActivityManagerService。
- IBinder token：启动该Activity的内部token，供系统识别。它其实是一个代理对象，指向了ActivityManagerService中一个类型为ActivityRecord的
Binder本地对象。
- Activity target：调用startActivity的tartget Activity。
- Intent intent：intent
- int requestCode：requestCode

Instrumentation.execStartActivity()先调用ActivityManagerNative.getDefault()获取ActivityManagerService的代理对象，再调用ActivityManagerService
.startActivity()启动Activity。

我们先来看一下getDefault()方法的实现。

```java
public abstract class ActivityManagerNative extends Binder implements IActivityManager{

      private static IActivityManager gDefault;

      /**
       * Cast a Binder object into an application thread interface, generating
       * a proxy if needed.
       */
      static public IApplicationThread asInterface(IBinder obj) {
          if (obj == null) {
              return null;
          }
          IApplicationThread in =
              (IApplicationThread)obj.queryLocalInterface(descriptor);
          if (in != null) {
              return in;
          }
          
          return new ApplicationThreadProxy(obj);
      }
    
      /**
       * Retrieve the system's default/global activity manager.
       */
      static public IActivityManager getDefault()
      {
          if (gDefault != null) {
              //if (Config.LOGV) Log.v(
              //    "ActivityManager", "returning cur default = " + gDefault);
              return gDefault;
          }
          IBinder b = ServiceManager.getService("activity");
          if (Config.LOGV) Log.v(
              "ActivityManager", "default service binder = " + b);
          gDefault = asInterface(b);
          if (Config.LOGV) Log.v(
              "ActivityManager", "default service = " + gDefault);
          return gDefault;
      }  
}
  
```
ActivityManagerNative.getDefault()通过ServiceManager.getService("activity")获取一个引用ActivityManagerService的服务代理对象，然后
调用asInterface(b)函数将其封装成一个类型为ApplicationThreadProxy的代理对象，并保存在gDefault静态变量中。ApplicationThreadProxy实现了
IActivityManager里的相关方法。

ApplicationThreadProxy.startActivity(IApplicationThread caller, Intent intent,
                                   String resolvedType, Uri[] grantedUriPermissions, int grantedMode,
                                   IBinder resultTo, String resultWho,
                                   int requestCode, boolean onlyIfNeeded,
                                   boolean debug) 

```java
class ActivityManagerProxy implements IActivityManager{

    public int startActivity(IApplicationThread caller, Intent intent,
            String resolvedType, Uri[] grantedUriPermissions, int grantedMode,
            IBinder resultTo, String resultWho,
            int requestCode, boolean onlyIfNeeded,
            boolean debug) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IActivityManager.descriptor);
        data.writeStrongBinder(caller != null ? caller.asBinder() : null);
        intent.writeToParcel(data, 0);
        data.writeString(resolvedType);
        data.writeTypedArray(grantedUriPermissions, 0);
        data.writeInt(grantedMode);
        data.writeStrongBinder(resultTo);
        data.writeString(resultWho);
        data.writeInt(requestCode);
        data.writeInt(onlyIfNeeded ? 1 : 0);
        data.writeInt(debug ? 1 : 0);
        mRemote.transact(START_ACTIVITY_TRANSACTION, data, reply, 0);
        reply.readException();
        int result = reply.readInt();
        reply.recycle();
        data.recycle();
        return result;
    }
    
}
```