# Android组件框架：Android视图容器Activity

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 Activity的启动流程
- 二 Activity的回退栈
- 三 Activity的生命周期
- 四 Activity的启动模式

本篇文章我们来分析Android的视图容器Activity，这也是我们在日常开发中接触的最多的一个组件，Android源码分析系列的文章终于写到了Activity，这个我们最常用源码也最复杂的一个组件，之前在网上
看到过很多关于Activity源码分析的文章，这些文章写得都挺好，它们往往是从Activity启动流程这个角度出发，一个一个函数的去分析整个流程。但是这种做法会给文章通篇看去全是源码，而且会让读者产生
一个疑问，这么长的流程，我该如何掌握，掌握了之后有有什么意义吗？🤔

事实上，单纯去分析流程，确实看不出有什么实践意义，因此我们最好能带着日常开发遇到的问题去看源码，例如特殊场景下的生命周期是怎么变化的，为什么会出现ANR，不同启动模式下对Activity入栈出栈有何影响等。
我们带着问题去看看源码里是怎么写的，这样更有目的性，不至于迷失在茫茫多的源码中。

好了，闲话不多说，我们开始吧。😁

Activity作为Android最为常用的组件，它的复杂程度是不言而喻的。当我们点击一个应用的图标，应用的LancherActivity（MainActivity）开始启动，伴随着IntentFilter的
匹配，Activity的生命周期从onCreate()方法开始变化，最终将界面呈现在用户的面前。

Activity的复杂性主要体现在两个方面：

- 复杂的启动流程，超长的函数调用链。
- 启动模式、Flag以及各种场景对Activity生命周期的影响。

针对这些问题，我们来一一分析。

我们先来分析Activity启动流程，对Activity组件有个整体性的认识。

## 一 Activity的启动流程

Activity的启动流程图（放大可查看）如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_start_flow.png" />

主要角色有：

- Instrumentation: 监控应用与系统相关的交互行为。
- AMS：组件管理调度中心，什么都不干，但是什么都管。
- ActivityStarter：处理Activity什么时候启动，怎么样启动相关问题，也就是处理Intent与Flag相关问题，平时提到的启动模式都可以在这里找到实现。
- ActivityStackSupervisior：这个类的作用你从它的名字就可以看出来，它用来管理Stack和Task。
- ActivityStack：用来管理栈里的Activity。
- ActivityThread：最终干活的人，是ActivityThread的内部类，Activity、Service、BroadcastReceiver的启动、切换、调度等各种操作都在这个类里完成。

通过上面的流程图整个流程可以概括如下：

>Activity的启动请求由Activity发送，以Binder通信的方式发送给了AMS，AMS接收到启动请求后，交付ActivityStarter处理Intent和Flag等信息，然后再交给ActivityStackSupervisior/ActivityStack
处理Activity进栈相关流程，最后交付ActivityThread利用ClassLoader去加载Activity、创建Activity实例，并回调Activity的onCreate()方法。这样便完成了Activity的启动。


## 二 Activity的回退栈

Activity的回退栈是由ActivityStack与ActivityStackSupervisior来管理，具体说来：

- ActivityStackSupervisior：这个类的作用你从它的名字就可以看出来，它用来管理Stack和Task。
- ActivityStack：用来管理栈里的Activity。

在理解这两个类的作用之前，我们要先理解连个数据类：



### 2.1 Activity栈

Activity栈是由ActivityStackSupervisior来完成的，

- ActivityStack：描述栈的状态和相关操作。
- ActivityRecord：描述栈里的Activity相关信息。

关于ActivityStack

关于ActivityRecord

```java
final class ActivityRecord {
    
     final ActivityManagerService service; // owner
     final IApplicationToken.Stub appToken; // window manager token
     final ActivityInfo info; // 包含了这个Activity的所有信息（AndroidManifest.xml里activity标签里定义的和代码里定义的）
     final ApplicationInfo appInfo; // 当前应用的所有信息（AndroidManifest.xml里application标签里定义的）
     final int launchedFromUid; // always the uid who started the activity.
     final String launchedFromPackage; // 启动当前Activity的包名
     
     final Intent intent;    // the original intent that generated us

     final String packageName; // Intent信息里的componentName
     final String processName; // 当前组件所在的进程名
     final String taskAffinity; // 和 ActivityInfo.taskAffinity相同，

     TaskRecord task;        // 当前Activity所在的task

     ActivityRecord resultTo; // 启动当前Activity的目标Activity，目标Activity会收到当前Activity返回的结果
     final String resultWho; // additional identifier for use by resultTo.
     final int requestCode;  // code given by requester (resultTo)
   
     ProcessRecord app;      // if non-null, hosting application
     ActivityState state;    // Activity当前的状态

     int launchMode;         // 启动模式
     final ActivityStackSupervisor mStackSupervisor;//栈管理器
}
```

Activity在栈里有种状态：

- INITIALIZING：初始化
- RESUMED：已显示
- PAUSING：暂停中
- PAUSED：已暂停
- STOPPING：停止中
- STOPPED：已停止
- FINISHING：结束中
- DESTROYING：销毁中
- DESTROYE：已销毁

## 三 Activity的生命周期

Activity与Fragment生命周期图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/complete_android_fragment_lifecycle.png"/>

注：该图出自项目[android-lifecycle](https://github.com/xxv/android-lifecycle)。

onCreate

onAttachFragment

onContentChanged

onStart

onRestoreInstanceState

onPostCreate

onResume

onPostResume

onAccachedToWindow

onCreateOptionsMenu

onPause

onSaveInstanceState

onStop

onDestory

## 四 Activity的启动模式

说起Activity的启动模式，可能是一个老生常谈的问题，很多文章也分析过，但如果不是阅读过源码或者有着很多的实践，总会有种云里雾里的感觉。

>启动模式会影响Activity的启动行为，默认情况下，启动一个Activity就是创建一个实例，然后进入回退栈，但是我们可以通过启动模式来改变这种行为，实现不同的交互效果。

启动模式可以在xml文件里定义

```xml
android:launchMode="singleTop"
```

也可以在代码里指定

```java
Intent intent = new Intent(StartActivity.this, SubInNewProcessActivity.class);
intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
startActivity(intent);
```

启动模式一共有四种：

- standard
- singleTop
- singleTask
- singleInstance