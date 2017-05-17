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

本篇文章我们正式来分析ActivityManagerService的实现。

## ActivityManagerService功能结构

>ActivityManagerService继承于ActivityManagerNative，它本质上是一个Binder对象，AMS作为Android最核心的服务，它负责系统中四大组件的
启动、切换、调度以及应用进程进程的管理与调度工作。

在正式介绍ActivityManagerService之前，我们先来了解一些关键的概念。

```
ActivityManager：用来与系统中所有运行的Activity进行交互，运行在用户进程中。
````

**ActivityManagerService类图**

[点击查看详细类图](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/20/UMLClassDiagram-am-ActivityManagerService.svg)
 
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/20/activity_manager_service_uml.png">
 
从类图可以看出，ActivityManagerService是典型的AIDL实现：IActivityManager是定义好的AIDL接口。ActivityManagerNative对应Stub，
ActivityManagerNative.ActivityManagerProxy对应的是Stub.proxy，它提供给客户端ActivityManager使用，而ActivityManagerService是
接口的真正实现者。
 

### AThread

>AThread是定义在ActivityManagerService内部一个线程，它具有消息循环以及处理的功能，它主要用来完成ActivityManagerService
对象初始化，然后通知main函数所在线程ActivityManagerService创建完成。

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
    static class AThread extends Thread {
        ActivityManagerService mService;
        boolean mReady = false;

        public AThread() {
			//线程名ActivityManager
            super("ActivityManager");
        }

        public void run() {
			//支持消息循环及处理功能
            Looper.prepare();

            android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_FOREGROUND);
            android.os.Process.setCanSelfBackground(false);

			//创建ActivityManagerService对象
            ActivityManagerService m = new ActivityManagerService();

            synchronized (this) {
                //mService指向ActivityManagerService对象
                mService = m;
                //通知main函数所在线程ActivityManagerService对象已经创建完成
                notifyAll();
            }

            synchronized (this) {
                while (!mReady) {
                    try {
                        //等待main函数所在线程的notifyAll()
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            //进入消息循环
            Looper.loop();
        }
    }
}
```

## ActivityManagerService启动流程

我们来看看ActivityManagerService是的初始化流程，ActivityManagerService是由SystemServer的ServerThread创建的。很多关键服务
例如：WindowManagerService、ConnectivityService等都是在这个线程里进行创建的。

### 1 ServerThread.run()

```java
class ServerThread extends Thread {
 @Override
    public void run() {
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_SYSTEM_RUN,
            SystemClock.uptimeMillis());

        Looper.prepare();

       ...

        // Critical services...
        try {
            ...
            
            Slog.i(TAG, "Activity Manager");
            //调用ActivityManagerService的main函数，并返回一个context对象
            context = ActivityManagerService.main(factoryTest);

            ...
            //将SystemServer进程添加到ActivityManagerService中，以便被它管理
            ActivityManagerService.setSystemProcess();

            ...

        } catch (RuntimeException e) {
            Slog.e("System", "Failure starting core service", e);
        }

        ...
    }
}
```
在该方法中调用ActivityManagerService.main(factoryTest)得到一个Context对象。并将SystemServer进程添加到ActivityManagerService中，以便被它管理。
我们接着来看该main函数的实现。

### 2 ActivityManagerService.main(int factoryTest)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    public static final Context main(int factoryTest) {
            //1 创建并启动AThread线程
            AThread thr = new AThread();
            thr.start();
    
            synchronized (thr) {
                while (thr.mService == null) {
                    try {
                        //main函数等待AThread创建成功
                        thr.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
    
            ActivityManagerService m = thr.mService;
            mSelf = m;
            
            //2 调用ActivityThread.systemMain()获取ActivityThread对象
            ActivityThread at = ActivityThread.systemMain();
            mSystemThread = at;
            //3 通过调用ActivityThread对象的getSystemContext()获取ontext对象。
            Context context = at.getSystemContext();
            m.mContext = context;
            m.mFactoryTest = factoryTest;
            //4 创建ActivityStack对象，该对象用来管理Activity的启动与调度
            m.mMainStack = new ActivityStack(m, context, true);
            
            m.mBatteryStatsService.publish(context);
            m.mUsageStatsService.publish(context);
            
            synchronized (thr) {
                thr.mReady = true;
                //通知thr，本线程工作完成
                thr.notifyAll();
            }
            
            //5 调用ActivityManagerService.startRunning()开启服务
            m.startRunning(null, null, null, null);
            
            return context;
        }
}
```

该函数主要做了以下事情：

```
1 创建并启动AThread线程
2 调用ActivityThread.systemMain()获取ActivityThread对象
3 通过调用ActivityThread对象的getSystemContext()获取ontext对象
4 创建ActivityStack对象，该对象用来管理Activity的启动与调度
5 调用ActivityManagerService.startRunning()开启服务
```
该函数最终返回了Context对象，这是一个非常重要的对象，它后续再SystemServer被大量使用，是应用运行的上下文环境
，利用它可以获取资源，服务等。

我们再来进一步看看AThread内部调用ActivityManagerService的构造函数的实现。

### 3 ActivityManagerService.ActivityManagerService()

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    private ActivityManagerService() {
        String v = System.getenv("ANDROID_SIMPLE_PROCESS_MANAGEMENT");
        if (v != null && Integer.getInteger(v) != 0) {
            mSimpleProcessManagement = true;
        }
        v = System.getenv("ANDROID_DEBUG_APP");
        if (v != null) {
            mSimpleProcessManagement = true;
        }

        Slog.i(TAG, "Memory class: " + ActivityManager.staticGetMemoryClass());
        
        //1 创建系统目录以及一些关键服务，例如：BatteryStatsService、UsageStatsService
        //这个是我们熟悉的data目录
        File dataDir = Environment.getDataDirectory();
        //data/system目录
        File systemDir = new File(dataDir, "system");
        systemDir.mkdirs();
        mBatteryStatsService = new BatteryStatsService(new File(
                systemDir, "batterystats.bin").toString());
        mBatteryStatsService.getActiveStatistics().readLocked();
        mBatteryStatsService.getActiveStatistics().writeAsyncLocked();
        mOnBattery = DEBUG_POWER ? true
                : mBatteryStatsService.getActiveStatistics().getIsOnBattery();
        mBatteryStatsService.getActiveStatistics().setCallback(this);
        
        mUsageStatsService = new UsageStatsService(new File(
                systemDir, "usagestats").toString());

        //获取OpenGL版本
        GL_ES_VERSION = SystemProperties.getInt("ro.opengles.version",
            ConfigurationInfo.GL_ES_VERSION_UNDEFINED);

        //设置Configuration对象，该对象描述资源文件的配置属性，例如：字体、语言等。
        mConfiguration.setToDefaults();
        mConfiguration.locale = Locale.getDefault();
        //初始化ProcessStats对象，该对象用于统计CPU、内存等信息，它会去读取并解析
        //由内核生成proc/stat文件的内容，该文件记录kernel以及system运行时的统计信息。
        mProcessStats.init();
        
        // Add ourself to the Watchdog monitors.
        Watchdog.getInstance().addMonitor(this);

        //2 创建一个名为ProcessStats的新线程，用于定时更新系统信息。
        mProcessStatsThread = new Thread("ProcessStats") {
            public void run() {
                while (true) {
                    try {
                        try {
                            synchronized(this) {
                                final long now = SystemClock.uptimeMillis();
                                long nextCpuDelay = (mLastCpuTime.get()+MONITOR_CPU_MAX_TIME)-now;
                                long nextWriteDelay = (mLastWriteTime+BATTERY_STATS_TIME)-now;
                                //Slog.i(TAG, "Cpu delay=" + nextCpuDelay
                                //        + ", write delay=" + nextWriteDelay);
                                if (nextWriteDelay < nextCpuDelay) {
                                    nextCpuDelay = nextWriteDelay;
                                }
                                if (nextCpuDelay > 0) {
                                    mProcessStatsMutexFree.set(true);
                                    this.wait(nextCpuDelay);
                                }
                            }
                        } catch (InterruptedException e) {
                        }
                        updateCpuStatsNow();
                    } catch (Exception e) {
                        Slog.e(TAG, "Unexpected exception collecting process stats", e);
                    }
                }
            }
        };
        mProcessStatsThread.start();
    }
        
}
```

在ActivityManagerService的构造函数中主要做了2件事：

```
1 创建系统目录\关键服务，例如：BatteryStatsService、UsageStatsService，以及一些气筒信息，例如：GL_ES_VERSION、Configuration
、ProcessStats等。
2 创建一个名为ProcessStats的新线程，用于定时更新系统信息。
```

ActivityManagerService创建完成后，我们继续来看ActivityThread对象的创建，它也是ActivityManagerService重要的一部分。


### 4 ActivityThread.systemMain()

```java
public final class ActivityThread {

    public static final ActivityThread systemMain() {
        ActivityThread thread = new ActivityThread();
        thread.attach(true);
        return thread;
    }
    
    private final void attach(boolean system) {
            sThreadLocal.set(this);
            //判断是否为系统进程，上面传过来的为true，表明它是一个系统进程
            mSystemThread = system;
            //应用进程的处理力促
            if (!system) {
                ViewRoot.addFirstDrawHandler(new Runnable() {
                    public void run() {
                        ensureJitEnabled();
                    }
                });
                android.ddm.DdmHandleAppName.setAppName("<pre-initialized>");
                RuntimeInit.setApplicationObject(mAppThread.asBinder());
                IActivityManager mgr = ActivityManagerNative.getDefault();
                try {
                    mgr.attachApplication(mAppThread);
                } catch (RemoteException ex) {
                }
            } else {
                //1 初始化系统组件，例如：Instrumentation、ContextImpl、Application
                //系统进程的名称为system_process
                // Don't set application object here -- if the system crashes,
                // we can't display an alert, we just want to die die die.
                android.ddm.DdmHandleAppName.setAppName("system_process");
                try {
                    //Instrumentation对象
                    mInstrumentation = new Instrumentation();
                    ContextImpl context = new ContextImpl();
                    context.init(getSystemContext().mPackageInfo, null, this);
                    //创建Application对象
                    Application app = Instrumentation.newApplication(Application.class, context);
                    //一个进程支持多个Application对象
                    mAllApplications.add(app);
                    mInitialApplication = app;
                    //调用Application.onCreate()方法，这个方法我们非常熟悉了，我们经常在这里做一些初始化库的工作。
                    app.onCreate();
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Unable to instantiate Application():" + e.toString(), e);
                }
            }
            
            //2 注册Configuration变化后的回调通知，当系统配置发生变化时，例如：语言切换，触发该回调。
            ViewRoot.addConfigCallback(new ComponentCallbacks() {
                public void onConfigurationChanged(Configuration newConfig) {
                    synchronized (mPackages) {
                        // We need to apply this change to the resources
                        // immediately, because upon returning the view
                        // hierarchy will be informed about it.
                        if (applyConfigurationToResourcesLocked(newConfig)) {
                            // This actually changed the resources!  Tell
                            // everyone about it.
                            if (mPendingConfiguration == null ||
                                    mPendingConfiguration.isOtherSeqNewer(newConfig)) {
                                mPendingConfiguration = newConfig;
                                
                                queueOrSendMessage(H.CONFIGURATION_CHANGED, newConfig);
                            }
                        }
                    }
                }
                public void onLowMemory() {
                }
            });
        }

}

```
>ActivityThread代表了应用进程的主线程，管理着主线程activities、broadcasts等组件的启动与调度工作以及响应ActivityManger发来的其他操作请求。

ActivityThread的构造函数是个空实现，我们主要看它的attach方法。该方法中主要做了2件事情：

```
1 初始化系统组件，例如：Instrumentation、ContextImpl、Application。
2 注册Configuration变化后的回调通知，当系统配置发生变化时，例如：语言切换，触发该回调。
```

该方法创建的几个系统组件，它们的功能如下：

>Instrumentation：系统交互监测器，系统会首先创建Instrumentation，再通过Instrumentation来创建其他组件，系统与
组件之间的交互也通过Instrumentation进行，Instrumentation会去检测系统与组件的交互情况。
Application：应用，可以理解为一个容器，内部包含了四大组件。
Context：应用上下文环境，它是一个接口，其实现类是ContextImpl，通过它可以获取并操作Application中对应的组件。

我们再来总结一下systemMain()最终创建了哪些东西：

```
1 ActivityThread对象，它代表应用进程的主线程。
2 Context对象，它指向应用的上下文环境，具体来说指向了framework-res-apk运行时的上下文环境。
```

由上面的分析，我们可以知道，该函数就是为SystemServer进程搭建一个和应用进程一样的运行环境，这样我们就可以通过这个运行环境
同SystemServer内部的Service进行交互。

分析完了这一步，我们再来看看ActivityThread.getSystemContext()的创建过程。

### 5 ActivityThread.getSystemContext()

```java
public final class ActivityThread {

    public ContextImpl getSystemContext() {
        synchronized (this) {
            if (mSystemContext == null) {
                ContextImpl context =
                    ContextImpl.createSystemContext(this);
                //1 创建LoadedApk对象，该对象代表已经加载到系统中的APK，这里实际上指的是framework-res.apk
                //该APK仅供SystemServer使用
                LoadedApk info = new LoadedApk(this, "android", context, null);
                //2 初始化Context对象
                context.init(info, null, this);
                //3 初始化资源信息
                context.getResources().updateConfiguration(
                        getConfiguration(), getDisplayMetricsLocked(false));
                mSystemContext = context;
                //Slog.i(TAG, "Created system resources " + context.getResources()
                //        + ": " + context.getResources().getConfiguration());
            }
        }
        return mSystemContext;
    }

}
```
该函数返回了一个系统进程ContextImpl对象，它主要做了以下3件事:

```
1 创建LoadedApk对象，该对象代表已经加载到系统中的APK，这里实际上指的是framework-res.apk该APK仅供SystemServer使用
2 初始化Context对象
3 初始化资源信息

我们在接着来看ActivityManagerService.startRunning函数的调用与实现。
```
### 6 ActivityManagerService.startRunning(String pkg, String cls, String action,  String data)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    public final void startRunning(String pkg, String cls, String action,
            String data) {
        synchronized(this) {
            //如果已经调用过该函数，则直接返回
            if (mStartRunning) {
                return;
            }
            mStartRunning = true;
            mTopComponent = pkg != null && cls != null
                    ? new ComponentName(pkg, cls) : null;
            mTopAction = action != null ? action : Intent.ACTION_MAIN;
            mTopData = data;
            if (!mSystemReady) {
                return;
            }
        }

        systemReady(null);
    }        
}
```

上面这几个函数分析完了以后，整个ActivityManagerService的main函数的执行逻辑就分析完成了，该函数完成了两个任务：

```
1 创建了ActivityManagerService对象。
2 创建了一个供SystemServer进程使用的Android运行环境，即：ActivityThread与ContextImpl
```

到这里ActivityManagerService已经创建完了，我们再来分析一下ActivityManagerService.setSystemProcess()，该
方法将SystemServer进程添加到ActivityManagerService中，以便被它管理。

### 7 ActivityManagerService.setSystemProcess()

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
     public static void setSystemProcess() {
            try {
                ActivityManagerService m = mSelf;
                //1 向ServiceManager注册服务，例如：ActivityManagerService、PermissionController等
                ServiceManager.addService("activity", m);
                ServiceManager.addService("meminfo", new MemBinder(m));
                if (MONITOR_CPU_USAGE) {
                    ServiceManager.addService("cpuinfo", new CpuBinder(m));
                }
                ServiceManager.addService("permission", new PermissionController(m));
    
                ApplicationInfo info =
                    mSelf.mContext.getPackageManager().getApplicationInfo(
                            "android", STOCK_PM_FLAGS);
                mSystemThread.installSystemApplicationInfo(info);
           
                synchronized (mSelf) {
                    ProcessRecord app = mSelf.newProcessRecordLocked(
                            mSystemThread.getApplicationThread(), info,
                            info.processName);
                    app.persistent = true;
                    app.pid = MY_PID;
                    app.maxAdj = SYSTEM_ADJ;
                    mSelf.mProcessNames.put(app.processName, app.info.uid, app);
                    synchronized (mSelf.mPidsSelfLocked) {
                        mSelf.mPidsSelfLocked.put(app.pid, app);
                    }
                    mSelf.updateLruProcessLocked(app, true, true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(
                        "Unable to find android system package", e);
            }
        }        
}
```