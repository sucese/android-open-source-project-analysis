# Android应用优化：内存优化

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

提到内存优化一块，最大问题就是OOM：

- 无处不在的OOM。
- 明明还有剩余内存，但是应用还是因为OOM而crash了。
- 崩溃率居高不下。
- 不断扩展的业务需求。


## 一 内存使用检测

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

## 二 内存泄漏检测

Java虽然自带垃圾回收机制，但是错误的编码方式会导致引用无法被回收，从而引发内存泄漏，甚至导致内存溢出，常见的导致内存泄漏的原因如下所示：

- 持有静态的Context（Activity）引用。
- 持有静态的View引用，
- 内部类&匿名内部类实例无法释放（有延迟时间等等），而内部类又持有外部类的强引用，导致外部类无法释放，这种匿名内部类常见于监听器、Handler、Thread、TimerTask
- 资源使用完成后没有关闭，例如：BraodcastReceiver，ContentObserver，File，Cursor，Stream，Bitmap。
- 不正确的单例模式，比如单例持有Activity。
- 集合类内存泄漏，如果一个集合类是静态的（缓存HashMap），只有添加方法，没有对应的删除方法，会导致引用无法被释放，引发内存泄漏。
- 错误的覆写了finalize()方法，finalize()方法执行执行不确定，可能会导致引用无法被释放。

内存泄漏检测可以通过leakcanary进行，如下所示：

> A memory leak detection library for Android and Java.

leakcanary：https://github.com/square/leakcanary

我们还可以通过观察Log里的GC日志来判断程序的运行情况，如下所示：

> D/dalvikvm( 745): GC_CONCURRENT freed 199K, 53% free 3023K/6343K,external 0K/0K, paused 2ms+2ms

- GC_MALLOC, 内存分配失败时触发
- GC_CONCURRENT，当分配的对象大小超过384K时 触发
- GC_EXPLICIT，对垃圾收集的显式调用(System.gc)
- GC_EXTERNAL_ALLOC，外部内存分配失败时触发

## 三 内存使用优化

内存优化在应用上主要体现在两个方面，如下所示：

- 保证应用进程的稳定性。
- 减少应用进程不必要的内存使用。

保证应用进程的稳定性就是保证系统不会kill掉我们的应用进程，最近两年一直提的进程保活实际上就是以一种黑科技的方式来保证应用的稳定性。

进程保活主要有两个思路：

1. 提升进程的优先级，降低进程被杀死的概率。
2. 拉活已经被杀死的进程。

如何提升优先级，如下所示：

> 监控手机锁屏事件，在屏幕锁屏时启动一个像素的Activity，在用户解锁时将Activity销毁掉，前台Activity可以将进程变成前台进程，优先级升级到最高。

如果拉活

> 利用广播拉活Activity。

但是这些方式都不是正途，减少应用进程不必要的内存使用才有我们优化内存的康庄大道。

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