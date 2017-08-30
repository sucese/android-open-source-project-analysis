# Android系统应用框架篇：Window大小与位置的计算流程

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

本篇文章主要分析窗口大小与位置（X轴、Y轴与Z轴）的计算流程，在介绍窗口的计算流程之前，我们先来了解一下窗口的组成。

窗口有内容窗口（Content Region），内容边距（Content Inset）与可见边距（Visible Inset）组成，如下图：

content-left、content-right、content-top、content-bottom分别用来描述内容区域与窗口区域的左右上下边界距离。
visible-left、visible-right、visible-top、visible-bottom分别用来描述可见区域与窗口区域的左右上下边界距离。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/view/09/window_inset.png"/>



### 2 ViewRoot.relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, boolean insetsPending)

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {

    private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility,
            boolean insetsPending) throws RemoteException {

        float appScale = mAttachInfo.mApplicationScale;
        boolean restore = false;
        if (params != null && mTranslator != null) {
            restore = true;
            params.backup();
            mTranslator.translateWindowLayout(params);
        }
        if (params != null) {
            if (DBG) Log.d(TAG, "WindowLayout in layoutWindow:" + params);
        }
        mPendingConfiguration.seq = 0;
        //调用Session.relayout()方法
        //Log.d(TAG, ">>>>>> CALLING relayout");
        int relayoutResult = sWindowSession.relayout(
                mWindow, params,
                (int) (mView.mMeasuredWidth * appScale + 0.5f),
                (int) (mView.mMeasuredHeight * appScale + 0.5f),
                viewVisibility, insetsPending, mWinFrame,
                mPendingContentInsets, mPendingVisibleInsets,
                mPendingConfiguration, mSurface);
        //Log.d(TAG, "<<<<<< BACK FROM relayout");
        if (restore) {
            params.restore();
        }
        
        //mTranslator如果指向一个Translator对象，则说明Activity窗口是运行在兼容模式下的，则调用
        //下列函数进行转换
        if (mTranslator != null) {
            mTranslator.translateRectInScreenToAppWinFrame(mWinFrame);
            mTranslator.translateRectInScreenToAppWindow(mPendingContentInsets);
            mTranslator.translateRectInScreenToAppWindow(mPendingVisibleInsets);
        }
        return relayoutResult;
    }        
}
```

sWindowSession的类型是Session，它是一个Binder对象，它引用了运行在WindowManagerService服务这一侧的一个
Session对象，通过调用Session.relayout()方法来请求ActivityManagerService计算Activity窗口的大小。

### 3 Session。elayout(IWindow window, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewFlags, boolean insetsPending, Rect outFrame, Rect outContentInsets, Rect outVisibleInsets, Configuration outConfig, Surface outSurface)

```java
public class WindowManagerService extends IWindowManager.Stub  
        implements Watchdog.Monitor { 
    
    private final class Session extends IWindowSession.Stub
        implements IBinder.DeathRecipient {
    
        public int relayout(IWindow window, WindowManager.LayoutParams attrs,
                int requestedWidth, int requestedHeight, int viewFlags,
                boolean insetsPending, Rect outFrame, Rect outContentInsets,
                Rect outVisibleInsets, Configuration outConfig, Surface outSurface) {
            //Log.d(TAG, ">>>>>> ENTERED relayout from " + Binder.getCallingPid());
            int res = relayoutWindow(this, window, attrs,
                    requestedWidth, requestedHeight, viewFlags, insetsPending,
                    outFrame, outContentInsets, outVisibleInsets, outConfig, outSurface);
            //Log.d(TAG, "<<<<<< EXITING relayout to " + Binder.getCallingPid());
            return res;
        }

    }    
}
```

这个函数参数有点多，我们来分析下各个参数的含义：

```
IWindow window：Activity窗口的唯一标识，用于与其他窗口区别开来。
WindowManager.LayoutParams attrs：布局规范LayoutParams
int requestedWidth, int requestedHeight：Activity窗口经过测量后得到的宽度与高度，另外，传递给
ActivityManagerService的宽高以及考虑了Activity窗口所设置的缩放因子了。
int viewFlags：Activity窗口的可见状态
boolean insetsPending：Activity窗口是否指定额外的内容边距与可见边距。
Rect outFrame：输出参数，用来保存WindowManagerService计算得出的窗口大小。
Rect outContentInsets：输出参数，用来保存WindowManagerService计算得出的内容边距的大小。
Rect outVisibleInsets：输出参数，用来保存WindowManagerService计算得出的可见边距的大小。
Configuration outConfig：输出参数，用来保存WindowManagerService返回来的配置信息。
Surface outSurface：输出参数，用来保存WindowManagerService返回给Activity的绘图画布。
```

接下来调用Session的relayoutWindow()方法，这个方法完成了Activity窗口大小的计算工作。

### 4 Session.relayoutWindow(Session session, IWindow client, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, boolean insetsPending, Rect outFrame, Rect outContentInsets, Rect outVisibleInsets, Configuration outConfig, Surface outSurface)

```java
public class WindowManagerService extends IWindowManager.Stub  
        implements Watchdog.Monitor { 
    
    private final class Session extends IWindowSession.Stub
        implements IBinder.DeathRecipient {
            
        public int relayoutWindow(Session session, IWindow client,
                             WindowManager.LayoutParams attrs, int requestedWidth,
                             int requestedHeight, int viewVisibility, boolean insetsPending,
                             Rect outFrame, Rect outContentInsets, Rect outVisibleInsets,
                             Configuration outConfig, Surface outSurface) {
                         boolean displayed = false;
                         boolean inTouchMode;
                         boolean configChanged;
                         long origId = Binder.clearCallingIdentity();
                 
                         synchronized(mWindowMap) {
                             //client对应的是应用进程的Activity窗口，每个IWindow对象都在WindowManagerService
                             //端对应着一个WindowState对象，该对象描述了窗口的信息。
                             WindowState win = windowForClientLocked(session, client, false);
                             if (win == null) {
                                 return 0;
                             }
                             
                             //requestedWidth与requestedHeight描述的是应用进程请求设置Activity窗口的宽高
                             win.mRequestedWidth = requestedWidth;
                             win.mRequestedHeight = requestedHeight;
                 
                             if (attrs != null) {
                                 mPolicy.adjustWindowParamsLw(attrs);
                             }
                 
                             int attrChanges = 0;
                             int flagChanges = 0;
                             if (attrs != null) {
                                 //win.mAttrs指向的是WindowManager.LayoutParams，它描述了Activity窗口的布局参数
                                 flagChanges = win.mAttrs.flags ^= attrs.flags;
                                 attrChanges = win.mAttrs.copyFrom(attrs);
                             }
                 
                             if (DEBUG_LAYOUT) Slog.v(TAG, "Relayout " + win + ": " + win.mAttrs);
                 
                             //透明度
                             if ((attrChanges & WindowManager.LayoutParams.ALPHA_CHANGED) != 0) {
                                 win.mAlpha = attrs.alpha;
                             }
                 
                             final boolean scaledWindow =
                                 ((win.mAttrs.flags & WindowManager.LayoutParams.FLAG_SCALED) != 0);
                 
                             //宽高缩放因子
                             if (scaledWindow) {
                                 // requested{Width|Height} Surface's physical size
                                 // attrs.{width|height} Size on screen
                                 win.mHScale = (attrs.width  != requestedWidth)  ?
                                         (attrs.width  / (float)requestedWidth) : 1.0f;
                                 win.mVScale = (attrs.height != requestedHeight) ?
                                         (attrs.height / (float)requestedHeight) : 1.0f;
                             } else {
                                 win.mHScale = win.mVScale = 1;
                             }
                 
                             //窗口焦点变化
                             boolean imMayMove = (flagChanges&(
                                     WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
                                     WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)) != 0;
                 
                             boolean focusMayChange = win.mViewVisibility != viewVisibility
                                     || ((flagChanges&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0)
                                     || (!win.mRelayoutCalled);
                 
                             boolean wallpaperMayMove = win.mViewVisibility != viewVisibility
                                     && (win.mAttrs.flags & FLAG_SHOW_WALLPAPER) != 0;
                 
                             win.mRelayoutCalled = true;
                             //窗口可见性
                             final int oldVisibility = win.mViewVisibility;
                             win.mViewVisibility = viewVisibility;
                             if (viewVisibility == View.VISIBLE &&
                                     (win.mAppToken == null || !win.mAppToken.clientHidden)) {
                                 displayed = !win.isVisibleLw();
                                 if (win.mExiting) {
                                     win.mExiting = false;
                                     win.mAnimation = null;
                                 }
                                 if (win.mDestroying) {
                                     win.mDestroying = false;
                                     mDestroySurface.remove(win);
                                 }
                                 if (oldVisibility == View.GONE) {
                                     win.mEnterAnimationPending = true;
                                 }
                                 if (displayed) {
                                     if (win.mSurface != null && !win.mDrawPending
                                             && !win.mCommitDrawPending && !mDisplayFrozen
                                             && mPolicy.isScreenOn()) {
                                         //窗口进入动画
                                         applyEnterAnimationLocked(win);
                                     }
                                     if ((win.mAttrs.flags
                                             & WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) != 0) {
                                         if (DEBUG_VISIBILITY) Slog.v(TAG,
                                                 "Relayout window turning screen on: " + win);
                                         win.mTurnOnScreen = true;
                                     }
                                     int diff = 0;
                                     if (win.mConfiguration != mCurConfiguration
                                             && (win.mConfiguration == null
                                                     || (diff=mCurConfiguration.diff(win.mConfiguration)) != 0)) {
                                         win.mConfiguration = mCurConfiguration;
                                         if (DEBUG_CONFIGURATION) {
                                             Slog.i(TAG, "Window " + win + " visible with new config: "
                                                     + win.mConfiguration + " / 0x"
                                                     + Integer.toHexString(diff));
                                         }
                                         outConfig.setTo(mCurConfiguration);
                                     }
                                 }
                                 if ((attrChanges&WindowManager.LayoutParams.FORMAT_CHANGED) != 0) {
                                     // To change the format, we need to re-build the surface.
                                     win.destroySurfaceLocked();
                                     displayed = true;
                                 }
                                 try {
                                     //创建Surface画布
                                     Surface surface = win.createSurfaceLocked();
                                     if (surface != null) {
                                         outSurface.copyFrom(surface);
                                         win.mReportDestroySurface = false;
                                         win.mSurfacePendingDestroy = false;
                                         if (SHOW_TRANSACTIONS) Slog.i(TAG,
                                                 "  OUT SURFACE " + outSurface + ": copied");
                                     } else {
                                         // For some reason there isn't a surface.  Clear the
                                         // caller's object so they see the same state.
                                         outSurface.release();
                                     }
                                 } catch (Exception e) {
                                     mInputMonitor.updateInputWindowsLw();
                                     
                                     Slog.w(TAG, "Exception thrown when creating surface for client "
                                              + client + " (" + win.mAttrs.getTitle() + ")",
                                              e);
                                     Binder.restoreCallingIdentity(origId);
                                     return 0;
                                 }
                                 if (displayed) {
                                     focusMayChange = true;
                                 }
                                 if (win.mAttrs.type == TYPE_INPUT_METHOD
                                         && mInputMethodWindow == null) {
                                     mInputMethodWindow = win;
                                     imMayMove = true;
                                 }
                                 if (win.mAttrs.type == TYPE_BASE_APPLICATION
                                         && win.mAppToken != null
                                         && win.mAppToken.startingWindow != null) {
                                     // Special handling of starting window over the base
                                     // window of the app: propagate lock screen flags to it,
                                     // to provide the correct semantics while starting.
                                     final int mask =
                                         WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                         | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                         | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
                                     WindowManager.LayoutParams sa = win.mAppToken.startingWindow.mAttrs;
                                     sa.flags = (sa.flags&~mask) | (win.mAttrs.flags&mask);
                                 }
                             } else {
                                 win.mEnterAnimationPending = false;
                                 if (win.mSurface != null) {
                                     if (DEBUG_VISIBILITY) Slog.i(TAG, "Relayout invis " + win
                                             + ": mExiting=" + win.mExiting
                                             + " mSurfacePendingDestroy=" + win.mSurfacePendingDestroy);
                                     // If we are not currently running the exit animation, we
                                     // need to see about starting one.
                                     if (!win.mExiting || win.mSurfacePendingDestroy) {
                                         // Try starting an animation; if there isn't one, we
                                         // can destroy the surface right away.
                                         int transit = WindowManagerPolicy.TRANSIT_EXIT;
                                         if (win.getAttrs().type == TYPE_APPLICATION_STARTING) {
                                             transit = WindowManagerPolicy.TRANSIT_PREVIEW_DONE;
                                         }
                                         if (!win.mSurfacePendingDestroy && win.isWinVisibleLw() &&
                                               applyAnimationLocked(win, transit, false)) {
                                             focusMayChange = true;
                                             win.mExiting = true;
                                         } else if (win.isAnimating()) {
                                             // Currently in a hide animation... turn this into
                                             // an exit.
                                             win.mExiting = true;
                                         } else if (win == mWallpaperTarget) {
                                             // If the wallpaper is currently behind this
                                             // window, we need to change both of them inside
                                             // of a transaction to avoid artifacts.
                                             win.mExiting = true;
                                             win.mAnimating = true;
                                         } else {
                                             if (mInputMethodWindow == win) {
                                                 mInputMethodWindow = null;
                                             }
                                             win.destroySurfaceLocked();
                                         }
                                     }
                                 }
                 
                                 if (win.mSurface == null || (win.getAttrs().flags
                                         & WindowManager.LayoutParams.FLAG_KEEP_SURFACE_WHILE_ANIMATING) == 0
                                         || win.mSurfacePendingDestroy) {
                                     // We are being called from a local process, which
                                     // means outSurface holds its current surface.  Ensure the
                                     // surface object is cleared, but we don't want it actually
                                     // destroyed at this point.
                                     win.mSurfacePendingDestroy = false;
                                     outSurface.release();
                                     if (DEBUG_VISIBILITY) Slog.i(TAG, "Releasing surface in: " + win);
                                 } else if (win.mSurface != null) {
                                     if (DEBUG_VISIBILITY) Slog.i(TAG,
                                             "Keeping surface, will report destroy: " + win);
                                     win.mReportDestroySurface = true;
                                     outSurface.copyFrom(win.mSurface);
                                 }
                             }
                 
                             if (focusMayChange) {
                                 //System.out.println("Focus may change: " + win.mAttrs.getTitle());
                                 if (updateFocusedWindowLocked(UPDATE_FOCUS_WILL_PLACE_SURFACES)) {
                                     imMayMove = false;
                                 }
                                 //System.out.println("Relayout " + win + ": focus=" + mCurrentFocus);
                             }
                 
                             // updateFocusedWindowLocked() already assigned layers so we only need to
                             // reassign them at this point if the IM window state gets shuffled
                             boolean assignLayers = false;
                 
                             if (imMayMove) {
                                 if (moveInputMethodWindowsIfNeededLocked(false) || displayed) {
                                     // Little hack here -- we -should- be able to rely on the
                                     // function to return true if the IME has moved and needs
                                     // its layer recomputed.  However, if the IME was hidden
                                     // and isn't actually moved in the list, its layer may be
                                     // out of data so we make sure to recompute it.
                                     assignLayers = true;
                                 }
                             }
                             if (wallpaperMayMove) {
                                 if ((adjustWallpaperWindowsLocked()&ADJUST_WALLPAPER_LAYERS_CHANGED) != 0) {
                                     assignLayers = true;
                                 }
                             }
                 
                             mLayoutNeeded = true;
                             win.mGivenInsetsPending = insetsPending;
                             if (assignLayers) {
                                 assignLayersLocked();
                             }
                             configChanged = updateOrientationFromAppTokensLocked();
                             //计算参数client描述的窗口的大小
                             performLayoutAndPlaceSurfacesLocked();
                             if (displayed && win.mIsWallpaper) {
                                 updateWallpaperOffsetLocked(win, mDisplay.getWidth(),
                                         mDisplay.getHeight(), false);
                             }
                             if (win.mAppToken != null) {
                                 win.mAppToken.updateReportedVisibilityLocked();
                             }
                             outFrame.set(win.mFrame);
                             outContentInsets.set(win.mContentInsets);
                             outVisibleInsets.set(win.mVisibleInsets);
                             if (localLOGV) Slog.v(
                                 TAG, "Relayout given client " + client.asBinder()
                                 + ", requestedWidth=" + requestedWidth
                                 + ", requestedHeight=" + requestedHeight
                                 + ", viewVisibility=" + viewVisibility
                                 + "\nRelayout returning frame=" + outFrame
                                 + ", surface=" + outSurface);
                 
                             if (localLOGV || DEBUG_FOCUS) Slog.v(
                                 TAG, "Relayout of " + win + ": focusMayChange=" + focusMayChange);
                 
                             inTouchMode = mInTouchMode;
                             
                             mInputMonitor.updateInputWindowsLw();
                         }
                 
                         if (configChanged) {
                             sendNewConfiguration();
                         }
                 
                         Binder.restoreCallingIdentity(origId);
                 
                         return (inTouchMode ? WindowManagerImpl.RELAYOUT_IN_TOUCH_MODE : 0)
                                | (displayed ? WindowManagerImpl.RELAYOUT_FIRST_TIME : 0);  
            
        }                                
     
    } 
}
```
该函数继续调用performLayoutAndPlaceSurfacesLocked()方法来计算client所描述的窗口的大小，计算完成后窗口的宽高、内容边距
与可见边距分别保存在WindowState对象的mFrame、mContentInsets与mVisibleInsets中。我们接着来看performLayoutAndPlaceSurfacesLocked()
方法的实现。

### 5 WindowManagerService.performLayoutAndPlaceSurfacesLocked() 

```java
public class WindowManagerService extends IWindowManager.Stub  
        implements Watchdog.Monitor { 
    
    private boolean mInLayout = false;
    private final void performLayoutAndPlaceSurfacesLocked() {
        if (mInLayout) {
            if (DEBUG) {
                throw new RuntimeException("Recursive call!");
            }
            Slog.w(TAG, "performLayoutAndPlaceSurfacesLocked called while in layout");
            return;
        }

        if (mWaitingForConfig) {
            // Our configuration has changed (most likely rotation), but we
            // don't yet have the complete configuration to report to
            // applications.  Don't do any window layout until we have it.
            return;
        }
        
        if (mDisplay == null) {
            // Not yet initialized, nothing to do.
            return;
        }

        boolean recoveringMemory = false;
        if (mForceRemoves != null) {
            recoveringMemory = true;
            //检查是否存在需要强制删除的窗口，在系统内存不足的情况下，一些窗口会被回收，
            //这些窗口会保存在列表mForceRemoves中。
            // Wait a little it for things to settle down, and off we go.
            for (int i=0; i<mForceRemoves.size(); i++) {
                WindowState ws = mForceRemoves.get(i);
                Slog.i(TAG, "Force removing: " + ws);
                //调用removeWindowInnerLocked()方法移除窗口
                removeWindowInnerLocked(ws.mSession, ws);
            }
            mForceRemoves = null;
            Slog.w(TAG, "Due to memory failure, waiting a bit for next layout");
            Object tmp = new Object();
            synchronized (tmp) {
                try {
                    tmp.wait(250);
                } catch (InterruptedException e) {
                }
            }
        }

        mInLayout = true;
        try {
            performLayoutAndPlaceSurfacesLockedInner(recoveringMemory);

            //检查是否有窗口需要被移除，如果需要则调用removeWindowInnerLocked()方法移除窗口
            //移除窗口后并没有回收内存，只有在内存不足时才会回收这些内存。
            int i = mPendingRemove.size()-1;
            if (i >= 0) {
                while (i >= 0) {
                    WindowState w = mPendingRemove.get(i);
                    removeWindowInnerLocked(w.mSession, w);
                    i--;
                }
                mPendingRemove.clear();

                //设置标志位防止performLayoutAndPlaceSurfacesLockedInner方法重复被调用
                mInLayout = false;
                assignLayersLocked();
                mLayoutNeeded = true;
                performLayoutAndPlaceSurfacesLocked();

            } else {
                mInLayout = false;
                if (mLayoutNeeded) {
                    requestAnimationLocked(0);
                }
            }
            if (mWindowsChanged && !mWindowChangeListeners.isEmpty()) {
                mH.removeMessages(H.REPORT_WINDOWS_CHANGE);
                mH.sendMessage(mH.obtainMessage(H.REPORT_WINDOWS_CHANGE));
            }
        } catch (RuntimeException e) {
            mInLayout = false;
            Slog.e(TAG, "Unhandled exception while layout out windows", e);
        }
    }    
    
}
```


该方法会进一步调用方法performLayoutAndPlaceSurfacesLockedInner(recoveringMemory)来完成它的工作。

在调用之前：

检查是否存在需要强制删除的窗口，在系统内存不足的情况下，一些窗口会被回收，这些窗口会保存在列表mForceRemoves中。调用方法removeWindowInnerLocked
移除这些窗口。

在调用之后：

检查系统中是否有窗口需要移除，如果有则调用方法removeWindowInnerLocked移除这些窗口，移除窗口后并没有回收内存，只有在内存不足时才会回收这些内存。

另外，移除窗口的流程也是比较复杂的，先要将窗口从WindowManagerService的相关成员变量中移除，然后将壁纸窗口与输入法窗口调整到合适的Z
轴位置上，以便可以被下个窗口利用，最后调整剩下窗口在Z轴上的位置，以便可以正确的反应系统UI的状态。

接下来我们来分析performLayoutAndPlaceSurfacesLockedInner()函数的实现，这个函数不仅名字长，实现也非常长，足足有1200多行。

### 6 WindowManagerService.performLayoutAndPlaceSurfacesLockedInner( boolean recoveringMemory)








