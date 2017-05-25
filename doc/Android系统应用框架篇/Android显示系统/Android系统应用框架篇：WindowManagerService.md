# Android系统应用框架篇：WindowManagerService

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作。技术栈主要涉及Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative，数据结构与算法等方面。热爱编程与吉他，喜欢有趣的事物和人。

**关于文章**

>作者的文章首发在[Github](https://github.com/guoxiaoxing)上，也会发在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与[CSDN](http://blog.csdn.net/allenwells)平台上，文章内容主要包含Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative, 数据结构与算法等方面的内容。如果有什么问题，也欢迎发邮件与我交流。

本篇文章来分析Android显示系统中重要的一环WindowManagerService，在具体介绍这方面内容之前，我们先来了解与WindowManagerService
相关的重要概念。

**WindowToken**

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


## 启动流程

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
    
            //将自己添加到Watchdog的监控中
            // Add ourself to the Watchdog monitors.
            Watchdog.getInstance().addMonitor(this);
        }    
}
```
从WindowManagerService的构造函数中可以看出，它主要做了mContext、mHaveInputMethods、mPowerManager等变量的保存，获取了ActivityManager
以及创建了InputManager来处理输入事件，这些都是为了后续绘制界面以及处理用户输入做准备。

**WindowState**

>WindowState表示窗口的各种属性，它持有Session、IWindow、WindowToken与AppWindowToken等各种属性
，所以它描述了窗口的详细信息，有点像ActivityRecord、ServiceRecord这写类。

## 窗口管理

Window的添加与移除都是由WindowManagerService来管理。

### 添加窗口

WindowManagerService.addWindow()方法实现了窗口的添加，方法比较长，我们一点点来分析。

```java
public class WindowManagerService extends IWindowManager.Stub
        implements Watchdog.Monitor {
    
     public int addWindow(Session session, IWindow client,
                WindowManager.LayoutParams attrs, int viewVisibility,
                Rect outContentInsets, InputChannel outInputChannel) {
            //1 检查窗口权限，没有权限的客户端不能添加窗口
            int res = mPolicy.checkAddPermission(attrs);
            if (res != WindowManagerImpl.ADD_OKAY) {
                return res;
            }
    
            boolean reportNewConfig = false;
            //当为某个窗口添加子窗口时，attachedWindow用来保存父窗口的实例
            WindowState attachedWindow = null;
            //win代表即将要添加的窗口
            WindowState win = null;
    
            synchronized(mWindowMap) {
                
                // 这段代码我们很熟悉，获取屏幕的宽高并传给InputManager
                // Instantiating a Display requires talking with the simulator,
                // so don't do it until we know the system is mostly up and
                // running.
                if (mDisplay == null) {
                    WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
                    mDisplay = wm.getDefaultDisplay();
                    mInitialDisplayWidth = mDisplay.getWidth();
                    mInitialDisplayHeight = mDisplay.getHeight();
                    mInputManager.setDisplaySize(0, mInitialDisplayWidth, mInitialDisplayHeight);
                    reportNewConfig = true;
                }
    
                if (mWindowMap.containsKey(client.asBinder())) {
                    Slog.w(TAG, "Window " + client + " is already added");
                    return WindowManagerImpl.ADD_DUPLICATE_ADD;
                }
    
                //如果添加的是子窗口，则需要父窗口已经存在，attrs.type表示窗口的类型，attrs.token
                //表示窗口所属的对象
                if (attrs.type >= FIRST_SUB_WINDOW && attrs.type <= LAST_SUB_WINDOW) {
                    attachedWindow = windowForClientLocked(null, attrs.token, false);
                    if (attachedWindow == null) {
                        Slog.w(TAG, "Attempted to add window with token that is not a window: "
                              + attrs.token + ".  Aborting.");
                        return WindowManagerImpl.ADD_BAD_SUBWINDOW_TOKEN;
                    }
                    //窗口的层级最多为2层
                    if (attachedWindow.mAttrs.type >= FIRST_SUB_WINDOW
                            && attachedWindow.mAttrs.type <= LAST_SUB_WINDOW) {
                        Slog.w(TAG, "Attempted to add window with token that is a sub-window: "
                                + attrs.token + ".  Aborting.");
                        return WindowManagerImpl.ADD_BAD_SUBWINDOW_TOKEN;
                    }
                }
    
                boolean addToken = false;
                //2 根据客户端的attrs.token取出已经注册的WindowToken
                WindowToken token = mTokenMap.get(attrs.token);
                if (token == null) {
                    if (attrs.type >= FIRST_APPLICATION_WINDOW
                            && attrs.type <= LAST_APPLICATION_WINDOW) {
                        Slog.w(TAG, "Attempted to add application window with unknown token "
                              + attrs.token + ".  Aborting.");
                        return WindowManagerImpl.ADD_BAD_APP_TOKEN;
                    }
                    if (attrs.type == TYPE_INPUT_METHOD) {
                        Slog.w(TAG, "Attempted to add input method window with unknown token "
                              + attrs.token + ".  Aborting.");
                        return WindowManagerImpl.ADD_BAD_APP_TOKEN;
                    }
                    if (attrs.type == TYPE_WALLPAPER) {
                        Slog.w(TAG, "Attempted to add wallpaper window with unknown token "
                              + attrs.token + ".  Aborting.");
                        return WindowManagerImpl.ADD_BAD_APP_TOKEN;
                    }
                    //如果WindowToken为空，除了上述3中类型的窗口，WindowManagerService会自动创建WindowToken
                    token = new WindowToken(attrs.token, -1, false);
                    addToken = true;
                } 
                else if (attrs.type >= FIRST_APPLICATION_WINDOW
                        && attrs.type <= LAST_APPLICATION_WINDOW) {
                    //以下表示Application Window的创建流程，对于Application Window，其token类型
                    //为AppWindowToken，AppWindowToken是WindowToken的子类
                    AppWindowToken atoken = token.appWindowToken;
                    if (atoken == null) {
                        Slog.w(TAG, "Attempted to add window with non-application token "
                              + token + ".  Aborting.");
                        return WindowManagerImpl.ADD_NOT_APP_TOKEN;
                    } else if (atoken.removed) {
                        Slog.w(TAG, "Attempted to add window with exiting application token "
                              + token + ".  Aborting.");
                        return WindowManagerImpl.ADD_APP_EXITING;
                    }
                    if (attrs.type == TYPE_APPLICATION_STARTING && atoken.firstWindowDrawn) {
                        // No need for this guy!
                        if (localLOGV) Slog.v(
                                TAG, "**** NO NEED TO START: " + attrs.getTitle());
                        return WindowManagerImpl.ADD_STARTING_NOT_NEEDED;
                    }
                } else if (attrs.type == TYPE_INPUT_METHOD) {
                    if (token.windowType != TYPE_INPUT_METHOD) {
                        Slog.w(TAG, "Attempted to add input method window with bad token "
                                + attrs.token + ".  Aborting.");
                          return WindowManagerImpl.ADD_BAD_APP_TOKEN;
                    }
                } else if (attrs.type == TYPE_WALLPAPER) {
                    if (token.windowType != TYPE_WALLPAPER) {
                        Slog.w(TAG, "Attempted to add wallpaper window with bad token "
                                + attrs.token + ".  Aborting.");
                          return WindowManagerImpl.ADD_BAD_APP_TOKEN;
                    }
                }
                //3 WindowMangerService要为添加的窗口创建一个WindowState对象，这个对象维护了
                //窗口的所有状体信息
                win = new WindowState(session, client, token,
                        attachedWindow, attrs, viewVisibility);
                if (win.mDeathRecipient == null) {
                    // Client has apparently died, so there is no reason to
                    // continue.
                    Slog.w(TAG, "Adding window client " + client.asBinder()
                            + " that is dead, aborting.");
                    return WindowManagerImpl.ADD_APP_EXITING;
                }
    
                //mPolicy的类型是WindowMangerPolicy，他会去调整LayoutParams的一些成员变量的取值
                mPolicy.adjustWindowParamsLw(win.mAttrs);
    
                res = mPolicy.prepareAddWindowLw(win, attrs);
                if (res != WindowManagerImpl.ADD_OKAY) {
                    return res;
                }
                
                if (outInputChannel != null) {
                    String name = win.makeInputChannelName();
                    InputChannel[] inputChannels = InputChannel.openInputChannelPair(name);
                    win.mInputChannel = inputChannels[0];
                    inputChannels[1].transferToBinderOutParameter(outInputChannel);
                    
                    mInputManager.registerInputChannel(win.mInputChannel);
                }
    
                // From now on, no exceptions or errors allowed!
    
                res = WindowManagerImpl.ADD_OKAY;
    
                final long origId = Binder.clearCallingIdentity();
    
                if (addToken) {
                    //上面创建了一个WindowToken，这里以attrs.token为key将它添加到了mTokenMap中
                    mTokenMap.put(attrs.token, token);
                    mTokenList.add(token);
                }
                win.attach();
                //将WIndowState对象添加mWindowMap中
                mWindowMap.put(client.asBinder(), win);
    
                if (attrs.type == TYPE_APPLICATION_STARTING &&
                        token.appWindowToken != null) {
                    token.appWindowToken.startingWindow = win;
                }
    
                boolean imMayMove = true;
    
                if (attrs.type == TYPE_INPUT_METHOD) {
                    mInputMethodWindow = win;
                    addInputMethodWindowToListLocked(win);
                    imMayMove = false;
                } else if (attrs.type == TYPE_INPUT_METHOD_DIALOG) {
                    mInputMethodDialogs.add(win);
                    addWindowToListInOrderLocked(win, true);
                    adjustInputMethodDialogsLocked();
                    imMayMove = false;
                } else {
                    addWindowToListInOrderLocked(win, true);
                    if (attrs.type == TYPE_WALLPAPER) {
                        mLastWallpaperTimeoutTime = 0;
                        adjustWallpaperWindowsLocked();
                    } else if ((attrs.flags&FLAG_SHOW_WALLPAPER) != 0) {
                        adjustWallpaperWindowsLocked();
                    }
                }
    
                win.mEnterAnimationPending = true;
    
                mPolicy.getContentInsetHintLw(attrs, outContentInsets);
    
                if (mInTouchMode) {
                    res |= WindowManagerImpl.ADD_FLAG_IN_TOUCH_MODE;
                }
                if (win == null || win.mAppToken == null || !win.mAppToken.clientHidden) {
                    res |= WindowManagerImpl.ADD_FLAG_APP_VISIBLE;
                }
    
                boolean focusChanged = false;
                if (win.canReceiveKeys()) {
                    focusChanged = updateFocusedWindowLocked(UPDATE_FOCUS_WILL_ASSIGN_LAYERS);
                    if (focusChanged) {
                        imMayMove = false;
                    }
                }
    
                if (imMayMove) {
                    moveInputMethodWindowsIfNeededLocked(false);
                }
    
                assignLayersLocked();
                // Don't do layout here, the window must call
                // relayout to be displayed, so we'll do it there.
    
                //dump();
    
                if (focusChanged) {
                    finishUpdateFocusedWindowAfterAssignLayersLocked();
                }
                
                if (localLOGV) Slog.v(
                    TAG, "New client " + client.asBinder()
                    + ": window=" + win);
                
                if (win.isVisibleOrAdding() && updateOrientationFromAppTokensLocked()) {
                    reportNewConfig = true;
                }
            }
    
            // sendNewConfiguration() checks caller permissions so we must call it with
            // privilege.  updateOrientationFromAppTokens() clears and resets the caller
            // identity anyway, so it's safe to just clear & restore around this whole
            // block.
            final long origId = Binder.clearCallingIdentity();
            if (reportNewConfig) {
                sendNewConfiguration();
            }
            Binder.restoreCallingIdentity(origId);
    
            return res;
        }

}
```

我们先来看看这个函数的参数：

```
Session session：继承于IWindowSession.Stub，IWindowSession是向WindowManagerService请求窗口操作的中间代
理，Session正是相当于AIDL里的Stub端。
IWindow client：
WindowManager.LayoutParams attrs：布局参数信息
int viewVisibility：是否可见
Rect outContentInsets：
InputChannel outInputChannel：
```

在这个函数中，我们看到2个重要的集合：

```
mTokenMap：它以LayoutParams里的token为key保存WindowToken，它保存了所有窗口token的信息。
mWindowMap：它以Binder对象为可以存储WindowState，它保存了系统中的所有窗口。
```

从上面可以看出：

WindowToken具有令牌的作用，是对应用组件的行为进行规范管理的一个手段。WindowToken由应用组件或其管理者负责向WindwoManagerService声明并持
有。应用组件在需要新的窗口时，必须提供WindowToken以表明自己的身份，并且窗口的类型必须与所持有的WindowToken的类型一致。

另外，在创建系统类型的窗口时不需要提供一个有效的Token，WindwoManagerService会隐式地为其声明一个WindowToken，但是在addWindow()函数一开始
的mPolicy.checkAddPermission()的目的就是如此。它要求客户端必须拥有SYSTEM_ALERT_WINDOW或INTERNAL_SYSTEM_WINDOW权限才能创建系统类
型的窗口。

该函数中还牵扯了一类型


```java

```

```java

```


```java

```

```java

```
```java

```

```java

```




