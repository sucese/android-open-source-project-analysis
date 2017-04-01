# Android系统应用框架篇：Activity启动流程(一)

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

- [5Android系统应用框架篇：Activity启动流程(一)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/5Android系统应用框架篇：Activity启动流程(一).md)
- [6Android系统应用框架篇：Activity启动流程(二)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/6Android系统应用框架篇：Activity启动流程(二).md)
- [7Android系统应用框架篇：Activity启动流程(三)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/7Android系统应用框架篇：Activity启动流程(三).md)

从这篇文章开始，我们来详细地分析Activity的启动流程，在分析的过程中会有各种各样的角色参与进来，例如：ActivityServiceManager、ActivityStack、ActivityRecord等，涉及的流程与代码也会比较长，但是莫慌，老司
机带你轻松看源码。<img src="https://github.com/guoxiaoxing/emoji/raw/master/emoji/d_doge.png" width="30" height="30" align="bottom" />

在分析源码过程中，我们专注流程与框架的理解，不要陷入到具体的细节之中，随着分析的深入，这些前面觉得疑惑的问题后面都会一一得到解决，毕竟代码岁虽多，流程虽长，但本质上都是组件间的协同，参数的包装与处理，只要我们抓
住核心原理，所有的问题就都迎刃而解。

笔者在分析的过程中，也会为读者提供各种结构图、时序图来辅助理解，每个小节完成后，也会再次做小节汇总，力求让读者看得明白，记得深刻。另外，Android四大组件的启动流程有异曲同工之处我们掌握了Activity，后面各组件以
及其他系统都可以举一反三，触类旁通。

由于本文篇幅比较长，正式开始本篇文章前，先说明一下文章中经常出现的名词的含义。

```
源Activity：执行启动操作的Activity组件
目标Activity：将要启动的Activity组件。
Launcher：如果目标Activity是应用的Launcher Activity，那么当用户点击应用图标后，由Launcher组件来进行启动启动。这里的Launcher也是一个Activity。
```

好了，让我们开始吧。<img src="https://github.com/guoxiaoxing/emoji/raw/master/emoji/d_xixi.png" width="30" height="30" align="bottom"/>

Activity组件的启动流程分为3种情况：

```
1 目标Activity是应用的LauncherActivity，启动目标Activity是Launcher组件，两者处在不同进程中，需要进行跨进程通信。这个启动流程同样也是一个应用的启动流程。
2 目标Activity与源Activity在同一进程中。启动目标Activity无需创建新进程。
3 目标Activity与源Activity在不同进程中，启动目标Activity需要创建新进程。
```

3种情况的启动流程大体相似，但是也有差别，下面分析的过程中，会一一说明这些差别。

Activity的启动流程一共分为7大步，35小步，5个进程通信，在10个组件中执行。我们先来看看整个启动流程的序列图，先对整个流程有个大致印象。

Activity启动流程序列图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/1/activity_start_sequence.png"/>

Activity启动流程结构图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/1/activity_start_structure.png"/>

看了上述两个图，我们来分析下两个问题。

Activity启动的过程中牵扯到了哪些组件？

```
Launcher：Launcher继承于Activity，它也是一个Activity。它就是我们手机的桌面，负责启动应用，显示桌面菜单等。
Activity：所有页面的基类。
Instrumentation：应用监控器，监控应用与系统的交互行为，还可以定义一些用于探测和分析应用性能呢等相关的类（Instrumentation测试框架）。
ActivityManagerProxy：实现了IActivityManager，ActivityManagerService的代理对象。
ActivityManagerService：继承于ActivityManagerNative，用来管理系统的四大组件Activity、ervice、Brocast Receiver与Content Provider。
ActivityStack：Activity栈，用来控制Activity的出栈与入栈。
ApplicationThreadProxy：ApplicationThreadd的代理对象。
ApplicationThread：它是ActivityThread的一个内部类，继承与ApplicationThreadNative，本质上是一个Binder对象，用于进程间通信。
ActivityThread：用来描述一个应用进程。

```

关于上述组件，读者可以先大致了解它们的功能，后续还会有有文章去介绍它们的源码和原理。

在这些组件的交互中，有哪些跨进程通信，这些进程通信都是为了完成什么工作？

```
START_ACTIVITY_TRANSACTION：Launcher发出，ActivityManagerService处理，启动Activity。
SCHEDULE_PAUSE_ACTIVITY_TRANSACTION：ActivityManagerService发出，Launcher处理，要求终止源Activity。
ACTIVITY_PAUSED_TRANSACTION：Launcher发出，ActivityManagerService处理，通知ActivityManagerService源Activity以及终止。
ATTACH_APPLICATION_TRANSACTION：新创建的应用进程发出，ActivityManagerService处理，通知ActivityManagerService新进程已经创建，可以开始目标Activity创建工作。
SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION：ActivityManagerService发出，新创建应用进程处理，ActivityManagerService通知新建应用进程创建目标Activity。
```

### 启动Launcher Activity

一 在Launcher中执行，把Activity的启动过程交由Instrumentation监控，并向ActivityManagerService发送一个启动目标Activity的进程间通信请求START_ACTIVITY_TRANSACTION，进一步执行目标Activity启动操作。

```
1 auncher.startActivitySafely(Intent intent, Object tag)
2 Activity.startActivity(Intent intent)
3 Activity.startActivityForResult(Intent intent, int requestCode)
4 Instrumentation.execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode)
5 ApplicationThreadProxy.startActivity(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded, boolean debug)
```
二 在ActivityManagerService中执行，接收Launcher发出的START_ACTIVITY_TRANSACTION进程通信请求。调用ActivityStack里的方法，解析Activity信息以及传递过来的Intent信息。并向Launcher
发送一个通知源Activity进入终止状态的进程间通信请求START_ACTIVITY_TRANSACTION，请求执行暂停源Activity的操作。

```
6 ActivityManagerService.startActivity(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded, boolean debug)
7 ActivityStack.startActivityMayWait(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded,  boolean debug, WaitResult outResult, Configuration config)
8 ActivityStack.startActivityLocked(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, ActivityInfo aInfo, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, boolean onlyIfNeeded, boolean componentSpecified)
9 ActivityStack.startActivityUncheckedLocked(ActivityRecord r, ActivityRecord sourceRecord, Uri[] grantedUriPermissions, int grantedMode, boolean onlyIfNeeded, boolean doResume) 
10 ActivityStack.resumeTopActivityLocked(ActivityRecord prev) 
11 ActivityStack.startPausingLocked(boolean userLeaving, boolean uiSleeping)
12 ApplicationThreadProxy。schedulePauseActivity(prev, prev.finishing, userLeaving, prev.configChangeFlags)
```
三 在Launcher中执行，接收ActivityManagerService发出的SCHEDULE_PAUSE_ACTIVITY_TRANSACTION进程通信请求。执行暂停源Activity的操作。并向ActivityManagerService发送一个源Activity已经进入终止状态的
进程通信请求SCHEDULE_PAUSE_ACTIVITY_TRANSACTION，通知源Activity已经被暂停。

```
13 ActivityThread.schedulePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges)
14 ActivityThread.queueOrSendMessage(int what, Object obj, int arg1, int arg2)
15 H.handleMessage(Message msg)
16 ActivityThread.handlePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges) 
17 ActivityManagerProxy.activityPaused(IBinder token, Bundle state)
```

四 在ActivityManagerService中执行，接收Launcher发出的ACTIVITY_PAUSED_TRANSACTION进程通信请求，创建新进程，为进一步启动目标Activity做准备。

```
18 ActivityManagerService.activityPaused(IBinder token, Bundle icicle)
19 ActivityStack.activityPaused(IBinder token, Bundle icicle, boolean timeout)
20 ActivityStack.completePauseLocked()
21 ActivityStack.resumeTopActivityLocked(ActivityRecord prev) 
22 ActivityStack.startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig)
23 ActivityManagerService.startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting)
```

五 在新创建的进程中执行，并向ActivityManagerService发送一个新进程创建完成的进程通信请求ATTACH_APPLICATION_TRANSACTION，通知新进程已经被创建，可以进一步执行Activity启动操作。

```
24 ActivityThread.main(String[] args)
25 ActivityManagerProxy.attachApplication(IApplicationThread app)
```

六 在ActivityManagerService中执行，接收新进程发出的ATTACH_APPLICATION_TRANSACTION进程通信请求，包装新进程信息，检查目标Activity进程信息与新进程信息是否一致，为最终在新进程中
启动目标Activity做准备。

```
26 ActivityManagerService.attachApplication(IApplicationThread thread)
27 ActivityManagerService.attachApplicationLocked(IApplicationThread thread, int pid)
28 ActivityStack.realStartActivityLocked(ActivityRecord r, ProcessRecord app, boolean andResume, boolean checkConfig)
29 ApplicationThreadProxy.scheduleLaunchActivity(Intent intent, IBinder token, int ident, ActivityInfo info, Bundle state, List<ResultInfo> pendingResults, List<Intent> pendingNewIntents, boolean notResumed, boolean isForward)
```

七 在新进程中执行，接收ActivityManagerService发出的SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION进程间通信请求，最终执行目标Activity的启动操作。

```
30 ActivityThread.scheduleRelaunchActivity(IBinder token, List<ResultInfo> pendingResults, List<Intent> pendingNewIntents, int configChanges, boolean notResumed, Configuration config)
31 ActivityThread.queueOrSendMessage(int what, Object obj)
32 H.handleMessage(Message msg)
33 ActivityThread.handleLaunchActivity(ActivityClientRecord r, Intent customIntent) 
34 ActivityThread.performLaunchActivity(ActivityClientRecord r, Intent customIntent)
35 Activity.onCreate(Bundle savedInstanceState) 
```
### 相同进程启动Activity

启动栈图：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/1/activity_in_same_process.png"/>

启动流程：

一 源Activity向ActivityManagerService发送一个启动目标Activity的进程间通信请求START_ACTIVITY_TRANSACTION。
二 ActivityManagerService首先将目标Activity的信息保存下来，然后再向源Activity发送一个通知源Activity进入终止状态的进程间通信请求START_ACTIVITY_TRANSACTION。
三 源Activity进入终止状态后，再向ActivityManagerService发送一个源Activity已经进入终止状态的进程通信请求SCHEDULE_PAUSE_ACTIVITY_TRANSACTION，以便ActivityManagerService进一步执行目标Activity启动操作。
四 ActivityManagerService发现运行目标Activity组件的应用进程已经存在，便把目标Activity的信息发送一个该应用进程，该应用进程最终执行目标Activity的启动操作。

### 新进程启动Activity

启动栈图：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/1/activity_in_new_process.png"/>

启动流程：
一 源Activity向ActivityManagerService发送一个启动目标Activity的进程间通信请求START_ACTIVITY_TRANSACTION。
二 ActivityManagerService首先将目标Activity的信息保存下来，然后再向源Activity发送一个通知源Activity进入终止状态的进程间通信请求START_ACTIVITY_TRANSACTION。
三 源Activity进入终止状态后，再向ActivityManagerService发送一个源Activity已经进入终止状态的进程通信请求SCHEDULE_PAUSE_ACTIVITY_TRANSACTION，以便ActivityManagerService进一步执行目标Activity启动操作。
四 ActivityManagerService发现运行目标Activity组件的应用进程并不存在，它会先去启动一个新的应用进程。
五 新的应用进程创建完成后，会向ActivityManagerService发送一个新进程创建完成的进程通信请求ATTACH_APPLICATION_TRANSACTION，以便ActivityManagerService进一步执行目标Activity的启动操作。
六 ActivityManagerService将目标Activity的信息发送给新创建的进程，新进程执行目标Activity的创建操作。

从上面可以看出，三种情况下的Activity的启动流程大同小异，好了，我们下一篇文章进入正式的源码分析吧。

>注：分析的过程中，会牵扯任务、应用进程、消息循环、Binder进程通信等方面内容，这些内容我们暂时先不讨论，后面会有文章详尽地去分析这些内容，本次文章的重点在于讨论Activity的启动流程。