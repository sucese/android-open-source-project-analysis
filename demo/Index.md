# Android 高级开发工程师面试题集

## Java

### 描述一个类的加载过程？

Person person = new Person()

1. 查找Person.class，并加载到内存中。
2. 执行类里的静态代码块。
3. 在堆内存里开辟内存空间，并分配内存地址。
4. 在堆内存里建立对象的属性，并进行默认的初始化。
5. 对属性就行显示初始化。
6. 对对象进行构造代码块初始化。
7. 调用对象的构造函数进行初始化。
8. 将对象的地址赋值给person变量。

### Java对象的生命周期是什么？

1. 加载：将类的信息加载到JVM的方法区，然后在堆区中实例化一个java.lang.Class对象，作为方法去中这个类的信息入口。
2. 连接：验证：验证类是否合法。准备：为静态变量分配内存并设置JVM默认值，非静态变量不会分配内存。解析：将常量池里的符号引用转换为直接引用。
3. 初始化：初始化类的静态赋值语句和静态代码块，主动引用会被触发类的初始化，被动引用不会触发类的初始化。
4. 使用：执行类的初始化，主动引用会被触发类的初始化，被动引用不会触发类的初始化。
5. 卸载：卸载过程就是清楚堆里类的信息，以下情况会被卸载：① 类的所有实例都已经被回收。② 类的ClassLoader被回收。③ 类的CLass对象没有被任何地方引用，无法在任何地方通过
反射访问该类。

### 描述一下类的加载机制？

>类的加载就是虚拟机通过一个类的全限定名来获取描述此类的二进制字节流，而完成这个加载动作的就是类加载器。

类和类加载器息息相关，判定两个类是否相等，只有在这两个类被同一个类加载器加载的情况下才有意义，否则即便是两个类来自同一个Class文件，被不同类加载器加载，它们也是不相等的。

注：这里的相等性保函Class对象的equals()方法、isAssignableFrom()方法、isInstance()方法的返回结果以及Instance关键字对对象所属关系的判定结果等。

类加载器可以分为三类：

- 启动类加载器（Bootstrap ClassLoader）：负责加载<JAVA_HOME>\lib目录下或者被-Xbootclasspath参数所指定的路径的，并且是被虚拟机所识别的库到内存中。
- 扩展类加载器（Extension ClassLoader）：负责加载<JAVA_HOME>\lib\ext目录下或者被java.ext.dirs系统变量所指定的路径的所有类库到内存中。
- 应用类加载器（Application ClassLoader）：负责加载用户类路径上的指定类库，如果应用程序中没有实现自己的类加载器，一般就是这个类加载器去加载应用程序中的类库。

这么多类加载器，那么当类在加载的时候会使用哪个加载器呢？🤔

这个时候就要提到类加载器的双亲委派模型，流程图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/classloader_model_structure.png" width="600"/>

双亲委派模型的整个工作流程非常的简单，如下所示：

>如果一个类加载器收到了加载类的请求，它不会自己立即去加载类，它会先去请求父类加载器，每个层次的类加载器都是如此。层层传递，直到传递到最高层的类加载器，只有当
父类加载器反馈自己无法加载这个类，才会有当前子类加载器去加载该类。

关于双亲委派机制，在ClassLoader源码里也可以看出，如下所示：

```java
public abstract class ClassLoader {
    
    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
            //首先，检查该类是否已经被加载
            Class c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    //先调用父类加载器去加载
                    if (parent != null) {
                        c = parent.loadClass(name, false);
                    } else {
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    //如果父类加载器没有加载到该类，则自己去执行加载
                    long t1 = System.nanoTime();
                    c = findClass(name);

                    // this is the defining class loader; record the stats
                }
            }
            return c;
    }
}
```

为什么要这么做呢？🤔

这是为了要让越基础的类由越高层的类加载器加载，例如Object类，无论哪个类加载器去尝试加载这个类，最终都会传递给最高层的类加载器去加载，前面我们也说过，类的相等性是由
类与其类加载器共同判定的，这样Object类无论在何种类加载器环境下都是同一个类。

相反如果没有双亲委派模型，那么每个类加载器都会去加载Object，那么系统中就会出现多个不同的Object类了，如此一来系统的最基础的行为也就无法保证了。

### 描述一下GC的原理和回收策略？

提到垃圾回收，我们可以先思考一下，如果我们去做垃圾回收需要解决哪些问题？ 🤔

一般说来，我们要解决一些三个问题：

- 哪些内存回收？
- 什么时候回收？
- 如何回收？

这些问题分别对应着引用管理和回收策略等方案。

提到引用，我们都知道Java中有四种引用类型：

- 强引用：代码中普遍存在的，只要强引用还存在，垃圾收集器就不会回收掉被引用的对象。
- 软引用：SoftReference，用来描述还有用但是非必须的对象，当内存不足的时候回回收这类对象。
- 弱引用：WeakReference，用来描述非必须对象，弱引用的对象只能生存到下一次GC发生时，当GC发生时，无论内存是否足够，都会回收该对象。
- 虚引用：PhantomReference，一个对象是否有虚引用的存在，完全不会对其生存时间产生影响，也无法通过虚引用取得一个对象的引用，它存在的唯一目的是在这个对象被回收时可以收到一个系统通知。

不同的引用类型，在做GC时会区别对待，我们平时生成的Java对象，默认都是强引用，也就是说只要强引用还在，GC就不会回收，那么如何判断强引用是否存在呢？🤔

一个简单的思路就是：引用计数法，有对这个对象的引用就+1，不再引用就-1，但是这种方式看起来简单美好，但它却不嫩解决循环引用计数的问题。

因此可达性分析算法登上历史舞台😎，用它来判断对象的引用是否存在。

>可达性分析算法通过一系列称为GC Roots的对象作为起始点，从这些节点从上向下搜索，搜索走过的路径称为引用链，当一个对象没有任何引用链
与GC Roots连接时就说明此对象不可用，也就是对象不可达。

GC Roots对象通常包括：

- 虚拟机栈中引用的对象（栈帧中的本地变量表）
- 方法去中类的静态属性引用的对象
- 方法区中常量引用的对象
- Native方法引用的对象

可达性分析算法整个流程如下所示：

1. 第一次标记：对象在经过可达性分析后发现没有与GC Roots有引用链，则进行第一次标记并进行一次筛选，筛选条件是：该对象是否有必要执行finalize()方法。没有覆盖finalize()方法或者finalize()方法已经被执行过都会被
认为**没有必要执行**。
    - 如果有必要执行：则该对象会被放在一个F-Queue队列，并稍后在由虚拟机建立的低优先级Finalizer线程中触发该对象的finalize()方法，但不保证一定等待它执行结束，因为如果这个对象的finalize()方法发生了死循环或者执行
    时间较长的情况，会阻塞F-Queue队列里的其他对象，影响GC。
2. 第二次标记：GC对F-Queue队列里的对象进行第二次标记，如果在第二次标记时该对象又成功被引用，则会被移除即将回收的集合，否则会被回收。

### 接口和抽象类有什么区别？

### 内部类、静态内部类在业务中的应用场景是什么？

### synchronized与ReentrantLock有什么区别？

ynchronized是互斥同步的一种实现。

>synchronized：当某个线程访问被synchronized标记的方法或代码块时，这个线程便获得了该对象的锁，其他线程暂时无法访问这个方法，只有等待这个方法执行完毕或者代码块执行完毕，这个
线程才会释放该对象的锁，其他线程才能执行这个方法或代码块。

前面我们已经说了volatile关键字，这里我们举个例子来综合分析volatile与synchronized关键字的使用。

:point_up:举个栗子

```java
public class Singleton {

    //volatile保证了：1 instance在多线程并发的可见性 2 禁止instance在操作是的指令重排序
    private volatile static Singleton instance;

    public static Singleton getInstance() {
        //第一次判空，保证不必要的同步
        if (instance == null) {
            //synchronized对Singleton加全局所，保证每次只要一个线程创建实例
            synchronized (Singleton.class) {
                //第二次判空时为了在null的情况下创建实例
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

这是一个经典的DSL单例。

它的字节码如下：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/synchronized_bytecode.png"/>

可以看到被synchronized同步的代码块，会在前后分别加上monitorenter和monitorexit，这两个字节码都需要指定加锁和解锁的对象。

关于加锁和解锁的对象：
                                               
- synchronized代码块 ：同步代码块，作用范围是整个代码块，作用对象是调用这个代码块的对象。
- synchronized方法 ：同步方法，作用范围是整个方法，作用对象是调用这个方法的对象。
- synchronized静态方法 ：同步静态方法，作用范围是整个静态方法，作用对象是调用这个类的所有对象。
- synchronized(this)：作用范围是该对象中所有被synchronized标记的变量、方法或代码块，作用对象是对象本身。
- synchronized(ClassName.class) ：作用范围是静态的方法或者静态变量，作用对象是Class对象。

synchronized(this)添加的是对象锁，synchronized(ClassName.class)添加的是类锁，它们的区别如下：

>对象锁：Java的所有对象都含有1个互斥锁，这个锁由JVM自动获取和释放。线程进入synchronized方法的时候获取该对象的锁，当然如果已经有线程获取了这个对象的锁，那么当前线
程会等待；synchronized方法正常返回或者抛异常而终止，JVM会自动释放对象锁。这里也体现了用synchronized来加锁的好处，方法抛异常的时候，锁仍然可以由JVM来自动释放。 

>类锁：对象锁是用来控制实例方法之间的同步，类锁是用来控制静态方法（或静态变量互斥体）之间的同步。其实类锁只是一个概念上的东西，并不是真实存在的，它只是用来帮助我们理
解锁定实例方法和静态方法的区别的。我们都知道，java类可能会有很多个对象，但是只有1个Class对象，也就是说类的不同实例之间共享该类的Class对象。Class对象其实也仅仅是1个
java对象，只不过有点特殊而已。由于每个java对象都有1个互斥锁，而类的静态方法是需要Class对象。所以所谓的类锁，不过是Class对象的锁而已。获取类的Class对象有好几种，最简
单的就是MyClass.class的方式。 类锁和对象锁不是同一个东西，一个是类的Class对象的锁，一个是类的实例的锁。也就是说：一个线程访问静态synchronized的时候，允许另一个线程访
问对象的实例synchronized方法。反过来也是成立的，因为他们需要的锁是不同的。

### volatile的原理是什么？

volatile也是互斥同步的一种实现，不过它非常的轻量级。

volatile有两条关键的语义：

- 保证被volatile修饰的变量对所有线程都是可见的
- 禁止进行指令重排序

要理解volatile关键字，我们得先从Java的线程模型开始说起。如图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/java_memory_model.png"/>

Java内存模型规定了所有字段（这些字段包括实例字段、静态字段等，不包括局部变量、方法参数等，因为这些是线程私有的，并不存在竞争）都存在主内存中，每个线程会
有自己的工作内存，工作内存里保存了线程所使用到的变量在主内存里的副本拷贝，线程对变量的操作只能在工作内存里进行，而不能直接读写主内存，当然不同内存之间也
无法直接访问对方的工作内存，也就是说主内存时线程传值的媒介。

我们来理解第一句话：

>保证被volatile修饰的变量对所有线程都是可见的

如何保证可见性？🤔

被volatile修饰的变量在工作内存修改后会被强制写回主内存，其他线程在使用时也会强制从主内存刷新，这样就保证了一致性。

关于“保证被volatile修饰的变量对所有线程都是可见的”，有种常见的错误理解：

>错误理解：由于volatile修饰的变量在各个线程里都是一致的，所以基于volatile变量的运算在多线程并发的情况下是安全的。

这句话的前半部分是对的，后半部分却错了，因此它忘记考虑变量的操作是否具有原子性这一问题。

:point_up:举个栗子

```java

    private volatile int start = 0;

    private void volatileKeyword() {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    start++;
                }
            }
        };

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
        Log.d(TAG, "start = " + start);
    }

```

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/volatile_thread_safe.png"/>

这段代码启动了10个线程，每次10次自增，按道理最终结果应该是100，但是结果并非如此。

为什么会这样？:thinking:

仔细看一下start++，它其实并非一个原子操作，简单来看，它有两步：

1. 取出start的值，因为有volatile的修饰，这时候的值是正确的。
2. 自增，但是自增的时候，别的线程可能已经把start加大了，这种情况下就有可能把较小的start写回主内存中。

所以volatile只能保证可见性，在不符合以下场景下我们依然需要通过加锁来保证原子性：

- 运算结果并不依赖变量当前的值，或者只有单一线程修改变量的值。（要么结果不依赖当前值，要么操作是原子性的，要么只要一个线程修改变量的值）
- 变量不需要与其他状态变量共同参与不变约束

比方说我们会在线程里加个boolean变量，来判断线程是否停止，这种情况就非常适合使用volatile。

我们再来理解第二句话。

- 禁止进行指令重排序

什么是指令重排序？🤔

>指令重排序是值指令乱序执行，即在条件允许的情况下，直接运行当前有能力立即执行的后续指令，避开为获取下一条指令所需数据而造成的等待，通过乱序执行的技术，提供执行效率。

指令重排序绘制被volatile修饰的变量的赋值操作前，添加一个内存屏障，指令重排序时不能把后面的指令重排序的内存屏障之前的位置。

关于指令重排序不是本篇文章重点讨论的内容，更多细节可以参考[指令重排序](https://tech.meituan.com/java-memory-reordering.html)。


### 线程为什么阻塞，为和要使用多线程？

使用多线程更多的是为了提高CPU的并发，可以让CPU同事处理多个事情，多线程场景的使用场景：

1. 为了不让耗时操作阻塞主线程，开启新线程执行耗时操作。
2. 某种任务虽然耗时但是不消耗CPU，例如：磁盘IO，可以开启新线程来做，可以显著的提高效率。
3. 优先级比较低的任务，但是需要经常去做，例如：GC，可以开启新线程来做。

### 线程如何关闭，如何避免线程内存泄漏？

### ThreadLocal的原理了解吗？

### wait和notify机制，手写一下生产者和消费者模型？

### 描述一下线程的几种状态？

### 死锁是如何发生的，如何避免死锁？

### Vector，ArrayList与LinkedList有什么区别，应用场景是什么？

- Vector实现了基于动态数组的数据结构，线程安全，可以设置增长因子，效率比较低，不建议使用。
- ArrayList实现了基于动态数组的数据结构，非线程安全，地址连续，查询效率比较高，插入和删除效率比较低。适合查询操作频繁的场景。
- LinkedList实现了基于链表的数据结构，非线程安全，地址不连续，查询效率比较低，插入和删除效率比较高。适合插入和删除操作频繁的场景。

### TreeSet和HashSet有什么区别，应用场景是什么？

- TreeSet基于红黑树实现，非线程安全，可以按照自然顺序或者自定义顺序自动排序，不允许插入null值。适合需要排序的场景。
- HashSet基于hash表实现，非线程安全，允许插入null，查找效率高。适合查找操作频繁的场景。

### HashMap是如何解决hash碰撞的？

- 开发定址法
- 链表法

### HashMap原理。TreeMap、ConcurrentHashMap的实现原理是什么？

- TreeMap基于红黑树实现，非线程安全，可以按照自然顺序或者自定义顺序自动排序，不允许插入null值。适合需要排序的场景。
- HashMap基于hash表实现，非线程安全，允许插入null，查找效率高。适合查找操作频繁的场景。
- ConcurrentHashMap基于hash表实现，线程安全且高效，分段锁的实现相对于HashTable的实现提高了很大的效率。

### SpareArray做了哪些优化？

### 了解Java注解的原理吗？

### String为什么要设计成不可变，StringBuffer与StringBuilder有什么区别？

StringBuffer是线程安全的，StringBuilder是非线程安全的。

## Android

### 手画一下Android系统架构图，描述一下各个层次的作用？

### 描述一下Android的事件分发机制？

### 描述一下View的绘制原理？

View的绘制流程主要分为三步：

1. onMeasure：测量视图的大小，从顶层父View到子View递归调用measure()方法，measure()调用onMeasure()方法，onMeasure()方法完成绘制工作。
2. onLayout：确定视图的位置，从顶层父View到子Viewdi'gu
3. onDraw：

### 了解APK的打包流程吗，描述一下？

### 了解APK的安装流程吗，描述一下？

### 当点击一个应用图标以后，都发生了什么，描述一下这个过程？

### BroadcastReceiver与LocalBroadcastReceiver有什么区别？

### Android Handler机制是做什么的，原理了解吗？

### Android Binder机制是做什么的，原理了解吗？

### 描述一下进程和Application的生命周期？

### Android的内存是如何管理的？

### Android有哪几种进程，是如何管理的？

### OOM如何发生的，是否可以try catch？


- SharePreference性能优化，进程同步。
- 动态布局
- SurfaceView
- 
- Bundle机制
- 权限管理系统
- 系统启动流程
- RecyclerView与ListView的区别
- Android事件的分发机制
- 进程的状态与调度
- 有序广播和标准广播的区别
- Service生命周期
- 数据库数据迁移问题
- 为什么设计Content Provider，进程共享和线程安全问题。
- Service与Activity的数据交互
- onStop做了网络请求，在onResume如何恢复
- Android进程分类
- Activity之间的通信方式
- Activity与Fragment之间的生命周期比较
- SQLite升级，增加字段里的语句
- 多线程读写文件的安全性
- App唤醒其他进程的方式
- 开启摄像头的步骤
- Activity上有Dialog按Hone键的生命周期变化
- 有几种Context对象，有何区别
- ViewPager的实现细节，懒加载如何实现
- 序列化，Android为什么引入Parcelable
- AIDL机制
- Android内进程的分配，能不能自己分配定额内存
- 后台保活

## 网络编程

- Https请求慢的解决办法，哪里用了对称加密，哪里用了非对称加密。
- TCP与UDP的区别
- WebSocket与Socket的区别

## 应用优化

- 冷启动时长优化
- 应用稳定性
- 应用保活
- 性能优化，保证应用不卡顿
- 内存优化、网络优化、布局优化、电量优化、业务优化
- View嵌套层级
- 多线程短短续传的原理
- ANR的原因，怎么分析，如何解决
- 内存泄漏的原因

## 工程实践

- App 热修复
- App 插件化
- App 模块化
- App 沙箱化
- MVP的实现

## 开源类库

- Fresco，图片加载机制
- LruCache & DiskLruCache
- Okhttp 网络缓存的实现
- RxJava实现原理
- EventBus的功能和原理，代替EventBus的方式

## 数据结构与算法

- 排序、快速排序、堆排序的实现，时间复杂度，空间复杂度
- 单链表反转，合并多个单链表
- 数组和链表的区别
- 树、B+树的介绍
- 二叉树的深度优先遍历和广度优先遍历
- 判断单链表是否连成环

## 设计模式

- 手写生成者与消费者模式
- 适配器模式、装饰者模式与外观模式的异同
