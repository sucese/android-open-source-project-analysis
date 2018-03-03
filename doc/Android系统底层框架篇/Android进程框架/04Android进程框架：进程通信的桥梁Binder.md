# Android进程框架：进程通信的桥梁Binder

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

千呼万唤始出来，Android系统源码分析终于来到了Binder IPC通知机制这一块，我们知道Android应用的基础是四大组件，而四大组件通信的基础就是就是Binder，可以说它是Android系统
最重要的组成部分，对于开发者而言也是最难理解的一部分。

但古人云"天下事有难易乎？为之，则难者亦易矣；不为，则易者亦难矣"，本文将尝试以通俗易懂的方式来讲解这一块的原理。

在分析Binder原理之前，我们先来思考一个问题，Linux系统本身有许多IPC手段，为什么Android要重新设计一套Binder机制呢？🤔

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

Binder是一套相对比较复杂的设计，如何去理解它呢？🤔

最好的方式就是去从不同层次，不同角度去理解它。

**从内核空间与用户空间角度**

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/binder_structure.png" width="600"/>

我们知道每一个Android应用都是一个独立的Android进程，它们拥有自己独立的虚拟地址空间，应用进程处于用户空间之中，彼此之间相互独立，不能共享。但是内核空间却是可以共享的，Client
进程向Server进程通信，就是利用进程间可以共享的内核地址空间来完成底层的通信的工作的。Client进程与Server端进程往往采用ioctl等方法跟内核空间的驱动进行交互。

**从Java与C++分层的角度**

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/process/binder_detail_structure.png" width="600"/>

可以发现，在整个Binder通信机制中，从大的方面分可以分为：

- Framework Binder
- Native Binder

Framework Binder最终通过JNI调用Native Binder的功能，它们在架构上的设计都是C/S架构。

主要涉及以下四个关键角色：

- Client：客户端。
- Server：服务端。
- ServiceManager：C++层的ServiceManager，Binder通信机制的大管家，Android进程间通信机制Binder的守护进程。
- Binder Driver：Binder驱动。

整个流程也十分简单，如下所示：

1. Server进程将服务注册到ServiceManager。
2. Client进程向ServiceManager获取服务。
3. Client进程得到的Service信息后，建立与Server进程的通信通道，然后就可以与Server进程进行交互了。

通过上面的描述，相信读者对Binder有了个整体的认识。

>Binder是Android平台独有的一种跨进程通信的方式，从底层来说，Binder是一种虚拟的物理设备，它的设备驱动是/dev/binder，从上层来说，Binder是客户端和服务端进行通信的桥梁，当
bindService的时候，服务端就会返回一个包含了服务端业务调用的Binder对象，通过这个Binder对象，客户端就可以获取服务端提供的服务（包含普通服务和基于AIDL的服务）或者数据。

## 一 Binder通信的流程

在理解具体的原理之前，我们先写一个关于AIDL进程通信的小例子，来直观的感受一下进程通信。

👉 举例

1. 定义AIDL文件IRemoteService.aidl，定义远程服务需要提供的功能。

```java
interface IRemoteService {

    String getMessage();
}

```

2. 定义服务端RemoteService，提供服务，在进程RemoteService.Process中。

```java
public class RemoteService extends Service {

    private static final String TAG = "RemoteService";

    private IRemoteService.Stub mBinder = new IRemoteService.Stub() {
        @Override
        public String getMessage() throws RemoteException {

            Log.d(TAG, "RemoteService Process Pid: " + android.os.Process.myPid());
            return "I am a message from RemoteService";
        }
    };

    public RemoteService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, " onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }
}
```
3. 定义客户端ClientActivity，与RemoteService绑定，获取服务，在进程ClientActivity.Process中。

```java
public class ClientActivity extends AppCompatActivity {

    private static final String TAG = "ClientActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binder);

        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                Log.d(TAG, "ClientActivity Process Pid : " + android.os.Process.myPid());
                IRemoteService iRemoteService = IRemoteService.Stub.asInterface(service);
                try {
                    Log.d(TAG, iRemoteService.getMessage());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
            }
        };
        Intent intent = new Intent(ClientActivity.this, RemoteService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
```

整个代码还是比较简单的，我们来看看两个进程输出的Log，如下所示：

RemoteService.Process进程：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/progress/remote_service_log.png"/>

ClientActivity.Process进程：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/progress/client_activity_log.png"/>

可以发现ClientActivity与RemoteService处于两个不同的进程中，但ClientActivity却获得了RemoteService返回的消息，这就是跨进程通信实现的效果。

我们来分析一下具体的流程，可以看到在RemoteService中IRemoteService文件自动编译生成了一个类，如下所示：


```java
public interface IRemoteService extends android.os.IInterface{
    
//Stub类实现，它继承于Binder，同样也实现了IRemoteService接口，读取Proxy传递过来的参数，并写入返回给Proxy的值。
public static abstract class Stub extends android.os.Binder implements com.guoxiaoxing.android.framework.demo.native_framwork.process.IRemoteService{
    
    private static final java.lang.String DESCRIPTOR = "com.guoxiaoxing.android.framework.demo.native_framwork.process.IRemoteService";
    //Stub构造函数
    public Stub(){
        this.attachInterface(this, DESCRIPTOR);
    }

    //将IBinder对象转换成IRemoteService接口的实现类，供客户端使用
    public static com.guoxiaoxing.android.framework.demo.native_framwork.process.IRemoteService asInterface(android.os.IBinder obj){
        if ((obj==null)) {
            return null;
        }
        android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
        if (((iin!=null)&&(iin instanceof com.guoxiaoxing.android.framework.demo.native_framwork.process.IRemoteService))) {
            return ((com.guoxiaoxing.android.framework.demo.native_framwork.process.IRemoteService)iin);
        }
     return new com.guoxiaoxing.android.framework.demo.native_framwork.process.IRemoteService.Stub.Proxy(obj);
    }


    @Override public android.os.IBinder asBinder(){
        return this;
    }
    
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException{
        switch (code){
            case INTERFACE_TRANSACTION:{
                reply.writeString(DESCRIPTOR);
                return true;
            }
         
            case TRANSACTION_getMessage:{
                data.enforceInterface(DESCRIPTOR);
                java.lang.String _result = this.getMessage();
                reply.writeNoException();
                reply.writeString(_result);
            return true;
            }
     }
     return super.onTransact(code, data, reply, flags);
}

//Proxy类，它实现了我们定义的IRemoteService接口，写入传递给Stub的参数，读取Stub返回的值。
private static class Proxy implements com.guoxiaoxing.android.framework.demo.native_framwork.process.IRemoteService{
    
    private android.os.IBinder mRemote;
    
    //Proxy构造函数，传入远程Binder。
    Proxy(android.os.IBinder remote){
        mRemote = remote;
    }
    
    @Override public android.os.IBinder asBinder(){
        return mRemote;
    }
    
    public java.lang.String getInterfaceDescriptor(){
        return DESCRIPTOR;
    }
    
    @Override public java.lang.String getMessage() throws android.os.RemoteException{
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
                _data.writeInterfaceToken(DESCRIPTOR);
                mRemote.transact(Stub.TRANSACTION_getMessage, _data, _reply, 0);
                _reply.readException();
                _result = _reply.readString();
         }
        finally {
                _reply.recycle();
                _data.recycle();
         }
        return _result;
    }
    }

     static final int TRANSACTION_getMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    }
    
    public java.lang.String getMessage() throws android.os.RemoteException;
}
```

>AIDL目的是为了实现跨进程访问，即获得另一个进程的对象，并访问其方法。它本质上是一个接口，它会自动生成一个继承Binder的接口和Stub、Proxy两个类。
其中Proxy是Stub的内部类。

- Stub：它继承于Binder，同样也实现了我们定义的IRemoteService接口，读取Proxy传递过来的参数，并写入返回给Proxy的值。
- Proxy：它是Stub的内部类，实现了我们定义的IRemoteService接口，写入传递给Stub的参数，读取Stub返回的值。它本身是私有的，通过Stub的asInterface()
方法暴露自己给外部使用。

获取Stub有两种方式：

1. 通过BbindService方式，绑定一个服务，绑定后，服务会返回给客户端一个Binder，该Binder可以继承自Stub，从而把Stub传递给客户端。
2. 把继承自Stub实现的的类提升为系统服务，然后我们可以通过ServiceManager获取该系统服务，并把它传递个客户端。

## 二 启动ServiceManager

## 三 注册服务

## 四 获取服务