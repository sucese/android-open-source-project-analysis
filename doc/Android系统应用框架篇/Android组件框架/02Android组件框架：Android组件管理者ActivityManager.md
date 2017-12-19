# Android组件框架：Android组件管理者ActivityManager

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 客户端ActivityManager
- 二 服务端ActivityManagerService
- 三 Activity栈ActivityStack
- 四 应用主线程ActivityThread

ActivityManagerService是贯穿Android系统组件的核心服务，在ServiceServer执行run()方法的时候被创建，运行在独立的线程中，负责Activity、Service、BroadcastReceiver的启动、切换、调度以及应用进程的管理和调度工作。

Activity、Service、BroadcastReceiver的启动、切换、调度都有着相似的流程，我们来看一下。

Activity的启动流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_start_flow.png" />

Service的启动流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/broadcast_start_flow.png" />

BroadcastReceiver的启动流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_start_flow.png" />

可以发现，除了一些辅助类，

注：不理解这些流程也没关系，后面会有专门的文章来具体的分析，你只需要知道在整个流程中涉及哪些重要角色就可以了。



从以上类图中我们不难发现在整个ActivityManager家族中涉及以下重要角色：

- ActivityManager：该类是客户端用来管理系统中正在运行的组件的task、memory等信息工具，它本身只是个Client端，提交相关信息给ActivityManagerService，具体功能
都有ActivityManagerService来完成。
- ActivityManagerService：该类作为Android系统组件的核心服务，在ServiceServer执行run()方法的时候被创建，运行在独立的线程中，负责四大组件的启动、切换、调度以
及应用进程的管理和调度工作。
- ActivityStack：Activity栈，用来管理Activity。
- ActivityThread：管理应用进程中的主线程，也就是UI线程，调度和执行AWS请求的的Activity、Broadcast的各种操作。

这么一分析，整个流程就非常明朗了。

>ActivityManager相当于前台接待，她将客户的各种需求传达给大总管ActivityMangerService，但是大总管自己不干活，他招来了两个小弟，ActivityStack替他管理组件的进出（栈），ActivityThread替他执行真正
的启动、退出等操作。

## 一 客户端ActivityManager

>Interact with the overall activities running in the system.

[ActivityManager](https://android.googlesource.com/platform/frameworks/base/+/742a67127366c376fdf188ff99ba30b27d3bf90c/core/java/android/app/ActivityManager.java)是提供给客户端调用的接口，日常开发中我们可以利用
ActivityManager来获取系统中正在运行的组件（Activity、Service）、进程（Process）、任务（Task）等信息，ActivityManager定义了相应的方法来获取和操作这些信息。

ActivityManager定义了很多静态内部类来描述这些信息，具体说来：

- ActivityManager.StackId： 描述组件栈ID信息
- ActivityManager.StackInfo： 描述组件栈信息，可以利用StackInfo去系统中检索某个栈。
- ActivityManager.MemoryInfo： 系统可用内存信息
- ActivityManager.RecentTaskInfo： 最近的任务信息
- ActivityManager.RunningAppProcessInfo： 正在运行的进程信息
- ActivityManager.RunningServiceInfo： 正在运行的服务信息
- ActivityManager.RunningTaskInfo： 正在运行的任务信息
- ActivityManager.AppTask： 描述应用任务信息

说道这里，我们有必要区分一些概念，以免以后混淆。

- 进程（Process）：Android系统进行资源调度和分配的基本单位，需要注意的是同一个栈的Activity可以运行在不同的进程里。
- 任务（Task）：Task是一组以栈的形式聚集在一起的Activity的集合，这个任务栈就是一个Task。
                      
在日常开发中，我们一般是不需要直接操作ActivityManager这个类，只有在一些特殊的开发场景才用的到。

- isLowRamDevice()：判断应用是否运行在一个低内存的Android设备上。
- clearApplicationUserData()：重置app里的用户数据。
- ActivityManager.AppTask/ActivityManager.RecentTaskInfo：我们如何需要操作Activity的栈信息也可以通过ActivityManager来做。

## 二 服务端ActivityManagerService

[ActivityManagerService](https://android.googlesource.com/platform/frameworks/base/+/4f868ed/services/core/java/com/android/server/am/ActivityManagerService.java)就是ActivityManager家族
的核心类了，四大组件的启动、切换、调度都是在ActivityManagerService里完成的。

ActivityManagerService类图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_manager_service_class.png" />

可以看到，和ActivityManagerService相关的还有两个类：

- ActivityManagerNative：该类是ActivityManagerService的父类，继承与Binder，主要用来负责进程通信，接收ActivityManager传递过来的信息，这么写可以将通信部分分离在ActivityManagerNative，使得
ActivityManagerService可以专注组件的调度，减小了类的体积。
- ActivityManagerProxy：该类定义在ActivityManagerNative内部，正如它的名字那样，它是ActivityManagerService的代理类，

注：这两个类其实涉及的是Android的Binder通信原理，后面我们会有专门的文章来分析Binder相关实现。

### 2.1 ActivityManagerService启动流程

我们知道所有的系统服务都是在[SystemServer](https://android.googlesource.com/platform/frameworks/base/+/7d276c3/services/java/com/android/server/SystemServer.java)的run()方法里启动的，SystemServer
将系统服务分为了三类：

- 引导服务
- 核心服务
- 其他服务

ActivityManagerService属于引导服务，在startBootstrapServices()方法里被创建，如下所示：

```java
mActivityManagerService = mSystemServiceManager.startService(
        ActivityManagerService.Lifecycle.class).getService();
```
SystemServiceManager的startService()方法利用反射来创建对象，Lifecycle是ActivityManagerService里的静态内部类，它继承于SystemService，在它的构造方法里
它会调用ActivityManagerService的构造方法创建ActivityManagerService对象。

```java
public static final class Lifecycle extends SystemService {
    private final ActivityManagerService mService;

    public Lifecycle(Context context) {
        super(context);
        mService = new ActivityManagerService(context);
    }

    @Override
    public void onStart() {
        mService.start();
    }

    public ActivityManagerService getService() {
        return mService;
    }
}
```

ActivityManagerService的构造方法如下所示：

```java
public ActivityManagerService(Context systemContext) {
    mContext = systemContext;
    mFactoryTest = FactoryTest.getMode();
    mSystemThread = ActivityThread.currentActivityThread();

    Slog.i(TAG, "Memory class: " + ActivityManager.staticGetMemoryClass());

    //创建并启动系统线程以及相关Handler
    mHandlerThread = new ServiceThread(TAG,
            android.os.Process.THREAD_PRIORITY_FOREGROUND, false /*allowIo*/);
    mHandlerThread.start();
    mHandler = new MainHandler(mHandlerThread.getLooper());
    mUiHandler = new UiHandler();
    /* static; one-time init here */
    if (sKillHandler == null) {
        sKillThread = new ServiceThread(TAG + ":kill",
                android.os.Process.THREAD_PRIORITY_BACKGROUND, true /* allowIo */);
        sKillThread.start();
        sKillHandler = new KillHandler(sKillThread.getLooper());
    }

    //创建用来存储各种组件Activity、Broadcast的数据结构
    mFgBroadcastQueue = new BroadcastQueue(this, mHandler,
            "foreground", BROADCAST_FG_TIMEOUT, false);
    mBgBroadcastQueue = new BroadcastQueue(this, mHandler,
            "background", BROADCAST_BG_TIMEOUT, true);
    mBroadcastQueues[0] = mFgBroadcastQueue;
    mBroadcastQueues[1] = mBgBroadcastQueue;

    mServices = new ActiveServices(this);
    mProviderMap = new ProviderMap(this);
    mAppErrors = new AppErrors(mContext, this);

    //创建system等各种文件夹，用来记录系统的一些事件
    ...
    
    //初始化一些记录工具
    ...
}
```
可以发现，ActivityManagerService的构造方法主要做了两个事情：

- 创建并启动系统线程以及相关Handler。
- 创建用来存储各种组件Activity、Broadcast的数据结构。

这里有个问题，这里创建了两个Hanlder（sKillHandler暂时忽略，它是用来kill进程的）分别是MainHandler与UiHandler，它们有什么区别呢？🤔

我们知道Handler是用来向所在线程发送消息的，也就是说决定Handler定位的是它构造方法里的Looper，我们分别来看下。

MainHandler里的Looper来源于线程ServiceThread，它的线程名是"ActivityManagerService"，该Handler主要用来处理组件调度相关操作。

```java
mHandlerThread = new ServiceThread(TAG,
        android.os.Process.THREAD_PRIORITY_FOREGROUND, false /*allowIo*/);
mHandlerThread.start();
mHandler = new MainHandler(mHandlerThread.getLooper());
```

UiHandler里的Looper来源于线程UiThread（继承于ServiceThread），它的线程名"android.ui"，该Handler主要用来处理UI相关操作。所以我们
平时用的getMainHandler()方法获取到的实际是这个UiHandler。

```java

private UiThread() {
    super("android.ui", Process.THREAD_PRIORITY_FOREGROUND, false /*allowIo*/);
    // Make sure UiThread is in the fg stune boost group
    Process.setThreadGroup(Process.myTid(), Process.THREAD_GROUP_TOP_APP);
}

public UiHandler() {
    super(com.android.server.UiThread.get().getLooper(), null, true);
}
```

以上便是整个ActivityManagerService的启动流程，还是比较简单的。

## 三 Activity栈ActivityStack

## 四 应用主线程ActivityThread





**ActivityManagerService类图**

[点击查看详细类图](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/04/UMLClassDiagram-am-ActivityManagerService.svg)
 
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/04/activity_manager_service_uml.png">
 
从类图可以看出，ActivityManagerService是典型的AIDL实现：IActivityManager是定义好的AIDL接口。ActivityManagerNative对应Stub，
ActivityManagerNative.ActivityManagerProxy对应的是Stub.proxy，它提供给客户端ActivityManager使用，而ActivityManagerService是
接口的真正实现者。
 
### 1.1 AThread

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

## 二 ActivityManagerService启动流程

我们来看看ActivityManagerService是的初始化流程，ActivityManagerService是由SystemServer的ServerThread创建的。很多关键服务
例如：WindowManagerService、ConnectivityService等都是在这个线程里进行创建的。

### 2.1 ServerThread.run()

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

### 2.2 ActivityManagerService.main(int factoryTest)

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

### 2.3 ActivityManagerService.ActivityManagerService()

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


### 2.4 ActivityThread.systemMain()

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

### 2.5 ActivityThread.getSystemContext()

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
### 2.6 ActivityManagerService.startRunning(String pkg, String cls, String action,  String data)

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

### 2.7 ActivityManagerService.setSystemProcess()

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
                    //向PackageManagerService查询名为"android"的ApplicationInfo
                    mSelf.mContext.getPackageManager().getApplicationInfo(
                            "android", STOCK_PM_FLAGS);
                mSystemThread.installSystemApplicationInfo(info);
           
                synchronized (mSelf) {
                    //管理进程
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