# Android组件框架：Android视图容器Activity

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

本篇文章我们来分析Android的视图容器Activity，这也是我们在日常开发中接触的最多的一个组件。

**文章目录**

- 一 Activity的生命周期
- 一 Activity的启动流程

😁一个简单的例子

```java
public class SimpleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
    }
}
```

基本上我们第一天接触Android的界面开发时就会看到这么一段代码，那么大家有没有思考过这段代码背后的调用逻辑是什么🤔，UI是如何呈现在Activity上的🤔
接下来我们就来一一分析这些问题。

## 一 Activity的生命周期


## 二 Activity的启动流程

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

好了，让我们开始吧。😁

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

1. 在Launcher中执行，把Activity的启动过程交由Instrumentation监控，并向ActivityManagerService发送一个启动目标Activity的进程间通信请求START_ACTIVITY_TRANSACTION，进一步执行目标Activity启动操作。

```
1 auncher.startActivitySafely(Intent intent, Object tag)
2 Activity.startActivity(Intent intent)
3 Activity.startActivityForResult(Intent intent, int requestCode)
4 Instrumentation.execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode)
5 ApplicationThreadProxy.startActivity(IApplicationThread caller, Intent intent, String resolvedType, Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode, boolean onlyIfNeeded, boolean debug)
```
2. 在ActivityManagerService中执行，接收Launcher发出的START_ACTIVITY_TRANSACTION进程通信请求。调用ActivityStack里的方法，解析Activity信息以及传递过来的Intent信息。并向Launcher
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
3. 在Launcher中执行，接收ActivityManagerService发出的SCHEDULE_PAUSE_ACTIVITY_TRANSACTION进程通信请求。执行暂停源Activity的操作。并向ActivityManagerService发送一个源Activity已经进入终止状态的
进程通信请求SCHEDULE_PAUSE_ACTIVITY_TRANSACTION，通知源Activity已经被暂停。

```
13 ActivityThread.schedulePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges)
14 ActivityThread.queueOrSendMessage(int what, Object obj, int arg1, int arg2)
15 H.handleMessage(Message msg)
16 ActivityThread.handlePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges) 
17 ActivityManagerProxy.activityPaused(IBinder token, Bundle state)
```

4. 在ActivityManagerService中执行，接收Launcher发出的ACTIVITY_PAUSED_TRANSACTION进程通信请求，创建新进程，为进一步启动目标Activity做准备。

```
18 ActivityManagerService.activityPaused(IBinder token, Bundle icicle)
19 ActivityStack.activityPaused(IBinder token, Bundle icicle, boolean timeout)
20 ActivityStack.completePauseLocked()
21 ActivityStack.resumeTopActivityLocked(ActivityRecord prev) 
22 ActivityStack.startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig)
23 ActivityManagerService.startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting)
```

5. 在新创建的进程中执行，并向ActivityManagerService发送一个新进程创建完成的进程通信请求ATTACH_APPLICATION_TRANSACTION，通知新进程已经被创建，可以进一步执行Activity启动操作。

```
24 ActivityThread.main(String[] args)
25 ActivityManagerProxy.attachApplication(IApplicationThread app)
```

6. 在ActivityManagerService中执行，接收新进程发出的ATTACH_APPLICATION_TRANSACTION进程通信请求，包装新进程信息，检查目标Activity进程信息与新进程信息是否一致，为最终在新进程中
启动目标Activity做准备。

```
26 ActivityManagerService.attachApplication(IApplicationThread thread)
27 ActivityManagerService.attachApplicationLocked(IApplicationThread thread, int pid)
28 ActivityStack.realStartActivityLocked(ActivityRecord r, ProcessRecord app, boolean andResume, boolean checkConfig)
29 ApplicationThreadProxy.scheduleLaunchActivity(Intent intent, IBinder token, int ident, ActivityInfo info, Bundle state, List<ResultInfo> pendingResults, List<Intent> pendingNewIntents, boolean notResumed, boolean isForward)
```

7. 在新进程中执行，接收ActivityManagerService发出的SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION进程间通信请求，最终执行目标Activity的启动操作。

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

1. 源Activity向ActivityManagerService发送一个启动目标Activity的进程间通信请求START_ACTIVITY_TRANSACTION。
2. ActivityManagerService首先将目标Activity的信息保存下来，然后再向源Activity发送一个通知源Activity进入终止状态的进程间通信请求START_ACTIVITY_TRANSACTION。
3. 源Activity进入终止状态后，再向ActivityManagerService发送一个源Activity已经进入终止状态的进程通信请求SCHEDULE_PAUSE_ACTIVITY_TRANSACTION，以便ActivityManagerService进一步执行目标Activity启动操作。
4. ActivityManagerService发现运行目标Activity组件的应用进程已经存在，便把目标Activity的信息发送一个该应用进程，该应用进程最终执行目标Activity的启动操作。

### 新进程启动Activity

启动栈图：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/1/activity_in_new_process.png"/>

启动流程：

1. 源Activity向ActivityManagerService发送一个启动目标Activity的进程间通信请求START_ACTIVITY_TRANSACTION。
2. ActivityManagerService首先将目标Activity的信息保存下来，然后再向源Activity发送一个通知源Activity进入终止状态的进程间通信请求START_ACTIVITY_TRANSACTION。
3. 源Activity进入终止状态后，再向ActivityManagerService发送一个源Activity已经进入终止状态的进程通信请求SCHEDULE_PAUSE_ACTIVITY_TRANSACTION，以便ActivityManagerService进一步执行目标Activity启动操作。
4. ActivityManagerService发现运行目标Activity组件的应用进程并不存在，它会先去启动一个新的应用进程。
5. 新的应用进程创建完成后，会向ActivityManagerService发送一个新进程创建完成的进程通信请求ATTACH_APPLICATION_TRANSACTION，以便ActivityManagerService进一步执行目标Activity的启动操作。
6. ActivityManagerService将目标Activity的信息发送给新创建的进程，新进程执行目标Activity的创建操作。

从上面可以看出，三种情况下的Activity的启动流程大同小异，好了，我们下一篇文章进入正式的源码分析吧。

>注：分析的过程中，会牵扯任务、应用进程、消息循环、Binder进程通信等方面内容，这些内容我们暂时先不讨论，后面会有文章详尽地去分析这些内容，本次文章的重点在于讨论Activity的启动流程。

讲到这里，你也许发现我只是在梳理流程，并没有提到源码，因为源码调用链实在是太长了，直接贴在这里影响阅读体验，如果你有足够的耐心，可以去[附录：Activity启动流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/附录：Activity启动流程.md)查看，一共有
35个方法，略长😎。
