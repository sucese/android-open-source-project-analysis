# Android系统应用框架篇：SurfaceFlinger服务启动流程分析

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 Context类图关系
- 二 ContextImpl的创建流程
    - 2.1 流程角色
    - 2.2 关键的分析


## 一 Context类图关系

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

## 二 ContextImpl的创建流程

我们之前分析过Activity的启动流程，可以得知这个流程的最后一步是调用ActivityThread.perforLaunchActivity()方法在应用进程中创建一个Activity实例，并为它蛇者一个
上下文环境，即创建一个ContexImpl对象。

ContexImpl的创建流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/Context_class.png" height="500"/>

## 2.1 流程角色

- Instrumenttation：记录应用与系统的交互过程

## 2.2 关键点分析

整个流程还是比较简单清晰的，我们着重分析里面的关键点。

**ActivityThread.performLaunchActivity(ActivityClientRecord r, Intent customIntent)**

这个方法完成了Activity启动以及ContextImpl创建的主要流程，它完成的工作有：

- 1 从Intent中获取Activity的组件名ComponentName，调用对应的类加载器进行加载。
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
    
            //1 从Intent中获取Activity的组件名ComponentName，调用对应的类加载器进行加载。
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