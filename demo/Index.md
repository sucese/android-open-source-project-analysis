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

共同点

1. 是上层的抽象层。
2. 都不能被实例化
3. 都能包含抽象的方法，这些抽象的方法用于描述类具备的功能，但是不比提供具体的实现。

区别

1. 在抽象类中可以写非抽象的方法，从而避免在子类中重复书写他们，这样可以提高代码的复用性，这是抽象类的优势；接口中只能有抽象的方法。
2. 一个类只能继承一个直接父类，这个父类可以是具体的类也可是抽象类；但是一个类可以实现多个接口。

### 内部类、静态内部类在业务中的应用场景是什么？

- 静态内部类：只是为了降低包的深度，方便类的使用，静态内部类适用于包含类当中，但又不依赖与外在的类，不用使用外在类的非静态属性和方法，只是为了方便管理类结构而定义。在创建静态内部类的时候，不需要外部类对象的引用。
- 非静态内部类：持有外部类的引用，可以自由使用外部类的所有变量和方法

### synchronized与ReentrantLock有什么区别？

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

### 了解线程的生命周期吗，描述一下？

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

### 线程池了解吗，有几种线程池，应用场景是什么？

Executors类提供了一系列工厂方法用来创建线程池。这些线程是适用于不同的场景。

- newCachedThreadPool()：无界可自动回收线程池，查看线程池中有没有以前建立的线程，如果有则复用，如果没有则建立一个新的线程加入池中，池中的线程超过60s不活动则自动终止。适用于生命
周期比较短的异步任务。
- newFixedThreadPool(int nThreads)：固定大小线程池，与newCachedThreadPool()类似，但是池中持有固定数目的线程，不能随时创建线程，如果创建新线程时，超过了固定
线程数，则放在队列里等待，直到池中的某个线程被移除时，才加入池中。适用于很稳定、很正规的并发线程，多用于服务器。
- newScheduledThreadPool(int corePoolSize)：周期任务线程池，该线程池的线程可以按照delay依次执行线程，也可以周期执行。
- newSingleThreadExecutor()：单例线程池，任意时间内池中只有一个线程。

### ThreadLocal的原理了解吗？

ThreadLocal是一个关于创建线程局部变量的类。使用场景如下所示：

1. 实现单个线程单例以及单个线程上下文信息存储，比如交易id等
2. 实现线程安全，非线程安全的对象使用ThreadLocal之后就会变得线程安全，因为每个线程都会有一个对应的实例
3. 承载一些线程相关的数据，避免在方法中来回传递参数

### wait和notify机制，手写一下生产者和消费者模型？

生成者消费者模型

生产者和消费者在同一时间段内共用同一个存储空间，生产者往存储空间中添加产品，消费者从存储空间中取走产品，当存储空间为空时，消费者阻塞，当存储空间满时，生产者阻塞。

wait()和notify()方法的实现生成者消费者模型，缓冲区满和为空时都调用wait()方法等待，当生产者生产了一个产品或者消费者消费了一个产品之后会唤醒所有线程。

```java
public class ProducerAndCustomerModel {
    
    private static Integer count = 0;
    private static final Integer FULL = 10;
    private static String LOCK = "lock";
    
    public static void main(String[] args) {
        Test1 test1 = new Test1();
        new Thread(test1.new Producer()).start();
        new Thread(test1.new Consumer()).start();
        new Thread(test1.new Producer()).start();
        new Thread(test1.new Consumer()).start();
        new Thread(test1.new Producer()).start();
        new Thread(test1.new Consumer()).start();
        new Thread(test1.new Producer()).start();
        new Thread(test1.new Consumer()).start();
    }
    class Producer implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (LOCK) {
                    while (count == FULL) {
                        try {
                            LOCK.wait();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    count++;
                    System.out.println(Thread.currentThread().getName() + "生产者生产，目前总共有" + count);
                    LOCK.notifyAll();
                }
            }
        }
    }
    class Consumer implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (LOCK) {
                    while (count == 0) {
                        try {
                            LOCK.wait();
                        } catch (Exception e) {
                        }
                    }
                    count--;
                    System.out.println(Thread.currentThread().getName() + "消费者消费，目前总共有" + count);
                    LOCK.notifyAll();
                }
            }
        }
    }
}
```

### 死锁是如何发生的，如何避免死锁？

当线程A持有独占锁a，并尝试去获取独占锁b的同时，线程B持有独占锁b，并尝试获取独占锁a的情况下，就会发生AB两个线程由于互相持有对方需要的锁，而发生的阻塞现象，我们称为死锁。

```java
public class DeadLockDemo {

    public static void main(String[] args) {
        // 线程a
        Thread td1 = new Thread(new Runnable() {
            public void run() {
                DeadLockDemo.method1();
            }
        });
        // 线程b
        Thread td2 = new Thread(new Runnable() {
            public void run() {
                DeadLockDemo.method2();
            }
        });

        td1.start();
        td2.start();
    }

    public static void method1() {
        synchronized (String.class) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程a尝试获取integer.class");
            synchronized (Integer.class) {

            }

        }
    }

    public static void method2() {
        synchronized (Integer.class) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程b尝试获取String.class");
            synchronized (String.class) {

            }

        }
    }
}
```

造成死锁的四个条件：

1. 互斥条件：一个资源每次只能被一个线程使用。
2. 请求与保持条件：一个线程因请求资源而阻塞时，对已获得的资源保持不放。
3. 不剥夺条件：线程已获得的资源，在未使用完之前，不能强行剥夺。
4. 循环等待条件：若干线程之间形成一种头尾相接的循环等待资源关系。

在并发程序中，避免了逻辑中出现复数个线程互相持有对方线程所需要的独占锁的的情况，就可以避免死锁，如下所示：

```java
public class BreakDeadLockDemo {

    public static void main(String[] args) {
        // 线程a
        Thread td1 = new Thread(new Runnable() {
            public void run() {
                DeadLockDemo2.method1();
            }
        });
        // 线程b
        Thread td2 = new Thread(new Runnable() {
            public void run() {
                DeadLockDemo2.method2();
            }
        });

        td1.start();
        td2.start();
    }

    public static void method1() {
        synchronized (String.class) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程a尝试获取integer.class");
            synchronized (Integer.class) {
                System.out.println("线程a获取到integer.class");
            }

        }
    }

    public static void method2() {
        // 不再获取线程a需要的Integer.class锁。
        synchronized (String.class) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程b尝试获取Integer.class");
            synchronized (Integer.class) {
                System.out.println("线程b获取到Integer.class");
            }

        }
    }
}
```

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

优点

- key保存在int mKeys[]数组中，相对于HashMap不再对key进行自动装箱，避免资源消耗。但是vaule是保存在Object[] mValues数组中还是需要自动装箱的。
- 相对于HashMap，不再使用额外的Entry对象来存储数据，减少了内存开销。
- 数据量小的情况下，随机访问效率更高。

缺点

- 插入操作需要复制数组，增删效率低。
- 数据量巨大时，复制数组成本巨大，gc()成本也巨大。
- 数据量巨大时，查询效率也会明显下降。

### 了解Java注解的原理吗？

注解相当于一种标记，在程序中加了注解就等于为程序打上了某种标记。程序可以利用ava的反射机制来了解你的类及各种元素上有无何种标记，针对不同的标记，就去做相
应的事件。标记可以加在包，类，字段，方法，方法的参数以及局部变量上。

### String为什么要设计成不可变，StringBuffer与StringBuilder有什么区别？

StringBuffer是线程安全的，StringBuilder是非线程安全的。

## Android

### 手画一下Android系统架构图，描述一下各个层次的作用？

Android系统架构图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/android_system_structure.png" width="600"/>

从上到下依次分为六层：

- 应用框架层
- 进程通信层
- 系统服务层
- Android运行时层
- 硬件抽象层
- Linux内核层

### 描述一下Android的事件分发机制？

Android事件分发机制的本质：事件从哪个对象发出，经过哪些对象，最终由哪个对象处理了该事件。此处对象指的是Activity、Window与View。

Android事件的分发顺序：Activity（Window） -> ViewGroup -> View

Android事件的分发主要由三个方法来完成，如下所示：

```java
// 父View调用dispatchTouchEvent()开始分发事件
public boolean dispatchTouchEvent(MotionEvent event){
    boolean consume = false;
    // 父View决定是否拦截事件
    if(onInterceptTouchEvent(event)){
        // 父View调用onTouchEvent(event)消费事件，如果该方法返回true，表示
        // 该View消费了该事件，后续该事件序列的事件（Down、Move、Up）将不会在传递
        // 该其他View。
        consume = onTouchEvent(event);
    }else{
        // 调用子View的dispatchTouchEvent(event)方法继续分发事件
        consume = child.dispatchTouchEvent(event);
    }
    return consume;
}
```

### 描述一下View的绘制原理？

View的绘制流程主要分为三步：

1. onMeasure：测量视图的大小，从顶层父View到子View递归调用measure()方法，measure()调用onMeasure()方法，onMeasure()方法完成绘制工作。
2. onLayout：确定视图的位置，从顶层父View到子View递归调用layout()方法，父View将上一步measure()方法得到的子View的布局大小和布局参数，将子View放在合适的位置上。
3. onDraw：绘制最终的视图，首先ViewRoot创建一个Canvas对象，然后调用onDraw()方法进行绘制。onDraw()方法的绘制流程为：① 绘制视图背景。② 绘制画布的图层。 ③ 绘制View内容。
④ 绘制子视图，如果有的话。⑤ 还原图层。⑥ 绘制滚动条。

### 了解APK的打包流程吗，描述一下？

Android的包文件APK分为两个部分：代码和资源，所以打包方面也分为资源打包和代码打包两个方面，这篇文章就来分析资源和代码的编译打包原理。

APK整体的的打包流程如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/apk_package_flow.png"/>

具体说来：

1. 通过AAPT工具进行资源文件（包括AndroidManifest.xml、布局文件、各种xml资源等）的打包，生成R.java文件。
2. 通过AIDL工具处理AIDL文件，生成相应的Java文件。
3. 通过Javac工具编译项目源码，生成Class文件。
4. 通过DX工具将所有的Class文件转换成DEX文件，该过程主要完成Java字节码转换成Dalvik字节码，压缩常量池以及清除冗余信息等工作。
5. 通过ApkBuilder工具将资源文件、DEX文件打包生成APK文件。
6. 利用KeyStore对生成的APK文件进行签名。
7. 如果是正式版的APK，还会利用ZipAlign工具进行对齐处理，对齐的过程就是将APK文件中所有的资源文件举例文件的起始距离都偏移4字节的整数倍，这样通过内存映射访问APK文件
的速度会更快。

### 了解APK的安装流程吗，描述一下？

APK的安装流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/package/apk_install_structure.png" width="600"/>

1. 复制APK到/data/app目录下，解压并扫描安装包。
2. 资源管理器解析APK里的资源文件。
3. 解析AndroidManifest文件，并在/data/data/目录下创建对应的应用数据目录。
4. 然后对dex文件进行优化，并保存在dalvik-cache目录下。
5. 将AndroidManifest文件解析出的四大组件信息注册到PackageManagerService中。
5. 安装完成后，发送广播。

### 当点击一个应用图标以后，都发生了什么，描述一下这个过程？

点击应用图标后会去启动应用的LauncherActivity，如果LancerActivity所在的进程没有创建，还会创建新进程，整体的流程就是一个Activity的启动流程。

Activity的启动流程图（放大可查看）如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_start_flow.png" />

整个流程涉及的主要角色有：

- Instrumentation: 监控应用与系统相关的交互行为。
- AMS：组件管理调度中心，什么都不干，但是什么都管。
- ActivityStarter：Activity启动的控制器，处理Intent与Flag对Activity启动的影响，具体说来有：1 寻找符合启动条件的Activity，如果有多个，让用户选择；2 校验启动参数的合法性；3 返回int参数，代表Activity是否启动成功。
- ActivityStackSupervisior：这个类的作用你从它的名字就可以看出来，它用来管理任务栈。
- ActivityStack：用来管理任务栈里的Activity。
- ActivityThread：最终干活的人，是ActivityThread的内部类，Activity、Service、BroadcastReceiver的启动、切换、调度等各种操作都在这个类里完成。

注：这里单独提一下ActivityStackSupervisior，这是高版本才有的类，它用来管理多个ActivityStack，早期的版本只有一个ActivityStack对应着手机屏幕，后来高版本支持多屏以后，就
有了多个ActivityStack，于是就引入了ActivityStackSupervisior用来管理多个ActivityStack。

整个流程主要涉及四个进程：

- 调用者进程，如果是在桌面启动应用就是Launcher应用进程。
- ActivityManagerService等所在的System Server进程，该进程主要运行着系统服务组件。
- Zygote进程，该进程主要用来fork新进程。
- 新启动的应用进程，该进程就是用来承载应用运行的进程了，它也是应用的主线程（新创建的进程就是主线程），处理组件生命周期、界面绘制等相关事情。

有了以上的理解，整个流程可以概括如下：

1. 点击桌面应用图标，Launcher进程将启动Activity（MainActivity）的请求以Binder的方式发送给了AMS。
2. AMS接收到启动请求后，交付ActivityStarter处理Intent和Flag等信息，然后再交给ActivityStackSupervisior/ActivityStack
处理Activity进栈相关流程。同时以Socket方式请求Zygote进程fork新进程。
3. Zygote接收到新进程创建请求后fork出新进程。
4. 在新进程里创建ActivityThread对象，新创建的进程就是应用的主线程，在主线程里开启Looper消息循环，开始处理创建Activity。
5. ActivityThread利用ClassLoader去加载Activity、创建Activity实例，并回调Activity的onCreate()方法。这样便完成了Activity的启动。

### BroadcastReceiver与LocalBroadcastReceiver有什么区别？

- BroadcastReceiver 是跨应用广播，利用Binder机制实现。
- LocalBroadcastReceiver 是应用内广播，利用Handler实现，利用了IntentFilter的match功能，提供消息的发布与接收功能，实现应用内通信，效率比较高。

### Android Handler机制是做什么的，原理了解吗？

Android消息循环流程图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/android_handler_structure.png"/>

主要涉及的角色如下所示：

- Message：消息，分为硬件产生的消息（例如：按钮、触摸）和软件产生的消息。
- MessageQueue：消息队列，主要用来向消息池添加消息和取走消息。
- Looper：消息循环器，主要用来把消息分发给相应的处理者。
- Handler：消息处理器，主要向消息队列发送各种消息以及处理各种消息。

整个消息的循环流程还是比较清晰的，具体说来：

1. Handler通过sendMessage()发送消息Message到消息队列MessageQueue。
2. Looper通过loop()不断提取触发条件的Message，并将Message交给对应的target handler来处理。
3. target handler调用自身的handleMessage()方法来处理Message。

事实上，在整个消息循环的流程中，并不只有Java层参与，很多重要的工作都是在C++层来完成的。我们来看下这些类的调用关系。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/android_handler_class.png"/>

注：虚线表示关联关系，实线表示调用关系。

在这些类中MessageQueue是Java层与C++层维系的桥梁，MessageQueue与Looper相关功能都通过MessageQueue的Native方法来完成，而其他虚线连接的类只有关联关系，并没有
直接调用的关系，它们发生关联的桥梁是MessageQueue。

### Android Binder机制是做什么的，为什么选用Binder，原理了解吗？

Android Binder是用来做进程通信的，Android的各个应用以及系统服务都运行在独立的进程中，它们的通信都依赖于Binder。

为什么选用Binder，在讨论这个问题之前，我们知道Android也是基于Linux内核，Linux现有的进程通信手段有以下几种：

1. 管道：在创建时分配一个page大小的内存，缓存区大小比较有限；
2. 消息队列：信息复制两次，额外的CPU消耗；不合适频繁或信息量大的通信；
3. 共享内存：无须复制，共享缓冲区直接付附加到进程虚拟地址空间，速度快；但进程间的同步问题操作系统无法实现，必须各进程利用同步工具解决；
4. 套接字：作为更通用的接口，传输效率低，主要用于不通机器或跨网络的通信；
5. 信号量：常作为一种锁机制，防止某进程正在访问共享资源时，其他进程也访问该资源。因此，主要作为进程间以及同一进程内不同线程之间的同步手段。6. 信号: 不适用于信息交换，更适用于进程中断控制，比如非法内存访问，杀死某个进程等；

既然有现有的IPC方式，为什么重新设计一套Binder机制呢。主要是出于以上三个方面的考量：

- 高性能：从数据拷贝次数来看Binder只需要进行一次内存拷贝，而管道、消息队列、Socket都需要两次，共享内存不需要拷贝，Binder的性能仅次于共享内存。
- 稳定性：上面说到共享内存的性能优于Binder，那为什么不适用共享内存呢，因为共享内存需要处理并发同步问题，控制负责，容易出现死锁和资源竞争，稳定性较差。而Binder基于C/S架构，客户端与服务端彼此独立，稳定性较好。
- 安全性：我们知道Android为每个应用分配了UID，用来作为鉴别进程的重要标志，Android内部也依赖这个UID进行权限管理，包括6.0以前的固定权限和6.0以后的动态权限，传荣IPC只能由用户在数据包里填入UID/PID，这个标记完全
是在用户空间控制的，没有放在内核空间，因此有被恶意篡改的可能，因此Binder的安全性更高。

### 描述一下Activity的生命周期，这些生命周期是如何管理的？

Activity与Fragment生命周期如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/complete_android_fragment_lifecycle.png"/>

读者可以从上图看出，Activity有很多种状态，状态之间的变化也比较复杂，在众多状态中，只有三种是常驻状态：

- Resumed（运行状态）：Activity处于前台，用户可以与其交互。
- Paused（暂停状态）：Activity被其他Activity部分遮挡，无法接受用户的输入。
- Stopped（停止状态）：Activity被完全隐藏，对用户不可见，进入后台。

其他的状态都是中间状态。

我们再来看看生命周期变化时的整个调度流程，生命周期调度流程图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/activity_lifecycle_structure.png" />

所以你可以看到，整个流程是这样的：

1. 比方说我们点击跳转一个新Activity，这个时候Activity会入栈，同时它的生命周期也会从onCreate()到onResume()开始变换，这个过程是在ActivityStack里完成的，ActivityStack
是运行在Server进程里的，这个时候Server进程就通过ApplicationThread的代理对象ApplicationThreadProxy向运行在app进程ApplicationThread发起操作请求。
2. ApplicationThread接收到操作请求后，因为它是运行在app进程里的其他线程里，所以ApplicationThread需要通过Handler向主线程ActivityThread发送操作消息。
3. 主线程接收到ApplicationThread发出的消息后，调用主线程ActivityThread执行响应的操作，并回调Activity相应的周期方法。

注：这里提到了主线程ActivityThread，更准确来说ActivityThread不是线程，因为它没有继承Thread类或者实现Runnable接口，它是运行在应用主线程里的对象，那么应用的主线程
到底是什么呢？从本质上来讲启动启动时创建的进程就是主线程，线程和进程处理是否共享资源外，没有其他的区别，对于Linux来说，它们都只是一个struct结构体。

### Activity的通信方式有哪些？

- startActivityForResult
- EventBus
- LocalBroadcastReceiver

### Android应用里有几种Context对象，

Context类图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/context_uml.png" width="600" />

可以发现Context是个抽象类，它的具体实现类是ContextImpl，ContextWrapper是个包装类，内部的成员变量mBase指向的也是个ContextImpl对象，ContextImpl完成了
实际的功能，Activity、Service与Application都直接或者间接的继承ContextWrapper。

### 描述一下进程和Application的生命周期？

一个安装的应用对应一个LoadedApk对象，对应一个Application对象，对于四大组件，Application的创建和获取方式也是不尽相同的，具体说来：

- Activity：通过LoadedApk的makeApplication()方法创建。
- Service：通过LoadedApk的makeApplication()方法创建。
- 静态广播：通过其回调方法onReceive()方法的第一个参数指向Application。
- ContentProvider：无法获取Application，因此此时Application不一定已经初始化。

### Android哪些情况会导致内存泄漏，如何分析内存泄漏？

常见的产生内存泄漏的情况如下所示：

- 持有静态的Context（Activity）引用。
- 持有静态的View引用，
- 内部类&匿名内部类实例无法释放（有延迟时间等等），而内部类又持有外部类的强引用，导致外部类无法释放，这种匿名内部类常见于监听器、Handler、Thread、TimerTask
- 资源使用完成后没有关闭，例如：BraodcastReceiver，ContentObserver，File，Cursor，Stream，Bitmap。
- 不正确的单例模式，比如单例持有Activity。
- 集合类内存泄漏，如果一个集合类是静态的（缓存HashMap），只有添加方法，没有对应的删除方法，会导致引用无法被释放，引发内存泄漏。
- 错误的覆写了finalize()方法，finalize()方法执行执行不确定，可能会导致引用无法被释放。

查找内存泄漏可以使用Android Profiler工具或者利用LeakCanary工具。

### Android有哪几种进程，是如何管理的？

Android的进程主要分为以下几种：

**前台进程**

用户当前操作所必需的进程。如果一个进程满足以下任一条件，即视为前台进程：

- 托管用户正在交互的 Activity（已调用 Activity 的 onResume() 方法）
- 托管某个 Service，后者绑定到用户正在交互的 Activity
- 托管正在“前台”运行的 Service（服务已调用 startForeground()）
- 托管正执行一个生命周期回调的 Service（onCreate()、onStart() 或 onDestroy()）
- 托管正执行其 onReceive() 方法的 BroadcastReceiver

通常，在任意给定时间前台进程都为数不多。只有在内存不足以支持它们同时继续运行这一万不得已的情况下，系统才会终止它们。 此时，设备往往已达到内存分页状态，因此需要终止一些前台进程来确保用户界面正常响应。

**可见进程**

没有任何前台组件、但仍会影响用户在屏幕上所见内容的进程。 如果一个进程满足以下任一条件，即视为可见进程：

- 托管不在前台、但仍对用户可见的 Activity（已调用其 onPause() 方法）。例如，如果前台 Activity 启动了一个对话框，允许在其后显示上一 Activity，则有可能会发生这种情况。
- 托管绑定到可见（或前台）Activity 的 Service。

可见进程被视为是极其重要的进程，除非为了维持所有前台进程同时运行而必须终止，否则系统不会终止这些进程。

**服务进程**

正在运行已使用 startService() 方法启动的服务且不属于上述两个更高类别进程的进程。尽管服务进程与用户所见内容没有直接关联，但是它们通常在执行一些用户关
心的操作（例如，在后台播放音乐或从网络下载数据）。因此，除非内存不足以维持所有前台进程和可见进程同时运行，否则系统会让服务进程保持运行状态。

**后台进程**

包含目前对用户不可见的 Activity 的进程（已调用 Activity 的 onStop() 方法）。这些进程对用户体验没有直接影响，系统可能随时终止它们，以回收内存供前台进程、可见进程或服务进程使用。 通常会有很多后台进程在运行，因此它们会保存在 LRU （最近最少使用）列表中，以确保包含用户最近查看的 Activity 的进程最后一个被终止。如果某个 Activity 正确实现了生命周期方法，并保存了其当前状态，则终止其进程不会对用户体验产生明显影响，因为当用户导航回该 Activity 时，Activity 会恢复其所有可见状态。

**空进程**

不含任何活动应用组件的进程。保留这种进程的的唯一目的是用作缓存，以缩短下次在其中运行组件所需的启动时间。 为使总体系统资源在进程缓存和底层内核缓存之间保持平衡，系统往往会终止这些进程。

ActivityManagerService负责根据各种策略算法计算进程的adj值，然后交由系统内核进行进程的管理。

### SharePreference性能优化，可以做进程同步吗？

在Android中, SharePreferences是一个轻量级的存储类，特别适合用于保存软件配置参数。使用SharedPreferences保存数据，其背后是用xml文件存放数据，文件
存放在/data/data/ < package name > /shared_prefs目录下.

之所以说SharedPreference是一种轻量级的存储方式，是因为它在创建的时候会把整个文件全部加载进内存，如果SharedPreference文件比较大，会带来以下问题：

1. 第一次从sp中获取值的时候，有可能阻塞主线程，使界面卡顿、掉帧。
2. 解析sp的时候会产生大量的临时对象，导致频繁GC，引起界面卡顿。
3. 这些key和value会永远存在于内存之中，占用大量内存。

优化建议

1. 不要存放大的key和value，会引起界面卡、频繁GC、占用内存等等。
2. 毫不相关的配置项就不要放在在一起，文件越大读取越慢。
3. 读取频繁的key和不易变动的key尽量不要放在一起，影响速度，如果整个文件很小，那么忽略吧，为了这点性能添加维护成本得不偿失。
4. 不要乱edit和apply，尽量批量修改一次提交，多次apply会阻塞主线程。
5. 尽量不要存放JSON和HTML，这种场景请直接使用JSON。
6. SharedPreference无法进行跨进程通信，MODE_MULTI_PROCESS只是保证了在API 11以前的系统上，如果sp已经被读取进内存，再次获取这个SharedPreference的时候，如果有这个flag，会重新读一遍文件，仅此而已。

### 如何做SQLite升级？

数据库升级增加表和删除表都不涉及数据迁移，但是修改表涉及到对原有数据进行迁移。升级的方法如下所示：

1. 将现有表命名为临时表。
2. 创建新表。
3. 将临时表的数据导入新表。
4. 删除临时表。

重写

如果是跨版本数据库升级，可以由两种方式，如下所示：

1. 逐级升级，确定相邻版本与现在版本的差别，V1升级到V2,V2升级到V3，依次类推。
2. 跨级升级，确定每个版本与现在数据库的差别，为每个case编写专门升级大代码。

### 进程保护如何做，如何唤醒其他进程？

进程保活主要有两个思路：

1. 提升进程的优先级，降低进程被杀死的概率。
2. 拉活已经被杀死的进程。

如何提升优先级，如下所示：

监控手机锁屏事件，在屏幕锁屏时启动一个像素的Activity，在用户解锁时将Activity销毁掉，前台Activity可以将进程变成前台进程，优先级升级到最高。

如果拉活

利用广播拉活Activity。

### 理解序列化吗，Android为什么引入Parcelable？

所谓序列化就是将对象变成二进制流，便于存储和传输。

- Serializable是java实现的一套序列化方式，可能会触发频繁的IO操作，效率比较低，适合将对象存储到磁盘上的情况。
- Parcelable是Android提供一套序列化机制，它将序列化后的字节流写入到一个共性内存中，其他对象可以从这块共享内存中读出字节流，并反序列化成对象。因此效率比较高，适合在对象间或者进程间传递信息。

### 如何计算一个Bitmap占用内存的大小，怎么保证加载Bitmap不产生内存溢出？

Bitamp 占用内存大小 = 宽度像素 x （inTargetDensity / inDensity） x 高度像素 x （inTargetDensity / inDensity）x 一个像素所占的内存

注：这里inDensity表示目标图片的dpi（放在哪个资源文件夹下），inTargetDensity表示目标屏幕的dpi，所以你可以发现inDensity和inTargetDensity会对Bitmap的宽高
进行拉伸，进而改变Bitmap占用内存的大小。

在Bitmap里有两个获取内存占用大小的方法。

- getByteCount()：API12 加入，代表存储 Bitmap 的像素需要的最少内存。
- getAllocationByteCount()：API19 加入，代表在内存中为 Bitmap 分配的内存大小，代替了 getByteCount() 方法。

在不复用 Bitmap 时，getByteCount() 和 getAllocationByteCount 返回的结果是一样的。在通过复用 Bitmap 来解码图片时，那么 getByteCount() 表示新解码图片占用内存的大
小，getAllocationByteCount() 表示被复用 Bitmap真实占用的内存大小（即 mBuffer 的长度）。

为了保证在加载Bitmap的时候不产生内存溢出，可以受用BitmapFactory进行图片压缩，主要有以下几个参数：

- BitmapFactory.Options.inPreferredConfig：将ARGB_8888改为RGB_565，改变编码方式，节约内存。
- BitmapFactory.Options.inSampleSize：缩放比例，可以参考Luban那个库，根据图片宽高计算出合适的缩放比例。
- BitmapFactory.Options.inPurgeable：让系统可以内存不足时回收内存。

### Android里的内存缓存和磁盘缓存是怎么实现的。

内存缓存基于LruCache实现，磁盘缓存基于DiskLruCache实现。这两个类都基于Lru算法和LinkedHashMap来实现。

LRU算法可以用一句话来描述，如下所示：

>LRU是Least Recently Used的缩写，最近最久未使用算法，从它的名字就可以看出，它的核心原则是如果一个数据在最近一段时间没有使用到，那么它在将来被
访问到的可能性也很小，则这类数据项会被优先淘汰掉。

LruCache的原理是利用LinkedHashMap持有对象的强引用，按照Lru算法进行对象淘汰。具体说来假设我们从表尾访问数据，在表头删除数据，当访问的数据项在链表中存在时，则将该数据项移动到表尾，否则在表尾新建一个数据项。当链表容量超过一定阈值，则移除表头的数据。
                                                      
为什么会选择LinkedHashMap呢？

这跟LinkedHashMap的特性有关，LinkedHashMap的构造函数里有个布尔参数accessOrder，当它为true时，LinkedHashMap会以访问顺序为序排列元素，否则以插入顺序为序排序元素。

DiskLruCache与LruCache原理相似，只是多了一个journal文件来做磁盘文件的管理和迎神，如下所示：

``
libcore.io.DiskLruCache
1
1
1

DIRTY 1517126350519
CLEAN 1517126350519 5325928
REMOVE 1517126350519
```

注：这里的缓存目录是应用的缓存目录/data/data/pckagename/cache，未root的手机可以通过以下命令进入到该目录中或者将该目录整体拷贝出来：

```java

//进入/data/data/pckagename/cache目录
adb shell
run-as com.your.packagename 
cp /data/data/com.your.packagename/

//将/data/data/pckagename目录拷贝出来
adb backup -noapk com.your.packagename
```

我们来分析下这个文件的内容：

- 第一行：libcore.io.DiskLruCache，固定字符串。
- 第二行：1，DiskLruCache源码版本号。
- 第三行：1，App的版本号，通过open()方法传入进去的。
- 第四行：1，每个key对应几个文件，一般为1.
- 第五行：空行
- 第六行及后续行：缓存操作记录。

第六行及后续行表示缓存操作记录，关于操作记录，我们需要了解以下三点：

1. DIRTY 表示一个entry正在被写入。写入分两种情况，如果成功会紧接着写入一行CLEAN的记录；如果失败，会增加一行REMOVE记录。注意单独只有DIRTY状态的记录是非法的。
2. 当手动调用remove(key)方法的时候也会写入一条REMOVE记录。
3. READ就是说明有一次读取的记录。
4. CLEAN的后面还记录了文件的长度，注意可能会一个key对应多个文件，那么就会有多个数字。

### 了解插件化和热修复吗，它们有什么区别，理解它们的原理吗？

- 插件化：插件化是体现在功能拆分方面的，它将某个功能独立提取出来，独立开发，独立测试，再插入到主应用中。依次来较少主应用的规模。
- 热修复：热修复是体现在bug修复方面的，它实现的是不需要重新发版和重新安装，就可以去修复已知的bug。

利用PathClassLoader和DexClassLoader去加载与bug类同名的类，替换掉bug类，进而达到修复bug的目的，原理是在app打包的时候阻止类打上CLASS_ISPREVERIFIED标志，然后在
热修复的时候动态改变BaseDexClassLoader对象间接引用的dexElements，替换掉旧的类。

## 网络编程

### TCP与UDP的区别

1. TCP面向连接（如打电话要先拨号建立连接）;UDP是无连接的，即发送数据之前不需要建立连接
2. TCP提供可靠的服务。也就是说，通过TCP连接传送的数据，无差错，不丢失，不重复，且按序到达;UDP尽最大努力交付，即不保证可靠交付
3. TCP面向字节流，实际上是TCP把数据看成一连串无结构的字节流;UDP是面向报文的UDP没有拥塞控制，因此网络出现拥塞不会使源主机的发送速率降低（对实时应用很有用，如IP电话，实时视频会议等）
4. 每一条TCP连接只能是点到点的;UDP支持一对一，一对多，多对一和多对多的交互通信
5. TCP首部开销20字节;UDP的首部开销小，只有8个字节
6. TCP的逻辑通信信道是全双工的可靠信道，UDP则是不可靠信道

### TCP三次握手与四次分手

TCP用[三次握手](https://zh.wikipedia.org/wiki/%E4%BC%A0%E8%BE%93%E6%8E%A7%E5%88%B6%E5%8D%8F%E8%AE%AE#建立通路)（three-way handshake）过程创建一个连接，使用四次分手
关闭一个连接。

三次握手与四次分手的流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/network/three_way_handshake.jpeg" width="500"/>

三次握手

- 第一次握手：建立连接。客户端发送连接请求报文段，将SYN位置为1，Sequence Number为x；然后，客户端进入SYN_SEND状态，等待服务器的确认；
- 第二次握手：服务器收到SYN报文段。服务器收到客户端的SYN报文段，需要对这个SYN报文段进行确认，设置Acknowledgment Number为x+1(Sequence Number+1)；同时，自己自己还要发送SYN请求信息，将SYN位置为1，Sequence Number为y；服务器端将上述所有信息放到一个报文段（即SYN+ACK报文段）中，一并发送给客户端，此时服务器进入SYN_RECV状态；
- 第三次握手：客户端收到服务器的SYN+ACK报文段。然后将Acknowledgment Number设置为y+1，向服务器发送ACK报文段，这个报文段发送完毕以后，客户端和服务器端都进入ESTABLISHED状态，完成TCP三次握手。
完成了三次握手，客户端和服务器端就可以开始传送数据。以上就是TCP三次握手的总体介绍。

四次分手

- 第一次分手：主机1（可以使客户端，也可以是服务器端），设置Sequence Number和Acknowledgment Number，向主机2发送一个FIN报文段；此时，主机1进入FIN_WAIT_1状态；这表示主机1没有数据要发送给主机2了；
- 第二次分手：主机2收到了主机1发送的FIN报文段，向主机1回一个ACK报文段，Acknowledgment Number为Sequence Number加1；主机1进入FIN_WAIT_2状态；主机2告诉主机1，我“同意”你的关闭请求；
- 第三次分手：主机2向主机1发送FIN报文段，请求关闭连接，同时主机2进入LAST_ACK状态；
- 第四次分手：主机1收到主机2发送的FIN报文段，向主机2发送ACK报文段，然后主机1进入TIME_WAIT状态；主机2收到主机1的ACK报文段以后，就关闭连接；此时，主机1等待2MSL后依然没有收到回复，则证明Server端已正常关闭，那好，主机1也可以关闭连接了。

三次握手与四次分手也是个老生常谈的概念，举个简单的例子说明一下。

三次握手

>例如你小时候出去玩，经常玩忘了回家吃饭。你妈妈也经常过来喊你。如果你没有走远，在门口的小土堆上玩泥巴，你妈妈会喊："小新，回家吃饭了"。你听到后会回应："知道了，一会就回去"。妈妈听
到你的回应后又说："快点回来，饭要凉了"。这样你妈妈和你就完成了三次握手的过程。😁说到这里你也可以理解三次握手的必要性，少了其中一个环节，另一方就会陷入等待之中。

三次握手的目的是为了防止已失效的连接请求报文段突然又传送到了服务端，因而产生错误.

四次分手

>例如偶像言情剧干净利落的分手，女主对男主说：我们分手吧🙄，男主说：分就分吧😰。女主说：你果然是不爱我了，你只知道让我多喝热水🙄。男主说：事到如今也没什么好说的了，祝你幸福🙃。四次分手完成。说到这里你可以理解
了四次分手的必要性，第一次是女方（客户端）提出分手，第二次是男主（服务端）同意女主分手，第三次是女主确定男主不再爱她，也同意男主分手。第四次两人彻底拜拜（断开连接）。

因为TCP是全双工模式，所以四次分手的目的就是为了可靠地关闭连接。

### HTTP与HTTPS

>[HTTPS](https://zh.wikipedia.org/wiki/%E8%B6%85%E6%96%87%E6%9C%AC%E4%BC%A0%E8%BE%93%E5%AE%89%E5%85%A8%E5%8D%8F%E8%AE%AE)是一种通过计算机网络进行安全通信的传输协议。HTTPS经由HTTP进行通信，但利用SSL/TLS来加密数据包。HTTPS开发的主要目的，是提供对网站服务器的身份
认证，保护交换数据的隐私与完整性。

如下图所示，可以很明显的看出两个的区别：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/network/http_https.png" width="500"/>

注：TLS是SSL的升级替代版，具体发展历史可以参考[传输层安全性协议](https://zh.wikipedia.org/wiki/%E5%82%B3%E8%BC%B8%E5%B1%A4%E5%AE%89%E5%85%A8%E6%80%A7%E5%8D%94%E5%AE%9A)。

HTTP与HTTPS在写法上的区别也是前缀的不同，客户端处理的方式也不同，具体说来：

- 如果URL的协议是HTTP，则客户端会打开一条到服务端端口80（默认）的连接，并向其发送老的HTTP请求。
- 如果URL的协议是HTTPS，则客户端会打开一条到服务端端口443（默认）的连接，然后与服务器握手，以二进制格式与服务器交换一些SSL的安全参数，附上加密的
HTTP请求。

所以你可以看到，HTTPS比HTTP多了一层与SSL的连接，这也就是客户端与服务端SSL握手的过程，整个过程主要完成以下工作：

- 交换协议版本号
- 选择一个两端都了解的密码
- 对两端的身份进行认证
- 生成临时的会话密钥，以便加密信道。

SSL握手是一个相对比较复杂的过程，更多关于SSL握手的过程细节可以参考[TLS/SSL握手过程](https://www.wosign.com/faq/faq2016-0309-04.htm)

SSL/TSL的常见开源实现是OpenSSL，OpenSSL是一个开放源代码的软件库包，应用程序可以使用这个包来进行安全通信，避免窃听，同时确认另一端连接者的身份。这个包广泛被应用在互联网的网页服务器上。
更多源于OpenSSL的技术细节可以参考[OpenSSL](https://www.openssl.org/)。

## 应用优化

Android的应用优化可以从以下几个角度去考虑。

### 如何做性能优化？

1. 节制的使用Service，当启动一个Service时，系统总是倾向于保留这个Service依赖的进程，这样会造成系统资源的浪费，可以使用IntentService，执行完成任务后会自动停止。
2. 当界面不可见时释放内存，可以重写Activity的onTrimMemory()方法，然后监听TRIM_MEMORY_UI_HIDDEN这个级别，这个级别说明用户离开了页面，可以考虑释放内存和资源。
3. 避免在Bitmap浪费过多的内存，使用压缩过的图片，也可以使用Fresco等库来优化对Bitmap显示的管理。
4. 使用优化过的数据集合SparseArray代替HashMap，HashMap为每个键值都提供一个对象入口，使用SparseArray可以免去基本对象类型转换为引用数据类想的时间。

### 如何做布局优化？

1. 使用include复用布局文件。
2. 使用merge标签避免嵌套布局。
3. 使用stub标签仅在需要的时候在展示出来。

### 如何做高性能编码？

1. 避免创建不必要的对象，尽可能避免频繁的创建临时对象，例如在for循环内，减少GC的次数。
2. 尽量使用基本数据类型代替引用数据类型。
3. 静态方法调用效率高于动态方法，也可以避免创建额外对象。
4. 对于基本数据类型和String类型的常量要使用static final修饰，这样常量会在dex文件的初始化器中进行初始化，使用的时候可以直接使用。
5. 多使用系统API，例如数组拷贝System.arrayCopy()方法，要比我们用for循环效率快9倍以上，因为系统API很多都是通过底层的汇编模式执行的，效率比较高。

## 数据结构与算法

### 字符串翻转

```
string:  Hello
  length:  5
  
          0 1 2 3 4 
  before: H e l l o
  after:  o l l e H
  
  index             sum
  0: H--->o  0-->4  4
  1: e--->l  1-->3  4
  2: l--->l  2-->2  4
```

解法一：使用数组

1. 将字符串转换为char数组
2. 遍历循环给char数组赋值

```java
public static String strReverseWithArray2(String string){
    if(string==null||string.length()==0)return string;
    int length = string.length();
    char [] array = string.toCharArray();
    for(int i = 0;i<length/2;i++){
        array[i] = string.charAt(length-1-i);
        array[length-1-i] = string.charAt(i);
    }
    return new String(array);
}
```

解法二：使用栈

1. 将字符串转换为char数组
2. 将char数组中的字符依次压入栈中
3. 将栈中的字符依次弹出赋值给char数组

```java
public static String strReverseWithStack(String string){
    if(string==null||string.length()==0)return string;
    Stack<Character> stringStack = new Stack<>();
    char [] array = string.toCharArray();
    for(Character c:array){
        stringStack.push(c);
    }
    int length = string.length();
    for(int i= 0;i<length;i++){
        array[i] = stringStack.pop();
    }
    return new String(array);
}
```

解法三：逆序遍历

1. 逆序遍历字符串中的字符，并将它依次添加到StringBuilder中

```java

public static String strReverseWithReverseLoop(String string){
        if(string==null||string.length()==0)return string;
        StringBuilder sb = new StringBuilder();
        for(int i = string.length()-1;i>=0;i--){
            sb.append(string.charAt(i));
        }
        return sb.toString();
    }
```

### 单链表反转，合并多个单链表

单链表的结构就像一个火车的结构，火车头拉着许多车厢，实现链表翻转，可以利用递归翻转法，在反转当前节点之前先反转后续节点。这样从头结点开始，层层深入直到尾结点才开始反转指针域的指向。简单的
说就是从尾结点开始，逆向反转各个结点的指针域指向，

```java
public class LinkedListReverse {  
    
    public static void main(String[] args) {  
        Node head = new Node(0);  
        Node node1 = new Node(1);  
        Node node2 = new Node(2);  
        Node node3 = new Node(3);  
        head.setNext(node1);  
        node1.setNext(node2);  
        node2.setNext(node3);  
  
        // 打印反转前的链表  
        Node h = head;  
        while (null != h) {  
            System.out.print(h.getData() + " ");  
            h = h.getNext();  
        }  
        // 调用反转方法  
        head = Reverse1(head);  
  
        System.out.println("\n**************************");  
        // 打印反转后的结果  
        while (null != head) {  
            System.out.print(head.getData() + " ");  
            head = head.getNext();  
        }  
    }  
  
    /** 
     * 递归，在反转当前节点之前先反转后续节点 
     */  
    public static Node Reverse1(Node head) {  
        // head看作是前一结点，head.getNext()是当前结点，reHead是反转后新链表的头结点  
        if (head == null || head.getNext() == null) {  
            return head;// 若为空链或者当前结点在尾结点，则直接还回  
        }  
        Node reHead = Reverse1(head.getNext());// 先反转后续节点head.getNext()  
        head.getNext().setNext(head);// 将当前结点的指针域指向前一结点  
        head.setNext(null);// 前一结点的指针域令为null;  
        return reHead;// 反转后新链表的头结点  
    }  
}  
  
    class Node {  
        private int Data;// 数据域  
        private Node Next;// 指针域  
  
        public Node(int Data) {  
            // super();  
            this.Data = Data;  
        }  
  
        public int getData() {  
            return Data;  
        }  
  
        public void setData(int Data) {  
            this.Data = Data;  
        }  
  
        public Node getNext() {  
            return Next;  
        }  
  
        public void setNext(Node Next) {  
            this.Next = Next;  
        }  
    }  

```

### 排序算法

以下排序算法内容来自：[面试中的 10 大排序算法总结.md](https://github.com/francistao/LearningNotes/blob/master/Part3/Algorithm/Sort/%E9%9D%A2%E8%AF%95%E4%B8%AD%E7%9A%84%2010%20%E5%A4%A7%E6%8E%92%E5%BA%8F%E7%AE%97%E6%B3%95%E6%80%BB%E7%BB%93.md)

<p>查找和排序算法是算法的入门知识，其经典思想可以用于很多算法当中。因为其实现代码较短，应用较常见。所以在面试中经常会问到排序算法及其相关的问题。但万变不离其宗，只要熟悉了思想，灵活运用也不是难事。一般在面试中最常考的是快速排序和归并排序，并且经常有面试官要求现场写出这两种排序的代码。对这两种排序的代码一定要信手拈来才行。还有插入排序、冒泡排序、堆排序、基数排序、桶排序等。面试官对于这些排序可能会要求比较各自的优劣、各种算法的思想及其使用场景。还有要会分析算法的时间和空间复杂度。通常查找和排序算法的考察是面试的开始，如果这些问题回答不好，估计面试官都没有继续面试下去的兴趣都没了。所以想开个好头就要把常见的排序算法思想及其特点要熟练掌握，有必要时要熟练写出代码。</p>
<p>接下来我们就分析一下常见的排序算法及其使用场景。限于篇幅，某些算法的详细演示和图示请自行寻找详细的参考。</p>
<h2>冒泡排序</h2>
<p>冒泡排序是最简单的排序之一了，其大体思想就是通过与相邻元素的比较和交换来把小的数交换到最前面。这个过程类似于水泡向上升一样，因此而得名。举个栗子，对5,3,8,6,4这个无序序列进行冒泡排序。首先从后向前冒泡，4和6比较，把4交换到前面，序列变成5,3,8,4,6。同理4和8交换，变成5,3,4,8,6,3和4无需交换。5和3交换，变成3,5,4,8,6,3.这样一次冒泡就完了，把最小的数3排到最前面了。对剩下的序列依次冒泡就会得到一个有序序列。冒泡排序的时间复杂度为O(n^2)。</p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;冒泡排序算法实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-3 下午8:54:27
 */
public class BubbleSort {

    public static void bubbleSort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;
        for(int i=0; i&lt;arr.length-1; i++) {
            for(int j=arr.length-1; j&gt;i; j--) {
                if(arr[j] &lt; arr[j-1]) {
                    swap(arr, j-1, j);
                }
            }
        }
    }

    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}</pre>
</div>
<h2>选择排序</h2>
<p>选择排序的思想其实和冒泡排序有点类似，都是在一次排序后把最小的元素放到最前面。但是过程不同，冒泡排序是通过相邻的比较和交换。而选择排序是通过对整体的选择。举个栗子，对5,3,8,6,4这个无序序列进行简单选择排序，首先要选择5以外的最小数来和5交换，也就是选择3和5交换，一次排序后就变成了3,5,8,6,4.对剩下的序列一次进行选择和交换，最终就会得到一个有序序列。其实选择排序可以看成冒泡排序的优化，因为其目的相同，只是选择排序只有在确定了最小数的前提下才进行交换，大大减少了交换的次数。选择排序的时间复杂度为O(n^2)</p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;简单选择排序算法的实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-3 下午9:13:35
 */
public class SelectSort {

    public static void selectSort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;
        int minIndex = 0;
        for(int i=0; i&lt;arr.length-1; i++) { //只需要比较n-1次
            minIndex = i;
            for(int j=i+1; j&lt;arr.length; j++) { //从i+1开始比较，因为minIndex默认为i了，i就没必要比了。
                if(arr[j] &lt; arr[minIndex]) {
                    minIndex = j;
                }
            }

            if(minIndex != i) { //如果minIndex不为i，说明找到了更小的值，交换之。
                swap(arr, i, minIndex);
            }
        }

    }

    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

}</pre>
</div>
<h2>插入排序</h2>
<p>插入排序不是通过交换位置而是通过比较找到合适的位置插入元素来达到排序的目的的。相信大家都有过打扑克牌的经历，特别是牌数较大的。在分牌时可能要整理自己的牌，牌多的时候怎么整理呢？就是拿到一张牌，找到一个合适的位置插入。这个原理其实和插入排序是一样的。举个栗子，对5,3,8,6,4这个无序序列进行简单插入排序，首先假设第一个数的位置时正确的，想一下在拿到第一张牌的时候，没必要整理。然后3要插到5前面，把5后移一位，变成3,5,8,6,4.想一下整理牌的时候应该也是这样吧。然后8不用动，6插在8前面，8后移一位，4插在5前面，从5开始都向后移一位。注意在插入一个数的时候要保证这个数前面的数已经有序。简单插入排序的时间复杂度也是O(n^2)。</p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;简单插入排序算法实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-3 下午9:38:55
 */
public class InsertSort {

    public static void insertSort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;

        for(int i=1; i&lt;arr.length; i++) { //假设第一个数位置时正确的；要往后移，必须要假设第一个。

            int j = i;
            int target = arr[i]; //待插入的

            //后移
            while(j &gt; 0 &amp;&amp; target &lt; arr[j-1]) {
                arr[j] = arr[j-1];
                j --;
            }

            //插入 
            arr[j] = target;
        }

    }

}</pre>
</div>
<h2>快速排序</h2>
<p>快速排序一听名字就觉得很高端，在实际应用当中快速排序确实也是表现最好的排序算法。冒泡排序虽然高端，但其实其思想是来自冒泡排序，冒泡排序是通过相邻元素的比较和交换把最小的冒泡到最顶端，而快速排序是比较和交换小数和大数，这样一来不仅把小数冒泡到上面同时也把大数沉到下面。</p>
<p>举个栗子：对5,3,8,6,4这个无序序列进行快速排序，思路是右指针找比基准数小的，左指针找比基准数大的，交换之。</p>
<p>5,3,8,6,4 用5作为比较的基准，最终会把5小的移动到5的左边，比5大的移动到5的右边。</p>
<p>5,3,8,6,4 首先设置i,j两个指针分别指向两端，j指针先扫描（思考一下为什么？）4比5小停止。然后i扫描，8比5大停止。交换i,j位置。</p>
<p>5,3,4,6,8 然后j指针再扫描，这时j扫描4时两指针相遇。停止。然后交换4和基准数。</p>
<p>4,3,5,6,8 一次划分后达到了左边比5小，右边比5大的目的。之后对左右子序列递归排序，最终得到有序序列。</p>
<p>上面留下来了一个问题为什么一定要j指针先动呢？首先这也不是绝对的，这取决于基准数的位置，因为在最后两个指针相遇的时候，要交换基准数到相遇的位置。一般选取第一个数作为基准数，那么就是在左边，所以最后相遇的数要和基准数交换，那么相遇的数一定要比基准数小。所以j指针先移动才能先找到比基准数小的数。</p>
<p>快速排序是不稳定的，其时间平均时间复杂度是O(nlgn)。</p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;实现快速排序算法&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-3 下午5:07:29
 */
public class QuickSort {
    //一次划分
    public static int partition(int[] arr, int left, int right) {
        int pivotKey = arr[left];
        int pivotPointer = left;

        while(left &lt; right) {
            while(left &lt; right &amp;&amp; arr[right] &gt;= pivotKey)
                right --;
            while(left &lt; right &amp;&amp; arr[left] &lt;= pivotKey)
                left ++;
            swap(arr, left, right); //把大的交换到右边，把小的交换到左边。
        }
        swap(arr, pivotPointer, left); //最后把pivot交换到中间
        return left;
    }

    public static void quickSort(int[] arr, int left, int right) {
        if(left &gt;= right)
            return ;
        int pivotPos = partition(arr, left, right);
        quickSort(arr, left, pivotPos-1);
        quickSort(arr, pivotPos+1, right);
    }

    public static void sort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;
        quickSort(arr, 0, arr.length-1);
    }

    public static void swap(int[] arr, int left, int right) {
        int temp = arr[left];
        arr[left] = arr[right];
        arr[right] = temp;
    }

}</pre>
</div>
<p>其实上面的代码还可以再优化，上面代码中基准数已经在pivotKey中保存了，所以不需要每次交换都设置一个temp变量，在交换左右指针的时候只需要先后覆盖就可以了。这样既能减少空间的使用还能降低赋值运算的次数。优化代码如下：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;实现快速排序算法&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-3 下午5:07:29
 */
public class QuickSort {

    /**
     * 划分
     * @param arr
     * @param left
     * @param right
     * @return
     */
    public static int partition(int[] arr, int left, int right) {
        int pivotKey = arr[left];

        while(left &lt; right) {
            while(left &lt; right &amp;&amp; arr[right] &gt;= pivotKey)
                right --;
            arr[left] = arr[right]; //把小的移动到左边
            while(left &lt; right &amp;&amp; arr[left] &lt;= pivotKey)
                left ++;
            arr[right] = arr[left]; //把大的移动到右边
        }
        arr[left] = pivotKey; //最后把pivot赋值到中间
        return left;
    }

    /**
     * 递归划分子序列
     * @param arr
     * @param left
     * @param right
     */
    public static void quickSort(int[] arr, int left, int right) {
        if(left &gt;= right)
            return ;
        int pivotPos = partition(arr, left, right);
        quickSort(arr, left, pivotPos-1);
        quickSort(arr, pivotPos+1, right);
    }

    public static void sort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;
        quickSort(arr, 0, arr.length-1);
    }

}</pre>
</div>
<p>总结快速排序的思想：冒泡+二分+递归分治，慢慢体会。。。</p>
<h2>堆排序</h2>
<p>堆排序是借助堆来实现的选择排序，思想同简单的选择排序，以下以大顶堆为例。注意：如果想升序排序就使用大顶堆，反之使用小顶堆。原因是堆顶元素需要交换到序列尾部。</p>
<p>首先，实现堆排序需要解决两个问题：</p>
<p>1. 如何由一个无序序列键成一个堆？</p>
<p>2. 如何在输出堆顶元素之后，调整剩余元素成为一个新的堆？</p>
<p>第一个问题，可以直接使用线性数组来表示一个堆，由初始的无序序列建成一个堆就需要自底向上从第一个非叶元素开始挨个调整成一个堆。</p>
<p>第二个问题，怎么调整成堆？首先是将堆顶元素和最后一个元素交换。然后比较当前堆顶元素的左右孩子节点，因为除了当前的堆顶元素，左右孩子堆均满足条件，这时需要选择当前堆顶元素与左右孩子节点的较大者（大顶堆）交换，直至叶子节点。我们称这个自堆顶自叶子的调整成为筛选。</p>
<p>从一个无序序列建堆的过程就是一个反复筛选的过程。若将此序列看成是一个完全二叉树，则最后一个非终端节点是n/2取底个元素，由此筛选即可。举个栗子：</p>
<p>49,38,65,97,76,13,27,49序列的堆排序建初始堆和调整的过程如下：</p>
<p><img src="http://static.codeceo.com/images/2016/03/2614bce119263edcf9d18b6365b39197.png" alt="" /></p>
<p><img src="http://static.codeceo.com/images/2016/03/ad373a589182dd1b7e443915c8775fcd.png" alt="" /></p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;堆排序算法的实现，以大顶堆为例。&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-4 上午9:26:02
 */
public class HeapSort {

    /**
     * 堆筛选，除了start之外，start~end均满足大顶堆的定义。
     * 调整之后start~end称为一个大顶堆。
     * @param arr 待调整数组
     * @param start 起始指针
     * @param end 结束指针
     */
    public static void heapAdjust(int[] arr, int start, int end) {
        int temp = arr[start];

        for(int i=2*start+1; i&lt;=end; i*=2) {
            //左右孩子的节点分别为2*i+1,2*i+2

            //选择出左右孩子较小的下标
            if(i &lt; end &amp;&amp; arr[i] &lt; arr[i+1]) {
                i ++; 
            }
            if(temp &gt;= arr[i]) {
                break; //已经为大顶堆，=保持稳定性。
            }
            arr[start] = arr[i]; //将子节点上移
            start = i; //下一轮筛选
        }

        arr[start] = temp; //插入正确的位置
    }

    public static void heapSort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;

        //建立大顶堆
        for(int i=arr.length/2; i&gt;=0; i--) {
            heapAdjust(arr, i, arr.length-1);
        }

        for(int i=arr.length-1; i&gt;=0; i--) {
            swap(arr, 0, i);
            heapAdjust(arr, 0, i-1);
        }

    }

    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

}</pre>
</div>
<h2>希尔排序</h2>
<p>希尔排序是插入排序的一种高效率的实现，也叫缩小增量排序。简单的插入排序中，如果待排序列是正序时，时间复杂度是O(n)，如果序列是基本有序的，使用直接插入排序效率就非常高。希尔排序就利用了这个特点。基本思想是：先将整个待排记录序列分割成为若干子序列分别进行直接插入排序，待整个序列中的记录基本有序时再对全体记录进行一次直接插入排序。</p>
<p>举个栗子：</p>
<p><img src="http://static.codeceo.com/images/2016/03/aef03b0d2339be34627bcd81a3809e62.png" alt="" /></p>
<p>从上述排序过程可见，希尔排序的特点是，子序列的构成不是简单的逐段分割，而是将某个相隔某个增量的记录组成一个子序列。如上面的例子，第一堂排序时的增量为5，第二趟排序的增量为3。由于前两趟的插入排序中记录的关键字是和同一子序列中的前一个记录的关键字进行比较，因此关键字较小的记录就不是一步一步地向前挪动，而是跳跃式地往前移，从而使得进行最后一趟排序时，整个序列已经做到基本有序，只要作记录的少量比较和移动即可。因此希尔排序的效率要比直接插入排序高。</p>
<p>希尔排序的分析是复杂的，时间复杂度是所取增量的函数，这涉及一些数学上的难题。但是在大量实验的基础上推出当n在某个范围内时，时间复杂度可以达到O(n^1.3)。</p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;希尔排序算法实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-3 下午10:53:55
 */
public class ShellSort {

    /**
     * 希尔排序的一趟插入
     * @param arr 待排数组
     * @param d 增量
     */
    public static void shellInsert(int[] arr, int d) {
        for(int i=d; i&lt;arr.length; i++) {
            int j = i - d;
            int temp = arr[i];    //记录要插入的数据  
            while (j&gt;=0 &amp;&amp; arr[j]&gt;temp) {  //从后向前，找到比其小的数的位置   
                arr[j+d] = arr[j];    //向后挪动  
                j -= d;  
            }  

            if (j != i - d)    //存在比其小的数 
                arr[j+d] = temp;

        }
    }

    public static void shellSort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;
        int d = arr.length / 2;
        while(d &gt;= 1) {
            shellInsert(arr, d);
            d /= 2;
        }
    }

}</pre>
</div>
<h2>归并排序</h2>
<p>归并排序是另一种不同的排序方法，因为归并排序使用了递归分治的思想，所以理解起来比较容易。其基本思想是，先递归划分子问题，然后合并结果。把待排序列看成由两个有序的子序列，然后合并两个子序列，然后把子序列看成由两个有序序列。。。。。倒着来看，其实就是先两两合并，然后四四合并。。。最终形成有序序列。空间复杂度为O(n)，时间复杂度为O(nlogn)。</p>
<p>举个栗子：</p>
<p><img src="http://static.codeceo.com/images/2016/03/df8478362d9b42913e022eff94d43eb4.png" alt="" /></p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;归并排序算法的实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-4 上午8:14:20
 */
public class MergeSort {

    public static void mergeSort(int[] arr) {
        mSort(arr, 0, arr.length-1);
    }

    /**
     * 递归分治
     * @param arr 待排数组
     * @param left 左指针
     * @param right 右指针
     */
    public static void mSort(int[] arr, int left, int right) {
        if(left &gt;= right)
            return ;
        int mid = (left + right) / 2;

        mSort(arr, left, mid); //递归排序左边
        mSort(arr, mid+1, right); //递归排序右边
        merge(arr, left, mid, right); //合并
    }

    /**
     * 合并两个有序数组
     * @param arr 待合并数组
     * @param left 左指针
     * @param mid 中间指针
     * @param right 右指针
     */
    public static void merge(int[] arr, int left, int mid, int right) {
        //[left, mid] [mid+1, right]
        int[] temp = new int[right - left + 1]; //中间数组

        int i = left;
        int j = mid + 1;
        int k = 0;
        while(i &lt;= mid &amp;&amp; j &lt;= right) {
            if(arr[i] &lt;= arr[j]) {
                temp[k++] = arr[i++];
            }
            else {
                temp[k++] = arr[j++];
            }
        }

        while(i &lt;= mid) {
            temp[k++] = arr[i++];
        }

        while(j &lt;= right) {
            temp[k++] = arr[j++];
        }

        for(int p=0; p&lt;temp.length; p++) {
            arr[left + p] = temp[p];
        }

    }
}</pre>
</div>
<h2>计数排序</h2>
<p>如果在面试中有面试官要求你写一个O(n)时间复杂度的排序算法，你千万不要立刻说：这不可能！虽然前面基于比较的排序的下限是O(nlogn)。但是确实也有线性时间复杂度的排序，只不过有前提条件，就是待排序的数要满足一定的范围的整数，而且计数排序需要比较多的辅助空间。其基本思想是，用待排序的数作为计数数组的下标，统计每个数字的个数。然后依次输出即可得到有序序列。</p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;计数排序算法实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-4 下午4:52:02
 */
public class CountSort {

    public static void countSort(int[] arr) {
        if(arr == null || arr.length == 0)
            return ;

        int max = max(arr);

        int[] count = new int[max+1];
        Arrays.fill(count, 0);

        for(int i=0; i&lt;arr.length; i++) {
            count[arr[i]] ++;
        }

        int k = 0;
        for(int i=0; i&lt;=max; i++) {
            for(int j=0; j&lt;count[i]; j++) {
                arr[k++] = i;
            }
        }

    }

    public static int max(int[] arr) {
        int max = Integer.MIN_VALUE;
        for(int ele : arr) {
            if(ele &gt; max)
                max = ele;
        }

        return max;
    }

}</pre>
</div>
<h2>桶排序</h2>
<p>桶排序算是计数排序的一种改进和推广，但是网上有许多资料把计数排序和桶排序混为一谈。其实桶排序要比计数排序复杂许多。</p>
<p>对桶排序的分析和解释借鉴这位兄弟的文章（有改动）：http://hxraid.iteye.com/blog/647759</p>
<p>桶排序的基本思想：</p>
<p>假设有一组长度为N的待排关键字序列K[1....n]。首先将这个序列划分成M个的子区间(桶) 。然后基于某种映射函数 ，将待排序列的关键字k映射到第i个桶中(即桶数组B的下标 i) ，那么该关键字k就作为B[i]中的元素(每个桶B[i]都是一组大小为N/M的序列)。接着对每个桶B[i]中的所有元素进行比较排序(可以使用快排)。然后依次枚举输出B[0]&#8230;.B[M]中的全部内容即是一个有序序列。bindex=f(key)   其中，bindex 为桶数组B的下标(即第bindex个桶), k为待排序列的关键字。桶排序之所以能够高效，其关键在于这个映射函数，它必须做到：如果关键字k1&lt;k2，那么f(k1)&lt;=f(k2)。也就是说B(i)中的最小数据都要大于B(i-1)中最大数据。很显然，映射函数的确定与数据本身的特点有很大的关系。</p>
<p>举个栗子：</p>
<p><img src="http://static.codeceo.com/images/2016/03/47bcf435950f64c848ce43d5ae5142aa.gif" alt="" /></p>
<p>假如待排序列K= {49、 38 、 35、 97 、 76、 73 、 27、 49 }。这些数据全部在1—100之间。因此我们定制10个桶，然后确定映射函数f(k)=k/10。则第一个关键字49将定位到第4个桶中(49/10=4)。依次将所有关键字全部堆入桶中，并在每个非空的桶中进行快速排序后得到如图所示。只要顺序输出每个B[i]中的数据就可以得到有序序列了。</p>
<p>桶排序分析：</p>
<p>桶排序利用函数的映射关系，减少了几乎所有的比较工作。实际上，桶排序的f(k)值的计算，其作用就相当于快排中划分，希尔排序中的子序列，归并排序中的子问题，已经把大量数据分割成了基本有序的数据块(桶)。然后只需要对桶中的少量数据做先进的比较排序即可。</p>
<p>对N个关键字进行桶排序的时间复杂度分为两个部分：</p>
<p>(1) 循环计算每个关键字的桶映射函数，这个时间复杂度是O(N)。</p>
<p>(2) 利用先进的比较排序算法对每个桶内的所有数据进行排序，其时间复杂度为  ∑ O(Ni*logNi) 。其中Ni 为第i个桶的数据量。</p>
<p>很显然，第(2)部分是桶排序性能好坏的决定因素。尽量减少桶内数据的数量是提高效率的唯一办法(因为基于比较排序的最好平均时间复杂度只能达到O(N*logN)了)。因此，我们需要尽量做到下面两点：</p>
<p>(1) 映射函数f(k)能够将N个数据平均的分配到M个桶中，这样每个桶就有[N/M]个数据量。</p>
<p>(2) 尽量的增大桶的数量。极限情况下每个桶只能得到一个数据，这样就完全避开了桶内数据的“比较”排序操作。当然，做到这一点很不容易，数据量巨大的情况下，f(k)函数会使得桶集合的数量巨大，空间浪费严重。这就是一个时间代价和空间代价的权衡问题了。</p>
<p>对于N个待排数据，M个桶，平均每个桶[N/M]个数据的桶排序平均时间复杂度为：</p>
<p>O(N)+O(M*(N/M)*log(N/M))=O(N+N*(logN-logM))=O(N+N*logN-N*logM)</p>
<p>当N=M时，即极限情况下每个桶只有一个数据时。桶排序的最好效率能够达到O(N)。</p>
<p><strong>总结：</strong> 桶排序的平均时间复杂度为线性的O(N+C)，其中C=N*(logN-logM)。如果相对于同样的N，桶数量M越大，其效率越高，最好的时间复杂度达到O(N)。 当然桶排序的空间复杂度 为O(N+M)，如果输入数据非常庞大，而桶的数量也非常多，则空间代价无疑是昂贵的。此外，桶排序是稳定的。</p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;桶排序算法实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-4 下午7:39:31
 */
public class BucketSort {

    public static void bucketSort(int[] arr) {
        if(arr == null &amp;&amp; arr.length == 0)
            return ;

        int bucketNums = 10; //这里默认为10，规定待排数[0,100)
        List&lt;List&lt;Integer&gt;&gt; buckets = new ArrayList&lt;List&lt;Integer&gt;&gt;(); //桶的索引

        for(int i=0; i&lt;10; i++) {
            buckets.add(new LinkedList&lt;Integer&gt;()); //用链表比较合适
        }

        //划分桶
        for(int i=0; i&lt;arr.length; i++) {
            buckets.get(f(arr[i])).add(arr[i]);
        }

        //对每个桶进行排序
        for(int i=0; i&lt;buckets.size(); i++) {
            if(!buckets.get(i).isEmpty()) {
                Collections.sort(buckets.get(i)); //对每个桶进行快排
            }
        }

        //还原排好序的数组
        int k = 0;
        for(List&lt;Integer&gt; bucket : buckets) {
            for(int ele : bucket) {
                arr[k++] = ele;
            }
        }
    }

    /**
     * 映射函数
     * @param x
     * @return
     */
    public static int f(int x) {
        return x / 10;
    }

}</pre>
</div>
<h2>基数排序</h2>
<p>基数排序又是一种和前面排序方式不同的排序方式，基数排序不需要进行记录关键字之间的比较。基数排序是一种借助多关键字排序思想对单逻辑关键字进行排序的方法。所谓的多关键字排序就是有多个优先级不同的关键字。比如说成绩的排序，如果两个人总分相同，则语文高的排在前面，语文成绩也相同则数学高的排在前面。。。如果对数字进行排序，那么个位、十位、百位就是不同优先级的关键字，如果要进行升序排序，那么个位、十位、百位优先级一次增加。基数排序是通过多次的收分配和收集来实现的，关键字优先级低的先进行分配和收集。</p>
<p>举个栗子：</p>
<p><img src="http://static.codeceo.com/images/2016/03/cb56ff40b1ade7e57d5aa9840fc1eba3.png" alt="" /></p>
<p><img src="http://static.codeceo.com/images/2016/03/75d38cce23336e133dd8eac33eabf5ad.png" alt="" /></p>
<p>实现代码：</p>
<div>
<pre>/**
 *@Description:&lt;p&gt;基数排序算法实现&lt;/p&gt;
 *@author 王旭
 *@time 2016-3-4 下午8:29:52
 */
public class RadixSort {

    public static void radixSort(int[] arr) {
        if(arr == null &amp;&amp; arr.length == 0)
            return ;

        int maxBit = getMaxBit(arr);

        for(int i=1; i&lt;=maxBit; i++) {

            List&lt;List&lt;Integer&gt;&gt; buf = distribute(arr, i); //分配
            collecte(arr, buf); //收集
        }

    }

    /**
     * 分配
     * @param arr 待分配数组
     * @param iBit 要分配第几位
     * @return
     */
    public static List&lt;List&lt;Integer&gt;&gt; distribute(int[] arr, int iBit) {
        List&lt;List&lt;Integer&gt;&gt; buf = new ArrayList&lt;List&lt;Integer&gt;&gt;();
        for(int j=0; j&lt;10; j++) {
            buf.add(new LinkedList&lt;Integer&gt;());
        }
        for(int i=0; i&lt;arr.length; i++) {
            buf.get(getNBit(arr[i], iBit)).add(arr[i]);
        }
        return buf;
    }

    /**
     * 收集
     * @param arr 把分配的数据收集到arr中
     * @param buf 
     */
    public static void collecte(int[] arr, List&lt;List&lt;Integer&gt;&gt; buf) {
        int k = 0;
        for(List&lt;Integer&gt; bucket : buf) {
            for(int ele : bucket) {
                arr[k++] = ele;
            }
        }

    }

    /**
     * 获取最大位数
     * @param x
     * @return
     */
    public static int getMaxBit(int[] arr) {
        int max = Integer.MIN_VALUE;
        for(int ele : arr) {
            int len = (ele+"").length();
            if(len &gt; max)
                max = len;
        }
        return max;
    }

    /**
     * 获取x的第n位，如果没有则为0.
     * @param x
     * @param n
     * @return
     */
    public static int getNBit(int x, int n) {

        String sx = x + "";
        if(sx.length() &lt; n)
            return 0;
        else
            return sx.charAt(sx.length()-n) - '0';
    }

}</pre>
</div>
<h2>总结</h2>
<p>在前面的介绍和分析中我们提到了冒泡排序、选择排序、插入排序三种简单的排序及其变种快速排序、堆排序、希尔排序三种比较高效的排序。后面我们又分析了基于分治递归思想的归并排序还有计数排序、桶排序、基数排序三种线性排序。我们可以知道排序算法要么简单有效，要么是利用简单排序的特点加以改进，要么是以空间换取时间在特定情况下的高效排序。但是这些排序方法都不是固定不变的，需要结合具体的需求和场景来选择甚至组合使用。才能达到高效稳定的目的。没有最好的排序，只有最适合的排序。</p>
<p>下面就总结一下排序算法的各自的使用场景和适用场合。</p>
<p><img src="http://static.codeceo.com/images/2016/03/2f0f5c6b5c7b007b00f0d33427a70db0.png" alt="" /></p>
<p>1. 从平均时间来看，快速排序是效率最高的，但快速排序在最坏情况下的时间性能不如堆排序和归并排序。而后者相比较的结果是，在n较大时归并排序使用时间较少，但使用辅助空间较多。</p>
<p>2. 上面说的简单排序包括除希尔排序之外的所有冒泡排序、插入排序、简单选择排序。其中直接插入排序最简单，但序列基本有序或者n较小时，直接插入排序是好的方法，因此常将它和其他的排序方法，如快速排序、归并排序等结合在一起使用。</p>
<p>3. 基数排序的时间复杂度也可以写成O(d*n)。因此它最使用于n值很大而关键字较小的的序列。若关键字也很大，而序列中大多数记录的最高关键字均不同，则亦可先按最高关键字不同，将序列分成若干小的子序列，而后进行直接插入排序。</p>
<p>4. 从方法的稳定性来比较，基数排序是稳定的内排方法，所有时间复杂度为O(n^2)的简单排序也是稳定的。但是快速排序、堆排序、希尔排序等时间性能较好的排序方法都是不稳定的。稳定性需要根据具体需求选择。</p>
<p>5. 上面的算法实现大多数是使用线性存储结构，像插入排序这种算法用链表实现更好，省去了移动元素的时间。具体的存储结构在具体的实现版本中也是不同的。</p>
<p>附：基于比较排序算法时间下限为O(nlogn)的证明：</p>
<p>基于比较排序下限的证明是通过决策树证明的，决策树的高度Ω（nlgn），这样就得出了比较排序的下限。</p>
<p><img src="http://static.codeceo.com/images/2016/03/b4d51a192d469b833a46695c0a7668f6.jpg" alt="" /></p>
<p>首先要引入决策树。 首先决策树是一颗二叉树，每个节点表示元素之间一组可能的排序，它予以京进行的比较相一致，比较的结果是树的边。 先来说明一些二叉树的性质，令T是深度为d的二叉树，则T最多有2^片树叶。 具有L片树叶的二叉树的深度至少是logL。 所以，对n个元素排序的决策树必然有n!片树叶（因为n个数有n!种不同的大小关系），所以决策树的深度至少是log(n!)，即至少需要log(n!)次比较。 而 log(n!)=logn+log(n-1)+log(n-2)+&#8230;+log2+log1 &gt;=logn+log(n-1)+log(n-2)+&#8230;+log(n/2) &gt;=(n/2)log(n/2) &gt;=(n/2)logn-n/2 =O(nlogn) 所以只用到比较的排序算法最低时间复杂度是O(nlogn)。</p>
