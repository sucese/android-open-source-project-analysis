# Android应用优化：兼容适配实践指南

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

提笔（好像是键盘，不要在意这些细节🙃）写到性能优化这个专题，我的内心是复杂的，笔者的整个性能优化的经验史就是从Android入门开始一路走来摸爬滚打的过程，在性能优化这个专题上，不管是官方
还是开发者都给出了很多宝贵的实践经验，这篇文章就是结合前人的经验以及自己这几年工作的总结来写的，笔者尽可能的将原理讲的更通透，内容讲的更充实，方法讲的更有实践性。

本篇文章是以我司应用[大风车](http://dafengche.souche.com/)为样本来分析的，随着业务的发展，大风车也变成了一个巨大体量的应用。

优化原则

- 持续测量：与性能相关的数据都需要通过工具获取，用眼睛去观测不是一个好方法（这里面可能带有主观意愿），但是工具获取的数据信息却不会说谎。
- 低配设备：与性能相关的测试主要应该覆盖低配机型，这样才能把性能问题暴露的更明显。
- 权衡利弊：性能优化的问题事实上是权衡的问题，当你优化了一个东西，可能损害了另一个东西，优化也不是绝对的，要做好取舍。


性能指标

- 布局复杂度
- 耗电量
- 内存占用
- 网络流量
- 程序执行效率

## 一 启动优化

启动优化官方文档：https://developer.android.com/topic/performance/launch-time.html
启动优化视频教程：https://www.youtube.com/watch?v=Vw1G1s73DsY&index=74&list=PLWz5rJ2EKKc9CBxr3BVjPTPoDPLdPIFCE

在App优化的诸多指标中，启动速度是一项非常重要的性能指标，很难想象当你打开一个应用，它经过了一分钟还是有将首页加载出来，这是一宗多么糟糕的体验，这种
启动时间过长，很有可能导致用户不愿使用甚至卸载我们的应用。

应用按照启动场景的不同可以分为三种启动方式：

- 冷启动：冷启动指应用进程还没有创建，应用从头开始启动。
- 暖启动：暖启动指的是应用进程已经创建，但是当前页面的Activity被销毁或者还未创建，需要重新创建。
- 热启动：热启动指的是应用进程已经创建，当前页面的Activity也已经创建，驻留在内存中，只需要将来重新带到前台来即可。

我们之前的文章也有分析过，当用户点击一个桌面上的图标，启动一个未经启动过的应用，其冷启动的流程如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/cold_start_flow.png"/>

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

### 1.1 启动时间分析

App冷启动流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/cold_start_flow.png"/>

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
App刚刚启动

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/app_start_record_1.png" width="250"/>

App启动完成

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/app_start_record_2.png" width="250"/>

我们可以借助播放器的满放功能，利用右上角的时间戳逐帧的去分析整个启动流程，这样我们就可以知道哪个环节出现了性能问题。

另外，在调试冷启动时间的时候，还可以把开发者选项里的不保留后台进程打开，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/app_start_no_background_process.png" width="250"/>

### 1.2 启动时间优化

前面我们说过启动耗时的地方其实主要有个两个地方：

- Application的onCreate()方法
- MainActivity的onCreate()方法

优化的手段也无非三种，如下所示：

- 延迟初始化
- 后台任务
- 界面预加载

#### 延迟初始化

延迟初始化主要指的是在Application的onCreate()方法里执行的组件库的初始化工作，这里可以仔细梳理一遍，哪些是一定要当前初始化完成的，哪些是可以放在后台任务里懒加载的。

#### 后台任务

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


#### 界面预加载

界面预加载主要指的是MainActivity的预加载问题，现在的应用一般都会有个闪屏页，闪屏页一般有个2s左右的广告时间，这个就是可以利用的黄金时间，可以利用它做MainActivity，利用首页的UI逻辑不要
过于负责，利用TraceView和Systrace做好耗时组件和方法的分析与优化，另外，如果主页是通过ViewPager实现的，要做好ViewPager的懒加载。

另外，还可以给MainActivity设置一个windowBackground，免得MainActivity在加载的时候一直白屏。

👉 注：关于TraceView和Systrace等工具的使用请参见附录。

## 二 界面优化

### 2.1 卡顿检测

我们可以利用BlockCanary去检查造成UI卡顿的地方，如下所示：

BlockCanary：https://github.com/markzhai/AndroidPerformanceMonitor

BlockCanary检查UI卡顿的原理如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/blockcanary_structure.png"/>

### 2.2 卡顿优化

Android界面优化主要解决界面卡顿的问题，Android系统每隔16ms就会发送一个VSYNC信号，触发UI渲染，如果绘制操作超过了16ms，就会引起掉帧，也就是会导致姐们卡顿。

导致界面卡顿的原因主要是过度绘制，绘制了多余的UI，开发者选项里有检测过度绘制的工具，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/overdraw_level.png" width="250"/>

1. 移除不必要的backgroud。
2. 自定义View的时候clipReact减少重叠区域的绘制。
3. 利用<merge>等标签减少View的层级。
4. 利用<ViewStub>在需要的时候再去加载View。

## 三 内存优化

Java虽然自带垃圾回收机制，但是错误的编码方式会导致引用无法被回收，从而引发内存泄漏，甚至导致内存溢出，常见的导致内存泄漏的原因如下所示：

- 持有静态的Context（Activity）引用。
- 持有静态的View引用，
- 内部类&匿名内部类实例无法释放（有延迟时间等等），而内部类又持有外部类的强引用，导致外部类无法释放，这种匿名内部类常见于监听器、Handler、Thread、TimerTask
- 资源使用完成后没有关闭，例如：BraodcastReceiver，ContentObserver，File，Cursor，Stream，Bitmap。
- 不正确的单例模式，比如单例持有Activity。
- 集合类内存泄漏，如果一个集合类是静态的（缓存HashMap），只有添加方法，没有对应的删除方法，会导致引用无法被释放，引发内存泄漏。
- 错误的覆写了finalize()方法，finalize()方法执行执行不确定，可能会导致引用无法被释放。

### 3.1 内存使用检测

每个App的堆内存大小是有限制的，我们可以调用系统的getMemoryInfo()来获取当前内存的使用情况，如下所示：

```java
ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
activityManager.getMemoryInfo(memoryInfo);
LogUtil.d("totalMem=" + memoryInfo.totalMem + ",availMem=" + memoryInfo.availMem);
if (!memoryInfo.lowMemory) {
    // 运行在低内存环境
}
```

关于内存使用情况以及内存泄漏分析可以使用Android Studio 的 Memory Profiler工具，具体使用方法见附录。

### 3.2 内存使用优化

1. 使用LeakCanary监测内存泄漏。

>LeakCanary的原理是监控每个activity，在activity ondestory后，在后台线程检测引用，然后过一段时间进行gc，gc后如果引用还在，那么dump出内存堆栈，并解析进行可视化显示。

2. 图片是Android里的内存占用大户，关于图片的使用一方面是图片缓存，另一方面是图片压缩。

- 图片缓存：https://github.com/facebook/fresco。

关于Fresco图片缓存的原理，可以参考我们之前写的这篇文章[04Android开源框架源码鉴赏：Fresco](https://github.com/guoxiaoxing/android-open-framwork-analysis/blob/master/doc/源码分析/04Android开源框架源码鉴赏：Fresco.md)。

- 图片压缩：https://github.com/Curzibn/Luban

关于Luban图片压缩的原理，可以参考我们之前写的这篇文章[Android平台图像压缩方案](https://github.com/guoxiaoxing/phoenix/blob/master/doc/Android平台图像压缩方案.md)。

3. 避免频繁的创建小对象，避免在循环中创建临时对象，例如大量的字符串拼接。因为大量创建小对象，会造成内存频繁的分配和回收（内存抖动），形成大量内存碎片，因为内存碎片不连续，无法直接分配，所以可能会导致OOM。

4. 当App切换到后台以后，应该停止一些不是必须要运行的服务，我们一般可以使用 JobScheduler 来实现后台任务，如果必须要使用服务，则应该使用 IntentService ， IntentService 
在处理完所有请求之后会自动停止，而不是会一直运行下去。

5. 使用优化的数据集合 SparseArray / SparseBooleanArray / LongSparseArray 代替常规的HashMap，HashMap为每个映射都单独创建一个对象，内存效率低下。

6. 注意控制代码量，减少无用代码，特别是对于一些庞大的第三方库，能少用则尽量善用，DEX文件加载到内存中也是需要占用不少内存的。

7. 界面不可及的时候释放一些资源，在页面里覆写onTrimMemory()方法，根据不同的情况进行不同的处理，如下所示：

```java
public class MainActivity extends AppCompatActivity
    implements ComponentCallbacks2 {
    // Other activity code ...
    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that was raised.
     */
    public void onTrimMemory(int level) {
        // Determine which lifecycle or system event was raised.
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                /*
                   Release any UI objects that currently hold memory.
                   The user interface has moved to the background.
                */
                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                /*
                   Release any memory that your app doesn't need to run.
                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
                break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                /*
                   Release as much memory as the process can.
                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
                break;
            default:
                /*
                  Release any non-critical data structures.
                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }
}
```

## 四 并发优化

AsyncTask

HandlerThread

ThreadPool

IntentService

## 五 电量优化

### 5.1 电量消耗统计

### 5.2 电量消耗优化

## 六 网络优化

网络优化主要涉及四个方面的内容，如下所示：

- 数据缓存
- 断点续传
- 流量优化
- 局部刷新

## 七 图像优化

webp

jpeg

png

## 附录

这里提供一下各种性能分析工具的使用方法。

### Android Studio Profiler

Android Studio 3.0 提供了全新的Profiler工具来分析应用的CPU、内存和网络的使用情况，可以跟踪函数来记录代码的执行时间，采集堆栈数据，查看内存分配以及查看网络状态等，功能
十分强大。

Android Studio Profiler 官方文档：https://developer.android.com/studio/profile/android-profiler.html

它的界面构造图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/android_studio_profile_structure.png"/>

CPU、内存和网络数据的展示都是通过Event时间线实时展示的，如果你想查看某个指标的详情，只需点击当前图表即可，如下所示：

CPU分析器

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/android_studio_profile_cpu.png"/>

内存分析器

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/android_studio_profile_memory.png"/>

网络分析器

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/android_studio_profile_network.png"/>

#### CPU分析器

> CPU分析器可以帮助我们实时的检查应用的CPU使用率，可以跟踪记录函数，帮助我们调试和优化应用代码，降低CPU使用率可以获得更加流畅的用户体验，延长电池续航，还可以
让我们的应用在一些旧设备上依然保持良好的性能。

CPU分析器界面如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/android_studio_cpu_prefiler.png"/>

我们来讲一下上述小红圈数字代表的含义：

- 红圈1：显示应用中在其生命周期不同状态间转换的Activity，而且包含了用户与设备交互的各种Event，例如：屏幕旋转Event。
- 红圈2：CPU时间线，实时显示当前应用的CPU的使用率和总线程数，以及其他进程的CPU使用率。
- 红圈3：线程时间线，不同的颜色代表不同的含义，绿色代表当前线程处于活动状态或者准备使用CPU（运行中，可运行），黄色代表线程处于活动
状态，但它正在等待一个IO操作，然后才能完成它的工作，灰色代表线程正在休眠状态或者没有消耗任何CPU时间，当线程需要访问可用资源的时候会
发生这种情况。
- 红圈4：函数跟踪配置，默认有两种配置，Sampled在应用执行期间捕获调用栈，这种配置下如果在捕获调用栈的时候进入了一个函数，在结束之前
退出了该函数，则跟踪器不会记录该函数。Instrumented会在应用执行期间给每个函数打上开始和结束的时间戳，记录每个函数的时间信息和CPU使用率。除此之外，我们
还可以自定义配置。
- 红圈5：点击开始跟踪函数调用，再次点击结束函数调用。

我们来看看如何去跟踪函数调用栈，当点击跟踪按钮就可以开始跟踪，再次点击结束跟踪，跳出以下界面：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/android_studio_cpu_prefiler.png"/>

- 红圈1：时间范围，用以选择跟踪的时间范围。
- 红圈2：时间戳，记录开始跟踪和结束跟踪的时间戳。
- 红圈3：跟踪窗口，显示具体的跟踪信息。
- 红圈4：以图表或者调用链的的形式显示跟踪信息，有Call Chart、Flame Chart、Top Down和Bottom Up四种。
- 红圈5：函数消耗的时间，有两种，Wall clock time表示实际经过的时间，Thread time表示Wall clock time减去线程没有消耗CPU的部分时间，即得出的是真正占用CPU的时间。

根据数据可以用图表或者调用链来表示，如下所示：

Call Chart：提供函数跟踪的图表表示形式，水平轴表示函数调用的时间段和时间，并妍垂直轴显示其被调用者，橙色表示系统API，绿色表示应用API，蓝色表示第三方API（包括Java API）。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/call_chart.png"/>

Flame Chart：提供了一个倒置的Call Chart，功能和Call Chart相同。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/flame_chart.png"/>

Top Down：展示了一个函数调用列表，它是一个树型结构。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/bottom_up.png"/>

Bottom Up：展示了一个函数调用列表，它按照CPU消耗时间的最多（或者最少）来排序函数。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/android_studio_cpu_prefiler.png"/>

除此之外，我们也可以通过 Recording Configurations 自定义跟踪配置。

#### 内存分析器

内存分析器可以用来实时展示各种内存使用的情况以及GC的情况等。

内存分析器界面如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/memory_profiler.png"/>

- 红圈1：强制执行GC。
- 红圈2：捕获堆转储备。
- 红圈3：跟踪内存的分配情况。
- 红圈4：放大/缩小时间线
- 红圈5：开启实时内存预览。
- 红圈6：Event时间线
- 红圈7：内存使用时间线
- 红圈1：

整个界面实时显示各种内存的使用情况：

- Java：从 Java 或 Kotlin 代码分配的对象内存。
- Native：从 C 或 C++ 代码分配的对象内存。
- Graphics：图形缓冲区队列向屏幕显示像素（包括 GL 表面、GL 纹理等等）所使用的内存。 （请注意，这是与 CPU 共享的内存，不是 GPU 专用内存。）
- Stack： 应用中的原生堆栈和 Java 堆栈使用的内存。 这通常与您的应用运行多少线程有关。
- Code：应用用于处理代码和资源（如 dex 字节码、已优化或已编译的 dex 码、.so 库和字体）的内存。
- Other：应用使用的系统不确定如何分类的内存。
- Allocated：应用分配的 Java/Kotlin 对象数。 它没有计入 C 或 C++ 中分配的对象。

内存分析器也可以针对函数对内存的使用情况进行跟踪，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/memory_profiler_record.png"/>                 

我们还可以点击上面的dunp java heap按钮来捕获堆转储，来帮助我们分析内存分配和内存泄漏相关信息，如下所示：

在类列表中，我们可以查看以下信息：

- Heap Count：堆中的实例数。
- Shallow Size：此堆中所有实例的总大小（以字节为单位）。
- Retained Size：为此类的所有实例而保留的内存总大小（以字节为单位）。

在类列表顶部，我们可以使用左侧下拉列表在以下堆转储之间进行切换：

- Default heap：系统未指定堆时。
- App heap：您的应用在其中分配内存的主堆。
- Image heap：系统启动映像，包含启动期间预加载的类。 此处的分配保证绝不会移动或消失。
- Zygote heap：写时复制堆，其中的应用进程是从 Android 系统中派生的。

默认情况下，此堆中的对象列表按类名称排列。 我们可以使用其他下拉列表在以下排列方式之间进行切换：

- Arrange by class：基于类名称对所有分配进行分组。
- Arrange by package：基于软件包名称对所有分配进行分组。
- Arrange by callstack：将所有分配分组到其对应的调用堆栈。 此选项仅在记录分配期间捕获堆转储时才有效。 即使如此，堆中的对象也很可能是在您开始记录之前分配的，因此这些分配会首先显示，且只按类名称列出。

默认情况下，此列表按 Retained Size 列排序。 您可以点击任意列标题以更改列表的排序方式。

在 Instance View 中，每个实例都包含以下信息：

Depth：从任意 GC 根到所选实例的最短 hop 数。
Shallow Size：此实例的大小。
Retained Size：此实例支配的内存大小（根据 dominator 树）。

另外，堆转储信息还可以被到处成文件，点击Export heap dump as HPROF file按钮可以将堆转储信息导出成HPROF文件，但是如果我们想要用其他工具（例如：MAT）分析HPROF文件，还要将其
转换成Java SE的HPROF文件，如下所示：

```java
hprof-conv heap-original.hprof heap-converted.hprof
```

除此之外我们还可以调用以下方法在代码里创建堆转储信息，如下所示：

```java
Debug.dumpHprofData() 
```

#### 网络分析器

网络分析器就比较简单了，用来实时显示网络请求的情况，网络的速度，接收和发出的数据量等信息，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/network_profiler.png"/>    

- 红圈1：无线功耗状态（低/高）
- 红圈2：时间线
- 红圈3：指定时间段段内收发的文件名称、大小、类型、状态和时间。
- 红圈4：文件详细信息


### Systrace

Systrace

Systrace 官方文档：https://developer.android.com/studio/command-line/systrace.html

### TraceView

TraceView可以用图形的形式来展示Trace Log，展示代码的执行时间、次数以及调用栈，便于我们分析。

TraceView 官方文档：https://developer.android.com/studio/profile/traceview.html

如何为应用生成跟踪日志呢，也很简单，如下所示：

```java
// 在开始跟踪的地方调用该方法
Debug.startMethodTracing();

// 在结束跟踪的地方调用该方法
Debug.startMethodTracing();
```

Trace文件一般放在sdcard/Android/data/包名目录下，如下所示：

双击即可打开，如下所示：

