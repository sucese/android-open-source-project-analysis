# Android系统应用框架篇：WindowManagerService

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

在Android系统中，同一时刻只有一个Activity窗口激活，但除此之外还状态栏、输入法等窗口，对于这些窗口的管理都由WindowManagerService来
进行

本篇文章来分析Android显示系统中重要的一环WindowManagerService，在具体介绍这方面内容之前，我们先来了解与WindowManagerService
相关的重要概念。

## WindowManagerService的功能结构

### WindowToken

>WindowToken指代了一个应用组件，例如：ActivityInputMethod、Wallpaper等，它把属于一个应用组件的窗口组合在了一起。
在进行窗口z-ordered排序时，会把属于同一个WindowToken的窗口放在一起。

WindowToken中定义了一些很关键的变量，我们来看看它们的作用。

```java
  class WindowToken {
    
        //实际的token，可以看到它是一个Binder对象
        // The actual token.
        final IBinder token;

        //窗口类型，与WindowManager.LayoutParams.type含义相同
        // The type of window this token is for, as per WindowManager.LayoutParams.
        final int windowType;

        //表示该token是否是客户端添加的，如果是客户端添加的则当其他窗口被移除时它不会被移除
        // Set if this token was explicitly added by a client, so should
        // not be removed when all windows are removed.
        final boolean explicit;

        // For printing.
        String stringName;
        
        //AppWindowToken是WindowToken的子类，它用来表示应用类型的窗口
        // If this is an AppWindowToken, this is non-null.
        AppWindowToken appWindowToken;

        //WindowState即窗口状态信息的列表
        // All of the windows associated with this token.
        final ArrayList<WindowState> windows = new ArrayList<WindowState>();

        //窗口是否暂停
        // Is key dispatching paused for this token?
        boolean paused = false;

        //窗口是否隐藏
        // Should this token's windows be hidden?
        boolean hidden;

        //窗口是否可见
        // Temporary for finding which tokens no longer have visible windows.
        boolean hasVisible;

        //窗口是否等待被展示
        // Set to true when this token is in a pending transaction where it
        // will be shown.
        boolean waitingToShow;

        //窗口是否等待被隐藏
        // Set to true when this token is in a pending transaction where it
        // will be hidden.
        boolean waitingToHide;

        //窗口层级是否被放入最低层
        // Set to true when this token is in a pending transaction where its
        // windows will be put to the bottom of the list.
        boolean sendingToBottom;

        //窗口层级是否被放入最高层
        // Set to true when this token is in a pending transaction where its
        // windows will be put to the top of the list.
        boolean sendingToTop;

        WindowToken(IBinder _token, int type, boolean _explicit) {
            token = _token;
            windowType = type;
            explicit = _explicit;
        }

        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix); pw.print("token="); pw.println(token);
            pw.print(prefix); pw.print("windows="); pw.println(windows);
            pw.print(prefix); pw.print("windowType="); pw.print(windowType);
                    pw.print(" hidden="); pw.print(hidden);
                    pw.print(" hasVisible="); pw.println(hasVisible);
            if (waitingToShow || waitingToHide || sendingToBottom || sendingToTop) {
                pw.print(prefix); pw.print("waitingToShow="); pw.print(waitingToShow);
                        pw.print(" waitingToHide="); pw.print(waitingToHide);
                        pw.print(" sendingToBottom="); pw.print(sendingToBottom);
                        pw.print(" sendingToTop="); pw.println(sendingToTop);
            }
        }

        @Override
        public String toString() {
            if (stringName == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("WindowToken{");
                sb.append(Integer.toHexString(System.identityHashCode(this)));
                sb.append(" token="); sb.append(token); sb.append('}');
                stringName = sb.toString();
            }
            return stringName;
        }
    };
```
### WindowState

>WindowState表示窗口的各种属性，它持有Session、IWindow、WindowToken与AppWindowToken等各种属性
，所以它描述了窗口的详细信息，有点像ActivityRecord、ServiceRecord这写类。

我们来看看WindowState的构造函数的实现，借此理解一下窗口排布次序的原理。



```java
private final class WindowState implements WindowManagerPolicy.WindowState {
    
     WindowState(Session s, IWindow c, WindowToken token,
                   WindowState attachedWindow, WindowManager.LayoutParams a,
                   int viewVisibility) {
                mSession = s;
                mClient = c;
                mToken = token;
                mAttrs.copyFrom(a);
                mViewVisibility = viewVisibility;
                DeathRecipient deathRecipient = new DeathRecipient();
                mAlpha = a.alpha;
                if (localLOGV) Slog.v(
                    TAG, "Window " + this + " client=" + c.asBinder()
                    + " token=" + token + " (" + mAttrs.token + ")");
                try {
                    c.asBinder().linkToDeath(deathRecipient, 0);
                } catch (RemoteException e) {
                    mDeathRecipient = null;
                    mAttachedWindow = null;
                    mLayoutAttached = false;
                    mIsImWindow = false;
                    mIsWallpaper = false;
                    mIsFloatingLayer = false;
                    mBaseLayer = 0;
                    mSubLayer = 0;
                    return;
                }
                mDeathRecipient = deathRecipient;
    
                //为子窗口分配z-order
                if ((mAttrs.type >= FIRST_SUB_WINDOW &&
                        mAttrs.type <= LAST_SUB_WINDOW)) {
                    // The multiplier here is to reserve space for multiple
                    // windows in the same type layer.
                    //主序
                    mBaseLayer = mPolicy.windowTypeToLayerLw(
                            attachedWindow.mAttrs.type) * TYPE_LAYER_MULTIPLIER
                            + TYPE_LAYER_OFFSET;
                    //子序
                    mSubLayer = mPolicy.subWindowTypeToLayerLw(a.type);
                    mAttachedWindow = attachedWindow;
                    mAttachedWindow.mChildWindows.add(this);
                    mLayoutAttached = mAttrs.type !=
                            WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
                    mIsImWindow = attachedWindow.mAttrs.type == TYPE_INPUT_METHOD
                            || attachedWindow.mAttrs.type == TYPE_INPUT_METHOD_DIALOG;
                    mIsWallpaper = attachedWindow.mAttrs.type == TYPE_WALLPAPER;
                    mIsFloatingLayer = mIsImWindow || mIsWallpaper;
                } 
                //为普通窗口分配z-order
                else {
                    // The multiplier here is to reserve space for multiple
                    // windows in the same type layer.
                    //主序
                    mBaseLayer = mPolicy.windowTypeToLayerLw(a.type)
                            * TYPE_LAYER_MULTIPLIER
                            + TYPE_LAYER_OFFSET;
                    //子序
                    mSubLayer = 0;
                    mAttachedWindow = null;
                    mLayoutAttached = false;
                    mIsImWindow = mAttrs.type == TYPE_INPUT_METHOD
                            || mAttrs.type == TYPE_INPUT_METHOD_DIALOG;
                    mIsWallpaper = mAttrs.type == TYPE_WALLPAPER;
                    mIsFloatingLayer = mIsImWindow || mIsWallpaper;
                }
    
                WindowState appWin = this;
                while (appWin.mAttachedWindow != null) {
                    appWin = mAttachedWindow;
                }
                WindowToken appToken = appWin.mToken;
                while (appToken.appWindowToken == null) {
                    WindowToken parent = mTokenMap.get(appToken.token);
                    if (parent == null || appToken == parent) {
                        break;
                    }
                    appToken = parent;
                }
                mRootToken = appToken;
                mAppToken = appToken.appWindowToken;
    
                mSurface = null;
                mRequestedWidth = 0;
                mRequestedHeight = 0;
                mLastRequestedWidth = 0;
                mLastRequestedHeight = 0;
                mXOffset = 0;
                mYOffset = 0;
                mLayer = 0;
                mAnimLayer = 0;
                mLastLayer = 0;
            }
    
}
```


### WindowManagerPolicy

>WindowManagerPolicy是一个接口，在Phone平台它的实现类是PhoneWindowManager，它由PolicyManager所创建，用来计算Window的大小与
位置。

## WindowManagerService的创建流程

WindowManagerService同其他系统服务一样，也是在SystemService中的ServerThread.run()函数中启动的。

### 1 ServerThread.run()

```java
class ServerThread extends Thread {
        @Override
        public void run() {
            ...
            WindowManagerService wm = null;
            ...
            // Critical services...
            try {
                ...
                Slog.i(TAG, "Window Manager");
                wm = WindowManagerService.main(context, power,
                        factoryTest != SystemServer.FACTORY_TEST_LOW_LEVEL);
                ServiceManager.addService(Context.WINDOW_SERVICE, wm);
    
                ((ActivityManagerService)ServiceManager.getService("activity"))
                        .setWindowManager(wm);
                ...
    
            } catch (RuntimeException e) {
                Slog.e("System", "Failure starting core service", e);
            }
    
            ...
            wm.systemReady();
            power.systemReady();
            try {
                pm.systemReady();
            } catch (RemoteException e) {
            }
            ...
        }
    }
}
```

在该函数中调用WindowManagerService的main方法创建了WindowManagerService实例，WindowManagerService实例也被添加到了
ActivityMangerService中。我们接着来看看main方法的实现。

### 2 WindowManagerService.main(Context context, PowerManagerService pm, boolean haveInputMethods) 

```java
public class WindowManagerService extends IWindowManager.Stub
        implements Watchdog.Monitor {
    
    public static WindowManagerService main(Context context,
                PowerManagerService pm, boolean haveInputMethods) {
            WMThread thr = new WMThread(context, pm, haveInputMethods);
            thr.start();
    
            synchronized (thr) {
                while (thr.mService == null) {
                    try {
                        thr.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
    
            return thr.mService;
        }
    
        static class WMThread extends Thread {
            WindowManagerService mService;
    
            private final Context mContext;
            private final PowerManagerService mPM;
            private final boolean mHaveInputMethods;
    
            public WMThread(Context context, PowerManagerService pm,
                    boolean haveInputMethods) {
                super("WindowManager");
                mContext = context;
                mPM = pm;
                mHaveInputMethods = haveInputMethods;
            }
    
            public void run() {
                Looper.prepare();
                WindowManagerService s = new WindowManagerService(mContext, mPM,
                        mHaveInputMethods);
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_DISPLAY);
                android.os.Process.setCanSelfBackground(false);
    
                synchronized (this) {
                    mService = s;
                    notifyAll();
                }
    
                Looper.loop();
            }
        }    
}
```
在main方法中，WindowManagerService创建了新线程WMThread，在该线程里初始化了WindowManagerService对象。我们接着来看看WindowManagerService
的构造方法。




```java
public class WindowManagerService extends IWindowManager.Stub
        implements Watchdog.Monitor {
    
    private WindowManagerService(Context context, PowerManagerService pm,
                boolean haveInputMethods) {
            //
            mContext = context;
            mHaveInputMethods = haveInputMethods;
            mLimitedAlphaCompositing = context.getResources().getBoolean(
                    com.android.internal.R.bool.config_sf_limitedAlpha);
    
            mPowerManager = pm;
            mPowerManager.setPolicy(mPolicy);
            PowerManager pmc = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            mScreenFrozenLock = pmc.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "SCREEN_FROZEN");
            mScreenFrozenLock.setReferenceCounted(false);
    
            //获取ActivityManager
            mActivityManager = ActivityManagerNative.getDefault();
            mBatteryStats = BatteryStatsService.getService();
    
            // Get persisted window scale setting
            mWindowAnimationScale = Settings.System.getFloat(context.getContentResolver(),
                    Settings.System.WINDOW_ANIMATION_SCALE, mWindowAnimationScale);
            mTransitionAnimationScale = Settings.System.getFloat(context.getContentResolver(),
                    Settings.System.TRANSITION_ANIMATION_SCALE, mTransitionAnimationScale);
    
            // Track changes to DevicePolicyManager state so we can enable/disable keyguard.
            IntentFilter filter = new IntentFilter();
            filter.addAction(DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, filter);
    
            mHoldingScreenWakeLock = pmc.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    "KEEP_SCREEN_ON_FLAG");
            mHoldingScreenWakeLock.setReferenceCounted(false);
    
            //创建InputManager，管理输入事件
            mInputManager = new InputManager(context, this);
    
            PolicyThread thr = new PolicyThread(mPolicy, this, context, pm);
            thr.start();
    
            synchronized (thr) {
                while (!thr.mRunning) {
                    try {
                        thr.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
    
            mInputManager.start();
    
            //将自己St添加到Watchdog的监控中
            // Add ourself to the Watchdog monitors.
            Watchdog.getInstance().addMonitor(this);
        }    
}
```
从WindowManagerService的构造函数中可以看出，它主要做了mContext、mHaveInputMethods、mPowerManager等变量的保存，获取了ActivityManager
以及创建了InputManager来处理输入事件，这些都是为了后续绘制界面以及处理用户输入做准备。
