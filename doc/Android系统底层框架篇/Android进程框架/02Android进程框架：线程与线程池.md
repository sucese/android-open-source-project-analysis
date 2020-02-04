# Android进程框架：线程与线程池

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

- 一 线程原理
    - 1.1 线程创建
    - 1.2 线程调度
- 二 线程同步
    - 2.1 volatile
    - 2.2 synchronized
- 三 线程池
    - 3.1 线程池调度
    - 3.2 线程池配置
    - 3.1 线程池监控
- 四 线程池应用
    - 4.1 AsyncTask
    - 4.2 Okhttp
    
本篇文章主要用来讨论Java中多线程并发原理与实践经验，并不是一篇使用例子教程，这方面内容可以参考网上其他文章。

## 一 线程原理

### 1.1 线程创建

>线程是比进程更加轻量级的调度单位，线程的引入可以把进程的资源分配和执行调度分开，各个线程既可以共享进程资源，又可以独立调度。

通常大家都会这么去解释进程与线程的区别，在文章[01Android进程框架：进程的启动创建、启动与调度流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android进程框架/01Android进程框架：进程的启动创建、启动与调度流程.md)中
我们剖析了进程的本质，我们这里再简单回忆一下。

关于进程本质的描述：

>我们知道，代码是静态的，有代码和资源组成的系统要想运行起来就需要一种动态的存在，进程就是程序的动态执行过程。何为进程？
进程就是处理执行状态的代码以及相关资源的集合，包括代码端段、文件、信号、CPU状态、内存地址空间等。

进程使用task_struct结构体来描述，如下所示：

- 代码段：编译后形成的一些指令
- 数据段：程序运行时需要的数据
    - 只读数据段：常量
    - 已初始化数据段：全局变量，静态变量
    - 未初始化数据段（bss)：未初始化的全局变量和静态变量
- 堆栈段：程序运行时动态分配的一些内存
- PCB：进程信息，状态标识等

我们接着来看看Java线程的创建序列图，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/java_thread_start_sequence.png"/>

可以看到，最终调用pthread库的pthread_create()方法创建了新的线程，该线程也使用task_struct结构体来描述，但是它没有自己独立的地址空间，而是与其所在的进程共享地址空间和资源。

所以你可以发现，对于虚拟机而言，除了是否具有独立的地址空间外，进程与线程并没有本质上的区别。

我们接着来看看线程是如何调度的。

### 1.2 线程调度

线程状态流程图图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/java_thread_state.png"/>

- NEW：创建状态，线程创建之后，但是还未启动。
- RUNNABLE：运行状态，处于运行状态的线程，但有可能处于等待状态，例如等待CPU、IO等。
- WAITING：等待状态，一般是调用了wait()、join()、LockSupport.spark()等方法。
- TIMED_WAITING：超时等待状态，也就是带时间的等待状态。一般是调用了wait(time)、join(time)、LockSupport.sparkNanos()、LockSupport.sparkUnit()等方法。
- BLOCKED：阻塞状态，等待锁的释放，例如调用了synchronized增加了锁。
- TERMINATED：终止状态，一般是线程完成任务后退出或者异常终止。 

NEW、WAITING、TIMED_WAITING都比较好理解，我们重点说一说RUNNABLE运行态和BLOCKED阻塞态。

线程进入RUNNABLE运行态一般分为五种情况：

- 线程调用sleep(time)后查出了休眠时间
- 线程调用的阻塞IO已经返回，阻塞方法执行完毕
- 线程成功的获取了资源锁
- 线程正在等待某个通知，成功的获得了其他线程发出的通知
- 线程处于挂起状态，然后调用了resume()恢复方法，解除了挂起。

线程进入BLOCKED阻塞态一般也分为五种情况：

- 线程调用sleep()方法主动放弃占有的资源
- 线程调用了阻塞式IO的方法，在该方法返回前，该线程被阻塞。
- 线程视图获得一个资源锁，但是该资源锁正被其他线程锁持有。
- 线程正在等待某个通知
- 线程调度器调用suspend()方法将该线程挂起

我们再来看看和线程状态相关的一些方法。

- sleep()方法让当前正在执行的线程在指定时间内暂停执行，正在执行的线程可以通过Thread.currentThread()方法获取。
- yield()方法放弃线程持有的CPU资源，将其让给其他任务去占用CPU执行时间。但放弃的时间不确定，有可能刚刚放弃，马上又获得CPU时间片。
- wait()方法是当前执行代码的线程进行等待，将当前线程放入预执行队列，并在wait()所在的代码处停止执行，知道接到通知或者被中断为止。该方法可以使得调用该方法的线程释放共享资源的锁，
然后从运行状态退出，进入等待队列，直到再次被唤醒。该方法只能在同步代码块里调用，否则会抛出IllegalMonitorStateException异常。
- wait(long millis)方法等待某一段时间内是否有线程对锁进行唤醒，如果超过了这个时间则自动唤醒。
- notify()方法用来通知那些可能等待该对象的对象锁的其他线程，该方法可以随机唤醒等待队列中等同一共享资源的一个线程，并使该线程退出等待队列，进入可运行状态。
- notifyAll()方法可以是所有正在等待队列中等待同一共享资源的全部线程从等待状态退出，进入可运行状态，一般会是优先级高的线程先执行，但是根据虚拟机的实现不同，也有可能是随机执行。
- join()方法可以让调用它的线程正常执行完成后，再去执行该线程后面的代码，它具有让线程排队的作用。

## 二 线程同步

>线程安全，通常所说的线程安全指的是相对的线程安全，它指的是对这个对象单独的操作是线程安全的，我们在调用的时候无需做额外的保障措施。

什么叫相对安全？🤔

🤞举个栗子

我们知道Java里的Vector是个线程安全的类，在多线程环境下对其插入、删除和读取都是安全的，但这仅限于每次只有一个线程对其操作，如果多个线程同时操作
Vector，那它就不再是线程安全的了。

```java
    final Vector<String> vector = new Vector<>();

    while (true) {
        for (int i = 0; i < 10; i++) {
            vector.add("项：" + i);
        }

        Thread removeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < vector.size(); i++) {
                    vector.remove(i);
                }
            }
        });

        Thread printThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < vector.size(); i++) {
                    Log.d(TAG, vector.get(i));
                }
            }
        });

        removeThread.start();
        printThread.start();

        if (Thread.activeCount() >= 20) {
            return;
        }
    }
```

但是程序却crash了

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/vector_thread_safe.png"/>

正确的做法应该是vector对象加上同步锁，如下：

```Java
        final Vector<String> vector = new Vector<>();

        while (true) {
            for (int i = 0; i < 10; i++) {
                vector.add("项：" + i);
            }

            Thread removeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (vector){
                        for (int i = 0; i < vector.size(); i++) {
                            vector.remove(i);
                        }
                    }
                }
            });

            Thread printThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (vector){
                        for (int i = 0; i < vector.size(); i++) {
                            Log.d(TAG, vector.get(i));
                        }
                    }
                }
            });

            removeThread.start();
            printThread.start();

            if (Thread.activeCount() >= 20) {
                return;
            }
        }
```

### 2.1 volatile

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

### 2.2 synchronized

synchronized是互斥同步的一种实现。

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

关不同步锁还有ReentrantLock，eentrantLockR相对于synchronized具有等待可中断、公平锁等更多功能，这里限于篇幅，不再展开。

## 三 线程池

我们知道线程的创建、切换与销毁都会花费比较大代价，所以很自然的我们使用线程池来复用和管理线程。Java里的线程池我们通常通过ThreadPoolExecutor来实现。
接下来我们就来分析ThreadPoolExecutor的相关原理，以及ThreadPoolExecutor在Android上的应用AsyncTask。

### 3.1 线程池调度

线程池有五种运行状态，如下所示：

线程池状态图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/ThreadPoolExecutor_state.png"  height="400"/>

- RUNNING：可以接受新任务，也可以处理等待队列里的任务。
- SHUTDOWN：不接受新任务，但可以处理等待队列里的任务。
- STOP：不接受新的任务，不再处理等待队列里的任务。中断正在处理的任务。
- TIDYING：所有任务都已经处理完了，当前线程池没有有效的线程，并且即将调用terminated()方法。
- TERMINATED：调用了terminated()方法，线程池终止。

另外，ThreadPoolExecutor是用一个AtomicInteger来记录线程池状态和线程池里的线程数量的，如下所示：

- 低29位：用来存放线程数
- 高3位：用来存放线程池状态

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
private static final int COUNT_BITS = Integer.SIZE - 3;
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

// runState is stored in the high-order bits
private static final int RUNNING    = -1 << COUNT_BITS;// 111
private static final int SHUTDOWN   =  0 << COUNT_BITS;// 000
private static final int STOP       =  1 << COUNT_BITS;// 001
private static final int TIDYING    =  2 << COUNT_BITS;// 010
private static final int TERMINATED =  3 << COUNT_BITS;// 110

// Packing and unpacking ctl
private static int runStateOf(int c)     { return c & ~CAPACITY; }//线程池状态
private static int workerCountOf(int c)  { return c & CAPACITY; }//线程池当前线程数
private static int ctlOf(int rs, int wc) { return rs | wc; }
```

在正式介绍线程池调度原理之前，我们先来回忆一下Java实现任务的两个接口：

- Runnable：在run()方法里完成任务，无返回值，且不会抛出异常。
- Callable：在call()方法里完成任务，有返回值，且可能抛出异常。

另外，还有个Future接口，它可以对Runnable、Callable执行的任务进行判断任务是否完成，中断任务以及获取任务结果的操作。我们通常会使用它的实现类FutureTask，FutureTask是一个Future、Runnable
以及Callable的包装类。利用它可以很方便的完成Future接口定义的操作。FutureTask内部的线程阻塞是基于LockSupport来实现的。

我们接下来看看线程池是和执行任务的。

ThreadPoolExecutor调度流程图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/ThreadPoolExecutor_flow.png"/>

**execute(Runnable command)**

```java
public class ThreadPoolExecutor extends AbstractExecutorService {
        public void execute(Runnable command) {
            if (command == null)
                throw new NullPointerException();
            int c = ctl.get();
            //1. 若线程池状态是RUNNING，线程池大小小于配置的核心线程数，则可以在线程池中创建新线程执行新任务。
            if (workerCountOf(c) < corePoolSize) {
                if (addWorker(command, true))
                    return;
                c = ctl.get();
            }
            //2. 若线程池状态是RUNNING，线程池大小大于配置的核心线程数，则尝试将任务插入阻塞队列进行等待
            if (isRunning(c) && workQueue.offer(command)) {
                int recheck = ctl.get();
                //若插入成功，则将次检查线程池的状态是否为RUNNING，如果不是则移除当前任务并进入拒绝策略。
                if (! isRunning(recheck) && remove(command))
                    reject(command);
                //如果线程池中的线程数为0，即线程池中的线程都执行完毕处于SHUTDOWN状态，此时添加了一个null任务
                //(因为SHUTDOWN状态不再接受新任务）
                else if (workerCountOf(recheck) == 0)
                    addWorker(null, false);
            }
            //3. 若无法插入阻塞队列，则尝试创建新线程，创建失败则进入拒绝策略。
            else if (!addWorker(command, false))
                reject(command);
        }
}
```

1. 若线程池大小小于配置的核心线程数，则可以在线程池中创建新线程执行新任务。
2. 若线程池状态是RUNNING，线程池大小大于配置的核心线程数，则尝试将任务插入阻塞队列进行等待。若插入成功，为了健壮性考虑，则将次检查线程池的状态是否为RUNNING
，如果不是则移除当前任务并进入拒绝策略。如果线程池中的线程数为0，即线程池中的线程都执行完毕处于SHUTDOWN状态，此时添加了一个null任务（因为SHUTDOWN状态不再接受
新任务）。
3. 若无法插入阻塞队列，则尝试创建新线程，创建失败则进入拒绝策略。

这个其实很好理解，打个比方。我们公司的一个小组来完成任务，

- 如果任务数量小于小组人数（核心线程数），则指派小组里人的完成；
- 如果任务数量大于小组人数，则去招聘新人来完成，则将任务加入排期等待（阻塞队列）。
- 如果没有排期，则试着去招新人来完成任务（最大线程数），如果招新人也完成不了，说明这不是人干的活，则去找产品经理砍需求（拒绝策略）。

**addWorker(Runnable firstTask, boolean core)**

addWorker(Runnable firstTask, boolean core) 表示添加个Worker，Worker实现了Runnable接口，是对Thread的封装，该方法添加完Worker后，则调用runWorker()来启动线程。 

```java
public class ThreadPoolExecutor extends AbstractExecutorService {
    
     private boolean addWorker(Runnable firstTask, boolean core) {
            //重试标签
            retry:
            for (;;) {
                int c = ctl.get();
                //获取当前线程池状态
                int rs = runStateOf(c);
    
                //以下情况表示不再接受新任务：1 线程池没有处于RUNNING状态 2 要执行的任务为空 3 阻塞队列已满
                if (rs >= SHUTDOWN &&
                    ! (rs == SHUTDOWN &&
                       firstTask == null &&
                       ! workQueue.isEmpty()))
                    return false;
    
                for (;;) {
                    //获取线程池当前的线程数
                    int wc = workerCountOf(c);
                    //如果超出容量，则不再接受新任务，core表示是否使用corePoolSize作为比较标准
                    if (wc >= CAPACITY ||
                        wc >= (core ? corePoolSize : maximumPoolSize))
                        return false;
                    //增加线程数
                    if (compareAndIncrementWorkerCount(c))
                        break retry;
                    c = ctl.get();  // Re-read ctl
                    //如果线程池状态发生变化，重新开始循环
                    if (runStateOf(c) != rs)
                        continue retry;
                    // else CAS failed due to workerCount change; retry inner loop
                }
            }
    
            //线程数增加成功，开始添加新线程，Worker是Thread的封装类
            boolean workerStarted = false;
            boolean workerAdded = false;
            Worker w = null;
            try {
                w = new Worker(firstTask);
                final Thread t = w.thread;
                if (t != null) {
                    final ReentrantLock mainLock = this.mainLock;
                    //加锁
                    mainLock.lock();
                    try {
                        // Recheck while holding lock.
                        // Back out on ThreadFactory failure or if
                        // shut down before lock acquired.
                        int rs = runStateOf(ctl.get());
    
                        if (rs < SHUTDOWN ||
                            (rs == SHUTDOWN && firstTask == null)) {
                            if (t.isAlive()) // precheck that t is startable
                                throw new IllegalThreadStateException();
                            //将新启动的线程添加到线程池中
                            workers.add(w);
                            //更新线程池中线程的数量，注意这个数量不能超过largestPoolSize
                            int s = workers.size();
                            if (s > largestPoolSize)
                                largestPoolSize = s;
                            workerAdded = true;
                        }
                    } finally {
                        mainLock.unlock();
                    }
                    if (workerAdded) {
                        //调用runWorker()方法，开始执行线程
                        t.start();
                        workerStarted = true;
                    }
                }
            } finally {
                if (! workerStarted)
                    addWorkerFailed(w);
            }
            return workerStarted;
        }
}
```

**runWorker(Worker w)**

runWorker()方法是整个阻塞队列的核心循环，在这个循环中，线程池会不断的从阻塞队列workerQueue中取出的新的task并执行。

```java
public class ThreadPoolExecutor extends AbstractExecutorService {
    
    final void runWorker(Worker w) {
           Thread wt = Thread.currentThread();
           Runnable task = w.firstTask;
           w.firstTask = null;
           w.unlock(); // allow interrupts
           boolean completedAbruptly = true;
           try {
               //从阻塞队列中不断取出任务，如果取出的任务为空，则循环终止
               while (task != null || (task = getTask()) != null) {
                   w.lock();
                   // If pool is stopping, ensure thread is interrupted;
                   // if not, ensure thread is not interrupted.  This
                   // requires a recheck in second case to deal with
                   // shutdownNow race while clearing interrupt
                   if ((runStateAtLeast(ctl.get(), STOP) ||
                        (Thread.interrupted() &&
                         runStateAtLeast(ctl.get(), STOP))) &&
                       !wt.isInterrupted())
                       wt.interrupt();
                   try {
                       //该方法为空，可以重新次方法，在任务执行开始前做一些处理
                       beforeExecute(wt, task);
                       Throwable thrown = null;
                       try {
                           task.run();
                       } catch (RuntimeException x) {
                           thrown = x; throw x;
                       } catch (Error x) {
                           thrown = x; throw x;
                       } catch (Throwable x) {
                           thrown = x; throw new Error(x);
                       } finally {
                           //该方法为空，可以重新次方法，在任务执行结束后做一些处理
                           afterExecute(task, thrown);
                       }
                   } finally {
                       task = null;
                       w.completedTasks++;
                       w.unlock();
                   }
               }
               completedAbruptly = false;
           } finally {
               processWorkerExit(w, completedAbruptly);
           }
       }
       
        //从阻塞队列workerQueue中取出Task
        private Runnable getTask() {
               boolean timedOut = false; // Did the last poll() time out?
               //循环
               for (;;) {
                   int c = ctl.get();
                   //获取线程池状态
                   int rs = runStateOf(c);
       
                   //以下情况停止循环：1 线程池状态不是RUNNING（>= SHUTDOWN）2 线程池状态>= STOP 或者阻塞队列为空
                   if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                       //递减workCount
                       decrementWorkerCount();
                       return null;
                   }
       
                   int wc = workerCountOf(c);
       
                   // 判断线程的IDLE超时机制是否生效，有两种情况：1 allowCoreThreadTimeOut = true，这是可以手动
                   //设置的 2 当前线程数大于核心线程数
                   boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
       
                   if ((wc > maximumPoolSize || (timed && timedOut))
                       && (wc > 1 || workQueue.isEmpty())) {
                       if (compareAndDecrementWorkerCount(c))
                           return null;
                       continue;
                   }
       
                   try {
                       //根据timed来决定是以poll超时等待的方式还是以take()阻塞等待的方式从阻塞队列中获取任务
                       Runnable r = timed ?
                           workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                           workQueue.take();
                       if (r != null)
                           return r;
                       timedOut = true;
                   } catch (InterruptedException retry) {
                       timedOut = false;
                   }
               }
           }
}
```
>所以你可以理解了，runWorker()方法是在新创建线程的run()方法里的，而runWorker()又不断的调用getTask()方法去获取阻塞队列里的任务，这样就实现了线程的复用。

### 3.2 线程池配置

我们先来看看ThreadPoolExecutor的构造方法：

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler)
```

- int corePoolSize：核心线程池大小
- int maximumPoolSize：线程池最大容量大小
- long keepAliveTime：线程不活动时存活的时间
- TimeUnit unit：时间单位
- BlockingQueue<Runnable> workQueue：任务队列
- ThreadFactory threadFactory：线程工程
- RejectedExecutionHandler handler：线程拒绝策略


那么这些参数我们应该怎么配置呢？要合理配置线程池就需要先了解我们的任务特性，一般说来：

- 任务性质：CPU密集型、IO密集型、混合型
- 任务优先级：低、中、高
- 任务执行时间：短、中、长
- 任务依赖性：是否依赖其他资源，数据库、网络

我们根据这些属性来一一分析这些参数的配置。

首先就是核心线程数corePoolSize与最大线程数maximumPoolSize。这个的配置我们通常要考虑CPU同时执行线程的阈值。一旦超过这个阈值，CPU就需要花费很多
时间来完成线程的切换与调度，这样会导致性能大幅下滑。

```java
/**
* CPU核心数，注意该方法并不可靠，它返回的有可能不是真实的CPU核心数，因为CPU在某些情况下会对某些核
* 心进行睡眠处理，这种情况返回的只是已激活的CPU核心数。
*/
private static final int NUMBER_OF_CPU = Runtime.getRuntime().availableProcessors();

/**
* 核心线程数
*/
private static final int corePoolSize = Math.max(2, Math.min(NUMBER_OF_CPU - 1, 4));

/**
* 最大线程数
*/
private static final int maximumPoolSize = NUMBER_OF_CPU * 2 + 1;
```

至于keepAliveTime，该参数描述了线程不活动时存活的时间，如果是CPU密集型任务，则将时间设置的小一些，如果是IO密集型或者数据库连接任务，则将时间设置的长一些。

我们再来看看BlockingQueue参数的配置。BlockingQueue用来描述阻塞队列。它的方法以四种形式存在，以此来满足不同需求。

|抛出异常   |	特殊值	   |阻塞	   |超时
|:---------|:---------|:-------|:-------|
|add(e)	    |offer(e) |put(e)	|offer(e, time, unit)
|remove()   |poll()	  |take()	|poll(time, unit)
|element()	|peek()	  |不可用	|不可用

它有以下特点：

- 不支持null元素
- 线程安全

它的实现类有：

- ArrayBlockingQueue ：一个数组实现的有界阻塞队列，此队列按照FIFO的原则对元素进行排序，支持公平访问队列（可重入锁实现ReenttrantLock）。
- LinkedBlockingQueue ：一个由链表结构组成的有界阻塞队列，此队列默认和最大长度为Integer.MAX_VALUE，按照FIFO的原则对元素进行排序。
- PriorityBlockingQueue ：一个支持优先级排序的无界阻塞队列，默认情况下采用自然顺序排列，也可以指定Comparator。
- DelayQueue：一个支持延时获取元素的无界阻塞队列，创建元素时可以指定多久以后才能从队列中获取当前元素，常用于缓存系统设计与定时任务调度等。
- SynchronousQueue：一个不存储元素的阻塞队列。存入操作必须等待获取操作，反之亦然，它相当于一个传球手，非常适合传递性场景。
- LinkedTransferQueue：一个由链表结构组成的无界阻塞队列，与LinkedBlockingQueue相比多了transfer和tryTranfer方法，该方法在有消费者等待接收元素时会立即将元素传递给消费者。
- LinkedBlockingDeque：一个由链表结构组成的双端阻塞队列，可以从队列的两端插入和删除元素。因为出入口都有两个，可以减少一半的竞争。适用于工作窃取的场景。

>工作窃取：例如有两个队列A、B，各自干自己的活，但是A效率比较高，很快把自己的活干完了，于是勤快的A就会去窃取B的任务来干，这是A、B会访问同一个队列，为了减少A、B的竞争，规定窃取者A
只从双端队列的尾部拿任务，被窃取者B只从双端队列的头部拿任务。

我们最后来看看RejectedExecutionHandler参数的配置。

RejectedExecutionHandler用来描述线程数大于或等于线程池最大线程数时的拒绝策略，它的实现类有：

- ThreadPoolExecutor.AbortPolicy：默认策略，当线程池中线程的数量大于或者等于最大线程数时，抛出RejectedExecutionException异常。
- ThreadPoolExecutor.DiscardPolicy：当线程池中线程的数量大于或者等于最大线程数时，默默丢弃掉不能执行的新任务，不报任何异常。
- ThreadPoolExecutor.CallerRunsPolicy：当线程池中线程的数量大于或者等于最大线程数时，如果线程池没有被关闭，则直接在调用者的线程里执行该任务。
- ThreadPoolExecutor.DiscardOldestPolicy：当线程池中线程的数量大于或者等于最大线程数时，丢弃阻塞队列头部的任务（即等待最近的任务），然后重新添加当前任务。

另外，Executors提供了一系列工厂方法用来创建线程池。这些线程是适用于不同的场景。

- newCachedThreadPool()：无界可自动回收线程池，查看线程池中有没有以前建立的线程，如果有则复用，如果没有则建立一个新的线程加入池中，池中的线程超过60s不活动则自动终止。适用于生命
周期比较短的异步任务。
- newFixedThreadPool(int nThreads)：固定大小线程池，与newCachedThreadPool()类似，但是池中持有固定数目的线程，不能随时创建线程，如果创建新线程时，超过了固定
线程数，则放在队列里等待，直到池中的某个线程被移除时，才加入池中。适用于很稳定、很正规的并发线程，多用于服务器。
- newScheduledThreadPool(int corePoolSize)：周期任务线程池，该线程池的线程可以按照delay依次执行线程，也可以周期执行。
- newSingleThreadExecutor()：单例线程池，任意时间内池中只有一个线程。

### 3.3 线程池监控

ThreadPoolExecutor里提供了一些空方法，我们可以通过继承ThreadPoolExecutor，复写这些方法来实现对线程池的监控。

```java
public class ThreadPoolExecutor extends AbstractExecutorService {
       
    protected void beforeExecute(Thread t, Runnable r) { }
    protected void afterExecute(Runnable r, Throwable t) { }
}
```
常见的监控指标有：

- taskCount：线程池需要执行的任务数量。 
- completedTaskCount：线程池在运行过程中已完成的任务数量，小于或等于taskCount。 
- largestPoolSize：线程池里曾经创建过的最大线程数量。通过这个数据可以知道线程池是否曾经满过。如该数值等于线程池的最大大小，则表示线程池曾经满过。 
- getPoolSize：线程池的线程数量。如果线程池不销毁的话，线程池里的线程不会自动销毁，所以这个大小只增不减。 
- getActiveCount：获取活动的线程数。 

## 四 线程池应用

### 4.1 AsyncTask

>AsyncTask基于ThreadPoolExecutor实现，内部封装了Thread+Handler，多用来执行耗时较短的任务。

一个简单的AsyncTask例子

```java
public class AsyncTaskDemo extends AsyncTask<String, Integer, String> {

    /**
     * 在后台任务开始执行之前调用，用于执行一些界面初始化操作，例如显示一个对话框，UI线程。
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * 执行后台线程，执行完成可以通过return语句返回，worker线程
     *
     * @param strings params
     * @return result
     */
    @Override
    protected String doInBackground(String... strings) {
        return null;
    }

    /**
     * 更新进度，UI线程
     *
     * @param values progress
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }


    /**
     * 后台任务执行完成并通过return语句返回后会调用该方法，UI线程。
     *
     * @param result result
     */
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }

    /**
     * 后台任务呗取消后回调
     *
     * @param reason reason
     */
    @Override
    protected void onCancelled(String reason) {
        super.onCancelled(reason);
    }

    /**
     * 后台任务呗取消后回调
     */
    @Override
    protected void onCancelled() {
        super.onCancelled();
    }
}
```

AsyncTask的使用非常的简单，接下来我们去分析AsyncTask的源码实现。

AsyncTask流程图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/AsyncTask_flow.png"/>

AsyncTask源码的一开始就是个创建线程池的流程。

```java
public abstract class AsyncTask<Params, Progress, Result> {
    
        private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        //核心线程数，最少2个，最多4个
        private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
        private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
        //线程不活动时的存活时间是30s
        private static final int KEEP_ALIVE_SECONDS = 30;
    
        //线程构建工厂，指定线程的名字
        private static final ThreadFactory sThreadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);
    
            public Thread newThread(Runnable r) {
                return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
            }
        };
    
        //一个由链表结构组成的无界阻塞队列
        private static final BlockingQueue<Runnable> sPoolWorkQueue =
                new LinkedBlockingQueue<Runnable>(128);
    
        public static final Executor THREAD_POOL_EXECUTOR;
    
        //构建线程池
        static {
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                    sPoolWorkQueue, sThreadFactory);
            threadPoolExecutor.allowCoreThreadTimeOut(true);
            THREAD_POOL_EXECUTOR = threadPoolExecutor;
        }
}
```

另外，我们可以通过AsyncTask.executeOnExecutor(Executor exec, Params... params) 来自定义线程池。

我们再来看看构造方法。

```java
public abstract class AsyncTask<Params, Progress, Result> {
    
      //构造方法需要在UI线程里调用
      public AsyncTask() {
          //创建一个Callable对象，WorkerRunnable实现了Callable接口
          mWorker = new WorkerRunnable<Params, Result>() {
              public Result call() throws Exception {
                  mTaskInvoked.set(true);
  
                  Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                  //noinspection unchecked
                  Result result = doInBackground(mParams);
                  Binder.flushPendingCommands();
                  return postResult(result);
              }
          };
  
          //创建一个FutureTask对象，该对象用来接收mWorker的结果
          mFuture = new FutureTask<Result>(mWorker) {
              @Override
              protected void done() {
                  try {
                      //将执行的结果通过发送给Handler处理，注意FutureTask的get()方法会阻塞直至结果返回
                      postResultIfNotInvoked(get());
                  } catch (InterruptedException e) {
                      android.util.Log.w(LOG_TAG, e);
                  } catch (ExecutionException e) {
                      throw new RuntimeException("An error occurred while executing doInBackground()",
                              e.getCause());
                  } catch (CancellationException e) {
                      postResultIfNotInvoked(null);
                  }
              }
          };
      } 
      
      private void postResultIfNotInvoked(Result result) {
          final boolean wasTaskInvoked = mTaskInvoked.get();
          if (!wasTaskInvoked) {
              postResult(result);
          }
      }
  
      private Result postResult(Result result) {
          @SuppressWarnings("unchecked")
          Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT,
                  new AsyncTaskResult<Result>(this, result));
          message.sendToTarget();
          return result;
      }
      
     //内部的Handler      
     private static class InternalHandler extends Handler {
        public InternalHandler() {
            //UI线程的Looper
            super(Looper.getMainLooper());
        }

        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
            switch (msg.what) {
                //返回结果
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                //返回进度
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
     }
}
```

可以看到当我们调用AsyncTask的构造方法时，就创建了一个FutureTask对象，它内部包装了Callable对象（就是我们要执行的任务），并在FutureTask对象的done()方法里
将结果发送给Handler。

接着看看执行方法execute()。

```java
public abstract class AsyncTask<Params, Progress, Result> {
    
        //需要在UI线程里调用
        @MainThread
        public final AsyncTask<Params, Progress, Result> execute(Params... params) {
            return executeOnExecutor(sDefaultExecutor, params);
        }

        @MainThread
        public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
                Params... params) {
            if (mStatus != Status.PENDING) {
                switch (mStatus) {
                    case RUNNING:
                        throw new IllegalStateException("Cannot execute task:"
                                + " the task is already running.");
                    case FINISHED:
                        throw new IllegalStateException("Cannot execute task:"
                                + " the task has already been executed "
                                + "(a task can be executed only once)");
                }
            }
    
            mStatus = Status.RUNNING;
            //任务执行前的处理，我们可以复写次方法
            onPreExecute();
    
            mWorker.mParams = params;
            //执行任务，exec为sDefaultExecutor
            exec.execute(mFuture);
    
            return this;
        }
}
```
接着看看这个sDefaultExecutor。

可以看到sDefaultExecutor是个SerialExecutor对象，SerialExecutor实现了Executor接口。

```java
public abstract class AsyncTask<Params, Progress, Result> {
    
        public static final Executor SERIAL_EXECUTOR = new SerialExecutor();
        private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
        
        private static class SerialExecutor implements Executor {
            //任务队列
            final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
            //当前执行的任务
            Runnable mActive;
    
            public synchronized void execute(final Runnable r) {
                
                mTasks.offer(new Runnable() {
                    public void run() {
                        try {
                            r.run();
                        } finally {
                            scheduleNext();
                        }
                    }
                });
                if (mActive == null) {
                    //开始执行任务
                    scheduleNext();
                }
            }
    
            protected synchronized void scheduleNext() {
                //取出队列头的任务开始执行
                if ((mActive = mTasks.poll()) != null) {
                    THREAD_POOL_EXECUTOR.execute(mActive);
                }
            }
        }
}
```
所以我们没调用一次AsyncTask.execute()方法就将FutureTask对象添加到队列尾部，然后会从队列头部取出任务放入线程池中执行，所以你可以看着这是一个串行执行器。

### 4.2 Okhttp

在Okhttp的任务调度器Dispatcher里有关于线程池的配置

```java
public final class Dispatcher {
    
      public synchronized ExecutorService executorService() {
        if (executorService == null) {
          executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
              new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
        }
        return executorService;
      }
}
```

你可以看到它的配置：

- 核心线程数为0，最大线程数为Integer.MAX_VALUE，不对核心线程数进行限制，随时创建新的线程，空闲存活时间为60s，用完即走。这也比较符合网络请求的特性。
- 阻塞队列为SynchronousQueue，该队列不存储任务，只传递任务，所以把任务添加进去就会执行。

这其实是Excutors.newCachedThreadPool()缓存池的实现。总结来说就是新任务过来进入SynchronousQueue，它是一个单工模式的队列，只传递任务，不存储任务，然后就创建
新线程执行任务，线程不活动的存活时间为60s。

Okhttp请求流程图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/okhttp_flow.png"/>

在发起网络请求时，每个请求执行完成后都会调用client.dispatcher().finished(this)。

```java
final class RealCall implements Call {
    
  final class AsyncCall extends NamedRunnable {
    private final Callback responseCallback;

    AsyncCall(Callback responseCallback) {
      super("OkHttp %s", redactedUrl());
      this.responseCallback = responseCallback;
    }

    String host() {
      return originalRequest.url().host();
    }

    Request request() {
      return originalRequest;
    }

    RealCall get() {
      return RealCall.this;
    }

    @Override protected void execute() {
      boolean signalledCallback = false;
      try {
        Response response = getResponseWithInterceptorChain();
        if (retryAndFollowUpInterceptor.isCanceled()) {
          signalledCallback = true;
          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
        } else {
          signalledCallback = true;
          responseCallback.onResponse(RealCall.this, response);
        }
      } catch (IOException e) {
        if (signalledCallback) {
          // Do not signal the callback twice!
          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
        } else {
          responseCallback.onFailure(RealCall.this, e);
        }
      } finally {
        //异步请求
        client.dispatcher().finished(this);
      }
    }
  }
}
```

我们来看看client.dispatcher().finished(this)这个方法。

```java
public final class Dispatcher {
    
  private <T> void finished(Deque<T> calls, T call, boolean promoteCalls) {
    int runningCallsCount;
    Runnable idleCallback;
    synchronized (this) {
      //将已经结束的请求call移除正在运行的队列calls
      if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
      //异步请求promoteCalls为true
      if (promoteCalls) promoteCalls();
      runningCallsCount = runningCallsCount();
      idleCallback = this.idleCallback;
    }

    if (runningCallsCount == 0 && idleCallback != null) {
      idleCallback.run();
    }
  }

    private void promoteCalls() {
      //当前异步请求数大于最大请求数，不继续执行
      if (runningAsyncCalls.size() >= maxRequests) return; // Already running max capacity.
      //异步等待队列为空，不继续执行
      if (readyAsyncCalls.isEmpty()) return; // No ready calls to promote.
  
      //遍历异步等待队列
      for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
        AsyncCall call = i.next();
  
        //如果没有超过相同host的最大请求数，则复用当前请求的线程
        if (runningCallsForHost(call) < maxRequestsPerHost) {
          i.remove();
          runningAsyncCalls.add(call);
          executorService().execute(call);
        }
  
        //运行队列达到上限，也不再执行
        if (runningAsyncCalls.size() >= maxRequests) return; // Reached max capacity.
      }
    }
}
```

所以你可以看到Okhttp不是用线程池来控制线程个数，线程池里的线程执行的都是正在运行请请求，控制线程的是Dispatcher，Dispatcher.promoteCalls()方法通过
最大请求数maxRequests和相同host最大请求数maxRequestsPerHost来控制异步请求不超过两个最大值，在值范围内不断的将等待队列readyAsyncCalls中的请求添加
到运行队列runningAsyncCalls中去。
