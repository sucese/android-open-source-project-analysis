# Android通信框架：线程通信的桥梁Handler

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

Android系统有两大通信手段，一个是进程通信Binder，另一个就是我们今天要讲的线程通信Handler，提到Handler实际上说的Android的消息系统，它由四个部分组成：

- Message：消息，分为硬件产生的消息（例如：按钮、触摸）和软件产生的消息。
- MessageQueue：消息队列，主要用来向消息池添加消息和取走消息。
- Handler：消息处理器，主要向消息池发送各种消息以及处理各种消息。
- Looper：循环器，主要用来把消息分发给相应的处理者。

Android消息机制流程图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/progresss/android_message_structure.png"/>

现在整个消息机制的流程就很清晰了，具体说来：

1. Handler通过sendMessage()发送消息Message到消息队列MessageQueue。
2. Looper通过loop()不断提取触发条件的Message，并将Message交给对应的target handler来处理。
3. target handler调用自身的handleMessage()方法来处理Message。

## Message

Message描述了消息机制里的消息，他有很多成员字段，具体说来：

- int	    what	    消息类别
- long	    when	    消息触发时间
- int	    arg1	    参数1
- int	    arg2	    参数2
- Object	obj	        消息内容
- Handler	target	    消息响应Handler
- Runnable	callback	回调方法

此外关于Message还有一个消息池的概念，它可以帮助我们回收对象，避免创建过多的对象，和消息池相关的主要有两个方法。

```java
public final class Message implements Parcelable {
    
        private static final Object sPoolSync = new Object();
        private static Message sPool;
        
        public void recycle() {
            //判断消息是否正在使用
            if (isInUse()) {
                if (gCheckRecycle) {
                    throw new IllegalStateException("This message cannot be recycled because it "
                            + "is still in use.");
                }
                return;
            }
            //对于不再使用的消息加入消息池
            recycleUnchecked();
        }
    
        void recycleUnchecked() {
            //将消息标记为FLAG_IN_USE并清空关于它的其他信息
            flags = FLAG_IN_USE;
            what = 0;
            arg1 = 0;
            arg2 = 0;
            obj = null;
            replyTo = null;
            sendingUid = -1;
            when = 0;
            target = null;
            callback = null;
            data = null;
    
            synchronized (sPoolSync) {
                //当消息池没有满时，将消息加入消息池
                if (sPoolSize < MAX_POOL_SIZE) {
                    //将sPool存放在next变量中
                    next = sPool;
                    //sPool引用当前对象
                    sPool = this;
                    //消息池数量自增1
                    sPoolSize++;
                }
            }
        }
}
```


```java
public final class Message implements Parcelable {
    
    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                
                //sPool当前持有的消息对象将作为结果返回
                Message m = sPool;
                //将m的后继重新赋值给sPool，这其实一个链表的删除操作
                sPool = m.next;
                //m的后继置为空
                m.next = null;
                //清除 in-use 标志位
                m.flags = 0;
                //消息池大小自减1
                sPoolSize--;
                return m;
            }
        }
        //当对象池为空时，直接创建新的Message()对象。
        return new Message();
    }
}
```

这里面有个巧妙的设计，这也给我们如何设计一个对象池提供了一个很好的思路，具体说来：

1. 在类中定义一个该类的静态对象sPool以及它的后继对象next。
2. 当对象加入对象池时，用sPool持有最新加入的对象，next持有上一次加入的对象（链表的添加操作）。
3. 当对象从对象池中取出时，返回sPool当前持有的对象即可，并且修改对应节点的后继（链表的删除操作）。

## Looper

>Class used to run a message loop for a thread. 

Looper可以为线程添加一个消息循环的功能，具体说来，为了给线程添加一个消息循环，我们通常会这么做：

```java
public class LooperThread extends Thread {

    public Handler mHandler;

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // process incoming messages here
            }
        };
        Looper.loop();
    }
}
```
这里面有两个关键的方法：

Looper.prepare()：为当前线程初始化一个Looper。
Looper.loop()：开启消息循环。

我们分别来看看它们的实现。


```java
public final class Looper {
    
   // sThreadLocal.get() will return null unless you've called prepare().
   static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
       
   public static void prepare() {
          prepare(true);
      }
  
  private static void prepare(boolean quitAllowed) {
      if (sThreadLocal.get() != null) {
          throw new RuntimeException("Only one Looper may be created per thread");
      }
      sThreadLocal.set(new Looper(quitAllowed));
  }
}

```

prepare()会调用prepare(true)方法，表示当前创建的是不会退出的Loope，这里使用的是ThreadLocal来存储新创建的Looper对象。

>ThreadLocal描述的是线程本地存储区，不同的线程不能访问对方的线程本地存储区，当前线程可以对自己的线程本地存储区进行独立的修改和读取。

之所以会采用ThreadLocal来存储Looper，是因为每个具备消息循环能力的线程都有自己独立的Looper，它们彼此独立，所以需要用线程本地存储区来存储Looper。

我们在接着来看看Looper的构造函数，如下所示：

```java
public final class Looper {
    
  private Looper(boolean quitAllowed) {
      //创建消息队列
      mQueue = new MessageQueue(quitAllowed);
      //指向当前线程
      mThread = Thread.currentThread();
   }  
}
```

Looper的构造函数也很简单，构造了一个消息队列MessageQueue，并将成员变量MThread指向当前线程。

关于这个方法我们再提一点，我们都知道Android里有个主线程（UI线程），它也是个有消息循环能力的线程，它在初始化的时候调用的是pre

```java
public final class Looper {
    
    private static Looper sMainLooper;  // guarded by Looper.class

    //创建主线程的Looper，应用启动的时候会右系统调用，我们一般不需要调用这个方法。
    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }
    
    //返回和当前线程相关的Looper
    public static @Nullable Looper myLooper() {
        return sThreadLocal.get();
    }
}
```

我们再来看看loop()方法的实现。

```java
```java
public final class Looper {
    
     public static void loop() {
        //获取当前线程的Looper
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        
        //获取当前线程的消息队列
        final MessageQueue queue = me.mQueue;

        //确保当前线程处于本地进程中，Handler仅限于处于同一进程间的不同线程的通信。
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();

        //进入loop主循环方法
        for (;;) {
            //不断的获取下一条消息，这个方法可能会被阻塞
            Message msg = queue.next();
            if (msg == null) {
                //如果没有消息需要处理，则退出当前循环。
                return;
            }

            // 默认为null，可通过setMessageLogging来指定输出，用于debug
            final Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }

            final long traceTag = me.mTraceTag;
            if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
                Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
            }
            //分发消息
            try {
                msg.target.dispatchMessage(msg);
            } finally {
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }

            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }

            // Make sure that during the course of dispatching the
            // identity of the thread wasn't corrupted.
            final long newIdent = Binder.clearCallingIdentity();
            if (ident != newIdent) {
                Log.wtf(TAG, "Thread identity changed from 0x"
                        + Long.toHexString(ident) + " to 0x"
                        + Long.toHexString(newIdent) + " while dispatching to "
                        + msg.target.getClass().getName() + " "
                        + msg.callback + " what=" + msg.what);
            }

            //把message回收到消息池，以便重复利用。
            msg.recycleUnchecked();
        }
     }
}
```
整体来看这个方法不断重复着以下三件事：

1. 读取MessageQueue的下一条Message。
2. 把Message分发给相应的target。
3. 再把分发的Message回收到消息池，以便重复利用。

我们再来看看MessageQueue里的一些操作。


## MessageQueue

MessageQueue是Android消息机制Java层和C++层的纽带，其中很多核心方法都交由native方法实现。

### next()

>next()从消息队列中提取一条消息并将其从消息队列中移除。

```java
public final class MessageQueue {
    
    Message next() {
           final long ptr = mPtr;
           //当前消息循环已退出，直接返回。
           if (ptr == 0) {
               return null;
           }
   
           //循环迭代首次为-1
           int pendingIdleHandlerCount = -1; 
           //nextPollTimeoutMillisb表示下一个消息到来前还需要等待的时长，-1表示会无线等待
           int nextPollTimeoutMillis = 0;
           for (;;) {
               if (nextPollTimeoutMillis != 0) {
                   Binder.flushPendingCommands();
               }
   
               //nativePollOnce是阻塞操作，当等待nextPollTimeoutMillis时长或者消息队列被唤醒，都会返回。
               nativePollOnce(ptr, nextPollTimeoutMillis);
   
               synchronized (this) {
                   // Try to retrieve the next message.  Return if found.
                   final long now = SystemClock.uptimeMillis();
                   Message prevMsg = null;
                   Message msg = mMessages;
                   //当处理该消息的Handler为空时，则查询消息队列中下一条异步消息。
                   if (msg != null && msg.target == null) {
                       do {
                           prevMsg = msg;
                           msg = msg.next;
                       } while (msg != null && !msg.isAsynchronous());
                   }
                   if (msg != null) {
                       if (now < msg.when) {
                           //当异步消息的触发时间大于当前时间，则使者下一次轮询的超时时长
                           nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                       } else {
                           //获取一条消息并返回
                           mBlocked = false;
                           if (prevMsg != null) {
                               prevMsg.next = msg.next;
                           } else {
                               mMessages = msg.next;
                           }
                           msg.next = null;
                           if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                           //设置消息的使用状态，即flags |= FLAG_IN_USE。
                           msg.markInUse();
                           return msg;
                       }
                   } else {
                       // 没有更多消息
                       nextPollTimeoutMillis = -1;
                   }
   
                   //所有消息都已经被处理，准备退出。
                   if (mQuitting) {
                       dispose();
                       return null;
                   }
   
                   
                   //pendingIdleHandlerCount指的是等待执行的Handler的数量，mIdleHandlers是一个空闲Handler列表
                   if (pendingIdleHandlerCount < 0
                           && (mMessages == null || now < mMessages.when)) {
                       pendingIdleHandlerCount = mIdleHandlers.size();
                   }
                   if (pendingIdleHandlerCount <= 0) {
                       //当没有空闲的Handler需要执行时进入阻塞状态，mBlocked设置为true
                       mBlocked = true;
                       continue;
                   }
   
                   //mPendingIdleHandler是一个IdleHandler数组
                   if (mPendingIdleHandlers == null) {
                       mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                   }
                   mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
               }
   
               //只有在第一次循环时，才会去执行idle handlers，执行完成后重置pendingIdleHandlerCount为0
               for (int i = 0; i < pendingIdleHandlerCount; i++) {
                   final IdleHandler idler = mPendingIdleHandlers[i];
                   //释放Handler的引用
                   mPendingIdleHandlers[i] = null;
   
                   boolean keep = false;
                   try {
                       keep = idler.queueIdle();
                   } catch (Throwable t) {
                       Log.wtf(TAG, "IdleHandler threw exception", t);
                   }
   
                   if (!keep) {
                       synchronized (this) {
                           mIdleHandlers.remove(idler);
                       }
                   }
               }
   
               //执行完成后，重置pendingIdleHandlerCount为0，以保证不会再次重复运行。
               pendingIdleHandlerCount = 0;
               
               //当调用一个空闲Handler时，新的消息可以立即被分发，因此无需再设置超时时间。
               nextPollTimeoutMillis = 0;
           }
       } 
}
```
从上面可以看出next()是一个无限循环方法，如果当前消息队列中没有消息，该方法会一直阻塞在这里，等到有新消息时，next()方法会返回这条
消息并将其从链表中移除。

### enqueueMessage()

>enqueueMessage()向消息队列插入一条消息。

```java
public final class MessageQueue {
    
      boolean enqueueMessage(Message msg, long when) {
            //每个消息都必须有个target handler
            if (msg.target == null) {
                throw new IllegalArgumentException("Message must have a target.");
            }
            
            //每个消息必须没有被使用
            if (msg.isInUse()) {
                throw new IllegalStateException(msg + " This message is already in use.");
            }
    
            synchronized (this) {
                //正在退出时，回收Message，加入消息池。
                if (mQuitting) {
                    IllegalStateException e = new IllegalStateException(
                            msg.target + " sending message to a Handler on a dead thread");
                    Log.w(TAG, e.getMessage(), e);
                    msg.recycle();
                    return false;
                }
    
                msg.markInUse();
                msg.when = when;
                Message p = mMessages;
                boolean needWake;
                //三个条件：消息队列里没有消息；触发时间为0代表立即执行；触发时间是队列中最早的
                if (p == null || when == 0 || when < p.when) {
                    // New head, wake up the event queue if blocked.
                    msg.next = p;
                    mMessages = msg;
                    needWake = mBlocked;
                } else {
                    //needWake代表了是否需要唤醒消息队列，一般不需要除非存在以下两种情况：
                    //① 在消息队列头部存在barrier，就是存在那种没有target handler的同步消息阻塞了当前队列的情况
                    //② 要插入的这个消息是当前消息队列最早需要执行的异步消息
                    needWake = mBlocked && p.target == null && msg.isAsynchronous();
                    
                    //将消息按照时间顺序插入到消息队列中
                    Message prev;
                    for (;;) {
                        prev = p;
                        p = p.next;
                        if (p == null || when < p.when) {
                            break;
                        }
                        if (needWake && p.isAsynchronous()) {
                            needWake = false;
                        }
                    }
                    msg.next = p; // invariant: p == prev.next
                    prev.next = msg;
                }
    
                // 唤醒消息队列
                if (needWake) {
                    nativeWake(mPtr);
                }
            }
            return true;
        }
}
```

可以看到，消息是按照触发时间的顺序插入到消息队列中的，消息队列头部的消息总是最早需要执行的消息。

## Handler

Handler主要用来发送和处理消息，它会和自己的Thread以及MessageQueue相关联，当创建一个Hanlder时，它就会被绑定到创建它的线程上，它会向
这个线程的消息队列分发Message和Runnable。

一般说来，Handler主要有两个用途：

- 调度Message和Runnable，延时执行任务。
- 进行线程的切换，请求别的线程完成相关操作。

我们先来看看Handler的构造函数。

```java
public class Handler {
    
    //无参构造方法，最常用。
    public Handler() {
        this(null, false);
    }

    public Handler(Callback callback) {
        this(callback, false);
    }

    public Handler(Looper looper) {
        this(looper, null, false);
    }

    public Handler(Looper looper, Callback callback) {
        this(looper, callback, false);
    }
    
    public Handler(Callback callback, boolean async) {
        //匿名类、本地类都必须声明为static，否则会警告可能出现内存泄漏，这个提示我们应该很熟悉了。
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }
    
        //获取当前线程的Looper
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        //获取当前线程的消息队列
        mQueue = mLooper.mQueue;
        //回调方法，这个Callback里面其实只有个handleMessage()方法，我们实现这个
        //接口，就不用去用匿名内部类那样的方式来创建Handler了。
        mCallback = callback;
        //设置消息是否为异步处理方式
        mAsynchronous = async;
    }   
}
```

对于构造方法而言，我们最常用的是无参构造方法，它没有Callback回调，且消息处理方式为同步处理，从这里我们也可以看出你在哪个线程里创建了Handler，就默认使用当前线程的Looper。


从上面的loop()方法中，我们知道Looper会调用MessageQueue的dispatchMessage()方法进行消息的分发，我们来看看这个方法的实现。

```java
public class Handler {
        public void dispatchMessage(Message msg) {
            //当Message存在回调方法时，优先调用Message的回调方法message.callback.run()
            if (msg.callback != null) {
                //实际调用的就是message.callback.run();
                handleCallback(msg);
            } else {
                //如果我们设置了Callback回调，优先调用Callback回调。
                if (mCallback != null) {
                    if (mCallback.handleMessage(msg)) {
                        return;
                    }
                }
                //如果我们没有设置了Callback回调，则回调自身的Callback方法。
                handleMessage(msg);
            }
        }
}
```

可以看到整个消息分发流程如下所示：

1. 当Message存在回调方法时，优先调用Message的回调方法message.callback.run()。
2. 果我们设置了Callback回调，优先调用Callback回调。
3. 如果我们没有设置了Callback回调，则回调自身的Callback方法。

由此我们也可以得知方法调用的优先级，从高到低依次为：

- message.callback.run()
- Handler.mCallback.handleMessage()
- Handler.handleMessage()

大部分代码都是以匿名内部类的形式实现了Handler，所以一般会走到第三个流程。

可以看到所以发送消息的方法最终都是调用MessageQueue的enqueueMessage()方法来实现，这个我们上面在分析MessageQueue的时候已经说过，这里就不再赘述。

理解了上面的内容，相信读者已经对Android的消息机制有了大致的了解，我们趁热打铁来聊一聊实际业务开发中遇到的一些问题。

>在日常的开发中，我们通常在子线程中执行耗时任务，主线程更新UI，更新的手段也多种多样，如Activity#runOnUIThread()、View#post()等等，它们之间有何区别呢？如果我的代码了
既没有Activity也没有View，我该如何将代码切换回主线程呢？🤔

我们一一来分析。

首先，Activity里的Handler直接调用的就是默认的无参构造方法。可以看到在上面的构造方法里调用Looper.myLooper()去获取当前线程的Looper，对于Activity而言当前线程就是主线程（UI线程），那主线程
的Looper是什么时候创建的呢？🤔

在[03Android组件框架：Android视图容器Activity](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/03Android组件框架：Android视图容器Activity.md)一文
里我们就分析过，ActivityThread的main()函数作为应用的入口，会去初始化Looper，并开启消息循环。

```java
public final class ActivityThread {
      public static void main(String[] args) {
          ...
          Looper.prepareMainLooper();
          ...
          if (sMainThreadHandler == null) {
              sMainThreadHandler = thread.getHandler();
          }
          ...
          Looper.loop();
          throw new RuntimeException("Main thread loop unexpectedly exited");
      }  
}
```

主线程的Looper已经准备就绪了，我们再调用Handler的构造函数去构建Handler对象时就会默认使用这个Handler，如下所示：

```java
public class Activity {
    
   final Handler mHandler = new Handler();

   public final void runOnUiThread(Runnable action) {
          if (Thread.currentThread() != mUiThread) {
              mHandler.post(action);
          } else {
              action.run();
          }
      }  
}
```

```java
public class View implements Drawable.Callback, KeyEvent.Callback,
        AccessibilityEventSource {
    
    public boolean post(Runnable action) {
        
        //当View被添加到window时会添加一些附加信息，这里面就包括Handler
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.post(action);
        }

        //Handler等相关信息还没有被关联到Activity，先建立一个排队队列。
        //这其实就相当于你去银行办事，银行没开门，你们在门口排队等着一样。
        getRunQueue().post(action);
        return true;
    }
}
```

这里面也是利用attachInfo.mHandler来处理消息，它事实上是一个Handler的子类ViewRootHandler，同样的它也是使用Looper.prepareMainLooper()构建出来的Looper。

所以你可以看出Activity#runOnUIThread()、View#post()这两种方式并没有本质上的区别，最终还都是通过Handler来发送消息。那么对于那些既不在Activity里、也不在View里的代码
当我们想向主线程发送消息或者将某段代码（通常都是接口的回调方法，在这些方法里需要更新UI）post到主线程中执行，就可以按照以下方式进行：

```java
Handler handler = new Handler(Looper.getMainLooper());
handler.post(new Runnable() {
    @Override
    public void run() {
        //TODO refresh ui
    }
})
```