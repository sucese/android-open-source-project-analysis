# Android系统应用框架篇：Window大小与位置的计算流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作。技术栈主要涉及Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative，数据结构与算法等方面。热爱编程与吉他，喜欢有趣的事物和人。

**关于文章**

>作者的文章首发在[Github](https://github.com/guoxiaoxing)上，也会发在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与[CSDN](http://blog.csdn.net/allenwells)平台上，文章内容主要包含Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative, 数据结构与算法等方面的内容。如果有什么问题，也欢迎发邮件与我交流。

本篇文章主要分析窗口大小与位置（X轴、Y轴与Z轴）的计算流程，在介绍窗口的计算流程之前，我们先来了解一下窗口的组成。

窗口有内容窗口（Content Region），内容边距（Content Inset）与可见边距（Visible Inset）组成，如下图：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/view/09/window_inset.png"/>

## 窗口大小的计算

**Window大小的计算序列图**

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/view/09/window_size_compute_sequence.png"/>

从前面的文章可知，Window大小的计算是从函数ViewRoot.performTraversals()开始，向WindowManagerService发送一个进程间通信请求，请求计算
Window窗口大小。

### 1 ViewRoot.performTraversals()

这个函数一共600多行代码，相对比较复杂，它主要用来计算窗口的大小。我们拆开一段段来看。

#### 1.1 获取Activity当前宽度desiredWindowWidth与当前高度desiredWindowHeight

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
     private void performTraversals() {
             
                //1 获取Activity当前宽度desiredWindowWidth与当前高度desiredWindowHeight
                // cache mView since it is used so much below...
                final View host = mView;
        
                if (DBG) {
                    System.out.println("======================================");
                    System.out.println("performTraversals");
                    host.debug();
                }
        
                if (host == null || !mAdded)
                    return;
        
                mTraversalScheduled = false;
                mWillDrawSoon = true;
                boolean windowResizesToFitContent = false;
                boolean fullRedrawNeeded = mFullRedrawNeeded;
                boolean newSurface = false;
                boolean surfaceChanged = false;
                WindowManager.LayoutParams lp = mWindowAttributes;
        
                int desiredWindowWidth;
                int desiredWindowHeight;
                int childWidthMeasureSpec;
                int childHeightMeasureSpec;
        
                final View.AttachInfo attachInfo = mAttachInfo;
        
                final int viewVisibility = getHostVisibility();
                boolean viewVisibilityChanged = mViewVisibility != viewVisibility
                        || mNewSurfaceNeeded;
        
                float appScale = mAttachInfo.mApplicationScale;
        
                WindowManager.LayoutParams params = null;
                if (mWindowAttributesChanged) {
                    mWindowAttributesChanged = false;
                    surfaceChanged = true;
                    params = lp;
                }
                //mWinFrame用来保存Activity窗口的高度与宽度信息，注意该变量的高度和宽度可能会被WindowManagerService
                //主动请求应用进程修改，修改后的值保存在mFrame中。
                Rect frame = mWinFrame;
                //mFirst为true则表示Activity窗口是第一次请求执行策略、布局与绘制操作，
                if (mFirst) {
                    fullRedrawNeeded = true;
                    mLayoutRequested = true;
        
                    DisplayMetrics packageMetrics =
                        mView.getContext().getResources().getDisplayMetrics();
                    desiredWindowWidth = packageMetrics.widthPixels;
                    desiredWindowHeight = packageMetrics.heightPixels;
        
                    // For the very first time, tell the view hierarchy that it
                    // is attached to the window.  Note that at this point the surface
                    // object is not initialized to its backing store, but soon it
                    // will be (assuming the window is visible).
                    attachInfo.mSurface = mSurface;
                    attachInfo.mUse32BitDrawingCache = PixelFormat.formatHasAlpha(lp.format) ||
                            lp.format == PixelFormat.RGBX_8888;
                    attachInfo.mHasWindowFocus = false;
                    attachInfo.mWindowVisibility = viewVisibility;
                    attachInfo.mRecomputeGlobalAttributes = false;
                    attachInfo.mKeepScreenOn = false;
                    viewVisibilityChanged = false;
                    mLastConfiguration.setTo(host.getResources().getConfiguration());
                    host.dispatchAttachedToWindow(attachInfo, 0);
                    //Log.i(TAG, "Screen on initialized: " + attachInfo.mKeepScreenOn);
        
                } else {
                    desiredWindowWidth = frame.width();
                    desiredWindowHeight = frame.height();
                    if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {
                        if (DEBUG_ORIENTATION) Log.v(TAG,
                                "View " + host + " resized to: " + frame);
                        fullRedrawNeeded = true;
                        mLayoutRequested = true;
                        //当前宽高不等于上次计算的宽高，则说明窗口大小发生了变化，将windowResizesToFitContent置为true
                        windowResizesToFitContent = true;
                    }
                }  
                ...
}
```

本小段函数主要用来获取Activity当前宽度desiredWindowWidth与当前高度desiredWindowHeight。我们来了解一下
这端代码涉及的几个变量的含义。

```
int mWidth：Activity窗口当前的宽度，它是由应用上一次请求WindowManagerService计算得到的，并且一直保持不变知道
下次WindowManagerService重新计算为止。
int mHeight：Activity窗口当前的高度，它是由应用上一次请求WindowManagerService计算得到的，并且一直保持不变知道
下次WindowManagerService重新计算为止。
React mWinFrame：该变量也保存了Activity窗口的宽度与高度，但是它保存的宽度与高度可能会被WindowManagerService
主动请求应用进程修改。
```
所以你可以看到这两组值可能不相等。

该函数片段主要做了以下事情：

```
1 mFirst为true则表示Activity窗口是第一次请求执行策略、布局与绘制操作，Activity窗口的宽高等于当前屏幕的宽高，否
则等于mWinFrame保存的宽高。
2 当前宽高不等于上次计算的宽高，则说明窗口大小发生了变化，将windowResizesToFitContent置为true。
```
#### 1.2 在Activity窗口主动请求WindowManagerService计算窗口大小之前，对它的顶层视图进行一次测量操作。

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
    
     private void performTraversals() {
            ...
            if (viewVisibilityChanged) {
                            attachInfo.mWindowVisibility = viewVisibility;
                            host.dispatchWindowVisibilityChanged(viewVisibility);
                            if (viewVisibility != View.VISIBLE || mNewSurfaceNeeded) {
                                if (mUseGL) {
                                    destroyGL();
                                }
                            }
                            if (viewVisibility == View.GONE) {
                                // After making a window gone, we will count it as being
                                // shown for the first time the next time it gets focus.
                                mHasHadWindowFocus = false;
                            }
                        }
                
                        boolean insetsChanged = false;
                
                        if (mLayoutRequested) {
                            // Execute enqueued actions on every layout in case a view that was detached
                            // enqueued an action after being detached
                            getRunQueue().executeActions(attachInfo.mHandler);
                
                            //Activity窗口第一次请求执行测量、布局与绘制操作
                            if (mFirst) {
                                //1 host指向的是顶级视图，调用View.fitSystemWindows设置它4个内边距
                                //mPaddingLeft，mPaddingTop，mPaddingRight，mPaddingBottom，设置的值
                                //为Activity窗口初始化时内容边距的大小，这样做的目的是为了在Activity窗口四周
                                //留下足够的区域来设置状态栏等系统窗口
                                host.fitSystemWindows(mAttachInfo.mContentInsets);
                                // make sure touch mode code executes by setting cached value
                                // to opposite of the added touch mode.
                                mAttachInfo.mInTouchMode = !mAddedTouchMode;
                                ensureTouchModeLocally(mAddedTouchMode);
                            } 
                            //Activity窗口不是第一次请求执行测量、布局与绘制操作
                            else {
                                
                                //2 检查WindowManagerService是否给Activity窗口设置了新的mContentInsets与mVisibleInsets
                                if (!mAttachInfo.mContentInsets.equals(mPendingContentInsets)) {
                                    mAttachInfo.mContentInsets.set(mPendingContentInsets);
                                    //mContentInsets发生变化，则重新设置顶层视图View的内容边距
                                    host.fitSystemWindows(mAttachInfo.mContentInsets);
                                    insetsChanged = true;
                                    if (DEBUG_LAYOUT) Log.v(TAG, "Content insets changing to: "
                                            + mAttachInfo.mContentInsets);
                                }
                                if (!mAttachInfo.mVisibleInsets.equals(mPendingVisibleInsets)) {
                                    mAttachInfo.mVisibleInsets.set(mPendingVisibleInsets);
                                    if (DEBUG_LAYOUT) Log.v(TAG, "Visible insets changing to: "
                                            + mAttachInfo.mVisibleInsets);
                                }
                                
                                //当Activity窗口的宽高参数都被设置为WRAP_CONTENT时，标明Activity窗口的大小
                                //要等于内容区域的大小，由于Activity窗口的大小是要覆盖整个屏幕的，所以它的宽
                                //高还是被设置成屏幕的宽高。也就是说我们将Activity窗口的宽高设置为WRAP_CONTENT时
                                //它实际上等于屏幕的宽高。
                                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                                        || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                                    windowResizesToFitContent = true;
                
                                    DisplayMetrics packageMetrics =
                                        mView.getContext().getResources().getDisplayMetrics();
                                    desiredWindowWidth = packageMetrics.widthPixels;
                                    desiredWindowHeight = packageMetrics.heightPixels;
                                }
                            }
                
                            //根据当前窗口的宽度与宽度测量规范获取它的顶层视图的测量规范
                            childWidthMeasureSpec = getRootMeasureSpec(desiredWindowWidth, lp.width);
                            //根据Activity窗口的高度与高度测量规范获取它的顶层视图的测量规范
                            childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height);
                
                            // Ask host how big it wants to be
                            if (DEBUG_ORIENTATION || DEBUG_LAYOUT) Log.v(TAG,
                                    "Measuring " + host + " in display " + desiredWindowWidth
                                    + "x" + desiredWindowHeight + "...");
                            //3 对顶层视图host进行测量
                            host.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                
                            if (DBG) {
                                System.out.println("======================================");
                                System.out.println("performTraversals -- after measure");
                                host.debug();
                            }
                        }
                        
                        if (attachInfo.mRecomputeGlobalAttributes) {
                            //Log.i(TAG, "Computing screen on!");
                            attachInfo.mRecomputeGlobalAttributes = false;
                            boolean oldVal = attachInfo.mKeepScreenOn;
                            attachInfo.mKeepScreenOn = false;
                            host.dispatchCollectViewAttributes(0);
                            if (attachInfo.mKeepScreenOn != oldVal) {
                                params = lp;
                                //Log.i(TAG, "Keep screen on changed: " + attachInfo.mKeepScreenOn);
                            }
                        }
                
                        if (mFirst || attachInfo.mViewVisibilityChanged) {
                            attachInfo.mViewVisibilityChanged = false;
                            int resizeMode = mSoftInputMode &
                                    WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
                            //根据Activity的resizeMode属性来调整窗口大小
                            // If we are in auto resize mode, then we need to determine
                            // what mode to use now.
                            if (resizeMode == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {
                                final int N = attachInfo.mScrollContainers.size();
                                for (int i=0; i<N; i++) {
                                    if (attachInfo.mScrollContainers.get(i).isShown()) {
                                        resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                                    }
                                }
                                if (resizeMode == 0) {
                                    resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                                }
                                if ((lp.softInputMode &
                                        WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) != resizeMode) {
                                    lp.softInputMode = (lp.softInputMode &
                                            ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) |
                                            resizeMode;
                                    params = lp;
                                }
                            }
                        }
                
                        if (params != null && (host.mPrivateFlags & View.REQUEST_TRANSPARENT_REGIONS) != 0) {
                            if (!PixelFormat.formatHasAlpha(params.format)) {
                                params.format = PixelFormat.TRANSLUCENT;
                            }
                        }
            ...
    }
}
```
我们先来了解一下本小段函数牵扯的几个变量的含义：

```
final Rect mPendingVisibleInsets = new Rect()：可见边距大小，由WindowManagerService主动请求Activity窗口设置。
final Rect mPendingContentInsets = new Rect()：内容边距大小，由WindowManagerService主动请求Activity窗口设置。
final View.AttachInfo mAttachInfo;：用来描述Activity窗口的属性，它内部也有mPendingVisibleInsets与
mPendingContentInsets属性，它用来描述Activity窗口上一次请求WindowManagerService计算得到的窗口属性值。
```

这一段代码主要做了Activity窗口的顶层视图的测量：

```
1 Activity窗口第一次请求执行测量、布局与绘制操作（mFirst = true）

调用View.fitSystemWindows设置它4个内边距mPaddingLeft，mPaddingTop，mPaddingRight，mPaddingBottom，
设置的值为Activity窗口初始化时内容边距的大小，这样做的目的是为了在Activity窗口四周留下足够的区域来设置状态栏
等系统窗口。

Activity窗口不是第一次请求执行测量、布局与绘制操作（mFirst = false）

检查WindowManagerService是否给Activity窗口设置了新的mContentInsets与mVisibleInsets，如果设置了则更新
mAttachInfo里面对应的值，并更新顶层视图host的内容边距。

2 根据当前窗口的宽度与宽度测量规范获取它的顶层视图的测量规范childWidthMeasureSpec，根据Activity窗口的高度与
高度测量规范获取它的顶层视图的测量规范childHeightMeasureSpec，利用childWidthMeasureSpec与
childHeightMeasureSpec对顶层视图host进行测量。
```

#### 1.3 检查是否需要处理Activity窗口大小变化事件以及Activity窗口是否需要指定额外的内容边距与可见边距

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
    
     private void performTraversals() {
            ...
            boolean windowShouldResize = mLayoutRequested && windowResizesToFitContent
                && ((mWidth != host.mMeasuredWidth || mHeight != host.mMeasuredHeight)
                    || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                            frame.width() < desiredWindowWidth && frame.width() != mWidth)
                    || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                            frame.height() < desiredWindowHeight && frame.height() != mHeight));
    
            final boolean computesInternalInsets =
                    attachInfo.mTreeObserver.hasComputeInternalInsetsListeners();
            ...
     }
}
```

这段函数主要做了两件事情：

1 检查是否需要处理Activity窗口大小变化事件，以下情形以下windowShouldResize被置为true，需要处理。

```
1 ViewRoot.mLayoutRequested = true，说明应用正在请求Activity窗口执行一次测量、布局与绘制操作。
2 ViewRoot.windowResizesToFitContent = true，说明我们前面的代码检查到了Activity窗口的大小发生了辩护。
3 如果测量出来的大小与当前的大小不相等时也认为窗口大小发生了变化。
```
2 检查Activity窗口是否需要指定额外的内容边距与课件可见边距，之所以这么做是为了放置一些额外的东西。

#### 1.4 调用View.measure()完成Activity窗口的测量工作

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
    
     private void performTraversals() {
            ...
 boolean insetsPending = false;
            int relayoutResult = 0;
            if (mFirst || windowShouldResize || insetsChanged
                    || viewVisibilityChanged || params != null) {
    
                if (viewVisibility == View.VISIBLE) {
                    // If this window is giving internal insets to the window
                    // manager, and it is being added or changing its visibility,
                    // then we want to first give the window manager "fake"
                    // insets to cause it to effectively ignore the content of
                    // the window during layout.  This avoids it briefly causing
                    // other windows to resize/move based on the raw frame of the
                    // window, waiting until we can finish laying out this window
                    // and get back to the window manager with the ultimately
                    // computed insets.
                    //insetsPending = true表示Activity窗口有额外的内容边距与可见边距等待指定
                    insetsPending = computesInternalInsets
                            && (mFirst || viewVisibilityChanged);
    
                    if (mWindowAttributes.memoryType == WindowManager.LayoutParams.MEMORY_TYPE_GPU) {
                        if (params == null) {
                            params = mWindowAttributes;
                        }
                        mGlWanted = true;
                    }
                }
    
                if (mSurfaceHolder != null) {
                    mSurfaceHolder.mSurfaceLock.lock();
                    mDrawingAllowed = true;
                }
                
                boolean initialized = false;
                boolean contentInsetsChanged = false;
                boolean visibleInsetsChanged;
                boolean hadSurface = mSurface.isValid();
                try {
                    int fl = 0;
                    if (params != null) {
                        fl = params.flags;
                        if (attachInfo.mKeepScreenOn) {
                            params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                        }
                    }
                    if (DEBUG_LAYOUT) {
                        Log.i(TAG, "host=w:" + host.mMeasuredWidth + ", h:" +
                                host.mMeasuredHeight + ", params=" + params);
                    }
                  
                    //调用relayoutWindow()方法来请求WindowManagerService来计算Activity窗口的大小
                    //以及内容边距和可见边距大小，计算完毕后Activity窗口的大小会保存在变量mWinFrame中
                    //Activity窗口的内容边距大小保存在mPendingContentInsets中，可见边距保存中mPending
                    //VisibleInsets中
                    relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);
    
                    if (params != null) {
                        params.flags = fl;
                    }
    
                    if (DEBUG_LAYOUT) Log.v(TAG, "relayout: frame=" + frame.toShortString()
                            + " content=" + mPendingContentInsets.toShortString()
                            + " visible=" + mPendingVisibleInsets.toShortString()
                            + " surface=" + mSurface);
    
                    if (mPendingConfiguration.seq != 0) {
                        if (DEBUG_CONFIGURATION) Log.v(TAG, "Visible with new config: "
                                + mPendingConfiguration);
                        updateConfiguration(mPendingConfiguration, !mFirst);
                        mPendingConfiguration.seq = 0;
                    }
                    
                    //Activity窗口内容边距是否发生变化
                    contentInsetsChanged = !mPendingContentInsets.equals(
                            mAttachInfo.mContentInsets);
                    //Activity窗口可见边距是否发生变化
                    visibleInsetsChanged = !mPendingVisibleInsets.equals(
                            mAttachInfo.mVisibleInsets);
                    if (contentInsetsChanged) {
                        mAttachInfo.mContentInsets.set(mPendingContentInsets);
                        host.fitSystemWindows(mAttachInfo.mContentInsets);
                        if (DEBUG_LAYOUT) Log.v(TAG, "Content insets changing to: "
                                + mAttachInfo.mContentInsets);
                    }
                    if (visibleInsetsChanged) {
                        mAttachInfo.mVisibleInsets.set(mPendingVisibleInsets);
                        if (DEBUG_LAYOUT) Log.v(TAG, "Visible insets changing to: "
                                + mAttachInfo.mVisibleInsets);
                    }
    
                    if (!hadSurface) {
                        if (mSurface.isValid()) {
                            // If we are creating a new surface, then we need to
                            // completely redraw it.  Also, when we get to the
                            // point of drawing it we will hold off and schedule
                            // a new traversal instead.  This is so we can tell the
                            // window manager about all of the windows being displayed
                            // before actually drawing them, so it can display then
                            // all at once.
                            newSurface = true;
                            fullRedrawNeeded = true;
                            mPreviousTransparentRegion.setEmpty();
    
                            if (mGlWanted && !mUseGL) {
                                initializeGL();
                                initialized = mGlCanvas != null;
                            }
                        }
                    } else if (!mSurface.isValid()) {
                        // If the surface has been removed, then reset the scroll
                        // positions.
                        mLastScrolledFocus = null;
                        mScrollY = mCurScrollY = 0;
                        if (mScroller != null) {
                            mScroller.abortAnimation();
                        }
                    }
                } catch (RemoteException e) {
                }
                
                if (DEBUG_ORIENTATION) Log.v(
                        TAG, "Relayout returned: frame=" + frame + ", surface=" + mSurface);
    
                attachInfo.mWindowLeft = frame.left;
                attachInfo.mWindowTop = frame.top;
    
                // !!FIXME!! This next section handles the case where we did not get the
                // window size we asked for. We should avoid this by getting a maximum size from
                // the window session beforehand.
                mWidth = frame.width();
                mHeight = frame.height();
    
                if (mSurfaceHolder != null) {
                    // The app owns the surface; tell it about what is going on.
                    if (mSurface.isValid()) {
                        // XXX .copyFrom() doesn't work!
                        //mSurfaceHolder.mSurface.copyFrom(mSurface);
                        mSurfaceHolder.mSurface = mSurface;
                    }
                    mSurfaceHolder.mSurfaceLock.unlock();
                    if (mSurface.isValid()) {
                        if (!hadSurface) {
                            mSurfaceHolder.ungetCallbacks();
    
                            mIsCreating = true;
                            mSurfaceHolderCallback.surfaceCreated(mSurfaceHolder);
                            SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback c : callbacks) {
                                    c.surfaceCreated(mSurfaceHolder);
                                }
                            }
                            surfaceChanged = true;
                        }
                        if (surfaceChanged) {
                            mSurfaceHolderCallback.surfaceChanged(mSurfaceHolder,
                                    lp.format, mWidth, mHeight);
                            SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback c : callbacks) {
                                    c.surfaceChanged(mSurfaceHolder, lp.format,
                                            mWidth, mHeight);
                                }
                            }
                        }
                        mIsCreating = false;
                    } else if (hadSurface) {
                        mSurfaceHolder.ungetCallbacks();
                        SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                        mSurfaceHolderCallback.surfaceDestroyed(mSurfaceHolder);
                        if (callbacks != null) {
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceDestroyed(mSurfaceHolder);
                            }
                        }
                        mSurfaceHolder.mSurfaceLock.lock();
                        // Make surface invalid.
                        //mSurfaceHolder.mSurface.copyFrom(mSurface);
                        mSurfaceHolder.mSurface = new Surface();
                        mSurfaceHolder.mSurfaceLock.unlock();
                    }
                }
                
                if (initialized) {
                    mGlCanvas.setViewport((int) (mWidth * appScale + 0.5f),
                            (int) (mHeight * appScale + 0.5f));
                }
    
                //检查是否需要重新测量Activity窗口的大小
                boolean focusChangedDueToTouchMode = ensureTouchModeLocally(
                        (relayoutResult&WindowManagerImpl.RELAYOUT_IN_TOUCH_MODE) != 0);
                if (focusChangedDueToTouchMode || mWidth != host.mMeasuredWidth
                        || mHeight != host.mMeasuredHeight || contentInsetsChanged) {
                    childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
                    childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
    
                    if (DEBUG_LAYOUT) Log.v(TAG, "Ooops, something changed!  mWidth="
                            + mWidth + " measuredWidth=" + host.mMeasuredWidth
                            + " mHeight=" + mHeight
                            + " measuredHeight" + host.mMeasuredHeight
                            + " coveredInsetsChanged=" + contentInsetsChanged);
    
                    //调用View.measure()方法进行测量
                     // Ask host how big it wants to be
                    host.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    
                    // Implementation of weights from WindowManager.LayoutParams
                    // We just grow the dimensions as needed and re-measure if
                    // needs be
                    int width = host.mMeasuredWidth;
                    int height = host.mMeasuredHeight;
                    boolean measureAgain = false;
    
                    if (lp.horizontalWeight > 0.0f) {
                        width += (int) ((mWidth - width) * lp.horizontalWeight);
                        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
                                MeasureSpec.EXACTLY);
                        measureAgain = true;
                    }
                    if (lp.verticalWeight > 0.0f) {
                        height += (int) ((mHeight - height) * lp.verticalWeight);
                        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
                                MeasureSpec.EXACTLY);
                        measureAgain = true;
                    }
    
                    if (measureAgain) {
                        if (DEBUG_LAYOUT) Log.v(TAG,
                                "And hey let's measure once more: width=" + width
                                + " height=" + height);
                        host.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                    }
    
                    mLayoutRequested = true;
                }
            }            
            ...
     }
}
```

先来说说这段代码前面的几个boolean值，也就是代码的执行条件：

```
1 mFirst = true：Activity窗口第一次执行策略、布局与绘制操作。
2 windowShouldResize = true：Activity窗口的大小发生了变化。
3 insetsChanged：Activity窗口的内容边距发生了变化。
4 viewVisibilityChanged：Activity窗口的可见性发生了变化。
5 params != null：变量params指向了一个WindowManagerParams对象，即Activity窗口的属性发生了变化。
```

这段代码主要做了以下几件事情：

1 调用relayoutWindow()方法来请求WindowManagerService来计算Activity窗口的大小以及内容边距和可见边距大小，计算
完毕后Activity窗口的大小会保存在变量mWinFrame中Activity窗口的内容边距大小保存在mPendingContentInsets中，可见
边距保存中mPendingVisibleInsets中

2 检查是否需要重新测量Activity窗口的大小，满足以下条件之一则需要重新测量：

```
1 focusChangedDueToTouchMode = true：即Activity窗口的触摸模式发生了变化，由此引发了Activity窗口获得当前
焦点的控件发生了变化。
2 Activity窗口新测量出来的宽度host.mMeasuredWidth和高度host.mMeasuredHeight不等于WindowManagerService
服务计算出来的宽度mWidth和高度mHeight。
3 contentInsetsChanged = true：Activity窗口的内容边距和可见边距发生了变化。
```

如果需要进行测量，则调用View.measure()方法进行测量。

#### 1.5 调用View.layout()方法完成布局工作，并将Activity窗口指定的额外的内容边距与可见边距通过sWindowSession发送给WindowManagerService。

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
    
     private void performTraversals() {
            ...
            
            final boolean didLayout = mLayoutRequested;
            boolean triggerGlobalLayoutListener = didLayout
                    || attachInfo.mRecomputeGlobalAttributes;
            if (didLayout) {
                mLayoutRequested = false;
                mScrollMayChange = true;
                if (DEBUG_ORIENTATION || DEBUG_LAYOUT) Log.v(
                    TAG, "Laying out " + host + " to (" +
                    host.mMeasuredWidth + ", " + host.mMeasuredHeight + ")");
                long startTime = 0L;
                if (Config.DEBUG && ViewDebug.profileLayout) {
                    startTime = SystemClock.elapsedRealtime();
                }
                
                //调用View.layout()方法完成布局工作
                host.layout(0, 0, host.mMeasuredWidth, host.mMeasuredHeight);
    
                if (Config.DEBUG && ViewDebug.consistencyCheckEnabled) {
                    if (!host.dispatchConsistencyCheck(ViewDebug.CONSISTENCY_LAYOUT)) {
                        throw new IllegalStateException("The view hierarchy is an inconsistent state,"
                                + "please refer to the logs with the tag "
                                + ViewDebug.CONSISTENCY_LOG_TAG + " for more infomation.");
                    }
                }
    
                if (Config.DEBUG && ViewDebug.profileLayout) {
                    EventLog.writeEvent(60001, SystemClock.elapsedRealtime() - startTime);
                }
    
                // By this point all views have been sized and positionned
                // We can compute the transparent area
    
                if ((host.mPrivateFlags & View.REQUEST_TRANSPARENT_REGIONS) != 0) {
                    // start out transparent
                    // TODO: AVOID THAT CALL BY CACHING THE RESULT?
                    host.getLocationInWindow(mTmpLocation);
                    mTransparentRegion.set(mTmpLocation[0], mTmpLocation[1],
                            mTmpLocation[0] + host.mRight - host.mLeft,
                            mTmpLocation[1] + host.mBottom - host.mTop);
    
                    host.gatherTransparentRegion(mTransparentRegion);
                    if (mTranslator != null) {
                        mTranslator.translateRegionInWindowToScreen(mTransparentRegion);
                    }
    
                    if (!mTransparentRegion.equals(mPreviousTransparentRegion)) {
                        mPreviousTransparentRegion.set(mTransparentRegion);
                        // reconfigure window manager
                        try {
                            sWindowSession.setTransparentRegion(mWindow, mTransparentRegion);
                        } catch (RemoteException e) {
                        }
                    }
                }
    
                if (DBG) {
                    System.out.println("======================================");
                    System.out.println("performTraversals -- after setFrame");
                    host.debug();
                }
            }
            
            if (triggerGlobalLayoutListener) {
                attachInfo.mRecomputeGlobalAttributes = false;
                attachInfo.mTreeObserver.dispatchOnGlobalLayout();
            }
    
            //computesInternalInsets为true时表明Activity窗口指定了额外的内容边距与可见边距，这个时候需要
            //通知WindowManagerService，以便WindowManagerService下次可以知道Activity的真实布局。
            if (computesInternalInsets) {
                ViewTreeObserver.InternalInsetsInfo insets = attachInfo.mGivenInternalInsets;
                final Rect givenContent = attachInfo.mGivenInternalInsets.contentInsets;
                final Rect givenVisible = attachInfo.mGivenInternalInsets.visibleInsets;
                givenContent.left = givenContent.top = givenContent.right
                        = givenContent.bottom = givenVisible.left = givenVisible.top
                        = givenVisible.right = givenVisible.bottom = 0;
                //调用TreeObserver.dispatchOnComputeInternalInsets(insets)来计算Activity窗口额外指定的
                //内容边距与可见边距的大小，计算完成后保存在变量attachInfo.mGivenInternalInsets中。
                attachInfo.mTreeObserver.dispatchOnComputeInternalInsets(insets);
                Rect contentInsets = insets.contentInsets;
                Rect visibleInsets = insets.visibleInsets;
                if (mTranslator != null) {
                    contentInsets = mTranslator.getTranslatedContentInsets(contentInsets);
                    visibleInsets = mTranslator.getTranslatedVisbileInsets(visibleInsets);
                }
                if (insetsPending || !mLastGivenInsets.equals(insets)) {
                    mLastGivenInsets.set(insets);
                    try {
                        //sWindowSession是一个Binder代理对象，通过它将内容边距与可见边距设置到WindowManagerService中
                        sWindowSession.setInsets(mWindow, insets.mTouchableInsets,
                                contentInsets, visibleInsets);
                    } catch (RemoteException e) {
                    }
                }
            }
    
            if (mFirst) {
                // handle first focus request
                if (DEBUG_INPUT_RESIZE) Log.v(TAG, "First: mView.hasFocus()="
                        + mView.hasFocus());
                if (mView != null) {
                    if (!mView.hasFocus()) {
                        mView.requestFocus(View.FOCUS_FORWARD);
                        mFocusedView = mRealFocusedView = mView.findFocus();
                        if (DEBUG_INPUT_RESIZE) Log.v(TAG, "First: requested focused view="
                                + mFocusedView);
                    } else {
                        mRealFocusedView = mView.findFocus();
                        if (DEBUG_INPUT_RESIZE) Log.v(TAG, "First: existing focused view="
                                + mRealFocusedView);
                    }
                }
            }
    
            mFirst = false;
            mWillDrawSoon = false;
            mNewSurfaceNeeded = false;
            mViewVisibility = viewVisibility;
    
            if (mAttachInfo.mHasWindowFocus) {
                final boolean imTarget = WindowManager.LayoutParams
                        .mayUseInputMethod(mWindowAttributes.flags);
                if (imTarget != mLastWasImTarget) {
                    mLastWasImTarget = imTarget;
                    InputMethodManager imm = InputMethodManager.peekInstance();
                    if (imm != null && imTarget) {
                        imm.startGettingWindowFocus(mView);
                        imm.onWindowFocus(mView, mView.findFocus(),
                                mWindowAttributes.softInputMode,
                                !mHasHadWindowFocus, mWindowAttributes.flags);
                    }
                }
            }
    
            boolean cancelDraw = attachInfo.mTreeObserver.dispatchOnPreDraw();
    
            if (!cancelDraw && !newSurface) {
                mFullRedrawNeeded = false;
                draw(fullRedrawNeeded);
    
                if ((relayoutResult&WindowManagerImpl.RELAYOUT_FIRST_TIME) != 0
                        || mReportNextDraw) {
                    if (LOCAL_LOGV) {
                        Log.v(TAG, "FINISHED DRAWING: " + mWindowAttributes.getTitle());
                    }
                    mReportNextDraw = false;
                    if (mSurfaceHolder != null && mSurface.isValid()) {
                        mSurfaceHolderCallback.surfaceRedrawNeeded(mSurfaceHolder);
                        SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                        if (callbacks != null) {
                            for (SurfaceHolder.Callback c : callbacks) {
                                if (c instanceof SurfaceHolder.Callback2) {
                                    ((SurfaceHolder.Callback2)c).surfaceRedrawNeeded(
                                            mSurfaceHolder);
                                }
                            }
                        }
                    }
                    try {
                        sWindowSession.finishDrawing(mWindow);
                    } catch (RemoteException e) {
                    }
                }
            } else {
                // We were supposed to report when we are done drawing. Since we canceled the
                // draw, remember it here.
                if ((relayoutResult&WindowManagerImpl.RELAYOUT_FIRST_TIME) != 0) {
                    mReportNextDraw = true;
                }
                if (fullRedrawNeeded) {
                    mFullRedrawNeeded = true;
                }
                // Try again
                scheduleTraversals();
            }            
     }
}
```

这段代码调用View.layout()方法完成布局工作，并将Activity窗口指定的额外的内容边距与可见边距通过sWindowSession发送给WindowManagerService。

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
从这个performLayoutAndPlaceSurfacesLocked长长的方法名字可以看出，它不仅仅用来计算窗口大小，它还负责刷新系统UI，事实上，每当
Activity窗口属性发生了变化，例如：可见性，大小等，又或者它要新增、删除子视图时，都会要求WindowManagerService刷新系统UI，所以
你可以看出WindowManagerService的主要工作就是刷新系统UI，

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

```java
public class WindowManagerService extends IWindowManager.Stub  
        implements Watchdog.Monitor { 
    
    private final void performLayoutAndPlaceSurfacesLockedInner(
            boolean recoveringMemory) {
        ...
        final long currentTime = SystemClock.uptimeMillis();
        //获取当前屏幕的宽高
        final int dw = mDisplay.getWidth();
        final int dh = mDisplay.getHeight();

        int i;
		
	     //更新窗口焦点
        if (mFocusMayChange) {
            mFocusMayChange = false;
            updateFocusedWindowLocked(UPDATE_FOCUS_WILL_PLACE_SURFACES);
        }
        
        // Initialize state of exiting tokens.
        for (i=mExitingTokens.size()-1; i>=0; i--) {
            mExitingTokens.get(i).hasVisible = false;
        }

        // Initialize state of exiting applications.
        for (i=mExitingAppTokens.size()-1; i>=0; i--) {
            mExitingAppTokens.get(i).hasVisible = false;
        }

        boolean orientationChangeComplete = true;
        Session holdScreen = null;
        float screenBrightness = -1;
        float buttonBrightness = -1;
        boolean focusDisplayed = false;
        boolean animating = false;
        boolean createWatermark = false;

        if (mFxSession == null) {
            mFxSession = new SurfaceSession();
            createWatermark = true;
        }

        if (SHOW_TRANSACTIONS) Slog.i(TAG, ">>> OPEN TRANSACTION");

		 //这个一个native方法，用来开启SurfaceFlinger事物
        Surface.openTransaction();

        if (createWatermark) {
            createWatermark();
        }
        if (mWatermark != null) {
            mWatermark.positionSurface(dw, dh);
        }

        try {
            boolean wallpaperForceHidingChanged = false;
            int repeats = 0;
            int changes = 0;
            
            //创建一个最多执行7次的while循环，计算窗口的大小以及执行窗口动画
            do {
                repeats++;
                if (repeats > 6) {
                    Slog.w(TAG, "Animation repeat aborted after too many iterations");
                    mLayoutNeeded = false;
                    break;
                }
                
                if ((changes&(WindowManagerPolicy.FINISH_LAYOUT_REDO_WALLPAPER
                        | WindowManagerPolicy.FINISH_LAYOUT_REDO_CONFIG
                        | WindowManagerPolicy.FINISH_LAYOUT_REDO_LAYOUT)) != 0) {
                    if ((changes&WindowManagerPolicy.FINISH_LAYOUT_REDO_WALLPAPER) != 0) {
                        if ((adjustWallpaperWindowsLocked()&ADJUST_WALLPAPER_LAYERS_CHANGED) != 0) {
                            assignLayersLocked();
                            mLayoutNeeded = true;
                        }
                    }
                    if ((changes&WindowManagerPolicy.FINISH_LAYOUT_REDO_CONFIG) != 0) {
                        if (DEBUG_LAYOUT) Slog.v(TAG, "Computing new config from layout");
                        if (updateOrientationFromAppTokensLocked()) {
                            mLayoutNeeded = true;
                            mH.sendEmptyMessage(H.SEND_NEW_CONFIGURATION);
                        }
                    }
                    if ((changes&WindowManagerPolicy.FINISH_LAYOUT_REDO_LAYOUT) != 0) {
                        mLayoutNeeded = true;
                    }
                }
                
                // FIRST LOOP: Perform a layout, if needed.
                if (repeats < 4) {
                    changes = performLayoutLockedInner();
                    if (changes != 0) {
                        continue;
                    }
                } else {
                    Slog.w(TAG, "Layout repeat skipped after too many iterations");
                    changes = 0;
                }

            ...
	}
}
```

这个方法不仅名字长，实现也非常长，足足有1200多行，但它也正是WindowManagerService的核心逻辑所在。这个方法主要做了以下事情：

```
1 创建一个最多执行7次的while循环，在该循环中：

① 调用performLayoutLockedInner()计算各个窗口的大小。
② 处理窗口显示动画与切换动画以及更新各个窗口的可见性。

2 将窗口的属性发送给SurfaceFlinger，SurfaceFlinger会去更新Layer属性，对Layer进行可见性计算与合成等操作，最后
渲染到硬件缓冲区中去。每个窗口的属性更新操作都被封装到一个SurfaceFlinger服务的一个事务中（Transaction）。

3 经过上面的操作，系统UI刷新完成，系统将执行不再显示窗口的清理工作，销毁掉不再显示窗口的绘图画布，移除窗口令牌
WindowToken与Activity窗口令牌AppWindowToken。
```

我们这里先分析窗口大小的计算流程，即performLayoutLockedInner()函数的实现，关于窗口动画后续会有文章分析。

### 7 WindowManagerService.performLayoutLockedInner()

```java
public class WindowManagerService extends IWindowManager.Stub  
        implements Watchdog.Monitor {
    
    final WindowManagerPolicy mPolicy = PolicyManager.makeNewWindowManager();  
     
    /** 
     * Z-ordered (bottom-most first) list of all Window objects. 
     */  
    final ArrayList<WindowState> mWindows = new ArrayList<WindowState>();  
    
    private final int performLayoutLockedInner() {
            if (!mLayoutNeeded) {
                return 0;
            }
            
            mLayoutNeeded = false;
            
            final int dw = mDisplay.getWidth();
            final int dh = mDisplay.getHeight();
    
            final int N = mWindows.size();
            int i;
    
            if (DEBUG_LAYOUT) Slog.v(TAG, "performLayout: needed="
                    + mLayoutNeeded + " dw=" + dw + " dh=" + dh);
            
            //调用PhoneWindowManager.beginLayoutLw()方法设置屏幕大小
            mPolicy.beginLayoutLw(dw, dh);
    
            int seq = mLayoutSeq+1;
            if (seq < 0) seq = 0;
            mLayoutSeq = seq;
            
            // First perform layout of any root windows (not attached
            // to another window).
            int topAttached = -1;
            for (i = N-1; i >= 0; i--) {
                WindowState win = mWindows.get(i);
    
                // Don't do layout of a window if it is not visible, or
                // soon won't be visible, to avoid wasting time and funky
                // changes while a window is animating away.
                final AppWindowToken atoken = win.mAppToken;
                final boolean gone = win.mViewVisibility == View.GONE
                        || !win.mRelayoutCalled
                        || win.mRootToken.hidden
                        || (atoken != null && atoken.hiddenRequested)
                        || win.mAttachedHidden
                        || win.mExiting || win.mDestroying;
    
                if (!win.mLayoutAttached) {
                    if (DEBUG_LAYOUT) Slog.v(TAG, "First pass " + win
                            + ": gone=" + gone + " mHaveFrame=" + win.mHaveFrame
                            + " mLayoutAttached=" + win.mLayoutAttached);
                    if (DEBUG_LAYOUT && gone) Slog.v(TAG, "  (mViewVisibility="
                            + win.mViewVisibility + " mRelayoutCalled="
                            + win.mRelayoutCalled + " hidden="
                            + win.mRootToken.hidden + " hiddenRequested="
                            + (atoken != null && atoken.hiddenRequested)
                            + " mAttachedHidden=" + win.mAttachedHidden);
                }
                
                // If this view is GONE, then skip it -- keep the current
                // frame, and let the caller know so they can ignore it
                // if they want.  (We do the normal layout for INVISIBLE
                // windows, since that means "perform layout as normal,
                // just don't display").
                if (!gone || !win.mHaveFrame) {
                    if (!win.mLayoutAttached) {
                        //调用PhoneWindowManager.layoutWindowLw()方法计算各个窗口大小
                        //内容边距大小以及可见边距大小
                        mPolicy.layoutWindowLw(win, win.mAttrs, null);
                        win.mLayoutSeq = seq;
                        if (DEBUG_LAYOUT) Slog.v(TAG, "-> mFrame="
                                + win.mFrame + " mContainingFrame="
                                + win.mContainingFrame + " mDisplayFrame="
                                + win.mDisplayFrame);
                    } else {
                        if (topAttached < 0) topAttached = i;
                    }
                }
            }
    
            // Now perform layout of attached windows, which usually
            // depend on the position of the window they are attached to.
            // XXX does not deal with windows that are attached to windows
            // that are themselves attached.
            for (i = topAttached; i >= 0; i--) {
                WindowState win = mWindows.get(i);
    
                // If this view is GONE, then skip it -- keep the current
                // frame, and let the caller know so they can ignore it
                // if they want.  (We do the normal layout for INVISIBLE
                // windows, since that means "perform layout as normal,
                // just don't display").
                if (win.mLayoutAttached) {
                    if (DEBUG_LAYOUT) Slog.v(TAG, "Second pass " + win
                            + " mHaveFrame=" + win.mHaveFrame
                            + " mViewVisibility=" + win.mViewVisibility
                            + " mRelayoutCalled=" + win.mRelayoutCalled);
                    if ((win.mViewVisibility != View.GONE && win.mRelayoutCalled)
                            || !win.mHaveFrame) {
                        mPolicy.layoutWindowLw(win, win.mAttrs, win.mAttachedWindow);
                        win.mLayoutSeq = seq;
                        if (DEBUG_LAYOUT) Slog.v(TAG, "-> mFrame="
                                + win.mFrame + " mContainingFrame="
                                + win.mContainingFrame + " mDisplayFrame="
                                + win.mDisplayFrame);
                    }
                }
            }
            
            // Window frames may have changed.  Tell the input dispatcher about it.
            mInputMonitor.updateInputWindowsLw();
    
            //调用PhoneWindowManager.finishLayoutLw()方法完成一些清理工作
            return mPolicy.finishLayoutLw();
        }    
}
```
在介绍该方法的实现之前，我们先了解下WindowMangerService两个成员变量：

```
WindowManagerPolicy mPolicy：WindowManagerPolicy是一个窗口管理策略类，它通过PolicyManager.makeNewWindow
Manager()方法创建，在Phone平台的实现类是PhoneWindowManager，它主要用来制定窗口大小的计算策略。

ArrayList<WindowState> mWindows：WindowState的列表，该列表保存了系统中的所有窗口，这些窗口按照Z轴的位置从小
到大排列在这个列表中。WindowState用来描述窗口的各种信息。
```

该函数的执行流程主要分为3个阶段：

```
1 调用PhoneWindowManager.beginLayoutLw()来设置屏幕大小
2 调用PhoneWindowManager.layoutWindowLw()来计算各个窗口的大小、内容边距以及可见边距大小
3 调用PhoneWindowManager.finishLayoutLw()来执行一些清理工作
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





