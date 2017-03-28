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


从这篇文章开始，我们来详细地分析Activity的启动流程，在分析的过程中会有各种各样的角色参与进来，例如：ActivityServiceManager、ActivityStack、ActivityRecord等，我们
可能不能一下都明白它们是做什么的，这个时候就需要我们专注流程的理解，不要陷入到具体的细节之中，随着分析的深入，这些前面觉得疑惑的问题后面都会一一得到解决，毕竟代码岁虽多，流程
虽长，但本质上都是组件间的协同，参数的包装与处理，只要我们抓住核心原理，所有的问题就都迎刃而解。

笔者在分析的过程中，也会为读者提供各种结构图、时序图来辅助理解，每个小节完成后，也会再次做小节汇总，力求让读者看得明白，记得深刻。另外，Android四大组件的启动流程有异曲同工
之处我们掌握了Activity，后面各组件以及其他系统都可以举一反三，触类旁通。

好，让我们开始吧，Activity的启动流程一共分为35个小步骤，主要在5个组件中运行。

Launcher

1. auncher.startActivitySafely(Intent intent, Object tag)
2. Activity.startActivity(Intent intent)
3. Activity.startActivityForResult(Intent intent, int requestCode)
4. Instrumentation.execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode)
5. ApplicationThreadProxy.startActivity(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded, boolean debug)

ActivityManagerService


### 1 Launcher.startActivitySafely(Intent intent, Object tag)


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


### 2 Activity.startActivity(Intent intent)

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


### 3 Activity.startActivityForResult(Intent intent, int requestCode)

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


### 4 Instrumentation.execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode)

<span id="jump">Hello World</span>


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

### 5 ApplicationThreadProxy.startActivity(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded, boolean debug)

```java
class ActivityManagerProxy implements IActivityManager{

    private IBinder mRemote;

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
我们先来看看主要的参数

- IApplicationThread caller：指向Launcher组件所运行在的应用进程的ApplicationThread对象。
- Intent intent：将要启动的Activity组件的信息。
- IBinder resultTo：指向ActivityMangerService内部的一个ActivityRecord对象，它保存了Launcher组件的详细信息。

ActivityManagerProxy.startActivity()将传递过来的参数写入Parcel对象总，并通过ActivityManagerProxy内部的Binder对象mRemote发起一个
类型为START_ACTIVITY_TRANSACTION的进程间通信请求。


### 6 ActivityManagerService.startActivity(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded, boolean debug)

```java

public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {

    public final int startActivity(IApplicationThread caller,
            Intent intent, String resolvedType, Uri[] grantedUriPermissions,
            int grantedMode, IBinder resultTo,
            String resultWho, int requestCode, boolean onlyIfNeeded,
            boolean debug) {
        return mMainStack.startActivityMayWait(caller, intent, resolvedType,
                grantedUriPermissions, grantedMode, resultTo, resultWho,
                requestCode, onlyIfNeeded, debug, null, null);
    }
        
}
```

ActivityManagerService.startActivity()接收ActivityManagerProxy传递过来的START_ACTIVITY_TRANSACTION的进程间通信请求，进一步调用ActivityStack.startActivityMayWait()
去处理。

>ActivityStack：用来描述Activity组件的堆栈，

### 7 ActivityStack.startActivityMayWait(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded,  boolean debug, WaitResult outResult, Configuration config)
```java
public class ActivityStack {

     final int startActivityMayWait(IApplicationThread caller,
                Intent intent, String resolvedType, Uri[] grantedUriPermissions,
                int grantedMode, IBinder resultTo,
                String resultWho, int requestCode, boolean onlyIfNeeded,
                boolean debug, WaitResult outResult, Configuration config) {
            // Refuse possible leaked file descriptors
            if (intent != null && intent.hasFileDescriptors()) {
                throw new IllegalArgumentException("File descriptors passed in Intent");
            }
    
            boolean componentSpecified = intent.getComponent() != null;
            
            // Don't modify the client's object!
            intent = new Intent(intent);
    
            //到PackageManagerService里去解析intent里的信息，并保存到ActivityInfo中。
            // Collect information about the target of the Intent.
            ActivityInfo aInfo;
            try {
                ResolveInfo rInfo =
                    AppGlobals.getPackageManager().resolveIntent(
                            intent, resolvedType,
                            PackageManager.MATCH_DEFAULT_ONLY
                            | ActivityManagerService.STOCK_PM_FLAGS);
                aInfo = rInfo != null ? rInfo.activityInfo : null;
            } catch (RemoteException e) {
                aInfo = null;
            }
    
            if (aInfo != null) {
                // Store the found target back into the intent, because now that
                // we have it we never want to do this again.  For example, if the
                // user navigates back to this point in the history, we should
                // always restart the exact same activity.
                intent.setComponent(new ComponentName(
                        aInfo.applicationInfo.packageName, aInfo.name));
    
                // Don't debug things in the system process
                if (debug) {
                    if (!aInfo.processName.equals("system")) {
                        mService.setDebugApp(aInfo.processName, true, false);
                    }
                }
            }
    
            synchronized (mService) {
                int callingPid;
                int callingUid;
                if (caller == null) {
                    callingPid = Binder.getCallingPid();
                    callingUid = Binder.getCallingUid();
                } else {
                    callingPid = callingUid = -1;
                }
                
                mConfigWillChange = config != null
                        && mService.mConfiguration.diff(config) != 0;
                if (DEBUG_CONFIGURATION) Slog.v(TAG,
                        "Starting activity when config will change = " + mConfigWillChange);
                
                final long origId = Binder.clearCallingIdentity();
                
                if (mMainStack && aInfo != null &&
                        (aInfo.applicationInfo.flags&ApplicationInfo.FLAG_CANT_SAVE_STATE) != 0) {
                    // This may be a heavy-weight process!  Check to see if we already
                    // have another, different heavy-weight process running.
                    if (aInfo.processName.equals(aInfo.applicationInfo.packageName)) {
                        if (mService.mHeavyWeightProcess != null &&
                                (mService.mHeavyWeightProcess.info.uid != aInfo.applicationInfo.uid ||
                                !mService.mHeavyWeightProcess.processName.equals(aInfo.processName))) {
                            int realCallingPid = callingPid;
                            int realCallingUid = callingUid;
                            if (caller != null) {
                                ProcessRecord callerApp = mService.getRecordForAppLocked(caller);
                                if (callerApp != null) {
                                    realCallingPid = callerApp.pid;
                                    realCallingUid = callerApp.info.uid;
                                } else {
                                    Slog.w(TAG, "Unable to find app for caller " + caller
                                          + " (pid=" + realCallingPid + ") when starting: "
                                          + intent.toString());
                                    return START_PERMISSION_DENIED;
                                }
                            }
                            
                            IIntentSender target = mService.getIntentSenderLocked(
                                    IActivityManager.INTENT_SENDER_ACTIVITY, "android",
                                    realCallingUid, null, null, 0, intent,
                                    resolvedType, PendingIntent.FLAG_CANCEL_CURRENT
                                    | PendingIntent.FLAG_ONE_SHOT);
                            
                            Intent newIntent = new Intent();
                            if (requestCode >= 0) {
                                // Caller is requesting a result.
                                newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_HAS_RESULT, true);
                            }
                            newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_INTENT,
                                    new IntentSender(target));
                            if (mService.mHeavyWeightProcess.activities.size() > 0) {
                                ActivityRecord hist = mService.mHeavyWeightProcess.activities.get(0);
                                newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_CUR_APP,
                                        hist.packageName);
                                newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_CUR_TASK,
                                        hist.task.taskId);
                            }
                            newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_NEW_APP,
                                    aInfo.packageName);
                            newIntent.setFlags(intent.getFlags());
                            newIntent.setClassName("android",
                                    HeavyWeightSwitcherActivity.class.getName());
                            intent = newIntent;
                            resolvedType = null;
                            caller = null;
                            callingUid = Binder.getCallingUid();
                            callingPid = Binder.getCallingPid();
                            componentSpecified = true;
                            try {
                                ResolveInfo rInfo =
                                    AppGlobals.getPackageManager().resolveIntent(
                                            intent, null,
                                            PackageManager.MATCH_DEFAULT_ONLY
                                            | ActivityManagerService.STOCK_PM_FLAGS);
                                aInfo = rInfo != null ? rInfo.activityInfo : null;
                            } catch (RemoteException e) {
                                aInfo = null;
                            }
                        }
                    }
                }
                
                //调用startActivityLocked()启动Activity组件
                int res = startActivityLocked(caller, intent, resolvedType,
                        grantedUriPermissions, grantedMode, aInfo,
                        resultTo, resultWho, requestCode, callingPid, callingUid,
                        onlyIfNeeded, componentSpecified);
                
                if (mConfigWillChange && mMainStack) {
                    // If the caller also wants to switch to a new configuration,
                    // do so now.  This allows a clean switch, as we are waiting
                    // for the current activity to pause (so we will not destroy
                    // it), and have not yet started the next activity.
                    mService.enforceCallingPermission(android.Manifest.permission.CHANGE_CONFIGURATION,
                            "updateConfiguration()");
                    mConfigWillChange = false;
                    if (DEBUG_CONFIGURATION) Slog.v(TAG,
                            "Updating to new configuration after starting activity.");
                    mService.updateConfigurationLocked(config, null);
                }
                
                Binder.restoreCallingIdentity(origId);
                
                if (outResult != null) {
                    outResult.result = res;
                    if (res == IActivityManager.START_SUCCESS) {
                        mWaitingActivityLaunched.add(outResult);
                        do {
                            try {
                                mService.wait();
                            } catch (InterruptedException e) {
                            }
                        } while (!outResult.timeout && outResult.who == null);
                    } else if (res == IActivityManager.START_TASK_TO_FRONT) {
                        ActivityRecord r = this.topRunningActivityLocked(null);
                        if (r.nowVisible) {
                            outResult.timeout = false;
                            outResult.who = new ComponentName(r.info.packageName, r.info.name);
                            outResult.totalTime = 0;
                            outResult.thisTime = 0;
                        } else {
                            outResult.thisTime = SystemClock.uptimeMillis();
                            mWaitingActivityVisible.add(outResult);
                            do {
                                try {
                                    mService.wait();
                                } catch (InterruptedException e) {
                                }
                            } while (!outResult.timeout && outResult.who == null);
                        }
                    }
                }
                
                return res;
            }
        }

}
```

这个函数代码比较多，但就做了两件事情：

```
1 到PackageManagerService里去解析intent里的信息，并保存到ActivityInfo中。
2 调用startActivityLocked()启动Activity组件
```

### 8 ActivityStack.startActivityLocked(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, ActivityInfo aInfo, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, boolean onlyIfNeeded, boolean componentSpecified)

```java
public class ActivityStack {

     final int startActivityLocked(IApplicationThread caller,
                Intent intent, String resolvedType,
                Uri[] grantedUriPermissions,
                int grantedMode, ActivityInfo aInfo, IBinder resultTo,
                String resultWho, int requestCode,
                int callingPid, int callingUid, boolean onlyIfNeeded,
                boolean componentSpecified) {
    
            int err = START_SUCCESS;
    
            ProcessRecord callerApp = null;
            if (caller != null) {
                //获取ProcessRecord对象，它实际指向了Launcher组件所运行的应用进程
                callerApp = mService.getRecordForAppLocked(caller);
                if (callerApp != null) {
                    //进程的pid与uid
                    callingPid = callerApp.pid;
                    callingUid = callerApp.info.uid;
                } else {
                    Slog.w(TAG, "Unable to find app for caller " + caller
                          + " (pid=" + callingPid + ") when starting: "
                          + intent.toString());
                    err = START_PERMISSION_DENIED;
                }
            }
    
            if (err == START_SUCCESS) {
                Slog.i(TAG, "Starting: " + intent + " from pid "
                        + (callerApp != null ? callerApp.pid : callingPid));
            }
    
            ActivityRecord sourceRecord = null;
            ActivityRecord resultRecord = null;
            if (resultTo != null) {
                //指向ActivityMangerService内部的一个ActivityRecord对象，它保存了Launcher组件的详细信息。
                int index = indexOfTokenLocked(resultTo);
                if (DEBUG_RESULTS) Slog.v(
                    TAG, "Sending result to " + resultTo + " (index " + index + ")");
                if (index >= 0) {
                    //在Activity组件堆栈中找到用来描述Launcher组件的一个ActivityRecord对象，并保存到sourceRecord变量中。
                    sourceRecord = (ActivityRecord)mHistory.get(index);
                    if (requestCode >= 0 && !sourceRecord.finishing) {
                        resultRecord = sourceRecord;
                    }
                }
            }
    
            int launchFlags = intent.getFlags();
    
            if ((launchFlags&Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0
                    && sourceRecord != null) {
                // Transfer the result target from the source activity to the new
                // one being started, including any failures.
                if (requestCode >= 0) {
                    return START_FORWARD_AND_REQUEST_CONFLICT;
                }
                resultRecord = sourceRecord.resultTo;
                resultWho = sourceRecord.resultWho;
                requestCode = sourceRecord.requestCode;
                sourceRecord.resultTo = null;
                if (resultRecord != null) {
                    resultRecord.removeResultsLocked(
                        sourceRecord, resultWho, requestCode);
                }
            }
    
            if (err == START_SUCCESS && intent.getComponent() == null) {
                // We couldn't find a class that can handle the given Intent.
                // That's the end of that!
                err = START_INTENT_NOT_RESOLVED;
            }
    
            if (err == START_SUCCESS && aInfo == null) {
                // We couldn't find the specific class specified in the Intent.
                // Also the end of the line.
                err = START_CLASS_NOT_FOUND;
            }
    
            if (err != START_SUCCESS) {
                if (resultRecord != null) {
                    sendActivityResultLocked(-1,
                        resultRecord, resultWho, requestCode,
                        Activity.RESULT_CANCELED, null);
                }
                return err;
            }
    
            final int perm = mService.checkComponentPermission(aInfo.permission, callingPid,
                    callingUid, aInfo.exported ? -1 : aInfo.applicationInfo.uid);
            if (perm != PackageManager.PERMISSION_GRANTED) {
                if (resultRecord != null) {
                    sendActivityResultLocked(-1,
                        resultRecord, resultWho, requestCode,
                        Activity.RESULT_CANCELED, null);
                }
                String msg = "Permission Denial: starting " + intent.toString()
                        + " from " + callerApp + " (pid=" + callingPid
                        + ", uid=" + callingUid + ")"
                        + " requires " + aInfo.permission;
                Slog.w(TAG, msg);
                throw new SecurityException(msg);
            }
    
            if (mMainStack) {
                if (mService.mController != null) {
                    boolean abort = false;
                    try {
                        // The Intent we give to the watcher has the extra data
                        // stripped off, since it can contain private information.
                        Intent watchIntent = intent.cloneFilter();
                        abort = !mService.mController.activityStarting(watchIntent,
                                aInfo.applicationInfo.packageName);
                    } catch (RemoteException e) {
                        mService.mController = null;
                    }
        
                    if (abort) {
                        if (resultRecord != null) {
                            sendActivityResultLocked(-1,
                                resultRecord, resultWho, requestCode,
                                Activity.RESULT_CANCELED, null);
                        }
                        // We pretend to the caller that it was really started, but
                        // they will just get a cancel result.
                        return START_SUCCESS;
                    }
                }
            }
            
            //创建一个ActivityRecord对象，用来描述即将启动的Activity组件。
            ActivityRecord r = new ActivityRecord(mService, this, callerApp, callingUid,
                    intent, resolvedType, aInfo, mService.mConfiguration,
                    resultRecord, resultWho, requestCode, componentSpecified);
    
            if (mMainStack) {
                if (mResumedActivity == null
                        || mResumedActivity.info.applicationInfo.uid != callingUid) {
                    if (!mService.checkAppSwitchAllowedLocked(callingPid, callingUid, "Activity start")) {
                        PendingActivityLaunch pal = new PendingActivityLaunch();
                        pal.r = r;
                        pal.sourceRecord = sourceRecord;
                        pal.grantedUriPermissions = grantedUriPermissions;
                        pal.grantedMode = grantedMode;
                        pal.onlyIfNeeded = onlyIfNeeded;
                        mService.mPendingActivityLaunches.add(pal);
                        return START_SWITCHES_CANCELED;
                    }
                }
            
                if (mService.mDidAppSwitch) {
                    // This is the second allowed switch since we stopped switches,
                    // so now just generally allow switches.  Use case: user presses
                    // home (switches disabled, switch to home, mDidAppSwitch now true);
                    // user taps a home icon (coming from home so allowed, we hit here
                    // and now allow anyone to switch again).
                    mService.mAppSwitchesAllowedTime = 0;
                } else {
                    mService.mDidAppSwitch = true;
                }
             
                mService.doPendingActivityLaunchesLocked(false);
            }
            
            return startActivityUncheckedLocked(r, sourceRecord,
                    grantedUriPermissions, grantedMode, onlyIfNeeded, true);
        }
}
```

又是一个长方法，莫慌O(∩_∩)O，方法虽长，理清逻辑就能很清晰的读懂这些方法了。写来看看上述方法里出现的几个成员变量。


```
ProcessRecord callerApp：每个应用进程都用一个ProcessRecord来描述，该对象保存在ActivityManagerService，它实际指向了Launcher组件所运行的应用进程。
ActivityManagerService mService：ActivityManagerService
IApplicationThread caller：指向的是Launcher组件所运行在的应用进程中的一个ApplicationThread对象。

IBinder resultTo：指向ActivityMangerService内部的一个ActivityRecord对象，它保存了Launcher组件的详细信息。
ActivityStack mHistory：用来描述系统的Activity堆栈。堆栈中的Activity对象都由ActivityRecord对象来描述。
ActivityRecord sourceRecord：执行启动Activity组件操作的源Activity信息。
ActivityRecord r：执行启动Activity组件操作的目标Activity信息。
```
            
了解了参数的意义，我们来总结一下该函数做了哪些事情。

```
1 获取Launcher组件所运行的应用进程信息，它保存在了ProcessRecord callerApp中。
2 在Activity组件堆栈中找到用来描述Launcher组件的一个ActivityRecord对象，并保存到sourceRecord变量中。sourceRecord描述了执行Activity启动操作的源Activity信息。
3 创建ActivityRecord r，它描述了执行启动Activity组件操作的目标Activity信息。
4 以上述创建的变量为参数，调用startActivityUncheckedLocked()，进一步执行启动操作。
```

### 9 ActivityStack.startActivityUncheckedLocked(ActivityRecord r, ActivityRecord sourceRecord, Uri[] grantedUriPermissions, int grantedMode, boolean onlyIfNeeded, boolean doResume) 

```java
public class ActivityStack {

     final int startActivityUncheckedLocked(ActivityRecord r,
                ActivityRecord sourceRecord, Uri[] grantedUriPermissions,
                int grantedMode, boolean onlyIfNeeded, boolean doResume) {
            final Intent intent = r.intent;
            final int callingUid = r.launchedFromUid;
            
            int launchFlags = intent.getFlags();
            
            // We'll invoke onUserLeaving before onPause only if the launching
            // activity did not explicitly state that this is an automated launch.
            mUserLeaving = (launchFlags&Intent.FLAG_ACTIVITY_NO_USER_ACTION) == 0;
            if (DEBUG_USER_LEAVING) Slog.v(TAG,
                    "startActivity() => mUserLeaving=" + mUserLeaving);
            
            // If the caller has asked not to resume at this point, we make note
            // of this in the record so that we can skip it when trying to find
            // the top running activity.
            if (!doResume) {
                r.delayedResume = true;
            }
            
            ActivityRecord notTop = (launchFlags&Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
                    != 0 ? r : null;
    
            // If the onlyIfNeeded flag is set, then we can do this if the activity
            // being launched is the same as the one making the call...  or, as
            // a special case, if we do not know the caller then we count the
            // current top activity as the caller.
            if (onlyIfNeeded) {
                ActivityRecord checkedCaller = sourceRecord;
                if (checkedCaller == null) {
                    checkedCaller = topRunningNonDelayedActivityLocked(notTop);
                }
                if (!checkedCaller.realActivity.equals(r.realActivity)) {
                    // Caller is not the same as launcher, so always needed.
                    onlyIfNeeded = false;
                }
            }
    
            if (sourceRecord == null) {
                // This activity is not being started from another...  in this
                // case we -always- start a new task.
                if ((launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
                    Slog.w(TAG, "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: "
                          + intent);
                    launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
                }
            } else if (sourceRecord.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                // The original activity who is starting us is running as a single
                // instance...  this new activity it is starting must go on its
                // own task.
                launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
            } else if (r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE
                    || r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
                // The activity being started is a single instance...  it always
                // gets launched into its own task.
                launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
            }
    
            if (r.resultTo != null && (launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
                // For whatever reason this activity is being launched into a new
                // task...  yet the caller has requested a result back.  Well, that
                // is pretty messed up, so instead immediately send back a cancel
                // and let the new task continue launched as normal without a
                // dependency on its originator.
                Slog.w(TAG, "Activity is launching as a new task, so cancelling activity result.");
                sendActivityResultLocked(-1,
                        r.resultTo, r.resultWho, r.requestCode,
                    Activity.RESULT_CANCELED, null);
                r.resultTo = null;
            }
    
            boolean addingToTask = false;
            if (((launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) != 0 &&
                    (launchFlags&Intent.FLAG_ACTIVITY_MULTIPLE_TASK) == 0)
                    || r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK
                    || r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                // If bring to front is requested, and no result is requested, and
                // we can find a task that was started with this same
                // component, then instead of launching bring that one to the front.
                if (r.resultTo == null) {
                    // See if there is a task to bring to the front.  If this is
                    // a SINGLE_INSTANCE activity, there can be one and only one
                    // instance of it in the history, and it is always in its own
                    // unique task, so we do a special search.
                    ActivityRecord taskTop = r.launchMode != ActivityInfo.LAUNCH_SINGLE_INSTANCE
                            ? findTaskLocked(intent, r.info)
                            : findActivityLocked(intent, r.info);
                    if (taskTop != null) {
                        if (taskTop.task.intent == null) {
                            // This task was started because of movement of
                            // the activity based on affinity...  now that we
                            // are actually launching it, we can assign the
                            // base intent.
                            taskTop.task.setIntent(intent, r.info);
                        }
                        // If the target task is not in the front, then we need
                        // to bring it to the front...  except...  well, with
                        // SINGLE_TASK_LAUNCH it's not entirely clear.  We'd like
                        // to have the same behavior as if a new instance was
                        // being started, which means not bringing it to the front
                        // if the caller is not itself in the front.
                        ActivityRecord curTop = topRunningNonDelayedActivityLocked(notTop);
                        if (curTop.task != taskTop.task) {
                            r.intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                            boolean callerAtFront = sourceRecord == null
                                    || curTop.task == sourceRecord.task;
                            if (callerAtFront) {
                                // We really do want to push this one into the
                                // user's face, right now.
                                moveTaskToFrontLocked(taskTop.task, r);
                            }
                        }
                        // If the caller has requested that the target task be
                        // reset, then do so.
                        if ((launchFlags&Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) != 0) {
                            taskTop = resetTaskIfNeededLocked(taskTop, r);
                        }
                        if (onlyIfNeeded) {
                            // We don't need to start a new activity, and
                            // the client said not to do anything if that
                            // is the case, so this is it!  And for paranoia, make
                            // sure we have correctly resumed the top activity.
                            if (doResume) {
                                resumeTopActivityLocked(null);
                            }
                            return START_RETURN_INTENT_TO_CALLER;
                        }
                        if ((launchFlags&Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0
                                || r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK
                                || r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                            // In this situation we want to remove all activities
                            // from the task up to the one being started.  In most
                            // cases this means we are resetting the task to its
                            // initial state.
                            ActivityRecord top = performClearTaskLocked(
                                    taskTop.task.taskId, r, launchFlags, true);
                            if (top != null) {
                                if (top.frontOfTask) {
                                    // Activity aliases may mean we use different
                                    // intents for the top activity, so make sure
                                    // the task now has the identity of the new
                                    // intent.
                                    top.task.setIntent(r.intent, r.info);
                                }
                                logStartActivity(EventLogTags.AM_NEW_INTENT, r, top.task);
                                top.deliverNewIntentLocked(callingUid, r.intent);
                            } else {
                                // A special case: we need to
                                // start the activity because it is not currently
                                // running, and the caller has asked to clear the
                                // current task to have this activity at the top.
                                addingToTask = true;
                                // Now pretend like this activity is being started
                                // by the top of its task, so it is put in the
                                // right place.
                                sourceRecord = taskTop;
                            }
                        } else if (r.realActivity.equals(taskTop.task.realActivity)) {
                            // In this case the top activity on the task is the
                            // same as the one being launched, so we take that
                            // as a request to bring the task to the foreground.
                            // If the top activity in the task is the root
                            // activity, deliver this new intent to it if it
                            // desires.
                            if ((launchFlags&Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
                                    && taskTop.realActivity.equals(r.realActivity)) {
                                logStartActivity(EventLogTags.AM_NEW_INTENT, r, taskTop.task);
                                if (taskTop.frontOfTask) {
                                    taskTop.task.setIntent(r.intent, r.info);
                                }
                                taskTop.deliverNewIntentLocked(callingUid, r.intent);
                            } else if (!r.intent.filterEquals(taskTop.task.intent)) {
                                // In this case we are launching the root activity
                                // of the task, but with a different intent.  We
                                // should start a new instance on top.
                                addingToTask = true;
                                sourceRecord = taskTop;
                            }
                        } else if ((launchFlags&Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) == 0) {
                            // In this case an activity is being launched in to an
                            // existing task, without resetting that task.  This
                            // is typically the situation of launching an activity
                            // from a notification or shortcut.  We want to place
                            // the new activity on top of the current task.
                            addingToTask = true;
                            sourceRecord = taskTop;
                        } else if (!taskTop.task.rootWasReset) {
                            // In this case we are launching in to an existing task
                            // that has not yet been started from its front door.
                            // The current task has been brought to the front.
                            // Ideally, we'd probably like to place this new task
                            // at the bottom of its stack, but that's a little hard
                            // to do with the current organization of the code so
                            // for now we'll just drop it.
                            taskTop.task.setIntent(r.intent, r.info);
                        }
                        if (!addingToTask) {
                            // We didn't do anything...  but it was needed (a.k.a., client
                            // don't use that intent!)  And for paranoia, make
                            // sure we have correctly resumed the top activity.
                            if (doResume) {
                                resumeTopActivityLocked(null);
                            }
                            return START_TASK_TO_FRONT;
                        }
                    }
                }
            }
    
            //String uri = r.intent.toURI();
            //Intent intent2 = new Intent(uri);
            //Slog.i(TAG, "Given intent: " + r.intent);
            //Slog.i(TAG, "URI is: " + uri);
            //Slog.i(TAG, "To intent: " + intent2);
    
            if (r.packageName != null) {
                // If the activity being launched is the same as the one currently
                // at the top, then we need to check if it should only be launched
                // once.
                ActivityRecord top = topRunningNonDelayedActivityLocked(notTop);
                if (top != null && r.resultTo == null) {
                    if (top.realActivity.equals(r.realActivity)) {
                        if (top.app != null && top.app.thread != null) {
                            if ((launchFlags&Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
                                || r.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP
                                || r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
                                logStartActivity(EventLogTags.AM_NEW_INTENT, top, top.task);
                                // For paranoia, make sure we have correctly
                                // resumed the top activity.
                                if (doResume) {
                                    resumeTopActivityLocked(null);
                                }
                                if (onlyIfNeeded) {
                                    // We don't need to start a new activity, and
                                    // the client said not to do anything if that
                                    // is the case, so this is it!
                                    return START_RETURN_INTENT_TO_CALLER;
                                }
                                top.deliverNewIntentLocked(callingUid, r.intent);
                                return START_DELIVERED_TO_TOP;
                            }
                        }
                    }
                }
    
            } else {
                if (r.resultTo != null) {
                    sendActivityResultLocked(-1,
                            r.resultTo, r.resultWho, r.requestCode,
                        Activity.RESULT_CANCELED, null);
                }
                return START_CLASS_NOT_FOUND;
            }
    
            boolean newTask = false;
    
            // Should this be considered a new task?
            if (r.resultTo == null && !addingToTask
                    && (launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
                // todo: should do better management of integers.
                mService.mCurTask++;
                if (mService.mCurTask <= 0) {
                    mService.mCurTask = 1;
                }
                r.task = new TaskRecord(mService.mCurTask, r.info, intent,
                        (r.info.flags&ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
                if (DEBUG_TASKS) Slog.v(TAG, "Starting new activity " + r
                        + " in new task " + r.task);
                newTask = true;
                if (mMainStack) {
                    mService.addRecentTaskLocked(r.task);
                }
                
            } else if (sourceRecord != null) {
                if (!addingToTask &&
                        (launchFlags&Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) {
                    // In this case, we are adding the activity to an existing
                    // task, but the caller has asked to clear that task if the
                    // activity is already running.
                    ActivityRecord top = performClearTaskLocked(
                            sourceRecord.task.taskId, r, launchFlags, true);
                    if (top != null) {
                        logStartActivity(EventLogTags.AM_NEW_INTENT, r, top.task);
                        top.deliverNewIntentLocked(callingUid, r.intent);
                        // For paranoia, make sure we have correctly
                        // resumed the top activity.
                        if (doResume) {
                            resumeTopActivityLocked(null);
                        }
                        return START_DELIVERED_TO_TOP;
                    }
                } else if (!addingToTask &&
                        (launchFlags&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) != 0) {
                    // In this case, we are launching an activity in our own task
                    // that may already be running somewhere in the history, and
                    // we want to shuffle it to the front of the stack if so.
                    int where = findActivityInHistoryLocked(r, sourceRecord.task.taskId);
                    if (where >= 0) {
                        ActivityRecord top = moveActivityToFrontLocked(where);
                        logStartActivity(EventLogTags.AM_NEW_INTENT, r, top.task);
                        top.deliverNewIntentLocked(callingUid, r.intent);
                        if (doResume) {
                            resumeTopActivityLocked(null);
                        }
                        return START_DELIVERED_TO_TOP;
                    }
                }
                // An existing activity is starting this new activity, so we want
                // to keep the new one in the same task as the one that is starting
                // it.
                r.task = sourceRecord.task;
                if (DEBUG_TASKS) Slog.v(TAG, "Starting new activity " + r
                        + " in existing task " + r.task);
    
            } else {
                // This not being started from an existing activity, and not part
                // of a new task...  just put it in the top task, though these days
                // this case should never happen.
                final int N = mHistory.size();
                ActivityRecord prev =
                    N > 0 ? (ActivityRecord)mHistory.get(N-1) : null;
                r.task = prev != null
                    ? prev.task
                    : new TaskRecord(mService.mCurTask, r.info, intent,
                            (r.info.flags&ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
                if (DEBUG_TASKS) Slog.v(TAG, "Starting new activity " + r
                        + " in new guessed " + r.task);
            }
    
            if (grantedUriPermissions != null && callingUid > 0) {
                for (int i=0; i<grantedUriPermissions.length; i++) {
                    mService.grantUriPermissionLocked(callingUid, r.packageName,
                            grantedUriPermissions[i], grantedMode, r.getUriPermissionsLocked());
                }
            }
    
            mService.grantUriPermissionFromIntentLocked(callingUid, r.packageName,
                    intent, r.getUriPermissionsLocked());
    
            if (newTask) {
                EventLog.writeEvent(EventLogTags.AM_CREATE_TASK, r.task.taskId);
            }
            logStartActivity(EventLogTags.AM_CREATE_ACTIVITY, r, r.task);
            startActivityLocked(r, newTask, doResume);
            return START_SUCCESS;
        }
 
}
```
         