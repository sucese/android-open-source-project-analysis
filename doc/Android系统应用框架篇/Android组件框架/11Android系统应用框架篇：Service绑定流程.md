# Android系统应用框架篇：Service启动流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

本篇文章我们来分析Service组件在进程内的绑定流程。

在分析具体的绑定流程之前，我们先简单回忆Service组件绑定的用法。

1 定义一个Activity，它将要绑定一个Service组件运行后台任务。

```java
public class ClientActivity extends AppCompatActivity  {

    private IServerService serverService;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serverService = ((ServerService.ServerBinder) service).getCounterService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        Intent intent = new Intent(ClientActivity.this, ServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
```

2 在定义一个ServerService，它即将要被一个Activity组件绑定。

```java
public class ServerService extends Service  {

    private IBinder binder = new ServerBinder();

    public class ServerBinder extends Binder {
        public ServerService getCounterService() {
            return ServerService.this;
        }

    }

    public ServerService() {
    }

    /**
     * 当Service组件被绑定时，onBind会被调用。
     *
     * @param intent intent
     * @return IBinder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
```

整个绑定流程可以概括为：

```
1 ClientActivity组件向ActivityManagerService发送一个绑定ServerService组件的进程间通信请求。
2 ActivityManagerService发现用来运行ServerService组件与ClientActivity组件运行在同一个进程里，它
便直接通知该进程将该erverService组件启动起来。
3 该erverService组件启动起来以后，ActivityManagerService就请求它返回一个Binder本地对象，以便
ClientActivity组件可以通过这个Binder对象与ServerService组件建立连接。
4 ActivityManagerService将从ServerService组件获得的Binder对象返回给调用者ClientActivity。
5 ClientActivity获得到ActivityManagerService发送给它的Binder对象后，它就可以通过这个BInder对象
获得ServerService组件的一个访问接口，从而获得ServerService的服务，这样便相当于ServerService组件
绑定在ClientActivity组件内部了。
```

**Service组件在程内绑定序列图**

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/10/service_bind_sequence.png">

我们来看一看具体的流程。

注：ClientActivity：要绑定ServerService的Activity组件。ServerService：将要被绑定的Service组件。

### 1 ClientActivity.onCreate()

```java
public class ClientActivity extends AppCompatActivity  {

    private IServerService serverService;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serverService = ((ServerService.ServerBinder) service).getCounterService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        Intent intent = new Intent(ClientActivity.this, ServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
```

可以看到ServiceConnection被作为参数传递到了bindService()方法中，当Service组件成功绑定后，serviceConnection的onServiceConnected()
方法会被调用以便ClientActivity可以获得ServerService组件的访问接口。

它接着会调用ContextWrapper.bindService()方法。

### 2 ContextWrapper.bindService()

```java
public class ContextWrapper extends Context {
    @Override
    public boolean bindService(Intent service, ServiceConnection conn,
            int flags) {
        return mBase.bindService(service, conn, flags);
    }
}
```
>注：Context是个抽象类，它的实现类是ContextImpl，ContextWrapper作为一个代理类，持有Context的引用，代理ContextImpl的功能，我们
在调用Context里的方法，最终通过ContextWrapper调用ContextImpl里的方法，从而隐藏ContextImpl的实现。

Activity里调用bindService()，最终会通过ContextWrapper调用到ContextImpl.bindService()方法,我们直接来看ContextImpl.bindService()的实现。

### 3 ContextImpl.bindService()

```java
class ContextImpl extends Context {

    @Override
    public boolean bindService(Intent service, ServiceConnection conn,
            int flags) {
        IServiceConnection sd;
        if (mPackageInfo != null) {
            //1 调用LoadedApk.getServiceDispatcher()方法将ServiceConnection对象
            //封装成了一个实现了IServiceConnection接口的Binder本地对象
            sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(),
                    mMainThread.getHandler(), flags);
        } else {
            throw new RuntimeException("Not supported in system context");
        }
        try {
            //2 将Binder对象、Intent信息等发送给ActivityManagerService，以便
            //它可以去启动该Service组件。
            int res = ActivityManagerNative.getDefault().bindService(
                mMainThread.getApplicationThread(), getActivityToken(),
                service, service.resolveTypeIfNeeded(getContentResolver()),
                sd, flags);
            if (res < 0) {
                throw new SecurityException(
                        "Not allowed to bind to service " + service);
            }
            return res != 0;
        } catch (RemoteException e) {
            return false;
        }
    }
}
```

该方法主要做了2件事情：

```
1 调用LoadedApk.getServiceDispatcher()方法将ServiceConnection对象封装成了一个实现了IServiceConnection接口的
Binder本地对象.
2 将Binder对象、Intent信息等发送给ActivityManagerService，以便它可以去启动该Service组件。
```
我们首先来看看ServiceConnection对象封装成了一个实现了IServiceConnection接口的Binder本地对象的。
它调用的方法时oadedApk.getServiceDispatcher()，该方法传入了以下4个参数：

```
conn：ServiceConnection对象对象。
getOuterContext()：它是一个Context对象，指向的是外部的Activity组件，也就是我们上面说的ClientActivity。通过
该函数的封装，conn会与ClientActivity组件关联在一起。
mMainThread.getHandler()：返回ActivityThread内部的Handler变量mH。
flags：启动的flags。
```
getServiceDispatcher()方法的实现如下所示：

```java
final class LoadedApk {

    public final IServiceConnection getServiceDispatcher(ServiceConnection c,
            Context context, Handler handler, int flags) {
        synchronized (mServices) {
            LoadedApk.ServiceDispatcher sd = null;
            //保存ServiceDispatcher对象的map也以context为key保存在mServices中
            HashMap<ServiceConnection, LoadedApk.ServiceDispatcher> map = mServices.get(context);
            if (map != null) {
                sd = map.get(c);
            }
            if (sd == null) {
                sd = new ServiceDispatcher(c, context, handler, flags);
                if (map == null) {
                    //ServiceDispatcher对象以ServiceConnection为key保存在map中
                    map = new HashMap<ServiceConnection, LoadedApk.ServiceDispatcher>();
                    mServices.put(context, map);
                }
                map.put(c, sd);
            } else {
                sd.validate(context, handler);
            }
            return sd.getIServiceConnection();
        }
    }
}
````

>ServiceDispatcher：每一个绑定过Service组件的Activity组件都是在LoadedApk类中又一个对应的ServiceDispatcher对象，它负责将
这个被绑定的Service组件与绑定它的Activity组件关联在一起。

getServiceDispatcher()方法会先查找ServiceDispatcher对象，如果没有则创建一个ServiceDispatcher对象，最后调用ServiceDispatcher对象的
getIServiceConnection()方法。

```java
    static final class ServiceDispatcher {
        //
        private final ServiceDispatcher.InnerConnection mIServiceConnection;
        //指向外部传入的ServiceConnection对象
        private final ServiceConnection mConnection;
        //指向外部传入的Activity组件，本例子中指向的是ClienActivity
        private final Context mContext;
        //指向的是与该Activity组件关联的Handler对象，即主线程中Handler对象ActivityThread.mH
        private final Handler mActivityThread;
        private final ServiceConnectionLeaked mLocation;
        //指向的是外部传递的flags
        private final int mFlags;

        private RuntimeException mUnbindLocation;

        private boolean mDied;

        private static class ConnectionInfo {
            IBinder binder;
            IBinder.DeathRecipient deathMonitor;
        }

        //这个大家应该很熟悉，AIDL里的Stub类
        private static class InnerConnection extends IServiceConnection.Stub {
            final WeakReference<LoadedApk.ServiceDispatcher> mDispatcher;

            InnerConnection(LoadedApk.ServiceDispatcher sd) {
                mDispatcher = new WeakReference<LoadedApk.ServiceDispatcher>(sd);
            }

            public void connected(ComponentName name, IBinder service) throws RemoteException {
                LoadedApk.ServiceDispatcher sd = mDispatcher.get();
                if (sd != null) {
                    sd.connected(name, service);
                }
            }
        }

        ...

        ServiceDispatcher(ServiceConnection conn,
                Context context, Handler activityThread, int flags) {
            mIServiceConnection = new InnerConnection(this);
            mConnection = conn;
            mContext = context;
            mActivityThread = activityThread;
            mLocation = new ServiceConnectionLeaked(null);
            mLocation.fillInStackTrace();
            mFlags = flags;
        }
        
        ...
        
        IServiceConnection getIServiceConnection() {
            return mIServiceConnection;
        }
        
        ...
}
```

当ContextImpl.bindServce()中将ServiceConnection对象封装成一个InnerConnection对象之后，就会调用ActivityManagerNative.getDefault().bindService()方法
将ServerService组件绑定到ClientActivity组件中。

### 4 ActivityManagerProxy.bindService()

```java
class ActivityManagerProxy implements IActivityManager{

    public int bindService(IApplicationThread caller, IBinder token,
            Intent service, String resolvedType, IServiceConnection connection,
            int flags) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IActivityManager.descriptor);
        data.writeStrongBinder(caller != null ? caller.asBinder() : null);
        data.writeStrongBinder(token);
        service.writeToParcel(data, 0);
        data.writeString(resolvedType);
        data.writeStrongBinder(connection.asBinder());
        data.writeInt(flags);
        mRemote.transact(BIND_SERVICE_TRANSACTION, data, reply, 0);
        reply.readException();
        int res = reply.readInt();
        data.recycle();
        reply.recycle();
        return res;
    }
}
```
这同样是一个典型的AIDL跨进程通信的调用过程，通过内部的Binder代理对象mRemote向ActivityManagerService发送一个BIND_SERVICE_TRANSACTION
进程通信请求。

该方法接着会调用ActivityManagerService.bindService()方法，我们来看下它的实现。

### 5 ActivityManagerService.bindService(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags) 

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    public int bindService(IApplicationThread caller, IBinder token,
                Intent service, String resolvedType,
                IServiceConnection connection, int flags) {
            // Refuse possible leaked file descriptors
            if (service != null && service.hasFileDescriptors() == true) {
                throw new IllegalArgumentException("File descriptors passed in Intent");
            }
            
            //1 创建启动Service组件所需的各种对象，例如：ProcessRecord对象、ActivityRecord对象与ServiceRecord对象等
            synchronized(this) {
                if (DEBUG_SERVICE) Slog.v(TAG, "bindService: " + service
                        + " type=" + resolvedType + " conn=" + connection.asBinder()
                        + " flags=0x" + Integer.toHexString(flags));
                //创建ProcessRecord对象，该对象描述发起请求的Activity组件所在的应用进程
                final ProcessRecord callerApp = getRecordForAppLocked(caller);
                if (callerApp == null) {
                    throw new SecurityException(
                            "Unable to find app for caller " + caller
                            + " (pid=" + Binder.getCallingPid()
                            + ") when binding service " + service);
                }
                //创建ActivityRecord对象，该对象描述发起请求的Activity组件
                ActivityRecord activity = null;
                if (token != null) {
                    int aindex = mMainStack.indexOfTokenLocked(token);
                    if (aindex < 0) {
                        Slog.w(TAG, "Binding with unknown activity: " + token);
                        return 0;
                    }
                    activity = (ActivityRecord)mMainStack.mHistory.get(aindex);
                }
    
                int clientLabel = 0;
                PendingIntent clientIntent = null;
                
                if (callerApp.info.uid == Process.SYSTEM_UID) {
                    // Hacky kind of thing -- allow system stuff to tell us
                    // what they are, so we can report this elsewhere for
                    // others to know why certain services are running.
                    try {
                        clientIntent = (PendingIntent)service.getParcelableExtra(
                                Intent.EXTRA_CLIENT_INTENT);
                    } catch (RuntimeException e) {
                    }
                    if (clientIntent != null) {
                        clientLabel = service.getIntExtra(Intent.EXTRA_CLIENT_LABEL, 0);
                        if (clientLabel != 0) {
                            // There are no useful extras in the intent, trash them.
                            // System code calling with this stuff just needs to know
                            // this will happen.
                            service = service.cloneFilter();
                        }
                    }
                }
                
                ServiceLookupResult res =
                    retrieveServiceLocked(service, resolvedType,
                            Binder.getCallingPid(), Binder.getCallingUid());
                if (res == null) {
                    return 0;
                }
                if (res.record == null) {
                    return -1;
                }
                
                //创建ServiceRecord对象，该对象描述即将要被绑定的Service组件
                ServiceRecord s = res.record;
    
                final long origId = Binder.clearCallingIdentity();
    
                if (unscheduleServiceRestartLocked(s)) {
                    if (DEBUG_SERVICE) Slog.v(TAG, "BIND SERVICE WHILE RESTART PENDING: "
                            + s);
                }
                
                //创建AppBindRecord对象，表示ServiceRecord所描述的Service组件时绑定在callerApp所
                //描述的进程中的
                AppBindRecord b = s.retrieveAppBindingLocked(service, callerApp);
                //创建ConnectionRecord对象，该对象描述组件绑定的情况。
                ConnectionRecord c = new ConnectionRecord(b, activity,
                        connection, flags, clientLabel, clientIntent);
    
                IBinder binder = connection.asBinder();
                //因为一个Service组件可以被多个Activity组件使用同一个InnerConnection对象来绑定，因此
                //会有多个ConnectionRecord对象，这些对象被保存一个列表中。
                ArrayList<ConnectionRecord> clist = s.connections.get(binder);
                if (clist == null) {
                    clist = new ArrayList<ConnectionRecord>();
                    s.connections.put(binder, clist);
                }
                clist.add(c);
                b.connections.add(c);
                if (activity != null) {
                    if (activity.connections == null) {
                        activity.connections = new HashSet<ConnectionRecord>();
                    }
                    activity.connections.add(c);
                }
                b.client.connections.add(c);
                clist = mServiceConnections.get(binder);
                if (clist == null) {
                    clist = new ArrayList<ConnectionRecord>();
                    mServiceConnections.put(binder, clist);
                }
                clist.add(c);
    
                if ((flags&Context.BIND_AUTO_CREATE) != 0) {
                    s.lastActivity = SystemClock.uptimeMillis();
                    //2 调用bringUpServiceLocked()来启动Service组件，等到这个Service组件启动起来之后，
                    //ActivityManagerService再将它与Activity组件绑定起来
                    if (!bringUpServiceLocked(s, service.getFlags(), false)) {
                        return 0;
                    }
                }
    
                if (s.app != null) {
                    // This could have made the service more important.
                    updateOomAdjLocked(s.app);
                }
    
                if (DEBUG_SERVICE) Slog.v(TAG, "Bind " + s + " with " + b
                        + ": received=" + b.intent.received
                        + " apps=" + b.intent.apps.size()
                        + " doRebind=" + b.intent.doRebind);
    
                if (s.app != null && b.intent.received) {
                    // Service is already running, so we can immediately
                    // publish the connection.
                    try {
                        c.conn.connected(s.name, b.intent.binder);
                    } catch (Exception e) {
                        Slog.w(TAG, "Failure sending service " + s.shortName
                                + " to connection " + c.conn.asBinder()
                                + " (in " + c.binding.client.processName + ")", e);
                    }
    
                    // If this is the first app connected back to this binding,
                    // and the service had previously asked to be told when
                    // rebound, then do so.
                    if (b.intent.apps.size() == 1 && b.intent.doRebind) {
                        requestServiceBindingLocked(s, b.intent, true);
                    }
                } else if (!b.intent.requested) {
                    requestServiceBindingLocked(s, b.intent, false);
                }
    
                Binder.restoreCallingIdentity(origId);
            }
    
            return 1;
        }        
}
```
该方法用来处理BIND_SERVICE_TRANSACTION进程通信请求，该方法主要做了2件事情：

```
1 创建启动Service组件所需的各种对象，例如：ProcessRecord对象、ActivityRecord对象与ServiceRecord对象等
2 调用bringUpServiceLocked()来启动Service组件，等到这个Service组件启动起来之后，ActivityManagerService再将它与Activity组件绑定起来
```
我们接着来看bringUpServiceLocked()的实现。

### 6 ActivityManagerService.bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean whileRestarting) 

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
     private final boolean bringUpServiceLocked(ServiceRecord r,
                int intentFlags, boolean whileRestarting) {
            //Slog.i(TAG, "Bring up service:");
            //r.dump("  ");
    
            if (r.app != null && r.app.thread != null) {
                sendServiceArgsLocked(r, false);
                return true;
            }
    
            if (!whileRestarting && r.restartDelay > 0) {
                // If waiting for a restart, then do nothing.
                return true;
            }
    
            if (DEBUG_SERVICE) Slog.v(TAG, "Bringing up " + r + " " + r.intent);
    
            // We are now bringing the service up, so no longer in the
            // restarting state.
            mRestartingServices.remove(r);
            
            //获取ServiceRecord对象里的processName属性，查找是否已经存在一个对应ProcessRecord的对象app
            //如果存在则说明该Service组件所在的应用进程已经运行起来了，则直接调用realStartServiceLocked()
            //方法启动这个Service组件。
            final String appName = r.processName;
            ProcessRecord app = getProcessRecordLocked(appName, r.appInfo.uid);
            if (app != null && app.thread != null) {
                try {
                    realStartServiceLocked(r, app);
                    return true;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception when starting service " + r.shortName, e);
                }
    
                // If a dead object exception was thrown -- fall through to
                // restart the application.
            }
    
            // Not running -- get it started, and enqueue this service record
            // to be executed when the app comes up.
            if (startProcessLocked(appName, r.appInfo, true, intentFlags,
                    "service", r.name, false) == null) {
                Slog.w(TAG, "Unable to launch app "
                        + r.appInfo.packageName + "/"
                        + r.appInfo.uid + " for service "
                        + r.intent.getIntent() + ": process is bad");
                bringDownServiceLocked(r, true);
                return false;
            }
            
            if (!mPendingServices.contains(r)) {
                mPendingServices.add(r);
            }
            
            return true;
        }        
}
        
```

该方法获取ServiceRecord对象里的processName属性，查找是否已经存在一个对应ProcessRecord的对象app如果存在则说明该Service组件所在的应用进程已经运行起
来了，则直接调用realStartServiceLocked()方法启动这个Service组件。

### 7 ActivityManagerService.realStartServiceLocked(ServiceRecord r, ProcessRecord app)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    private final void realStartServiceLocked(ServiceRecord r,
                ProcessRecord app) throws RemoteException {
            if (app.thread == null) {
                throw new RemoteException();
            }
    
            r.app = app;
            r.restartTime = r.lastActivity = SystemClock.uptimeMillis();
    
            app.services.add(r);
            bumpServiceExecutingLocked(r, "create");
            updateLruProcessLocked(app, true, true);
    
            boolean created = false;
            try {
                mStringBuilder.setLength(0);
                r.intent.getIntent().toShortString(mStringBuilder, false, true);
                EventLog.writeEvent(EventLogTags.AM_CREATE_SERVICE,
                        System.identityHashCode(r), r.shortName,
                        mStringBuilder.toString(), r.app.pid);
                synchronized (r.stats.getBatteryStats()) {
                    r.stats.startLaunchedLocked();
                }
                ensurePackageDexOpt(r.serviceInfo.packageName);
                //ProcessRecord对象的成员变量thread是一个类型为ApplicationThreadProxy的Binder代理对象
                //它指向了app所描述的应用程序进程中的一个ApplicationThread对象。调用它的scheduleCreateService()
                //方法启动Service组件。
                app.thread.scheduleCreateService(r, r.serviceInfo);
                r.postNotification();
                created = true;
            } finally {
                if (!created) {
                    app.services.remove(r);
                    scheduleServiceRestartLocked(r, false);
                }
            }
    
            requestServiceBindingsLocked(r);
            
            // If the service is in the started state, and there are no
            // pending arguments, then fake up one so its onStartCommand() will
            // be called.
            if (r.startRequested && r.callStart && r.pendingStarts.size() == 0) {
                r.lastStartId++;
                if (r.lastStartId < 1) {
                    r.lastStartId = 1;
                }
                r.pendingStarts.add(new ServiceRecord.StartItem(r, r.lastStartId, null, -1));
            }
            
            sendServiceArgsLocked(r, true);
        }            
}
```
### 8 ApplicationThreadProxy.scheduleCreateService(IBinder token, ServiceInfo info)

```java
class ApplicationThreadProxy implements IApplicationThread {

    public final void scheduleCreateService(IBinder token, ServiceInfo info)
            throws RemoteException {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken(IApplicationThread.descriptor);
        data.writeStrongBinder(token);
        info.writeToParcel(data, 0);
        mRemote.transact(SCHEDULE_CREATE_SERVICE_TRANSACTION, data, null,
                IBinder.FLAG_ONEWAY);
        data.recycle();
    }
}

```

该方法向ServerService组件所在的进程发送一个SCHEDULE_CREATE_SERVICE_TRANSACTION进程间通信请求，以便
它可以把ServerService组件启动起来。

接下来的流程大家就很熟悉了。ApplicationThread.scheduleCreateService()调用ActivityThread.queueOrSendMessage()，ActivityThread
发出CREATE_SERVICE的Message，最终调用ActivityThreaqd.handleCreateService()来创建Service。这一部分属于Service的创建流程，可以
参见文章[10Android系统应用框架篇：Service启动流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/10Android系统应用框架篇：Service启动流程.md)。

### 9 ApplicationThread.scheduleCreateService()
### 10 ActivityThread.queueOrSendMessage()
### 11 H.handleMesssage()
### 12 ActivityThreaqd.handleCreateService()
### 13 ServerService.onCreate()

当ServerService.onCreate()被调用之后，ServerService组件就被启动起来了。接下来我们继续回到第7步ActivityManagerService.realStartServiceLocked(ServiceRecord r, ProcessRecord app)
它会继续调用 requestServiceBindingsLocked()方法来绑定Service组件。

### 14 ActivityManagerService.requestServiceBindingsLocked(ServiceRecord r)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
    private final void requestServiceBindingsLocked(ServiceRecord r) {
        Iterator<IntentBindRecord> bindings = r.bindings.values().iterator();
        while (bindings.hasNext()) {
            //IntentBindRecord对象描述若干个需要将ServiceRecord对象r描述的Service组件
            //绑定到对应的应用进程中去。
            IntentBindRecord i = bindings.next();
            if (!requestServiceBindingLocked(r, i, false)) {
                break;
            }
        }
    } 
    
    private final boolean requestServiceBindingLocked(ServiceRecord r,
                IntentBindRecord i, boolean rebind) {
            if (r.app == null || r.app.thread == null) {
                // If service is not currently running, can't yet bind.
                return false;
            }
            if ((!i.requested || rebind) && i.apps.size() > 0) {
                try {
                    bumpServiceExecutingLocked(r, "bind");
                    //调用ApplicationThreadProxy.scheduleBindService()方法
                    r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind);
                    if (!rebind) {
                        i.requested = true;
                    }
                    i.hasBound = true;
                    i.doRebind = false;
                } catch (RemoteException e) {
                    if (DEBUG_SERVICE) Slog.v(TAG, "Crashed while binding " + r);
                    return false;
                }
            }
            return true;
        }
}
```

该方法最终会调用ApplicationThreadProxy.scheduleBindService()方法去完成Service组件的绑定。

### 15 ApplicationThreadProxy.scheduleBindService(IBinder token, Intent intent, boolean rebind)

```java
class ApplicationThreadProxy implements IApplicationThread {
    public final void scheduleBindService(IBinder token, Intent intent, boolean rebind)
            throws RemoteException {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken(IApplicationThread.descriptor);
        data.writeStrongBinder(token);
        intent.writeToParcel(data, 0);
        data.writeInt(rebind ? 1 : 0);
        mRemote.transact(SCHEDULE_BIND_SERVICE_TRANSACTION, data, null,
                IBinder.FLAG_ONEWAY);
        data.recycle();
    }
}
```
ApplicationThreadProxy调用scheduleBindService()方法向ApplicationThread发送一个SCHEDULE_BIND_SERVICE_TRANSACTION进程通信请求，
这个ApplicationThread代表的正是ClientActivity所在的进程。

接下来的流程还是跟上面创建Service一样，最后走到了ActivityThreaqd.handleBindService()方法来绑定Service组件。

### 16 ApplicationThread.scheduleCreateService()
### 17 ActivityThread.queueOrSendMessage()
### 18 H.handleMesssage()
### 19 ActivityThreaqd.handleBindService()

```java
public final class ActivityThread {

    private final void handleBindService(BindServiceData data) {
        //data.token指向了一个Binder代理对象，它引用了ActivityManagerService中的一个ServiceRecord对象
        //该对象指向的正式我们要绑定的ServerService组件。
        // 1 以data.token为key获取前面已经启动ServerService组件。
        Service s = mServices.get(data.token);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                try {
                    if (!data.rebind) {
                        //2 调用Service.onBind()方法获得其内部的一个Binder本地对象
                        IBinder binder = s.onBind(data.intent);
                        //3 调用ActivityMangerProxy.publishService()将该Binder对象传递给ActivityManagerService
                        ActivityManagerNative.getDefault().publishService(
                                data.token, data.intent, binder);
                    } else {
                        s.onRebind(data.intent);
                        ActivityManagerNative.getDefault().serviceDoneExecuting(
                                data.token, 0, 0, 0);
                    }
                    ensureJitEnabled();
                } catch (RemoteException ex) {
                }
            } catch (Exception e) {
                if (!mInstrumentation.onException(s, e)) {
                    throw new RuntimeException(
                            "Unable to bind to service " + s
                            + " with " + data.intent + ": " + e.toString(), e);
                }
            }
        }
    }

}

```

>BindServiceData：封装了一些绑定Service组件的信息。

```java
private static final class BindServiceData {
    IBinder token;
    Intent intent;
    boolean rebind;
    public String toString() {
        return "BindServiceData{token=" + token + " intent=" + intent + "}";
    }
}
````

该方法主要做了3件事情：

```
1 以data.token为key获取前面已经启动ServerService组件。
2 调用Service.onBind()方法获得其内部的一个Binder本地对象
3 调用ActivityMangerProxy.publishService()将该Binder对象传递给ActivityManagerService

```
我们接着来看ServerService.onBind()的实现。

### 20 ServerService.onBind()


```java
public class ServerService extends Service  {

    private IBinder binder = new ServerBinder();

    public class ServerBinder extends Binder {
        public ServerService getService() {
            return ServerService.this;
        }

    }

    public ServerService() {
    }

    /**
     * 当Service组件被绑定时，onBind会被调用。
     *
     * @param intent intent
     * @return IBinder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
```
该方法将内部的一个binder对象返回给了onBind()方法，通过ServerBinder内部的getService()方法就可以获得访问
ServerService组件的接口。

调用用onBind()方法后，接着调用ActivityMangerProxy.publishService()将该Binder对象传递给ActivityManagerService。

### 21 ActivityManagerProxy.publishService(IBinder token, Intent intent, IBinder service)

```java
class ActivityManagerProxy implements IActivityManager{
    public void publishService(IBinder token,
            Intent intent, IBinder service) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IActivityManager.descriptor);
        data.writeStrongBinder(token);
        intent.writeToParcel(data, 0);
        data.writeStrongBinder(service);
        mRemote.transact(PUBLISH_SERVICE_TRANSACTION, data, reply, 0);
        reply.readException();
        data.recycle();
        reply.recycle();
    }
}
```

ServerService组件所在进程通过ActivityManagerProxy向ActivityManagerService发送一个PUBLISH_SERVICE_TRANSACTION进程间
通信请求。

### 22 ActivityManagerService.publishService(IBinder token, Intent intent, IBinder service)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        
     public void publishService(IBinder token, Intent intent, IBinder service) {
            // Refuse possible leaked fiInle descriptors
            if (intent != null && intent.hasFileDescriptors() == true) {
                throw new IllegalArgumentException("File descriptors passed in Intent");
            }
    
            synchronized(this) {
                if (!(token instanceof ServiceRecord)) {
                    throw new IllegalArgumentException("Invalid service token");
                }
                ServiceRecord r = (ServiceRecord)token;
    
                final long origId = Binder.clearCallingIdentity();
    
                if (DEBUG_SERVICE) Slog.v(TAG, "PUBLISHING " + r
                        + " " + intent + ": " + service);
                if (r != null) {
                    Intent.FilterComparison filter
                            = new Intent.FilterComparison(intent);
                    IntentBindRecord b = r.bindings.get(filter);
                    if (b != null && !b.received) {
                        b.binder = service;
                        b.requested = true;
                        b.received = true;
                        if (r.connections.size() > 0) {
                            Iterator<ArrayList<ConnectionRecord>> it
                                    = r.connections.values().iterator();
                            while (it.hasNext()) {
                                ArrayList<ConnectionRecord> clist = it.next();
                                for (int i=0; i<clist.size(); i++) {
                                    ConnectionRecord c = clist.get(i);
                                    if (!filter.equals(c.binding.intent.intent)) {
                                        if (DEBUG_SERVICE) Slog.v(
                                                TAG, "Not publishing to: " + c);
                                        if (DEBUG_SERVICE) Slog.v(
                                                TAG, "Bound intent: " + c.binding.intent.intent);
                                        if (DEBUG_SERVICE) Slog.v(
                                                TAG, "Published intent: " + intent);
                                        continue;
                                    }
                                    if (DEBUG_SERVICE) Slog.v(TAG, "Publishing to: " + c);
                                    try {
                                        c.conn.connected(r.name, service);
                                    } catch (Exception e) {
                                        Slog.w(TAG, "Failure sending service " + r.name +
                                              " to connection " + c.conn.asBinder() +
                                              " (in " + c.binding.client.processName + ")", e);
                                    }
                                }
                            }
                        }
                    }
    
                    serviceDoneExecutingLocked(r, mStoppingServices.contains(r));
    
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }        
}        
```
该方法主要用来处理ServerService组件所在进程通过ActivityManagerProxy向ActivityManagerService发送一个PUBLISH_SERVICE_TRANSACTION进程间通信请求。

我们先来看看传递进这个方法的3个参数：

```
IBinder token：指向的是一个ServiceRecord对象，用来描述ClientActivity请求绑定的ServerService组件。
Intent intent：Intent对象。
IBinder service：指向ServerService组件内部的一个Binder本地对象。
```

### 

```java

```



### 

```java

```



### 

```java

```



### 

```java

```


### 

```java

```


### 

```java

```


