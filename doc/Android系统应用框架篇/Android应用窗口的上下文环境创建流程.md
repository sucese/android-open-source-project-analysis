# Android显示框架：Activity应用视图的创建流程

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 创建应用上下文环境
- 二 创建应用窗口
- 三 创建应用视图
 
Android应用在运行的过程中需要访问一些特定的资源和类，这些特定的资源或者类构成了Android应用运行的上下文环境，即Context。Context是一个抽象类，ContextImpl继承了Context，
并实现它的抽象方法。

因此，每个Activity组件关联的是ContextImpl对象，它们的类图关系如下：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/Context_class.png" height="500"/>

Context家族相关类采用装饰模式设计而成，ContextWrapper与ContextThemeWrapper继承于Context，是它的包装类，用于完成更多的功能。ContextWrapper与ContextThemeWrapper背部都通过
成员变量mBasae引用了一个ContextImpl对象，Activity正是通过这个ContextImpl对象执行一些具体的操作，例如：启动Activity、启动Service等。

```java
Context mBase;

public ContextWrapper(Context base) {
    mBase = base;
}
```
比较有意思的是，ContextImpl内部也有一个mOuterContext对象，它在自己初始化的时候传入，它引用的正是与它关联的Activity，这样它也可以把一些操作转交给Activity。

```java
private Context mOuterContext;

ContextImpl() {
    mOuterContext = this;
}
```

## 一 创建应用上下文环境C

我们之前分析过Activity的启动流程，可以得知这个流程的最后一步是调用ActivityThread.perforLaunchActivity()方法在应用进程中创建一个Activity实例，并为它蛇者一个
上下文环境，即创建一个ContexImpl对象。

ContexImpl的创建流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/Context_sequence.png" height="500"/>

主要角色：

- Instrumenttation：记录应用与系统的交互过程
- Contextrapper: ContextImpl的代理类，包装了ContextImpl里的相关操作。
- ContextThemeWrapper：用来维护一个应用的窗口主题

整个流程还是比较简单清晰的，我们着重分析里面的关键点。

**关键点1：ActivityThread.performLaunchActivity(ActivityClientRecord r, Intent customIntent)**

这个方法完成了Activity启动以及ContextImpl创建的主要流程，它完成的工作有：

- 1 从Intent中获取Activity的组件名ComponentName，调用对应的类加载器进行加载，调用Activity的默认构造方法进行实例创建。
- 2 调用ContextImpl的构造方法创建ContextImpl，并调用ContextImpl.setOuterContext()方法将已经创建完成的Activity关了给ContextImpl。
- 3 调用Activity.attach()关联上下文信息、Activity信息、Intent信息等Activity运行所需要的信息。
- 4 调用InstrumentationcallActivityOnCreate()，通知Activity你已经被创建，相关环境与信息也已经准备好，可以执行你的onCreate()方法辣，接着Activity就去执行它的onCreate()方法了。

```java
public final class ActivityThread {
    
    private final Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
            // System.out.println("##### [" + System.currentTimeMillis() + "] ActivityThread.performLaunchActivity(" + r + ")");
    
            ActivityInfo aInfo = r.activityInfo;
            if (r.packageInfo == null) {
                r.packageInfo = getPackageInfo(aInfo.applicationInfo,
                        Context.CONTEXT_INCLUDE_CODE);
            }
    
            //1 从Intent中获取Activity的组件名ComponentName，调用对应的类加载器进行加载，调用
            //Activity的默认构造方法进行实例创建。
            ComponentName component = r.intent.getComponent();
            if (component == null) {
                component = r.intent.resolveActivity(
                    mInitialApplication.getPackageManager());
                r.intent.setComponent(component);
            }
    
            if (r.activityInfo.targetActivity != null) {
                component = new ComponentName(r.activityInfo.packageName,
                        r.activityInfo.targetActivity);
            }
    
            Activity activity = null;
            try {
                java.lang.ClassLoader cl = r.packageInfo.getClassLoader();
                activity = mInstrumentation.newActivity(
                        cl, component.getClassName(), r.intent);
                r.intent.setExtrasClassLoader(cl);
                if (r.state != null) {
                    r.state.setClassLoader(cl);
                }
            } catch (Exception e) {
                if (!mInstrumentation.onException(activity, e)) {
                    throw new RuntimeException(
                        "Unable to instantiate activity " + component
                        + ": " + e.toString(), e);
                }
            }
    
            try {
                Application app = r.packageInfo.makeApplication(false, mInstrumentation);
    
                if (localLOGV) Slog.v(TAG, "Performing launch of " + r);
                if (localLOGV) Slog.v(
                        TAG, r + ": app=" + app
                        + ", appName=" + app.getPackageName()
                        + ", pkg=" + r.packageInfo.getPackageName()
                        + ", comp=" + r.intent.getComponent().toShortString()
                        + ", dir=" + r.packageInfo.getAppDir());
    
                if (activity != null) {
                    //2 调用ContextImpl的构造方法创建ContextImpl，并调用ContextImpl.setOuterContext()方法将已经创建完成的Activity关了给ContextImpl。
                    ContextImpl appContext = new ContextImpl();
                    appContext.init(r.packageInfo, r.token, this);
                    appContext.setOuterContext(activity);
                    CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());
                    Configuration config = new Configuration(mConfiguration);
                    if (DEBUG_CONFIGURATION) Slog.v(TAG, "Launching activity "
                            + r.activityInfo.name + " with config " + config);
                    //3 调用Activity.attach()关联上下文信息、Activity信息、Intent信息等Activity运行所需要的信息。
                    activity.attach(appContext, this, getInstrumentation(), r.token,
                            r.ident, app, r.intent, r.activityInfo, title, r.parent,
                            r.embeddedID, r.lastNonConfigurationInstance,
                            r.lastNonConfigurationChildInstances, config);
    
                    if (customIntent != null) {
                        activity.mIntent = customIntent;
                    }
                    r.lastNonConfigurationInstance = null;
                    r.lastNonConfigurationChildInstances = null;
                    activity.mStartedActivity = false;
                    int theme = r.activityInfo.getThemeResource();
                    if (theme != 0) {
                        activity.setTheme(theme);
                    }
    
                    activity.mCalled = false;
                    mInstrumentation.callActivityOnCreate(activity, r.state);
                    if (!activity.mCalled) {
                        throw new SuperNotCalledException(
                            "Activity " + r.intent.getComponent().toShortString() +
                            " did not call through to super.onCreate()");
                    }
                    r.activity = activity;
                    r.stopped = true;
                    if (!r.activity.mFinished) {
                        activity.performStart();
                        r.stopped = false;
                    }
                    if (!r.activity.mFinished) {
                        if (r.state != null) {
                            //4 调用InstrumentationcallActivityOnCreate()，通知Activity你已经被创建，相关环境与信息也已经准备好，可以执行
                            // 你的onCreate()方法辣，接着Activity就去执行它的onCreate()方法了。
                            mInstrumentation.callActivityOnRestoreInstanceState(activity, r.state);
                        }
                    }
                    if (!r.activity.mFinished) {
                        activity.mCalled = false;
                        mInstrumentation.callActivityOnPostCreate(activity, r.state);
                        if (!activity.mCalled) {
                            throw new SuperNotCalledException(
                                "Activity " + r.intent.getComponent().toShortString() +
                                " did not call through to super.onPostCreate()");
                        }
                    }
                }
                r.paused = true;
    
                mActivities.put(r.token, r);
    
            } catch (SuperNotCalledException e) {
                throw e;
    
            } catch (Exception e) {
                if (!mInstrumentation.onException(activity, e)) {
                    throw new RuntimeException(
                        "Unable to start activity " + component
                        + ": " + e.toString(), e);
                }
            }
    
            return activity;
        }
}
```

**关键点2：Activity.attach()**

Activity在被类加载器加载时调用的是默认的构造方法，这个方法什么都没有做，只是创建了个实例，真正的初始化流程在attach()方法里完成。

你可以看到attach()方法会调用ContextWrapper.attachBaseContext(context)进一步设置Context信息，这个方法就是将创建的ContextImpl赋值
给它的成员变量mBase。

除此之外，它还做了两件事：

- 1 调用PolicyManager.makeNewWindow(this)创建了应用窗口Window，它实际是个PhoneWindow对象，它会接收一些事件，例如：键盘、触摸事件，它会
转发这些事件给它关联的Activity，转发操作通过Window.Callback接口实现。
- 2 将Activity运行的一些关键信息带入Activity。


后续的UI绘制就砸Window上完成，并被Window设置了WindowManager。

```java
public class Activity extends ContextThemeWrapper{
    
    final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            Object lastNonConfigurationInstance,
            HashMap<String,Object> lastNonConfigurationChildInstances,
            Configuration config) {
        attachBaseContext(context);

        //1 调用PolicyManager.makeNewWindow(this)创建了应用窗口Window，它实际是个PhoneWindow对象，它会接收一些事
        // 件，例如：键盘、触摸事件，它会转发这些事件给它关联的Activity，转发操作通过Window.Callback接口实现。
        mWindow = PolicyManager.makeNewWindow(this);
        mWindow.setCallback(this);
        if (info.softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
            mWindow.setSoftInputMode(info.softInputMode);
        }
        mUiThread = Thread.currentThread();
        //2 将Activity运行的一些关键信息带入Activity。
        mMainThread = aThread;
        mInstrumentation = instr;
        mToken = token;
        mIdent = ident;
        mApplication = application;
        mIntent = intent;
        mComponent = intent.getComponent();
        mActivityInfo = info;
        mTitle = title;
        mParent = parent;
        mEmbeddedID = id;
        mLastNonConfigurationInstance = lastNonConfigurationInstance;
        mLastNonConfigurationChildInstances = lastNonConfigurationChildInstances;

        mWindow.setWindowManager(null, mToken, mComponent.flattenToString());
        if (mParent != null) {
            mWindow.setContainer(mParent.getWindow());
        }
        mWindowManager = mWindow.getWindowManager();
        mCurrentConfig = config;
    }
}
```

以上便是ContextImpl对象创建过程的一些关键点，还是比较简单的，我们再来总结一下。


```
1 一个Android应用窗口的运行上下文环境是使用一个ContextImpl对象来描述的，这个ContextImpl对象会分别保存在Activity类的
父类ContextThemeWrapper和ContextWrapper的成员变量mBase中，即ContextThemeWrapper类和ContextWrapper类的成员变量
mBase指向的是一个ContextImpl对象。
2 Activity组件在创建过程中，即在它的成员函数attach被调用的时候，会创建一个PhoneWindow对象，并且保存在成员变量mWindow
中，用来描述一个具体的Android应用程序窗口。
3 Activity组件在创建的最后，即在它的子类所重写的成员函数onCreate中，会调用父类Activity的成员函数setContentView来创建
一个Android应用程序窗口的视图。
```

## 二 创建应用窗口Window

从上面的Activity.attach()方法的分析我们得知了ContextImpl的创建流程，我们发现它不仅创建了上下文环境Context，它还创建了Window对象，用来描述一个具体的应用窗口，可以看出
Activity只不过是一个高度抽象的UI组件，它的具体UI实现是由它的一系列对象来完成的，它们的类图关系如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/Window_class.png" height="500"/>

从上文的描述我们可以知道，Windows是在Activity的attach()方法中开始创建的，我们来看下它的创建流程。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/Window_sequence.png" height="500"/>

主要角色：

- PhoneWindow：Window的子类，应用视图窗口。
- WindowManagerImpl：实现了WIndowManager接口，用来管理窗口。

**关键点1：PhoneWindow(Context context)**

PolicyManager.makeNewWindow(this)用来创建Window对象，该函数通过反射最终调用Policy.makeNewWindow(Context context)，在这个
方法里调用了PhoneWindow的构造函数，返回了一个PhoneWindow对象。

在PhoneWindow的构造函数里，我们很惊奇的发现它返回了一个LayoutInflater对象。这货就是我们用来绘制xml里面UI的东西。


```java
public PhoneWindow(Context context) {
    super(context);
    mLayoutInflater = LayoutInflater.from(context);
}
```

PhoneWindow其实就是我们最终要用的视图窗口了，除了mLayoutInflater，它里面还有两个重要的成员变量：

-  private DecorView mDecor：顶级View视图，它由mLayoutInflater来创建。
-  private ViewGroup mContentParent：视图容器。

**关键点2：Window.setCallback(this)**

Activity实现了Window.Callback接口，将Activity关联给Window，Window就可以将一些事件交由Activity处理，具体有哪些事情呢？

```java
 public interface Callback {

        //键盘事件分发
        public boolean dispatchKeyEvent(KeyEvent event);
        
        //触摸事件分发
        public boolean dispatchTouchEvent(MotionEvent event);
        
        //轨迹球事件分发
        public boolean dispatchTrackballEvent(MotionEvent event);

        //可见性事件分发
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event);

        //创建Panel View
        public View onCreatePanelView(int featureId);

        //创建menu
        public boolean onCreatePanelMenu(int featureId, Menu menu);

        //画板准备好时回调
        public boolean onPreparePanel(int featureId, View view, Menu menu);

        //menu打开时回调
        public boolean onMenuOpened(int featureId, Menu menu);

        //menu item被选择时回调
        public boolean onMenuItemSelected(int featureId, MenuItem item);

        //Window Attributes发生变化时回调
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs);

        //Content View发生变化时回调
        public void onContentChanged();

        //窗口焦点发生变化时回调
        public void onWindowFocusChanged(boolean hasFocus);

        //Window被添加到WIndowManager时回调
        public void onAttachedToWindow();
        
        //Window被从WIndowManager中移除时回调
        public void onDetachedFromWindow();
        
         */
        //画板关闭时回调
        public void onPanelClosed(int featureId, Menu menu);
        
        //用户开始执行搜索操作时回调
        public boolean onSearchRequested();
    }
```

**关键点3：Window.setSoftInputMode(int mode)**

这个我们就比较熟悉了，我们会在AndroidManifest.xml里Activity的标签下设置android:windowSoftInputMode="adjustNothing"，来控制输入键盘显示行为。

可选的有6个参数，源码里也有6个值与之对应：

- SOFT_INPUT_STATE_UNSPECIFIED：没有指定软键盘输入区域的显示状态。
- SOFT_INPUT_STATE_UNCHANGED：不要改变软键盘输入区域的显示状态。
- SOFT_INPUT_STATE_HIDDEN：在合适的时候隐藏软键盘输入区域，例如，当用户导航到当前窗口时。
- SOFT_INPUT_STATE_ALWAYS_HIDDEN：当窗口获得焦点时，总是隐藏软键盘输入区域。
- SOFT_INPUT_STATE_VISIBLE：在合适的时候显示软键盘输入区域，例如，当用户导航到当前窗口时。
- SOFT_INPUT_STATE_ALWAYS_VISIBLE：当窗口获得焦点时，总是显示软键盘输入区域。

**关键点4： Window.setWindowManager(WindowManager wm, IBinder appToken, String appName)**

```java
public void setWindowManager(WindowManager wm,
        IBinder appToken, String appName) {
    mAppToken = appToken;
    mAppName = appName;
    if (wm == null) {
        wm = WindowManagerImpl.getDefault();
    }
    mWindowManager = new LocalWindowManager(wm);
}
```

上述的Activity.attach()最后会调用Window.setWindowManager(WindowManager wm, IBinder appToken, String appName)来为已经创建的Window对象
设置一个WindowManger，用来管理Window。

这个LocalWindowManager我们来说道说道，它是Window的一个内部类，实现了WIndowManager接口，它主要用来管理两个内部变量

- private final WindowManager mWindowManager：真正的Window管理者，它的实现类是WindowManagerImpl，可以通过WindowManagerImpl.getDefault()获得。
- private final Display mDefaultDisplay：它是一个Display对象，它描述了屏幕的相关信息。

到这为止，我们的Window对象就创建完成了，我们来总结一下。

```
1 一个Activity组件所关联的应用程序窗口对象的类型为PhoneWindow。
2 这个类型为PhoneWindow的应用程序窗口是通过一个类型为LocalWindowManager的本地窗口管理器来维护的。
3 这个类型为LocalWindowManager的本地窗口管理器又是通过一个类型为WindowManagerImpl的窗口管理器来维护应用
程序窗口的。
4 这个类型为PhoneWindow的应用程序窗口内部有一个类型为DecorView的视图对象，这个视图对象才是真正用来描述一个
Activity组件的UI的
```

## 三 创建应用视图View

从上文分析可知，每个Activity组件关联一个Window对象（PhoneWindow），而每个Window内部又包含一个View对象（DecorView），用来描述应用视图。
它们的类图关系如下：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/View_class.png" height="500"/>

我们来看下View的创建流程

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/View_class.png" height="500"/>

**关键点1：ActivityThread.handleLaunchActivity(ActivityClientRecord r, Intent customIntent)**

```java
public final class ActivityThread{
    
    private final void handleLaunchActivity(ActivityClientRecord r, Intent customIntent) {
            // If we are getting ready to gc after going to the background, well
            // we are back active so skip it.
            unscheduleGcIdler();
    
            if (localLOGV) Slog.v(
                TAG, "Handling launch of " + r);
            Activity a = performLaunchActivity(r, customIntent);
    
            if (a != null) {
                r.createdConfig = new Configuration(mConfiguration);
                Bundle oldState = r.state;
                handleResumeActivity(r.token, false, r.isForward);
    
                if (!r.activity.mFinished && r.startsNotResumed) {
                    // The activity manager actually wants this one to start out
                    // paused, because it needs to be visible but isn't in the
                    // foreground.  We accomplish this by going through the
                    // normal startup (because activities expect to go through
                    // onResume() the first time they run, before their window
                    // is displayed), and then pausing it.  However, in this case
                    // we do -not- need to do the full pause cycle (of freezing
                    // and such) because the activity manager assumes it can just
                    // retain the current state it has.
                    try {
                        r.activity.mCalled = false;
                        mInstrumentation.callActivityOnPause(r.activity);
                        // We need to keep around the original state, in case
                        // we need to be created again.
                        r.state = oldState;
                        if (!r.activity.mCalled) {
                            throw new SuperNotCalledException(
                                "Activity " + r.intent.getComponent().toShortString() +
                                " did not call through to super.onPause()");
                        }
    
                    } catch (SuperNotCalledException e) {
                        throw e;
    
                    } catch (Exception e) {
                        if (!mInstrumentation.onException(r.activity, e)) {
                            throw new RuntimeException(
                                    "Unable to pause activity "
                                    + r.intent.getComponent().toShortString()
                                    + ": " + e.toString(), e);
                        }
                    }
                    r.paused = true;
                }
            } else {
                // If there was an error, for any reason, tell the activity
                // manager to stop us.
                try {
                    ActivityManagerNative.getDefault()
                        .finishActivity(r.token, Activity.RESULT_CANCELED, null);
                } catch (RemoteException ex) {
                }
            }
        }
       
}
 
```
在Activity启动流程的文章里我们分析过这个方法是ActivityManagerService接收到SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION进程间通信请求是触发的。

它的执行流程如下：

1. 先去调用performLaunchActivity()方法，创建Context、Window等对象。最终会调用到Activity.onCreate()方法。
2. 再去调用handleResumeActivity()方法，handleResumeActivity()会调用performResumeActivity()来通知Activity组件它将要被激活，最终会调用
Activity.onResume()方法。


**关键点2：PhoneWindow.setContentView(int layoutResID)**

在上面的描述中，我们知道ActivityThread.performLaunchActivity()方法会去调用Activity.onCreate()方法。当我们在覆写Activity的onCreate()方法
时，里面有个非常熟悉的方法setContentView()，它实际上调用的是Window.setContentView()。我们来看看Window子类PhoneWindow里
对这个方法的实现。

```java
public class PhoneWindow extends Window implements MenuBuilder.Callback {
 
        // This is the top-level view of the window, containing the window decor.
        private DecorView mDecor;
    
        // This is the view in which the window contents are placed. It is either
        // mDecor itself, or a child of mDecor where the contents go.
        private ViewGroup mContentParent;
        
        @Override
        public void setContentView(int layoutResID) {
            if (mContentParent == null) {
                installDecor();
            } else {
                mContentParent.removeAllViews();
            }
            mLayoutInflater.inflate(layoutResID, mContentParent);
            final Callback cb = getCallback();
            if (cb != null) {
                cb.onContentChanged();
            }
        }
        
        private void installDecor() {
            if (mDecor == null) {
                mDecor = generateDecor();
                mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                mDecor.setIsRootNamespace(true);
            }
            if (mContentParent == null) {
                mContentParent = generateLayout(mDecor);
    
                mTitleView = (TextView)findViewById(com.android.internal.R.id.title);
                if (mTitleView != null) {
                    if ((getLocalFeatures() & (1 << FEATURE_NO_TITLE)) != 0) {
                        View titleContainer = findViewById(com.android.internal.R.id.title_container);
                        if (titleContainer != null) {
                            titleContainer.setVisibility(View.GONE);
                        } else {
                            mTitleView.setVisibility(View.GONE);
                        }
                        if (mContentParent instanceof FrameLayout) {
                            ((FrameLayout)mContentParent).setForeground(null);
                        }
                    } else {
                        mTitleView.setText(mTitle);
                    }
                }
            }
        }
}
```
mContentParent用来描述一个类型为DecorView的视图对象，如果它为空，则调用installDecor()方法创建窗口视图。如果不空则清除原来的UI。
然后根据mLayoutInflater根据layoutResID去构建UI，并通过Window.Callback通知窗口视图内容已经发生变化。通过前面的分析，我们知道
Activity实现了该Callback，因此最终调用的是Activity里的nContentChanged()方法。

我们再来看看installDecor()方法：

1. 如果mDecor为空则通过generateDecor()调用DecorView的构造方法构建一个DecorView对象。
2. 如果mContentParent为空，则通过generateLayout(mDecor)构建一个mContentParent对象。

generateLayout(mDecor)这个方法比较长

彩蛋：你在installDecor()这个方法里还可以我们经常用来隐藏标题栏的状态标志位FEATURE_NO_TITLE。

我们从里也了解到了源码里xml文件的id，就不往这里贴了，它主要用来设置窗口的标志位，mContentParent通过

```java
public static final int ID_ANDROID_CONTENT = com.android.internal.R.id.content;

ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
```

通过上面的描述，我们还了解到了一些源码内部的View Id：

- com.android.internal.R.id.title：标题
- com.android.internal.R.id.title_container：标题容器
- com.android.internal.R.id.content：内容

**关键点3：ActivityThread.handleResumeActivity(IBinder token, boolean clearHide, boolean isForward)** 

```java
public final class ActivityThread{
     
        final void handleResumeActivity(IBinder token, boolean clearHide, boolean isForward) {
                // If we are getting ready to gc after going to the background, well
                // we are back active so skip it.
                unscheduleGcIdler();
        
                //1. 调用performResumeActivity()来通知Activity组件它将要被激活，最终会调用Activity.onResume()方法。该方法还返回一个ActivityClientRecord
                ActivityClientRecord r = performResumeActivity(token, clearHide);
        
                if (r != null) {
                    final Activity a = r.activity;
        
                    if (localLOGV) Slog.v(
                        TAG, "Resume " + r + " started activity: " +
                        a.mStartedActivity + ", hideForNow: " + r.hideForNow
                        + ", finished: " + a.mFinished);
        
                    final int forwardBit = isForward ?
                            WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION : 0;
        
                    // If the window hasn't yet been added to the window manager,
                    // and this guy didn't finish itself or start another activity,
                    // then go ahead and add the window.
                    boolean willBeVisible = !a.mStartedActivity;
                    if (!willBeVisible) {
                        try {
                            willBeVisible = ActivityManagerNative.getDefault().willActivityBeVisible(
                                    a.getActivityToken());
                        } catch (RemoteException e) {
                        }
                    }
                    if (r.window == null && !a.mFinished && willBeVisible) {
                        r.window = r.activity.getWindow();
                        View decor = r.window.getDecorView();
                        decor.setVisibility(View.INVISIBLE);
                        ViewManager wm = a.getWindowManager();
                        WindowManager.LayoutParams l = r.window.getAttributes();
                        a.mDecor = decor;
                        l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
                        l.softInputMode |= forwardBit;
                        if (a.mVisibleFromClient) {
                            a.mWindowAdded = true;
                            wm.addView(decor, l);
                        }
        
                    // If the window has already been added, but during resume
                    // we started another activity, then don't yet make the
                    // window visible.
                    } else if (!willBeVisible) {
                        if (localLOGV) Slog.v(
                            TAG, "Launch " + r + " mStartedActivity set");
                        r.hideForNow = true;
                    }
        
                    // The window is now visible if it has been added, we are not
                    // simply finishing, and we are not starting another activity.
                    
                    //2. 判断将要激活的Activity组件是否可见，即willBeVisible的值。Activity里有个成员变量mStartedActivity描述一个Activity组件是否正在启动一个新的
                    //Activity组件，并且等待这个Activity的执行结果，也就是startActivityForResult()的情况。这种情况下mStartedActivity为true，那么在这个新的Activity
                    //组件返回之前，这个Activity始终处于不可见状态，但是，如果这个新的Activity组件不是全屏的，那么即便mStartedActivity == true，willBeVisible也要设置
                    //为true，即该Activity组件可见。
                    if (!r.activity.mFinished && willBeVisible
                            && r.activity.mDecor != null && !r.hideForNow) {
                        if (r.newConfig != null) {
                            if (DEBUG_CONFIGURATION) Slog.v(TAG, "Resuming activity "
                                    + r.activityInfo.name + " with newConfig " + r.newConfig);
                            performConfigurationChanged(r.activity, r.newConfig);
                            r.newConfig = null;
                        }
                        if (localLOGV) Slog.v(TAG, "Resuming " + r + " with isForward="
                                + isForward);
                        WindowManager.LayoutParams l = r.window.getAttributes();
                        if ((l.softInputMode
                                & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION)
                                != forwardBit) {
                            l.softInputMode = (l.softInputMode
                                    & (~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION))
                                    | forwardBit;
                            if (r.activity.mVisibleFromClient) {
                                ViewManager wm = a.getWindowManager();
                                View decor = r.window.getDecorView();
                                wm.updateViewLayout(decor, l);
                            }
                        }
                        r.activity.mVisibleFromServer = true;
                        mNumVisibleActivities++;
                        if (r.activity.mVisibleFromClient) {
                            r.activity.makeVisible();
                        }
                    }
        
                    r.nextIdle = mNewActivities;
                    mNewActivities = r;
                    if (localLOGV) Slog.v(
                        TAG, "Scheduling idle handler for " + r);
                    Looper.myQueue().addIdleHandler(new Idler());
        
                } else {
                    // If an exception was thrown when trying to resume, then
                    // just end this activity.
                    try {
                        ActivityManagerNative.getDefault()
                            .finishActivity(token, Activity.RESULT_CANCELED, null);
                    } catch (RemoteException ex) {
                    }
                }
            }
}
```

这个方法主要用来处理Activity.onCreate()之后Activity.onResume()的流程，它的主要流程如下：

1. 调用performResumeActivity()来通知Activity组件它将要被激活，最终会调用Activity.onResume()方法。该方法还返回一个ActivityClientRecord
对象，该对象描述正在激活的Activity组件。
2. 判断将要激活的Activity组件是否可见，即willBeVisible的值。Activity里有个成员变量mStartedActivity描述一个Activity组件是否正在启动一个新的
Activity组件，并且等待这个Activity的执行结果，也就是startActivityForResult()的情况。这种情况下mStartedActivity为true，那么在这个新的Activity
组件返回之前，这个Activity始终处于不可见状态，但是，如果这个新的Activity组件不是全屏的，那么即便mStartedActivity == true，willBeVisible也要设置
为true，即该Activity组件可见。
3. 调用WIndowManager.addView()方法为当前正在激活的Activity组件关联一个ViewRoot对象，调用链比较长，可以参考序列图。

关于LocalWindowManager、WindowManager与WindowManagerImpl的关系我们前面已经分析过，我们直接来看WindowManagerImpl.addView()方法。


**关键点4：WindowManagerImpl.addView(View view, ViewGroup.LayoutParams params, boolean nest)**

```java
public class WindowManagerImpl implements WindowManager {
    
    private View[] mViews;
    private ViewRoot[] mRoots;
    private WindowManager.LayoutParams[] mParams;
    
    private void addView(View view, ViewGroup.LayoutParams params, boolean nest)
        {
            if (Config.LOGV) Log.v("WindowManager", "addView view=" + view);
    
            if (!(params instanceof WindowManager.LayoutParams)) {
                throw new IllegalArgumentException(
                        "Params must be WindowManager.LayoutParams");
            }
    
            final WindowManager.LayoutParams wparams
                    = (WindowManager.LayoutParams)params;
            
            ViewRoot root;
            View panelParentView = null;
            
            synchronized (this) {
                // Here's an odd/questionable case: if someone tries to add a
                // view multiple times, then we simply bump up a nesting count
                // and they need to remove the view the corresponding number of
                // times to have it actually removed from the window manager.
                // This is useful specifically for the notification manager,
                // which can continually add/remove the same view as a
                // notification gets updated.
                int index = findViewLocked(view, false);
                if (index >= 0) {
                    if (!nest) {
                        throw new IllegalStateException("View " + view
                                + " has already been added to the window manager.");
                    }
                    root = mRoots[index];
                    root.mAddNesting++;
                    // Update layout parameters.
                    view.setLayoutParams(wparams);
                    root.setLayoutParams(wparams, true);
                    return;
                }
                
                // If this is a panel window, then find the window it is being
                // attached to for future reference.
                if (wparams.type >= WindowManager.LayoutParams.FIRST_SUB_WINDOW &&
                        wparams.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
                    final int count = mViews != null ? mViews.length : 0;
                    for (int i=0; i<count; i++) {
                        if (mRoots[i].mWindow.asBinder() == wparams.token) {
                            panelParentView = mViews[i];
                        }
                    }
                }
                
                root = new ViewRoot(view.getContext());
                root.mAddNesting = 1;
    
                view.setLayoutParams(wparams);
                
                if (mViews == null) {
                    index = 1;
                    mViews = new View[1];
                    mRoots = new ViewRoot[1];
                    mParams = new WindowManager.LayoutParams[1];
                } else {
                    index = mViews.length + 1;
                    Object[] old = mViews;
                    mViews = new View[index];
                    System.arraycopy(old, 0, mViews, 0, index-1);
                    old = mRoots;
                    mRoots = new ViewRoot[index];
                    System.arraycopy(old, 0, mRoots, 0, index-1);
                    old = mParams;
                    mParams = new WindowManager.LayoutParams[index];
                    System.arraycopy(old, 0, mParams, 0, index-1);
                }
                index--;
    
                mViews[index] = view;
                mRoots[index] = root;
                mParams[index] = wparams;
            }
            // do this last because it fires off messages to start doing things
            root.setView(view, wparams, panelParentView);
        }
}
```

你可以看到在WIndowManagerImpl这个类了有三个数组，这三个数组的大小始终都是相等的。

- private View[] mViews：View对象
- private ViewRoot[] mRoots：与View关联的ViewRoot对象
- private WindowManager.LayoutParams[] mParams：与View关联的WindowManager.LayoutParams对象，它用来描述窗口视图的布局属性。

如果mViews包含目标View，则说明View已经关联过ViewRoot与WindowManager.LayoutParams，则直接查找对应位置的索引。
如果mViews不包含目标View，则创建新的ViewRoot，并添加到这三个数组中。

**关键点5：ViewRoot.setView(View view, WindowManager.LayoutParams attrs, View panelParentView)**

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
    
    public void setView(View view, WindowManager.LayoutParams attrs,
                View panelParentView) {
            synchronized (this) {
                if (mView == null) {
                    mView = view;
                    mWindowAttributes.copyFrom(attrs);
                    attrs = mWindowAttributes;
                    if (view instanceof RootViewSurfaceTaker) {
                        mSurfaceHolderCallback =
                                ((RootViewSurfaceTaker)view).willYouTakeTheSurface();
                        if (mSurfaceHolderCallback != null) {
                            mSurfaceHolder = new TakenSurfaceHolder();
                            mSurfaceHolder.setFormat(PixelFormat.UNKNOWN);
                        }
                    }
                    Resources resources = mView.getContext().getResources();
                    CompatibilityInfo compatibilityInfo = resources.getCompatibilityInfo();
                    mTranslator = compatibilityInfo.getTranslator();
    
                    if (mTranslator != null || !compatibilityInfo.supportsScreen()) {
                        mSurface.setCompatibleDisplayMetrics(resources.getDisplayMetrics(),
                                mTranslator);
                    }
    
                    boolean restore = false;
                    if (mTranslator != null) {
                        restore = true;
                        attrs.backup();
                        mTranslator.translateWindowLayout(attrs);
                    }
                    if (DEBUG_LAYOUT) Log.d(TAG, "WindowLayout in setView:" + attrs);
    
                    if (!compatibilityInfo.supportsScreen()) {
                        attrs.flags |= WindowManager.LayoutParams.FLAG_COMPATIBLE_WINDOW;
                    }
    
                    mSoftInputMode = attrs.softInputMode;
                    mWindowAttributesChanged = true;
                    mAttachInfo.mRootView = view;
                    mAttachInfo.mScalingRequired = mTranslator != null;
                    mAttachInfo.mApplicationScale =
                            mTranslator == null ? 1.0f : mTranslator.applicationScale;
                    if (panelParentView != null) {
                        mAttachInfo.mPanelParentWindowToken
                                = panelParentView.getApplicationWindowToken();
                    }
                    mAdded = true;
                    int res; /* = WindowManagerImpl.ADD_OKAY; */
    
                    // Schedule the first layout -before- adding to the window
                    // manager, to make sure we do the relayout before receiving
                    // any other events from the system.
                    requestLayout();
                    mInputChannel = new InputChannel();
                    try {
                        res = sWindowSession.add(mWindow, mWindowAttributes,
                                getHostVisibility(), mAttachInfo.mContentInsets,
                                mInputChannel);
                    } catch (RemoteException e) {
                        mAdded = false;
                        mView = null;
                        mAttachInfo.mRootView = null;
                        mInputChannel = null;
                        unscheduleTraversals();
                        throw new RuntimeException("Adding window failed", e);
                    } finally {
                        if (restore) {
                            attrs.restore();
                        }
                    }
                    
                    if (mTranslator != null) {
                        mTranslator.translateRectInScreenToAppWindow(mAttachInfo.mContentInsets);
                    }
                    mPendingContentInsets.set(mAttachInfo.mContentInsets);
                    mPendingVisibleInsets.set(0, 0, 0, 0);
                    if (Config.LOGV) Log.v(TAG, "Added window " + mWindow);
                    if (res < WindowManagerImpl.ADD_OKAY) {
                        mView = null;
                        mAttachInfo.mRootView = null;
                        mAdded = false;
                        unscheduleTraversals();
                        switch (res) {
                            case WindowManagerImpl.ADD_BAD_APP_TOKEN:
                            case WindowManagerImpl.ADD_BAD_SUBWINDOW_TOKEN:
                                throw new WindowManagerImpl.BadTokenException(
                                    "Unable to add window -- token " + attrs.token
                                    + " is not valid; is your activity running?");
                            case WindowManagerImpl.ADD_NOT_APP_TOKEN:
                                throw new WindowManagerImpl.BadTokenException(
                                    "Unable to add window -- token " + attrs.token
                                    + " is not for an application");
                            case WindowManagerImpl.ADD_APP_EXITING:
                                throw new WindowManagerImpl.BadTokenException(
                                    "Unable to add window -- app for token " + attrs.token
                                    + " is exiting");
                            case WindowManagerImpl.ADD_DUPLICATE_ADD:
                                throw new WindowManagerImpl.BadTokenException(
                                    "Unable to add window -- window " + mWindow
                                    + " has already been added");
                            case WindowManagerImpl.ADD_STARTING_NOT_NEEDED:
                                // Silently ignore -- we would have just removed it
                                // right away, anyway.
                                return;
                            case WindowManagerImpl.ADD_MULTIPLE_SINGLETON:
                                throw new WindowManagerImpl.BadTokenException(
                                    "Unable to add window " + mWindow +
                                    " -- another window of this type already exists");
                            case WindowManagerImpl.ADD_PERMISSION_DENIED:
                                throw new WindowManagerImpl.BadTokenException(
                                    "Unable to add window " + mWindow +
                                    " -- permission denied for this window type");
                        }
                        throw new RuntimeException(
                            "Unable to add window -- unknown error code " + res);
                    }
    
                    if (view instanceof RootViewSurfaceTaker) {
                        mInputQueueCallback =
                            ((RootViewSurfaceTaker)view).willYouTakeTheInputQueue();
                    }
                    if (mInputQueueCallback != null) {
                        mInputQueue = new InputQueue(mInputChannel);
                        mInputQueueCallback.onInputQueueCreated(mInputQueue);
                    } else {
                        InputQueue.registerInputChannel(mInputChannel, mInputHandler,
                                Looper.myQueue());
                    }
                    
                    view.assignParent(this);
                    mAddedTouchMode = (res&WindowManagerImpl.ADD_FLAG_IN_TOUCH_MODE) != 0;
                    mAppVisible = (res&WindowManagerImpl.ADD_FLAG_APP_VISIBLE) != 0;
                }
            }
        }
}
```

这个函数主要做了三件事情：

1. 保存上一步传递进来的View view, WindowManager.LayoutParams attrs等参数。
2. 调用ViewRoot.requestLayout()方法进行应用窗口UI的第一次布局。
3. 调用ViewRoot.sWindowSession.add(方法来请求WindowManagerService增加一个WindowState对象，以便可以描述当前ViewRoot正在处理的应用的窗口。

走到这里，我们的应用视图View就创建完成了。