# Android系统应用框架篇：Service启动流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

本篇文章分析Service组件在新进程内的启动流程。

启动Service组件的流程如下所示：

```
1 向ActivityManagerService发送一个启动Service组件的请求。
2 ActivityManagerService发现用来运行Service组件的进程不存在，它会先保存Service组件的信息，接着再创建一个新的应用进程。
3 新的应用进程创建完成后，就会向ActivityManagerService发送一个启动完成的进程间通信请求，以便ActivityManagerService可
以继续执行启动Service组件的的操作。
4 ActivityManagerService将第2步保存的Service组件信息发送给新床架的应用进程，以便它可以将Service组件启动起来。
```
#### 1 Activity.startService(Intent service)

当我们在Activity里调用startService(Intent service)方法时，它实际上在调用ContextWrapper.startService(Intent service)。


#### 2 ContextWrapper.startService(Intent service)

```java
public class ContextWrapper{
    @Override
    public ComponentName startService(Intent service) {
        return mBase.startService(service);
    }
}
````

mBase的对象类型是Context，它实际上指向了Context的实现类ContextImpl，所以该方法进一步调用ContextImpl.startService.


#### 3 ContextImpl.startService(Intent service)

```java
public class ContextImpl{
    @Override
    public ComponentName startService(Intent service) {
        try {
            ComponentName cn = ActivityManagerNative.getDefault().startService(
                mMainThread.getApplicationThread(), service,
                service.resolveTypeIfNeeded(getContentResolver()));
            if (cn != null && cn.getPackageName().equals("!")) {
                throw new SecurityException(
                        "Not allowed to start service " + service
                        + " without permission " + cn.getClassName());
            }
            return cn;
        } catch (RemoteException e) {
            return null;
        }
    }
}
```
ActivityManagerNative.getDefault()获取的是ActivityManagerService的一个代理对象，即ActivityManagerProxy。我们再看看看传递的参数：

```
mMainThread.getApplicationThread(：获取当前应用进程的一个类型为ApplicationThread的Binder本地对象。将它传递给ActivityManagerService，以便
ActivityManagerService知道是谁在请求它启动Service组件。
service：intent对象。
service.resolveTypeIfNeeded(getContentResolver())：返回Intent的MIME类型。
```
该方法进一步调用即ActivityManagerProxy.startService(IApplicationThread caller, Intent service, String resolvedType) 

#### 4 ActivityManagerProxy.startService(IApplicationThread caller, Intent service, String resolvedType) 

```java
public abstract class ActivityManagerNative extends Binder implements IActivityManager{

    class ActivityManagerProxy implements IActivityManager{
        public ComponentName startService(IApplicationThread caller, Intent service,
                String resolvedType) throws RemoteException{
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(IActivityManager.descriptor);
            data.writeStrongBinder(caller != null ? caller.asBinder() : null);
            service.writeToParcel(data, 0);
            data.writeString(resolvedType);
            mRemote.transact(START_SERVICE_TRANSACTION, data, reply, 0);
            reply.readException();
            ComponentName res = ComponentName.readFromParcel(reply);
            data.recycle();
            reply.recycle();
            return res;
        }
    }
}

```

将传递金磊的参数封装成Parcel对象，然后利用ActivityManagerProxy内部的Binder对象mRemote向ActivityManagerService发送一个START_SERVICE_TRANSACTION进程间通信请求。
该函数进一步调用ActivityManagerService.startService(IApplicationThread caller, Intent service, String resolvedType)来处理这个请求。

#### 5 ActivityManagerService.startService(IApplicationThread caller, Intent service, String resolvedType)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    public ComponentName startService(IApplicationThread caller, Intent service,
            String resolvedType) {
        // Refuse possible leaked file descriptors
        if (service != null && service.hasFileDescriptors() == true) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }

        synchronized(this) {
            final int callingPid = Binder.getCallingPid();
            final int callingUid = Binder.getCallingUid();
            final long origId = Binder.clearCallingIdentity();
            ComponentName res = startServiceLocked(caller, service,
                    resolvedType, callingPid, callingUid);
            Binder.restoreCallingIdentity(origId);
            return res;
        }
    }
}
```

这个函数的参数的含义上面第2步已经介绍过，它进一步调用ActivityManagerService.startServiceLocked()方法执行启动Service组件的操作。

#### 6 ActivityManagerService.startServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int callingPid, int callingUid)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    ComponentName startServiceLocked(IApplicationThread caller,
            Intent service, String resolvedType,
            int callingPid, int callingUid) {
        synchronized(this) {
            if (DEBUG_SERVICE) Slog.v(TAG, "startService: " + service
                    + " type=" + resolvedType + " args=" + service.getExtras());

            if (caller != null) {
                final ProcessRecord callerApp = getRecordForAppLocked(caller);
                if (callerApp == null) {
                    throw new SecurityException(
                            "Unable to find app for caller " + caller
                            + " (pid=" + Binder.getCallingPid()
                            + ") when starting service " + service);
                }
            }

            //查找是否有与参数service对应的一个ServiceRecord对象，如果没有则ActivityManagerService就会到PackageManagerService中
            //去获取与参数service对应的一个Service组件信息，并把它封装成一个ServiceRecord对象。
            ServiceLookupResult res =
                retrieveServiceLocked(service, resolvedType,
                        callingPid, callingUid);
            if (res == null) {
                return null;
            }
            if (res.record == null) {
                return new ComponentName("!", res.permission != null
                        ? res.permission : "private to package");
            }
            //每个Service组件都用一个ServiceRecord对象类描述，就行每个Activity组件都用一个ActivityRecord来描述一样。
            ServiceRecord r = res.record;
            int targetPermissionUid = checkGrantUriPermissionFromIntentLocked(
                    callingUid, r.packageName, service);
            if (unscheduleServiceRestartLocked(r)) {
                if (DEBUG_SERVICE) Slog.v(TAG, "START SERVICE WHILE RESTART PENDING: " + r);
            }
            r.startRequested = true;
            r.callStart = false;
            r.lastStartId++;
            if (r.lastStartId < 1) {
                r.lastStartId = 1;
            }
            r.pendingStarts.add(new ServiceRecord.StartItem(r, r.lastStartId,
                    service, targetPermissionUid));
            r.lastActivity = SystemClock.uptimeMillis();
            synchronized (r.stats.getBatteryStats()) {
                r.stats.startRunningLocked();
            }
            //根据上面封装的ServiceRecord对象，调用bringUpServiceLocked()方法进一步启动Service组件的启动操作。
            if (!bringUpServiceLocked(r, service.getFlags(), false)) {
                return new ComponentName("!", "Service process is bad");
            }
            return r.name;
        }
    }        
}
```

>ServiceRecord：用来描述Service组件信息，每个Service组件都用一个ServiceRecord对象类描述，就行每个Activity组件都用一个ActivityRecord来描述一样。

这个函数主要做了两件事情：

```
1 查找是否有与参数service对应的一个ServiceRecord对象，如果没有则ActivityManagerService就会到PackageManagerService中
去获取与参数service对应的一个Service组件信息，并把它封装成一个ServiceRecord对象。
2 根据上面封装的ServiceRecord对象，调用bringUpServiceLocked()方法进一步启动Service组件的启动操作。
```
我们来看ActivityManagerService.bringUpServiceLocked()方法的实现。

#### 7 ActivityManagerService.bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean whileRestarting)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
  private final boolean bringUpServiceLocked(ServiceRecord r,
            int intentFlags, boolean whileRestarting) {
        //Slog.i(TAG, "Bring up service:");
        //r.dump("  ");

        if (r.app != null && r.app.thread != null) {
            sendServiceArgsLocked(r, false);
            return true;
        }

        if (!whileRestarting && r.restartDelay > 0) {
            // If waiting for a restart, then do nothing.
            return true;
        }

        if (DEBUG_SERVICE) Slog.v(TAG, "Bringing up " + r + " " + r.intent);

        // We are now bringing the service up, so no longer in the
        // restarting state.
        mRestartingServices.remove(r);
        
        //获取ServiceRecord里的processName属性
        final String appName = r.processName;
        //然后根据processName属性与Service组件的用户ID去查找ActivityManagerService是否已经存在
        //一个ProcessRecord对象。
        ProcessRecord app = getProcessRecordLocked(appName, r.appInfo.uid);
        if (app != null && app.thread != null) {
            try {
                //如果存在该ProcessRecord对象则在app所描述的应用进程中启动该Service组件
                realStartServiceLocked(r, app);
                return true;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting service " + r.shortName, e);
            }

            // If a dead object exception was thrown -- fall through to
            // restart the application.
        }

        //如果没有查找到该进程，则会去启动一个新的应用进程。
        // Not running -- get it started, and enqueue this service record
        // to be executed when the app comes up.
        if (startProcessLocked(appName, r.appInfo, true, intentFlags,
                "service", r.name, false) == null) {
            Slog.w(TAG, "Unable to launch app "
                    + r.appInfo.packageName + "/"
                    + r.appInfo.uid + " for service "
                    + r.intent.getIntent() + ": process is bad");
            bringDownServiceLocked(r, true);
            return false;
        }
        
        if (!mPendingServices.contains(r)) {
            //将该Service组件对象保存在ActivityManagerService的成员变量mPendingService中，表示它是一个
            //正在等待启动的Service组件。
            mPendingServices.add(r);
        }
        
        return true;
    }
}
```

该函数主要做了两件事情：

```
1 获取ServiceRecord里的processName属性，然后根据processName属性与Service组件的用户ID去查找ActivityManagerService
是否已经存在一个ProcessRecord对象。

如果存在：则在app所描述的应用进程中启动该Service组件
如果不存在：则会去启动一个新的应用进程。

2 将该Service组件对象保存在ActivityManagerService的成员变量mPendingService中，表示它是一个正在等待启动的Service组件。
```

我们分析的是在新进程创建Service组件的情况，因此我们接着来看ActivityManagerService.startProcessLocked()的实现。

#### 8 ActivityManagerService.tartProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {

    final ProcessRecord startProcessLocked(String processName,
            ApplicationInfo info, boolean knownToBeDead, int intentFlags,
            String hostingType, ComponentName hostingName, boolean allowWhileBooting) {
        ProcessRecord app = getProcessRecordLocked(processName, info.uid); 
        ...  
        startProcessLocked(app, hostingType, hostingNameStr);
        return (app.pid != 0) ? app : null;
    }

    private final void startProcessLocked(ProcessRecord app,
            String hostingType, String hostingNameStr) {
            ...
            int pid = Process.start("android.app.ActivityThread",
                    mSimpleProcessManagement ? app.processName : null, uid, uid,
                    gids, debugFlags, null);
            ...
    }        
}
```

从这个函数开始就开始创建新的应用进程了，它主要调用Process的静态函数start()来创建一个新的应用进程，这个新进程以ActivityThread
类的静态成员函数main()为入口。

#### 9 ActivityThread.main(String[] args) 

```java
public final class ActivityThread {
    public static final void main(String[] args) {
            SamplingProfilerIntegration.start();
    
            Process.setArgV0("<pre-initialized>");
    
            Looper.prepareMainLooper();
            if (sMainThreadHandler == null) {
                sMainThreadHandler = new Handler();
            }
    
            ActivityThread thread = new ActivityThread();
            //该方法会去调用ActivityManagerProxy.attachApplication()方法
            thread.attach(false);
    
            if (false) {
                Looper.myLooper().setMessageLogging(new
                        LogPrinter(Log.DEBUG, "ActivityThread"));
            }
    
            Looper.loop();
    
            if (Process.supportsProcesses()) {
                throw new RuntimeException("Main thread loop unexpectedly exited");
            }
    
            thread.detach();
            String name = (thread.mInitialApplication != null)
                ? thread.mInitialApplication.getPackageName()
                : "<unknown>";
            Slog.i(TAG, "Main thread of " + name + " is now exiting");
        }
}
```
main函数是新创建进程的入口，该函数会创建一个ActivityThread与ApplicationThread对象，并调用ActivityManagerProxy.attachApplication()方
法进一步执行Service组件启动操作。

#### 10 ActivityManagerProxy.attachApplication(IApplicationThread app)

```java
class ActivityManagerProxy implements IActivityManager{
    public void attachApplication(IApplicationThread app) throws RemoteException{
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IActivityManager.descriptor);
        data.writeStrongBinder(app.asBinder());
        mRemote.transact(ATTACH_APPLICATION_TRANSACTION, data, reply, 0);
        reply.readException();
        data.recycle();
        reply.recycle();
    }
}
```
该函数中ActivityManagerProxy接受上一步main方法创建的ApplicationThread对象，并向ActivityManagerService发送一个类型为
ATTACH_APPLICATION_TRANSACTION进程间通信请求，并将ApplicationThread对象传递给ActivityManagerService，以便
ActivityManagerService可以和这个新创建的进程进行通信。

#### 11 ActivityManagerService.attachApplication(IApplicationThread thread)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
    public final void attachApplication(IApplicationThread thread) {
        synchronized (this) {
            int callingPid = Binder.getCallingPid();
            final long origId = Binder.clearCallingIdentity();
            attachApplicationLocked(thread, callingPid);
            Binder.restoreCallingIdentity(origId);
        }
    }        
}
```
该方法进一步调用ActivityManagerService.attachApplicationLocked()来处理上一步发出的ATTACH_APPLICATION_TRANSACTION
进程间通信请求。这个方法我们应该很熟悉，我们以前在分析新进程启动Activity组件时就走到了这个函数，我们再来看一看它的实现。

#### 12 ActivityManagerService.attachApplicationLocked(IApplicationThread thread, int pid)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
 private final boolean attachApplicationLocked(IApplicationThread thread,
            int pid) {

        // Find the application record that is being attached...  either via
        // the pid if we are running in multiple processes, or just pull the
        // next app record if we are emulating process with anonymous threads.
        ProcessRecord app;
        //pid指向的前面新创建的应用进程的PID，在第8步中，ActivityManagerService以这个PID
        //为key将一个ProcessRecord存在了mPidsSelfLocked，现在把它取出来保存在变量app中。
        if (pid != MY_PID && pid >= 0) {
            synchronized (mPidsSelfLocked) {
                app = mPidsSelfLocked.get(pid);
            }
        } else if (mStartingProcesses.size() > 0) {
            app = mStartingProcesses.remove(0);
            app.setPid(pid);
        } else {
            app = null;
        }

        if (app == null) {
            Slog.w(TAG, "No pending application record for pid " + pid
                    + " (IApplicationThread " + thread + "); dropping process");
            EventLog.writeEvent(EventLogTags.AM_DROP_PROCESS, pid);
            if (pid > 0 && pid != MY_PID) {
                Process.killProcess(pid);
            } else {
                try {
                    thread.scheduleExit();
                } catch (Exception e) {
                    // Ignore exceptions.
                }
            }
            return false;
        }

        // If this application record is still attached to a previous
        // process, clean it up now.
        if (app.thread != null) {
            handleAppDiedLocked(app, true);
        }

        // Tell the process all about itself.

        if (localLOGV) Slog.v(
                TAG, "Binding process pid " + pid + " to record " + app);

        String processName = app.processName;
        try {
            thread.asBinder().linkToDeath(new AppDeathRecipient(
                    app, pid, thread), 0);
        } catch (RemoteException e) {
            app.resetPackageList();
            startProcessLocked(app, "link fail", processName);
            return false;
        }

        EventLog.writeEvent(EventLogTags.AM_PROC_BOUND, app.pid, app.processName);
        
        //指向上一步传递过来的ApplicationThread对象，ActivityManagerService以后就可以通过
        //这个ApplicationThread对象同新创建的应用进程通信了。
        app.thread = thread;
        app.curAdj = app.setAdj = -100;
        app.curSchedGroup = Process.THREAD_GROUP_DEFAULT;
        app.setSchedGroup = Process.THREAD_GROUP_BG_NONINTERACTIVE;
        app.forcingToForeground = null;
        app.foregroundServices = false;
        app.debugging = false;

        mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);

        boolean normalMode = mProcessesReady || isAllowedWhileBooting(app.info);
        List providers = normalMode ? generateApplicationProvidersLocked(app) : null;

        if (!normalMode) {
            Slog.i(TAG, "Launching preboot mode app: " + app);
        }
        
        if (localLOGV) Slog.v(
            TAG, "New app record " + app
            + " thread=" + thread.asBinder() + " pid=" + pid);
        try {
            int testMode = IApplicationThread.DEBUG_OFF;
            if (mDebugApp != null && mDebugApp.equals(processName)) {
                testMode = mWaitForDebugger
                    ? IApplicationThread.DEBUG_WAIT
                    : IApplicationThread.DEBUG_ON;
                app.debugging = true;
                if (mDebugTransient) {
                    mDebugApp = mOrigDebugApp;
                    mWaitForDebugger = mOrigWaitForDebugger;
                }
            }
            
            // If the app is being launched for restore or full backup, set it up specially
            boolean isRestrictedBackupMode = false;
            if (mBackupTarget != null && mBackupAppName.equals(processName)) {
                isRestrictedBackupMode = (mBackupTarget.backupMode == BackupRecord.RESTORE)
                        || (mBackupTarget.backupMode == BackupRecord.BACKUP_FULL);
            }
            
            ensurePackageDexOpt(app.instrumentationInfo != null
                    ? app.instrumentationInfo.packageName
                    : app.info.packageName);
            if (app.instrumentationClass != null) {
                ensurePackageDexOpt(app.instrumentationClass.getPackageName());
            }
            if (DEBUG_CONFIGURATION) Slog.v(TAG, "Binding proc "
                    + processName + " with config " + mConfiguration);
            thread.bindApplication(processName, app.instrumentationInfo != null
                    ? app.instrumentationInfo : app.info, providers,
                    app.instrumentationClass, app.instrumentationProfileFile,
                    app.instrumentationArguments, app.instrumentationWatcher, testMode, 
                    isRestrictedBackupMode || !normalMode,
                    mConfiguration, getCommonServicesLocked());
            updateLruProcessLocked(app, false, true);
            app.lastRequestedGc = app.lastLowMemory = SystemClock.uptimeMillis();
        } catch (Exception e) {
            // todo: Yikes!  What should we do?  For now we will try to
            // start another process, but that could easily get us in
            // an infinite loop of restarting processes...
            Slog.w(TAG, "Exception thrown during bind!", e);

            app.resetPackageList();
            startProcessLocked(app, "bind fail", processName);
            return false;
        }

        // Remove this record from the list of starting applications.
        mPersistentStartingProcesses.remove(app);
        if (DEBUG_PROCESSES && mProcessesOnHold.contains(app)) Slog.v(TAG,
                "Attach application locked removing on hold: " + app);
        mProcessesOnHold.remove(app);

        boolean badApp = false;
        boolean didSomething = false;

        //ActivityManagerService优先处理在它里面启动或显示的Activity组件
        // See if the top visible activity is waiting to run in this process...
        ActivityRecord hr = mMainStack.topRunningActivityLocked(null);
        if (hr != null && normalMode) {
            if (hr.app == null && app.info.uid == hr.info.applicationInfo.uid
                    && processName.equals(hr.processName)) {
                try {
                    if (mMainStack.realStartActivityLocked(hr, app, true, true)) {
                        didSomething = true;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Exception in new application when starting activity "
                          + hr.intent.getComponent().flattenToShortString(), e);
                    badApp = true;
                }
            } else {
                mMainStack.ensureActivitiesVisibleLocked(hr, null, processName, 0);
            }
        }

        //然后再处理Service组件
        // Find any services that should be running in this process...
        if (!badApp && mPendingServices.size() > 0) {
            ServiceRecord sr = null;
            try {
                for (int i=0; i<mPendingServices.size(); i++) {
                    sr = mPendingServices.get(i);
                    //检查保存在mPendingServices里的Service组件是否需要在新进程中启动
                    if (app.info.uid != sr.appInfo.uid
                            || !processName.equals(sr.processName)) {
                        continue;
                    }
                    //如果需要在新进程中启动，则将其在mPendingServices移除，并调用
                    //realStartServiceLocked启动该Service组件。
                    mPendingServices.remove(i);
                    i--;
                    realStartServiceLocked(sr, app);
                    didSomething = true;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Exception in new application when starting service "
                      + sr.shortName, e);
                badApp = true;
            }
        }

        // Check if the next broadcast receiver is in this process...
        BroadcastRecord br = mPendingBroadcast;
        if (!badApp && br != null && br.curApp == app) {
            try {
                mPendingBroadcast = null;
                processCurBroadcastLocked(br, app);
                didSomething = true;
            } catch (Exception e) {
                Slog.w(TAG, "Exception in new application when starting receiver "
                      + br.curComponent.flattenToShortString(), e);
                badApp = true;
                logBroadcastReceiverDiscardLocked(br);
                finishReceiverLocked(br.receiver, br.resultCode, br.resultData,
                        br.resultExtras, br.resultAbort, true);
                scheduleBroadcastsLocked();
                // We need to reset the state if we fails to start the receiver.
                br.state = BroadcastRecord.IDLE;
            }
        }

        // Check whether the next backup agent is in this process...
        if (!badApp && mBackupTarget != null && mBackupTarget.appInfo.uid == app.info.uid) {
            if (DEBUG_BACKUP) Slog.v(TAG, "New app is backup target, launching agent for " + app);
            ensurePackageDexOpt(mBackupTarget.appInfo.packageName);
            try {
                thread.scheduleCreateBackupAgent(mBackupTarget.appInfo, mBackupTarget.backupMode);
            } catch (Exception e) {
                Slog.w(TAG, "Exception scheduling backup agent creation: ");
                e.printStackTrace();
            }
        }

        if (badApp) {
            // todo: Also need to kill application to deal with all
            // kinds of exceptions.
            handleAppDiedLocked(app, false);
            return false;
        }

        if (!didSomething) {
            updateOomAdjLocked();
        }

        return true;
    }       
}
```

#### 13 ActivityManagerService.realStartServiceLocked(ServiceRecord r, ProcessRecord app)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
     private final void realStartServiceLocked(ServiceRecord r,
                ProcessRecord app) throws RemoteException {
            ...
            
            r.app = app;
            ...
            try {
                ...
                app.thread.scheduleCreateService(r, r.serviceInfo);
                ...
            } finally {
                if (!created) {
                    app.services.remove(r);
                    scheduleServiceRestartLocked(r, false);
                }
            }
            ...     
}
```

#### 14 ActivityManagerService.attachApplicationLocked(IApplicationThread thread, int pid)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
}
```
#### 

```java

```


#### 

```java

```


#### 

```java

```


#### 

```java

```

#### 

```java

```

#### 

```java

```

#### 

```java

```

#### 

```java

```

#### 

```java

```

#### 

```java

```

