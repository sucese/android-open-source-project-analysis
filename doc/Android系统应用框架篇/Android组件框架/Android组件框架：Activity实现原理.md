# Android系统应用框架篇：Activity基础知识

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

在正式介绍Activity源码结构之前，我们先来回忆一下Activity相关的基础知识。

## Activity生命周期

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

Activity与Fragment生命周期对比图:

![](https://github.com/YannanGuo/android-advanced-learning-route/blob/master/doc/Android%E5%88%9D%E7%B%84%E4%BB%B6%E5%9F%BA%E7%A1%80%E7%90%86%E8%AE%BA/art/complete_android_fragment_lifecycle.png)A%A7%E5%86%85%E5%AE%B9/Android%E5%9B%9B%E5%A4%A7%E7%BB

[点击查看高清SVG大图](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/1/UMLClassDiagram-app-ActivityGroup.png)

## Activity启动模式


## Activity启动标识


### FLAG_ACTIVITY_BROUGHT_TO_FRONT 

```
这个标志一般不是由程序代码设置的，如在launchMode中设置singleTask模式时系统帮你设定。
```

### FLAG_ACTIVITY_CLEAR_TOP

```
如果设置，并且这个Activity已经在当前的Task中运行，因此，不再是重新启动一个这个Activity的实例，而是在这个Activity上方的所有Activity都将关闭，然后这个Intent会作为一个新的Intent投递到老
的Activity（现在位于顶端）中。
```
例如:

假设一个Task中包含这些Activity：A，B，C，D。如果D调用了startActivity()，并且包含一个指向Activity B的Intent，那么，C和D都将结束，然后B接收到这个Intent，因此，目前stack的状况是：A，B。
上例中正在运行的Activity B既可以在onNewIntent()中接收到这个新的Intent，也可以把自己关闭然后重新启动来接收这个Intent。如果它的启动模式声明为 “multiple”(默认值)，并且你没有在这个Intent中
设置FLAG_ACTIVITY_SINGLE_TOP标志，那么它将关闭然后重新创建；对于其它的启动模式，或者在这个Intent中设置FLAG_ACTIVITY_SINGLE_TOP标志，都将把这个Intent投递到当前这个实例的onNewIntent()中。
这个启动模式还可以与FLAG_ACTIVITY_NEW_TASK结合起来使用：用于启动一个Task中的根Activity，它会把那个Task中任何运行的实例带入前台，然后清除它直到根Activity。这非常有用，例如，当从Notification Manager处
启动一个Activity。

### FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET

```
如果设置，这将在Task的Activity stack中设置一个还原点，当Task恢复时，需要清理Activity。也就是说，下一次Task带着 FLAG_ACTIVITY_RESET_TASK_IF_NEEDED标记进入前台时（典型的操作是用户在主画
面重启它），这个Activity和它之上的都将关闭，以至于用户不能再返回到它们，但是可以回到之前的Activity。
```
这在你的程序有分割点的时候很有用。例如，一个e-mail应用程序可能有一个操作是查看一个附件，需要启动图片浏览Activity来显示。这个Activity应该作为e-mail应用程序Task的一部分，因为这是用户在这个Task中触发的
操作。然而，当用户离开这个Task，然后从主画面选择e-mail app，我们可能希望回到查看的会话中，但不是查看图片附件，因为这让人困惑。通过在启动图片浏览时设定这个标志，浏览及其它启动的Activity在下次用户返回到mail程
序时都将全部清除。

### FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

```
如果设置，新的Activity不会在最近启动的Activity的列表中保存。
```

### FLAG_ACTIVITY_FORWARD_RESULT

```
如果设置，并且这个Intent用于从一个存在的Activity启动一个新的Activity，那么，这个作为答复目标的Activity将会传到这个新的Activity中。这种方式下，新的Activity可以调用setResult(int)，并且这个结果值将发送给那
个作为答复目标的 Activity。
```

### FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY 

```
这个标志一般不由应用程序代码设置，如果这个Activity是从历史记录里启动的（常按HOME键），那么，系统会帮你设定。
```

### FLAG_ACTIVITY_MULTIPLE_TASK 

```
不要使用这个标志，除非你自己实现了应用程序启动器。与FLAG_ACTIVITY_NEW_TASK结合起来使用，可以禁用把已存的Task送入前台的行为。当设置时，新的Task总是会启动来处理Intent，而不管这是是否已经有一个Task可
以处理相同的事情。 由于默认的系统不包含图形Task管理功能，因此，你不应该使用这个标志，除非你提供给用户一种方式可以返回到已经启动的Task。如果FLAG_ACTIVITY_NEW_TASK标志没有设置，这个标志被忽略。
```

### FLAG_ACTIVITY_NEW_TASK 

```
如果设置，这个Activity会成为历史stack中一个新Task的开始。一个Task（从启动它的Activity到下一个Task中的 Activity）定义了用户可以迁移的Activity原子组。Task可以移动到前台和后台；在某个特定Task中的所有
Activity总是保持相同的次序。这个标志一般用于呈现“启动”类型的行为：它们提供用户一系列可以单独完成的事情，与启动它们的Activity完全无关。使用这个标志，如果正在启动的Activity的Task已经在运行的话，那么，新的
Activity将不会启动；代替的，当前Task会简单的移入前台。参考FLAG_ACTIVITY_MULTIPLE_TASK标志，可以禁用这一行为。这个标志不能用于调用方对已经启动的Activity请求结果。
```

### FLAG_ACTIVITY_NO_ANIMATION 

```
如果在Intent中设置，并传递给Context.startActivity()的话，这个标志将阻止系统进入下一个Activity时应用 Acitivity迁移动画。这并不意味着动画将永不运行——如果另一个Activity在启动显示之前，没有指定这个标
志，那么，动画将被应用。这个标志可以很好的用于执行一连串的操作，而动画被看作是更高一级的事件的驱动。
```

### FLAG_ACTIVITY_NO_HISTORY 

```
如果设置，新的Activity将不再历史stack中保留。用户一离开它，这个Activity就关闭了。这也可以通过设置noHistory特性。
```

### FLAG_ACTIVITY_NO_USER_ACTION 

```
如果设置，作为新启动的Activity进入前台时，这个标志将在Activity暂停之前阻止从最前方的Activity回调的onUserLeaveHint()。
典型的，一个Activity可以依赖这个回调指明显式的用户动作引起的Activity移出后台。这个回调在Activity的生命周期中标记一个合适的点，并关闭一些Notification。
如果一个Activity通过非用户驱动的事件，如来电或闹钟，启动的，这个标志也应该传递给Context.startActivity，保证暂停的Activity不认为用户已经知晓其Notification。
```

### FLAG_ACTIVITY_PREVIOUS_IS_TOP 

```
If set and this intent is being used to launch a new activity from an existing one, the current activity will not be counted as the top activity for deciding whether the new 
intent should be delivered to the top instead of starting a new one. The previous activity will be used as the top, with the assumption being that the current activity will 
finish itself immediately. 
```

### FLAG_ACTIVITY_REORDER_TO_FRONT

```
如果在Intent中设置，并传递给Context.startActivity()，这个标志将引发已经运行的Activity移动到历史stack的顶端。例如，假设一个Task由四个Activity组成：A,B,C,D。如果D调用startActivity()来启
动Activity B，那么，B会移动到历史stack的顶端，现在的次序变成A,C,D,B。如果FLAG_ACTIVITY_CLEAR_TOP标志也设置的话，那么这个标志将被忽略。
```
### FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

```
If set, and this activity is either being started in a new task or bringing to the top an existing task, then it will be launched as the front door of the task. This will 
result in the application of any affinities needed to have that task in the proper state (either moving activities to or from it), or simply resetting that task to its 
initial state if needed. 
```
### FLAG_ACTIVITY_SINGLE_TOP

```
如果设置，当这个Activity位于历史stack的顶端运行时，不再启动一个新的。 
```

注意：如果是从BroadcastReceiver启动一个新的Activity，或者是从Service往一个Activity跳转时，不要忘记添加Intent的Flag为FLAG_ACTIVITY_NEW_TASK。



以上便是Activity相关的基础知识，我们下面来看看Activity相关源码结构，为后面的源码分析准个准备

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/1/UMLClassDiagram-app-ActivityGroup.png"/>

我们来介绍下上图中主要的类

- Context：抽象类，应用的全局运行环境。
- ContextWrapper：继承于Context，Context的代理类。ContextWrapper里的方法都最终调用Context里的方法来实现。
- ContextThemeWrapper：继承于ContextWrapper，可以进行主题修改。
- Activity：继承于ContextThemeWrapper，展示在用户面前的类，绘制UI，处理用户交互。
- ActivityGroup：继承于Activity，一个屏幕可以包含多个Activity。


我们再来看看在Activity提供各种功能的内部模块。

- Instrumentation
- IBinder
- ActivityInfo
- ActivityThread
- SearchManager
- Window
- WindowManager

