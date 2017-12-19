# Android组件框架：Android组件管理者ActivityManager

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 组件管家ActivityManagerService
    - 1.1 ActivityManagerService启动流程
    - 1.1 ActivityManagerService工作流程
- 二 应用主线程ActivityThread
    - 2.1 ActivityThread启动流程
    - 2.2 ActivityThread工作

ActivityManagerService是贯穿Android系统组件的核心服务，在ServiceServer执行run()方法的时候被创建，运行在独立的线程中，负责Activity、Service、BroadcastReceiver的启动、切换、调度以及应用进程的管理和调度工作。

Activity、Service、BroadcastReceiver的启动、切换、调度都有着相似的流程，我们来看一下。

Activity的启动流程图（放大可查看）如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_start_flow.png" />

主要角色有：

- Instrumentation: 监控应用与系统相关的交互行为。
- AMS：组件管理调度中心，什么都不干，但是什么都管。
- ActivityStarter：处理Activity什么时候启动，怎么样启动相关问题，也就是处理Intent与Flag相关问题，平时提到的启动模式都可以在这里找到实现。
- ActivityStackSupervisior：这个类的作用你从它的名字就可以看出来，它用来管理Stack和Task。
- ActivityStack：用来管理栈里的Activity。
- ActivityThread：最终干活的人，是ActivityThread的内部类，Activity、Service、BroadcastReceiver的启动、切换、调度等各种操作都在这个类里完成。

Service的启动流程图（放大可查看）如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/service_start_flow.png" />

主要角色有：

- AMS：组件管理调度中心，什么都不干，但是什么都管。
- ApplicationThread：最终干活的人，是ActivityThread的内部类，Activity、Service、BroadcastReceiver的启动、切换、调度等各种操作都在这个类里完成。
- ActiveServices：主要用来管理Service，内部维护了三份列表：将启动Service列表、重启Service列表以及以销毁Service列表。

BroadcastReceiver的启动流程图（放大可查看）如下所示：

主要角色有：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/broadcast_start_flow.png" />

- AMS：组件管理调度中心，什么都不干，但是什么都管。
- BroadcastQueue：广播队列，根据广播的优先级来管理广播。
- ApplicationThread：最终干活的人，是ActivityThread的内部类，Activity、Service、BroadcastReceiver的启动、切换、调度等各种操作都在这个类里完成。
- ReceiverDispatcher：广播调度中心，采用反射的方式获取BroadcastReceiver的实例，然后调用它的onReceive()方法。

可以发现，除了一些辅助类外，最主要的组件管家AMS和应用主线程ActivityThread。本篇文章重点分析这两个类的实现，至于其他类会在
Activity、Service与BroadcastReceiver启动流程的文章中一一分析。

通过上面的分析，AMS的整个调度流程就非常明朗了。

>ActivityManager相当于前台接待，她将客户的各种需求传达给大总管ActivityMangerService，但是大总管自己不干活，他招来了很多小弟，他最信赖的小弟ApplicationThread
替他完成真正的启动、切换、以及退出操作，至于其他的中间环节就交给ActivityStack、ActivityStarter等其他小弟来完成。

## 一 组件管家ActivityManagerService

### 1.1 ActivityManagerService启动流程

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

UiHandler里的Looper来源于线程UiThread（继承于ServiceThread），它的线程名"android.ui"，该Handler主要用来处理UI相关操作。

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

## 1.2 ActivityManagerService工作流程

[ActivityManagerService](https://android.googlesource.com/platform/frameworks/base/+/4f868ed/services/core/java/com/android/server/am/ActivityManagerService.java)就是ActivityManager家族
的核心类了，四大组件的启动、切换、调度都是在ActivityManagerService里完成的。

ActivityManagerService类图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_manager_service_class.png" />

- ActivityManager：AMS给客户端调用的接口。
- ActivityManagerNative：该类是ActivityManagerService的父类，继承与Binder，主要用来负责进程通信，接收ActivityManager传递过来的信息，这么写可以将通信部分分离在ActivityManagerNative，使得
ActivityManagerService可以专注组件的调度，减小了类的体积。
- ActivityManagerProxy：该类定义在ActivityManagerNative内部，正如它的名字那样，它是ActivityManagerService的代理类，

关于ActivityManager

>[ActivityManager](https://android.googlesource.com/platform/frameworks/base/+/742a67127366c376fdf188ff99ba30b27d3bf90c/core/java/android/app/ActivityManager.java)是提供给客户端调用的接口，日常开发中我们可以利用
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

关于ActivityManagerNative与ActivityManagerProxy

>这两个类其实涉及的是Android的Binder通信原理，后面我们会有专门的文章来分析Binder相关实现。

## 二 应用主线程ActivityThread

>[ActivityThread](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ActivityThread.java)管理着应用进程里的主线程，负责Activity、Service、BroadcastReceiver的启动、切换、
以及销毁等操作。

### 2.1 ActivityThread启动流程

先来聊聊ActivityThread，这个类也厉害了😎，它就是我们app的入口，写过Java程序的同学都知道，Java程序的入口类都会有一个main()方法，ActivityThread也是这样，它的main()方法在新的应用
进程被创建后就会被调用，我们来看看这个main()方法实现了什么东西。

````java
public final class ActivityThread {
    
     public static void main(String[] args) {
         Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");
         SamplingProfilerIntegration.start();
 
         // CloseGuard defaults to true and can be quite spammy.  We
         // disable it here, but selectively enable it later (via
         // StrictMode) on debug builds, but using DropBox, not logs.
         CloseGuard.setEnabled(false);
 
         Environment.initForCurrentUser();
 
         // Set the reporter for event logging in libcore
         EventLogger.setReporter(new EventLoggingReporter());
 
         // Make sure TrustedCertificateStore looks in the right place for CA certificates
         final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
         TrustedCertificateStore.setDefaultUserDirectory(configDir);
 
         Process.setArgV0("<pre-initialized>");
         //主线程的looper
         Looper.prepareMainLooper();
         //创建ActivityThread实例
         ActivityThread thread = new ActivityThread();
         //调用attach()方法将ApplicationThread对象关联给AMS，以便AMS调用ApplicationThread里的方法，这同样也是一个IPC的过程。
         thread.attach(false);
 
         //主线程的Handler
         if (sMainThreadHandler == null) {
             sMainThreadHandler = thread.getHandler();
         }
 
         if (false) {
             Looper.myLooper().setMessageLogging(new
                     LogPrinter(Log.DEBUG, "ActivityThread"));
         }
 
         // End of event ActivityThreadMain.
         Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
         //开始消息循环
         Looper.loop();
 
         throw new RuntimeException("Main thread loop unexpectedly exited");
     }   
}
````
这里面还有关键的attach()方法，我们来看一下。

```java
public final class ActivityThread {
    
   private void attach(boolean system) {
        sCurrentActivityThread = this;
        //判断是否为系统进程，上面传过来的为false，表明它不是一个系统进程
        mSystemThread = system;
        //应用进程的处理流程
        if (!system) {
            ViewRootImpl.addFirstDrawHandler(new Runnable() {
                @Override
                public void run() {
                    ensureJitEnabled();
                }
            });
            android.ddm.DdmHandleAppName.setAppName("<pre-initialized>",
                                                    UserHandle.myUserId());
            RuntimeInit.setApplicationObject(mAppThread.asBinder());
            final IActivityManager mgr = ActivityManagerNative.getDefault();
            try {
                //将ApplicationThread对象关联给AMS，以便AMS调用ApplicationThread里的方法，这
                //同样也是一个IPC的过程。
                mgr.attachApplication(mAppThread);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
            // Watch for getting close to heap limit.
            BinderInternal.addGcWatcher(new Runnable() {
                @Override public void run() {
                    if (!mSomeActivitiesChanged) {
                        return;
                    }
                    Runtime runtime = Runtime.getRuntime();
                    long dalvikMax = runtime.maxMemory();
                    long dalvikUsed = runtime.totalMemory() - runtime.freeMemory();
                    if (dalvikUsed > ((3*dalvikMax)/4)) {
                        if (DEBUG_MEMORY_TRIM) Slog.d(TAG, "Dalvik max=" + (dalvikMax/1024)
                                + " total=" + (runtime.totalMemory()/1024)
                                + " used=" + (dalvikUsed/1024));
                        mSomeActivitiesChanged = false;
                        try {
                            mgr.releaseSomeActivities(mAppThread);
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                }
            });
        } 
        //系统进程的处理流程
        else {
            //初始化系统组件，例如：Instrumentation、ContextImpl、Application
            //系统进程的名称为system_process
            android.ddm.DdmHandleAppName.setAppName("system_process",
                    UserHandle.myUserId());
            try {
                //创建Instrumentation对象
                mInstrumentation = new Instrumentation();
                //创建ContextImpl对象
                ContextImpl context = ContextImpl.createAppContext(
                        this, getSystemContext().mPackageInfo);
                //创建Application对象
                mInitialApplication = context.mPackageInfo.makeApplication(true, null);
                //调用Application.onCreate()方法，这个方法我们非常熟悉了，我们经常在这里做一些初始化库的工作。
                mInitialApplication.onCreate();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to instantiate Application():" + e.toString(), e);
            }
        }

        // add dropbox logging to libcore
        DropBox.setReporter(new DropBoxReporter());
        
        //注册Configuration变化后的回调通知，当系统配置发生变化时，例如：语言切换，触发该回调。
        ViewRootImpl.addConfigCallback(new ComponentCallbacks2() {
            //配置发生变化
            @Override
            public void onConfigurationChanged(Configuration newConfig) {
                synchronized (mResourcesManager) {
                    // We need to apply this change to the resources
                    // immediately, because upon returning the view
                    // hierarchy will be informed about it.
                    if (mResourcesManager.applyConfigurationToResourcesLocked(newConfig, null)) {
                        updateLocaleListFromAppContext(mInitialApplication.getApplicationContext(),
                                mResourcesManager.getConfiguration().getLocales());

                        // This actually changed the resources!  Tell
                        // everyone about it.
                        if (mPendingConfiguration == null ||
                                mPendingConfiguration.isOtherSeqNewer(newConfig)) {
                            mPendingConfiguration = newConfig;

                            sendMessage(H.CONFIGURATION_CHANGED, newConfig);
                        }
                    }
                }
            }
            //低内存
            @Override
            public void onLowMemory() {
            }
            @Override
            public void onTrimMemory(int level) {
            }
        });
    }
}
```

从上面这两个方法我们可以看出ActivityThread主要做了两件事情：

- 创建并开启主线程的消息循环。
- 将ApplicationThread对象（Binder对象）关联给AMS，以便AMS调用ApplicationThread里的方法，这同样也是一个IPC的过程。

### 2.2 ActivityThread工作流程

ActivityThread工作流程图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_thread_structure.png" />

通过前面的分析，ActivityThread的整个工作流程就非常明朗了。ActivityThread内部有个Binder对象ApplicationThread，AMS可以调用ApplicationThread里的方法，而
ApplicationThread里的方法利用mH（Handler）发送消息给ActivityThread里的消息队列，ActivityThread再去处理这些消息，进而完成诸如Activity启动等各种操作。

到这里我们已经把ActivityManager家族的主要框架都梳理完了，本篇文章并没有大篇幅的去分析源码，我们的重点是梳理整体框架，让大家有整体上的认识，至于具体的细节，可以根据自己的需要有的
放矢的去研究。这也是我们提倡的阅读Android源码的方法：不要揪着细节不放，要有整体意识。

理解了AMS的内容，后续就接着来分析Activity、Service、BroadcastReceiver的启动、切换和销毁等流程，分析的过程中也会结合着日常开发中经常遇到的一些问题，带着这些问题，我们去看看源
码里怎么写的，为什么会出现这些问题。应该如何去解决。