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

### 7 ActivityManagerService.bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean whileRestarting)

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

### 8 ActivityManagerService.

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {

    final ProcessRecord startProcessLocked(String processName,
            ApplicationInfo info, boolean knownToBeDead, int intentFlags,
            String hostingType, ComponentName hostingName, boolean allowWhileBooting) {
        ProcessRecord app = getProcessRecordLocked(processName, info.uid);
        // We don't have to do anything more if:
        // (1) There is an existing application record; and
        // (2) The caller doesn't think it is dead, OR there is no thread
        //     object attached to it so we know it couldn't have crashed; and
        // (3) There is a pid assigned to it, so it is either starting or
        //     already running.
        if (DEBUG_PROCESSES) Slog.v(TAG, "startProcess: name=" + processName
                + " app=" + app + " knownToBeDead=" + knownToBeDead
                + " thread=" + (app != null ? app.thread : null)
                + " pid=" + (app != null ? app.pid : -1));
        if (app != null && app.pid > 0) {
            if (!knownToBeDead || app.thread == null) {
                // We already have the app running, or are waiting for it to
                // come up (we have a pid but not yet its thread), so keep it.
                if (DEBUG_PROCESSES) Slog.v(TAG, "App already running: " + app);
                return app;
            } else {
                // An application record is attached to a previous process,
                // clean it up now.
                if (DEBUG_PROCESSES) Slog.v(TAG, "App died: " + app);
                handleAppDiedLocked(app, true);
            }
        }

        String hostingNameStr = hostingName != null
                ? hostingName.flattenToShortString() : null;
        
        if ((intentFlags&Intent.FLAG_FROM_BACKGROUND) != 0) {
            // If we are in the background, then check to see if this process
            // is bad.  If so, we will just silently fail.
            if (mBadProcesses.get(info.processName, info.uid) != null) {
                if (DEBUG_PROCESSES) Slog.v(TAG, "Bad process: " + info.uid
                        + "/" + info.processName);
                return null;
            }
        } else {
            // When the user is explicitly starting a process, then clear its
            // crash count so that we won't make it bad until they see at
            // least one crash dialog again, and make the process good again
            // if it had been bad.
            if (DEBUG_PROCESSES) Slog.v(TAG, "Clearing bad process: " + info.uid
                    + "/" + info.processName);
            mProcessCrashTimes.remove(info.processName, info.uid);
            if (mBadProcesses.get(info.processName, info.uid) != null) {
                EventLog.writeEvent(EventLogTags.AM_PROC_GOOD, info.uid,
                        info.processName);
                mBadProcesses.remove(info.processName, info.uid);
                if (app != null) {
                    app.bad = false;
                }
            }
        }
        
        if (app == null) {
            app = newProcessRecordLocked(null, info, processName);
            mProcessNames.put(processName, info.uid, app);
        } else {
            // If this is a new package in the process, add the package to the list
            app.addPackage(info.packageName);
        }

        // If the system is not ready yet, then hold off on starting this
        // process until it is.
        if (!mProcessesReady
                && !isAllowedWhileBooting(info)
                && !allowWhileBooting) {
            if (!mProcessesOnHold.contains(app)) {
                mProcessesOnHold.add(app);
            }
            if (DEBUG_PROCESSES) Slog.v(TAG, "System not ready, putting on hold: " + app);
            return app;
        }

        startProcessLocked(app, hostingType, hostingNameStr);
        return (app.pid != 0) ? app : null;
    }

    boolean isAllowedWhileBooting(ApplicationInfo ai) {
        return (ai.flags&ApplicationInfo.FLAG_PERSISTENT) != 0;
    }
    
    private final void startProcessLocked(ProcessRecord app,
            String hostingType, String hostingNameStr) {
        if (app.pid > 0 && app.pid != MY_PID) {
            synchronized (mPidsSelfLocked) {
                mPidsSelfLocked.remove(app.pid);
                mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);
            }
            app.pid = 0;
        }

        if (DEBUG_PROCESSES && mProcessesOnHold.contains(app)) Slog.v(TAG,
                "startProcessLocked removing on hold: " + app);
        mProcessesOnHold.remove(app);

        updateCpuStats();
        
        System.arraycopy(mProcDeaths, 0, mProcDeaths, 1, mProcDeaths.length-1);
        mProcDeaths[0] = 0;
        
        try {
            int uid = app.info.uid;
            int[] gids = null;
            try {
                gids = mContext.getPackageManager().getPackageGids(
                        app.info.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(TAG, "Unable to retrieve gids", e);
            }
            if (mFactoryTest != SystemServer.FACTORY_TEST_OFF) {
                if (mFactoryTest == SystemServer.FACTORY_TEST_LOW_LEVEL
                        && mTopComponent != null
                        && app.processName.equals(mTopComponent.getPackageName())) {
                    uid = 0;
                }
                if (mFactoryTest == SystemServer.FACTORY_TEST_HIGH_LEVEL
                        && (app.info.flags&ApplicationInfo.FLAG_FACTORY_TEST) != 0) {
                    uid = 0;
                }
            }
            int debugFlags = 0;
            if ((app.info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                debugFlags |= Zygote.DEBUG_ENABLE_DEBUGGER;
            }
            // Run the app in safe mode if its manifest requests so or the
            // system is booted in safe mode.
            if ((app.info.flags & ApplicationInfo.FLAG_VM_SAFE_MODE) != 0 ||
                Zygote.systemInSafeMode == true) {
                debugFlags |= Zygote.DEBUG_ENABLE_SAFEMODE;
            }
            if ("1".equals(SystemProperties.get("debug.checkjni"))) {
                debugFlags |= Zygote.DEBUG_ENABLE_CHECKJNI;
            }
            if ("1".equals(SystemProperties.get("debug.assert"))) {
                debugFlags |= Zygote.DEBUG_ENABLE_ASSERT;
            }
            int pid = Process.start("android.app.ActivityThread",
                    mSimpleProcessManagement ? app.processName : null, uid, uid,
                    gids, debugFlags, null);
            BatteryStatsImpl bs = app.batteryStats.getBatteryStats();
            synchronized (bs) {
                if (bs.isOnBattery()) {
                    app.batteryStats.incStartsLocked();
                }
            }
            
            EventLog.writeEvent(EventLogTags.AM_PROC_START, pid, uid,
                    app.processName, hostingType,
                    hostingNameStr != null ? hostingNameStr : "");
            
            if (app.persistent) {
                Watchdog.getInstance().processStarted(app.processName, pid);
            }
            
            StringBuilder buf = mStringBuilder;
            buf.setLength(0);
            buf.append("Start proc ");
            buf.append(app.processName);
            buf.append(" for ");
            buf.append(hostingType);
            if (hostingNameStr != null) {
                buf.append(" ");
                buf.append(hostingNameStr);
            }
            buf.append(": pid=");
            buf.append(pid);
            buf.append(" uid=");
            buf.append(uid);
            buf.append(" gids={");
            if (gids != null) {
                for (int gi=0; gi<gids.length; gi++) {
                    if (gi != 0) buf.append(", ");
                    buf.append(gids[gi]);

                }
            }
            buf.append("}");
            Slog.i(TAG, buf.toString());
            if (pid == 0 || pid == MY_PID) {
                // Processes are being emulated with threads.
                app.pid = MY_PID;
                app.removed = false;
                mStartingProcesses.add(app);
            } else if (pid > 0) {
                app.pid = pid;
                app.removed = false;
                synchronized (mPidsSelfLocked) {
                    this.mPidsSelfLocked.put(pid, app);
                    Message msg = mHandler.obtainMessage(PROC_START_TIMEOUT_MSG);
                    msg.obj = app;
                    mHandler.sendMessageDelayed(msg, PROC_START_TIMEOUT);
                }
            } else {
                app.pid = 0;
                RuntimeException e = new RuntimeException(
                        "Failure starting process " + app.processName
                        + ": returned pid=" + pid);
                Slog.e(TAG, e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            // XXX do better error recovery.
            app.pid = 0;
            Slog.e(TAG, "Failure starting process " + app.processName, e);
        }
    }        
}
```

从这个函数开始就开始创建新的应用进程了，

### 

```java

```


### 

```java
```



### 

```java
```


### 

```java
```
