# Android应用优化：启动优化

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

- 启动优化官方文档：https://developer.android.com/topic/performance/launch-time.html
- 启动优化视频教程：https://www.youtube.com/watch?v=Vw1G1s73DsY&index=74&list=PLWz5rJ2EKKc9CBxr3BVjPTPoDPLdPIFCE

在App优化的诸多指标中，启动速度是一项非常重要的性能指标，很难想象当你打开一个应用，它经过了一分钟还是有将首页加载出来，这是一宗多么糟糕的体验，这种
启动时间过长，很有可能导致用户不愿使用甚至卸载我们的应用。

应用按照启动场景的不同可以分为三种启动方式：

- 冷启动：冷启动指应用进程还没有创建，应用从头开始启动。
- 暖启动：暖启动指的是应用进程已经创建，但是当前页面的Activity被销毁或者还未创建，需要重新创建。
- 热启动：热启动指的是应用进程已经创建，当前页面的Activity也已经创建，驻留在内存中，只需要将来重新带到前台来即可。

我们之前的文章也有分析过，当用户点击一个桌面上的图标，启动一个未经启动过的应用，其冷启动的流程如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/app_start_structure.png"/>

1. 加载和启动应用程序。
2. 启动后立即显示应用程序的空白开始窗口。
3. 创建应用程序进程。
4. 创建应用程序对象。
5. 启动主线程。
6. 创建MainActivity。
7. 加载布局文件。
8. 铺设屏幕。
9. 执行初始绘制。

其中创建MainActivity的时间也是冷启动的时间之一，MainActivity的创建主要包含以下步骤：

1. 初始化成员变量。
2. 调用构造函数。
3. 调用回调方法，例如：onCreate()，其中onCreate()往往是占用最多时间的那个方法。

当应用完成第一次绘制以后，系统就会将当前显示的背景窗口替换为NainActivity，用户就看到了应用的首页，一个应用的冷启动流程也就算完成了。

通常来说应用的启动优化的重点就在于冷启动的优化上，暖启动和热启动它们的优化涉及到具体的页面，在做这些页面时做好缓存，减少不必要的创建工作和网络请求都是常用的优化手段，我们重点来看看
冷启动的优化。

首先，要学会如何计算冷启动时间，找出冷启动的性能瓶颈。

## 一 启动时间分析

从上面启动图可以看出启动时间分为两个部分，系统创建进程的时间和应用进程启动的时间，前者是由系统自行完成的，一般都会很快，我们也干预不了，我们能做的就是去优化应用进程启动，具体说来就是
从发Application的onCreate()执行开始到MainActivity的onCreate()执行结束这一段时间。

那么如何计算这个启动时间呢，API 19 之后系统会给一个启动时间Displayed Time的Log，如下所示：

```java
Displayed com.guoxiaoxing.android.framework.demo/.MainActivity: +557ms
```

入上图所示，这个Displayed Time的Log只是布局加载完成显示的时间，如果我们在MainActivity做了一些数据的加载，时间就不止这么多了，API 19 以后系统为我们提供了一个自定义上报时间的工具，如下所示：

```java
try{
    // 模拟数据加载
    Thread.sleep(2000);
    // 上报启动时间，这个方法在API 19 以下不支持，会crash，所以调用
    // 的时候做一下try catch
    reportFullyDrawn();
}catch(Exception e){
    e.printStackTrace();
}
```

然后你就可以看到一条这样的Log，如下所示：

```java
Fully drawn com.guoxiaoxing.android.framework.demo/.MainActivity: +2s557ms
```

此外，我们还可以通过ADB命令测量启动时间，如下所示：

```java
// 启动首页MainActivity
adb shell am start -W com.guoxiaoxing.android.framework.demo/.MainActivity
```

该命令会打印以下Log：

```java
Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] cmp=com.guoxiaoxing.android.framework.demo/.MainActivity }
Status: ok
Activity: com.guoxiaoxing.android.framework.demo/.MainActivity
ThisTime: 2658
TotalTime: 2658
WaitTime: 2697
Complete
```

以上三个时间的概念如下所示：

- ThisTime: 最后一个Activity的启动耗时。
- TotalTime: 自己所有Activity的启动耗时。
- WaitTime: ActivityManagerService启动Activity的总耗时（包括当前Activity的onPause()和自己Activity的启动时间）

处理上述两种方法外，还有一个录屏法，如下所示：

```java
adb shell screenrecord --bugreport /sdcard/test.mp4
```
App开始启动到App启动完成如下所示：

<p>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/app_start_record_1.png" width="250"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/app_start_record_2.png" width="250"/>
<p/>

我们可以借助播放器的满放功能，利用右上角的时间戳逐帧的去分析整个启动流程，这样我们就可以知道哪个环节出现了性能问题。

另外，在调试冷启动时间的时候，还可以把开发者选项里的不保留后台进程打开，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/app_start_no_background_process.png" width="250"/>

## 二 启动时间优化

前面我们说过启动耗时的地方其实主要有个两个地方：

- Application的onCreate()方法
- MainActivity的onCreate()方法

优化的手段也无非三种，如下所示：

- 延迟初始化
- 后台任务
- 界面预加载

### 2.1 延迟初始化

延迟初始化主要指的是在Application的onCreate()方法里执行的组件库的初始化工作，这里可以仔细梳理一遍，哪些是一定要当前初始化完成的，哪些是可以放在后台任务里懒加载的。

### 2.2 后台任务

后台任务指的是将一些耗时操作添加到后台执行，减少对界面的阻塞，后台任务的实现也有很多种，如下所示：

方式一：ContentView是通过mDecorView.addView()添加到根布局的，通过post方式可以将一些工作推迟到布局完成完成之后进行。

```java
getWindow().getDecorView().post(new Runnable() {

  @Override public void run() {
     // TODO do some work
  }
});
```
方式二：IntentService执行后台任务

```java
public class InitIntentService extends IntentService {

    private static final String ACTION = "com.guoxiaoxing.framework.action";

    public InitIntentService() {
        super("InitIntentService");
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, InitIntentService.class);
        intent.setAction(ACTION);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
         // TODO do some work
    }
```

### 2.3 界面预加载

界面预加载主要指的是MainActivity的预加载问题，现在的应用一般都会有个闪屏页，闪屏页一般有个2s左右的广告时间，这个就是可以利用的黄金时间，可以利用它做MainActivity，利用首页的UI逻辑不要
过于负责，利用TraceView和Systrace做好耗时组件和方法的分析与优化，另外，如果主页是通过ViewPager实现的，要做好ViewPager的懒加载。

另外，还可以给MainActivity设置一个windowBackground，免得MainActivity在加载的时候一直白屏。
