## Android显示框架：Android应用窗口的管理者WindowManager

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 Window的添加流程
- 二 Window的删除流程
- 三 Window的更新流程

>The interface that apps use to talk to the window manager.

WindowManager是应用与窗口管理服务WindowManagerService交互的接口，WindowManagerService是位于Framework层的窗口管理服务，它的职责是管理系统中的所有窗口，也就是Window，
关于Window的介绍，我们在文章[03Android显示框架：Android应用视图的管理者Window](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/03Android显示框架：Android应用视图管理者Window.md)已经
详细分析过，通俗来说，Window就是手机上一块显示区域，也就是Android中的绘制画布Surface，添加一个Window的过程，也就是申请分配一块Surface的过程。而整个流程的管理者正是WindowManagerService。

Window在WindowManagerService的管理下，有序的显示在屏幕上，构成了多姿多彩的用户界面，关于Android的整个窗口系统，可以用下图来表示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/window_mansger_service_structure.png" width="500"/>

- WindowManager：应用与窗口管理服务WindowManagerService交互的接口
- WindowManagerService：窗口管理服务，该服务运行在一个单独的进程中，因此WindowManager与WindowManagerService的交互也是一个IPC的过程。
- SurfaceFlinger：SurfaceFlinger服务运行在Android系统的System进程中，它负责管理Android系统的帧缓冲区（Frame Buffer)，Android设备的显示屏被抽象为一个
帧缓冲区，而Android系统中的SurfaceFlinger服务就是通过向这个帧缓冲区写入内容来绘制应用程序的用户界面的。
- Surface：每个显示界面的窗口都是一个Surface。

WindowManager是一个接口，继承于ViewManager，实现类是WindowManagerImpl，实际上我们常用的功能，也是定义在ViewManager里的。

```java
public interface ViewManager{
    //添加View
    public void addView(View view, ViewGroup.LayoutParams params);
    //更新View
    public void updateViewLayout(View view, ViewGroup.LayoutParams params);
    //删除View
    public void removeView(View view);
}
```

WindowManager可以通过Context来获取，WindowManager也会和其他服务一样在开机时注册到ContextImpl里的map容器里，然后通过他们的key来获取。

```java
windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
```

WindowManager的实现类是WindowManagerImpl，在WindowManagerImpl内部实际的功能是有WindowManagerGlobal来完成的，我们直接来分析它里面这三个方法的实现。

## 一 Window的添加流程

```java
public final class WindowManagerGlobal {
    
     public void addView(View view, ViewGroup.LayoutParams params,
                Display display, Window parentWindow) {
            //校验参数的合法性
            ...
            
            //ViewRootImpl封装了View与WindowManager的交互力促
            ViewRootImpl root;
            View panelParentView = null;
    
            synchronized (mLock) {
                // Start watching for system property changes.
                if (mSystemPropertyUpdater == null) {
                    mSystemPropertyUpdater = new Runnable() {
                        @Override public void run() {
                            synchronized (mLock) {
                                for (int i = mRoots.size() - 1; i >= 0; --i) {
                                    mRoots.get(i).loadSystemProperties();
                                }
                            }
                        }
                    };
                    SystemProperties.addChangeCallback(mSystemPropertyUpdater);
                }
    
                int index = findViewLocked(view, false);
                if (index >= 0) {
                    if (mDyingViews.contains(view)) {
                        // Don't wait for MSG_DIE to make it's way through root's queue.
                        mRoots.get(index).doDie();
                    } else {
                        throw new IllegalStateException("View " + view
                                + " has already been added to the window manager.");
                    }
                    // The previous removeView() had not completed executing. Now it has.
                }
    
                // If this is a panel window, then find the window it is being
                // attached to for future reference.
                if (wparams.type >= WindowManager.LayoutParams.FIRST_SUB_WINDOW &&
                        wparams.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
                    final int count = mViews.size();
                    for (int i = 0; i < count; i++) {
                        if (mRoots.get(i).mWindow.asBinder() == wparams.token) {
                            panelParentView = mViews.get(i);
                        }
                    }
                }
    
                //通过上下文构建ViewRootImpl
                root = new ViewRootImpl(view.getContext(), display);
    
                view.setLayoutParams(wparams);
    
                //mViews存储着所有Window对应的View对象
                mViews.add(view);
                //mRoots存储着所有Window对应的ViewRootImpl对象
                mRoots.add(root);
                //mParams存储着所有Window对应的WindowManager.LayoutParams对象
                mParams.add(wparams);
            }
    
            // do this last because it fires off messages to start doing things
            try {
                //调用ViewRootImpl.setView()方法完成Window的添加并更新界面
                root.setView(view, wparams, panelParentView);
            } catch (RuntimeException e) {
                // BadTokenException or InvalidDisplayException, clean up.
                synchronized (mLock) {
                    final int index = findViewLocked(view, false);
                    if (index >= 0) {
                        removeViewLocked(index, true);
                    }
                }
                throw e;
            }
        }
    
}
```

在这个方法里有三个重要的成员变量：

- mViews存储着所有Window对应的View对象
- mRoots存储着所有Window对应的ViewRootImpl对象
- mParams存储着所有Window对应的WindowManager.LayoutParams对象

这里面提到了一个我们不是很熟悉的类ViewRootImpl，它其实就是一个封装类，封装了View与WindowManager的交互方式，它是View与WindowManagerService通信的桥梁。
最后也是调用ViewRootImpl.setView()方法完成Window的添加并更新界面。

我们来看看这个方法的实现。

```java
public final class ViewRootImpl implements ViewParent,
        View.AttachInfo.Callbacks, ThreadedRenderer.HardwareDrawCallbacks {
    
     public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
            synchronized (this) {
                if (mView == null) {
                    mView = view;
    
                    //参数校验与预处理
                    ...
    
                    // Schedule the first layout -before- adding to the window
                    // manager, to make sure we do the relayout before receiving
                    // any other events from the system.
                    
                    //1. 调用requestLayout()完成界面异步绘制的请求
                    requestLayout();
                    if ((mWindowAttributes.inputFeatures
                            & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
                        mInputChannel = new InputChannel();
                    }
                    mForceDecorViewVisibility = (mWindowAttributes.privateFlags
                            & PRIVATE_FLAG_FORCE_DECOR_VIEW_VISIBILITY) != 0;
                    try {
                        mOrigWindowType = mWindowAttributes.type;
                        mAttachInfo.mRecomputeGlobalAttributes = true;
                        collectViewAttributes();
                        //2. 创建WindowSession并通过WindowSession请求WindowManagerService来完成Window添加的过程
                        //这是一个IPC的过程。
                        res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                                getHostVisibility(), mDisplay.getDisplayId(),
                                mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                                mAttachInfo.mOutsets, mInputChannel);
                    } catch (RemoteException e) {
                        mAdded = false;
                        mView = null;
                        mAttachInfo.mRootView = null;
                        mInputChannel = null;
                        mFallbackEventHandler.setView(null);
                        unscheduleTraversals();
                        setAccessibilityFocus(null, null);
                        throw new RuntimeException("Adding window failed", e);
                    } finally {
                        if (restore) {
                            attrs.restore();
                        }
                    }
                    ...
            }
        }    
}
```
这个方法主要做了两件事：

1. 调用requestLayout()完成界面异步绘制的请求, requestLayout()会去调用scheduleTraversals()来完成View的绘制，scheduleTraversals()方法将一个TraversalRunnable提交到工作队列中执行View的绘制。而
TraversalRunnable最终调用了performTraversals()方法来完成实际的绘制操作。提到performTraversals()方法我们已经很熟悉了，在文章[02Android显示框架：Android应用视图的载体View](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/02Android显示框架：Android应用视图载体View.md)中
我们已经详细的分析过它的实现。
2. 创建WindowSession并通过WindowSession请求WindowManagerService来完成Window添加的过程这是一个IPC的过程，WindowManagerService作为实际的窗口管理者，窗口的创建、删除和更新都是由它来完成的，它同时还负责了窗口的层叠排序和大小计算
等工作。

注：在文章[02Android显示框架：Android应用视图的载体View](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/02Android显示框架：Android应用视图载体View.md)中
我们已经详细的分析过performTraversals()方法的实现，这里我们再简单提一下：

1. 获取Surface对象，用于图形绘制。
2. 调用performMeasure()方法测量视图树各个View的大小。
2. 调用performLayout()方法计算视图树各个View的位置，进行布局。
2. 调用performMeasure()方法对视图树的各个View进行绘制。

既然提到WindowManager与WindowManagerService的跨进程通信，我们再讲一下它们的通信流程。Android的各种服务都是基于C/S结构来设计的，系统层提供服务，应用层使用服务。WindowManager也是一样，它与
WindowManagerService的通信是通过WindowSession来完成的。

1. 首先调用ServiceManager.getService("window")获取WindowManagerService，该方法返回的是IBinder对象，然后调用IWindowManager.Stub.asInterface()方法将WindowManagerService转换为一个IWindowManager对象。
2. 然后调用openSession()方法与WindowManagerService建立一个通信会话，方便后续的跨进程通信。这个通信会话就是后面我们用到的WindowSession。

基本上所有的Android系统服务都是基于这种方式实现的，它是一种基于AIDL实现的IPC的过程。关于AIDL读者可自行查阅资料。

```java
public final class WindowManagerGlobal {
    
    public static IWindowSession getWindowSession() {
        synchronized (WindowManagerGlobal.class) {
            if (sWindowSession == null) {
                try {
                    InputMethodManager imm = InputMethodManager.getInstance();
                    //获取WindowManagerService对象，并将它转换为IWindowManager类型
                    IWindowManager windowManager = getWindowManagerService();
                    //调用openSession()方法与WindowManagerService建立一个通信会话，方便后续的
                    //跨进程通信。
                    sWindowSession = windowManager.openSession(
                            new IWindowSessionCallback.Stub() {
                                @Override
                                public void onAnimatorScaleChanged(float scale) {
                                    ValueAnimator.setDurationScale(scale);
                                }
                            },
                            imm.getClient(), imm.getInputContext());
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return sWindowSession;
        }
    }
    
    public static IWindowManager getWindowManagerService() {
        synchronized (WindowManagerGlobal.class) {
            if (sWindowManagerService == null) {
                //调用ServiceManager.getService("window")获取WindowManagerService，该方法返回的是IBinder对象
                //，然后调用IWindowManager.Stub.asInterface()方法将WindowManagerService转换为一个IWindowManager对象
                sWindowManagerService = IWindowManager.Stub.asInterface(
                        ServiceManager.getService("window"));
                try {
                    sWindowManagerService = getWindowManagerService();
                    ValueAnimator.setDurationScale(sWindowManagerService.getCurrentAnimatorScale());
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return sWindowManagerService;
        }
    }
 }
```

## 二 Window的删除流程

Window的删除流程也是在WindowManagerGlobal里完成的。

```java
public final class WindowManagerGlobal {
    
   public void removeView(View view, boolean immediate) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }

        synchronized (mLock) {
            //1. 查找待删除View的索引
            int index = findViewLocked(view, true);
            View curView = mRoots.get(index).getView();
            //2. 调用removeViewLocked()完成View的删除, removeViewLocked()方法
            //继续调用ViewRootImpl.die()方法来完成View的删除。
            removeViewLocked(index, immediate);
            if (curView == view) {
                return;
            }

            throw new IllegalStateException("Calling with view " + view
                    + " but the ViewAncestor is attached to " + curView);
        }
    }
    
    private void removeViewLocked(int index, boolean immediate) {
        ViewRootImpl root = mRoots.get(index);
        View view = root.getView();

        if (view != null) {
            InputMethodManager imm = InputMethodManager.getInstance();
            if (imm != null) {
                imm.windowDismissed(mViews.get(index).getWindowToken());
            }
        }
        boolean deferred = root.die(immediate);
        if (view != null) {
            view.assignParent(null);
            if (deferred) {
                mDyingViews.add(view);
            }
        }
    }

}
```

我们再来看看ViewRootImpl.die()方法的实现。

```java
public final class ViewRootImpl implements ViewParent,

        View.AttachInfo.Callbacks, ThreadedRenderer.HardwareDrawCallbacks {
      boolean die(boolean immediate) {
            // Make sure we do execute immediately if we are in the middle of a traversal or the damage
            // done by dispatchDetachedFromWindow will cause havoc on return.
            
            //根据immediate参数来判断是执行异步删除还是同步删除
            if (immediate && !mIsInTraversal) {
                doDie();
                return false;
            }
    
            if (!mIsDrawing) {
                destroyHardwareRenderer();
            } else {
                Log.e(mTag, "Attempting to destroy the window while drawing!\n" +
                        "  window=" + this + ", title=" + mWindowAttributes.getTitle());
            }
            //如果是异步删除，则发送一个删除View的消息MSG_DIE就会直接返回
            mHandler.sendEmptyMessage(MSG_DIE);
            return true;
        }
    
        void doDie() {
            checkThread();
            if (LOCAL_LOGV) Log.v(mTag, "DIE in " + this + " of " + mSurface);
            synchronized (this) {
                if (mRemoved) {
                    return;
                }
                mRemoved = true;
                if (mAdded) {
                    //调用dispatchDetachedFromWindow()完成View的删除
                    dispatchDetachedFromWindow();
                }
    
                if (mAdded && !mFirst) {
                    destroyHardwareRenderer();
    
                    if (mView != null) {
                        int viewVisibility = mView.getVisibility();
                        boolean viewVisibilityChanged = mViewVisibility != viewVisibility;
                        if (mWindowAttributesChanged || viewVisibilityChanged) {
                            // If layout params have been changed, first give them
                            // to the window manager to make sure it has the correct
                            // animation info.
                            try {
                                if ((relayoutWindow(mWindowAttributes, viewVisibility, false)
                                        & WindowManagerGlobal.RELAYOUT_RES_FIRST_TIME) != 0) {
                                    mWindowSession.finishDrawing(mWindow);
                                }
                            } catch (RemoteException e) {
                            }
                        }
    
                        mSurface.release();
                    }
                }
    
                mAdded = false;
            }
            //刷新数据，将当前移除View的相关信息从我们上面说过了三个列表：mRoots、mParms和mViews中移除。
            WindowManagerGlobal.getInstance().doRemoveView(this);
        }
        
        void dispatchDetachedFromWindow() {
                //1. 回调View的dispatchDetachedFromWindow方法，通知该View已从Window中移除
                if (mView != null && mView.mAttachInfo != null) {
                    mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(false);
                    mView.dispatchDetachedFromWindow();
                }
        
                mAccessibilityInteractionConnectionManager.ensureNoConnection();
                mAccessibilityManager.removeAccessibilityStateChangeListener(
                        mAccessibilityInteractionConnectionManager);
                mAccessibilityManager.removeHighTextContrastStateChangeListener(
                        mHighContrastTextManager);
                removeSendWindowContentChangedCallback();
        
                destroyHardwareRenderer();
        
                setAccessibilityFocus(null, null);
        
                mView.assignParent(null);
                mView = null;
                mAttachInfo.mRootView = null;
        
                mSurface.release();
        
                if (mInputQueueCallback != null && mInputQueue != null) {
                    mInputQueueCallback.onInputQueueDestroyed(mInputQueue);
                    mInputQueue.dispose();
                    mInputQueueCallback = null;
                    mInputQueue = null;
                }
                if (mInputEventReceiver != null) {
                    mInputEventReceiver.dispose();
                    mInputEventReceiver = null;
                }
                
                //调用WindowSession.remove()方法，这同样是一个IPC过程，最终调用的是
                //WindowManagerService.removeWindow()方法来移除Window。
                try {
                    mWindowSession.remove(mWindow);
                } catch (RemoteException e) {
                }
        
                // Dispose the input channel after removing the window so the Window Manager
                // doesn't interpret the input channel being closed as an abnormal termination.
                if (mInputChannel != null) {
                    mInputChannel.dispose();
                    mInputChannel = null;
                }
        
                mDisplayManager.unregisterDisplayListener(mDisplayListener);
        
                unscheduleTraversals();
            }

}
```

我们再来总结一下Window的删除流程：

1. 查找待删除View的索引
2. 调用removeViewLocked()完成View的删除, removeViewLocked()方法继续调用ViewRootImpl.die()方法来完成View的删除。
3. ViewRootImpl.die()方法根据immediate参数来判断是执行异步删除还是同步删除，如果是异步删除则则发送一个删除View的消息MSG_DIE就会直接返回。
如果是同步删除，则调用doDie()方法。
4. doDie()方法调用dispatchDetachedFromWindow()完成View的删除，在该方法里首先回调View的dispatchDetachedFromWindow方法，通知该View已从Window中移除，
然后调用WindowSession.remove()方法，这同样是一个IPC过程，最终调用的是WindowManagerService.removeWindow()方法来移除Window。

## 三 Window的更新流程

```java
public final class WindowManagerGlobal {
    
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        if (!(params instanceof WindowManager.LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }

        final WindowManager.LayoutParams wparams = (WindowManager.LayoutParams)params;

        //更新View的LayoutParams参数
        view.setLayoutParams(wparams);

        synchronized (mLock) {
            //查找Viewd的索引，更新mParams里的参数
            int index = findViewLocked(view, true);
            ViewRootImpl root = mRoots.get(index);
            mParams.remove(index);
            mParams.add(index, wparams);
            //调用ViewRootImpl.setLayoutParams()完成重新布局的工作。
            root.setLayoutParams(wparams, false);
        }
    }   
}
```

我们再来看看ViewRootImpl.setLayoutParams()方法的实现。

```java
public final class ViewRootImpl implements ViewParent,

   void setLayoutParams(WindowManager.LayoutParams attrs, boolean newView) {
           synchronized (this) {
               //参数预处理
               ...
   
                //如果是新View，调用requestLayout()进行重新绘制
               if (newView) {
                   mSoftInputMode = attrs.softInputMode;
                   requestLayout();
               }
   
               // Don't lose the mode we last auto-computed.
               if ((attrs.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                       == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {
                   mWindowAttributes.softInputMode = (mWindowAttributes.softInputMode
                           & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                           | (oldSoftInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);
               }
   
               mWindowAttributesChanged = true;
               //如果不是新View，调用requestLayout()进行重新绘制
               scheduleTraversals();
           }
       } 
}
```

Window的更新流程也和其他流程相似：

1. 更新View的LayoutParams参数，查找Viewd的索引，更新mParams里的参数。
2. 调用ViewRootImpl.setLayoutParams()方法完成重新布局的工作，在setLayoutParams()方法里最终会调用scheduleTraversals()
进行解码重绘制，scheduleTraversals()后续的流程就是View的measure、layout和draw流程了，这个我们在上面已经说过了。

好了本篇文章的内容到这里就讲完了，在这篇文章中我们侧重分析Android窗口服务Client这一侧的实现，事实上更多的内容是在Server这一侧，也就是WindowManagerService。只不过在日常的开发中我们较少
接触到WindowManagerService，它属于系统的内部服务，就暂时不做进一步的展开。总体上来说，本系列文章的目的还是在于更好的服务应用层的开发者，等到关于Android应用层Framework实现原理分析完成以后，
我们再进一步深入，去分析系统层Framework的实现。