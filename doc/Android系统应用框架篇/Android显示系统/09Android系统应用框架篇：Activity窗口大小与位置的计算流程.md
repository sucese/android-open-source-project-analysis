# Android系统应用框架篇：Window大小与位置的计算流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作。技术栈主要涉及Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative，数据结构与算法等方面。热爱编程与吉他，喜欢有趣的事物和人。

**关于文章**

>作者的文章首发在[Github](https://github.com/guoxiaoxing)上，也会发在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与[CSDN](http://blog.csdn.net/allenwells)平台上，文章内容主要包含Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative, 数据结构与算法等方面的内容。如果有什么问题，也欢迎发邮件与我交流。

## Window大小的计算

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

### WindowManagerService.performLayoutAndPlaceSurfacesLocked() 

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


```java
public class WindowManagerService extends IWindowManager.Stub  
        implements Watchdog.Monitor { 
    
    private final void performLayoutAndPlaceSurfacesLockedInner(
            boolean recoveringMemory) {
        final long currentTime = SystemClock.uptimeMillis();
        final int dw = mDisplay.getWidth();
        final int dh = mDisplay.getHeight();

        int i;

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
                
                final int transactionSequence = ++mTransactionSequence;

                // Update animations of all applications, including those
                // associated with exiting/removed apps
                boolean tokensAnimating = false;
                final int NAT = mAppTokens.size();
                for (i=0; i<NAT; i++) {
                    if (mAppTokens.get(i).stepAnimationLocked(currentTime, dw, dh)) {
                        tokensAnimating = true;
                    }
                }
                final int NEAT = mExitingAppTokens.size();
                for (i=0; i<NEAT; i++) {
                    if (mExitingAppTokens.get(i).stepAnimationLocked(currentTime, dw, dh)) {
                        tokensAnimating = true;
                    }
                }

                // SECOND LOOP: Execute animations and update visibility of windows.
                
                if (DEBUG_APP_TRANSITIONS) Slog.v(TAG, "*** ANIM STEP: seq="
                        + transactionSequence + " tokensAnimating="
                        + tokensAnimating);
                        
                animating = tokensAnimating;

                boolean tokenMayBeDrawn = false;
                boolean wallpaperMayChange = false;
                boolean forceHiding = false;

                mPolicy.beginAnimationLw(dw, dh);

                final int N = mWindows.size();

                for (i=N-1; i>=0; i--) {
                    WindowState w = mWindows.get(i);

                    final WindowManager.LayoutParams attrs = w.mAttrs;

                    if (w.mSurface != null) {
                        // Execute animation.
                        if (w.commitFinishDrawingLocked(currentTime)) {
                            if ((w.mAttrs.flags
                                    & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER) != 0) {
                                if (DEBUG_WALLPAPER) Slog.v(TAG,
                                        "First draw done in potential wallpaper target " + w);
                                wallpaperMayChange = true;
                            }
                        }

                        boolean wasAnimating = w.mAnimating;
                        if (w.stepAnimationLocked(currentTime, dw, dh)) {
                            animating = true;
                            //w.dump("  ");
                        }
                        if (wasAnimating && !w.mAnimating && mWallpaperTarget == w) {
                            wallpaperMayChange = true;
                        }

                        if (mPolicy.doesForceHide(w, attrs)) {
                            if (!wasAnimating && animating) {
                                if (DEBUG_VISIBILITY) Slog.v(TAG,
                                        "Animation done that could impact force hide: "
                                        + w);
                                wallpaperForceHidingChanged = true;
                                mFocusMayChange = true;
                            } else if (w.isReadyForDisplay() && w.mAnimation == null) {
                                forceHiding = true;
                            }
                        } else if (mPolicy.canBeForceHidden(w, attrs)) {
                            boolean changed;
                            if (forceHiding) {
                                changed = w.hideLw(false, false);
                                if (DEBUG_VISIBILITY && changed) Slog.v(TAG,
                                        "Now policy hidden: " + w);
                            } else {
                                changed = w.showLw(false, false);
                                if (DEBUG_VISIBILITY && changed) Slog.v(TAG,
                                        "Now policy shown: " + w);
                                if (changed) {
                                    if (wallpaperForceHidingChanged
                                            && w.isVisibleNow() /*w.isReadyForDisplay()*/) {
                                        // Assume we will need to animate.  If
                                        // we don't (because the wallpaper will
                                        // stay with the lock screen), then we will
                                        // clean up later.
                                        Animation a = mPolicy.createForceHideEnterAnimation();
                                        if (a != null) {
                                            w.setAnimation(a);
                                        }
                                    }
                                    if (mCurrentFocus == null ||
                                            mCurrentFocus.mLayer < w.mLayer) {
                                        // We are showing on to of the current
                                        // focus, so re-evaluate focus to make
                                        // sure it is correct.
                                        mFocusMayChange = true;
                                    }
                                }
                            }
                            if (changed && (attrs.flags
                                    & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER) != 0) {
                                wallpaperMayChange = true;
                            }
                        }

                        mPolicy.animatingWindowLw(w, attrs);
                    }

                    final AppWindowToken atoken = w.mAppToken;
                    if (atoken != null && (!atoken.allDrawn || atoken.freezingScreen)) {
                        if (atoken.lastTransactionSequence != transactionSequence) {
                            atoken.lastTransactionSequence = transactionSequence;
                            atoken.numInterestingWindows = atoken.numDrawnWindows = 0;
                            atoken.startingDisplayed = false;
                        }
                        if ((w.isOnScreen() || w.mAttrs.type
                                == WindowManager.LayoutParams.TYPE_BASE_APPLICATION)
                                && !w.mExiting && !w.mDestroying) {
                            if (DEBUG_VISIBILITY || DEBUG_ORIENTATION) {
                                Slog.v(TAG, "Eval win " + w + ": isDrawn="
                                        + w.isDrawnLw()
                                        + ", isAnimating=" + w.isAnimating());
                                if (!w.isDrawnLw()) {
                                    Slog.v(TAG, "Not displayed: s=" + w.mSurface
                                            + " pv=" + w.mPolicyVisibility
                                            + " dp=" + w.mDrawPending
                                            + " cdp=" + w.mCommitDrawPending
                                            + " ah=" + w.mAttachedHidden
                                            + " th=" + atoken.hiddenRequested
                                            + " a=" + w.mAnimating);
                                }
                            }
                            if (w != atoken.startingWindow) {
                                if (!atoken.freezingScreen || !w.mAppFreezing) {
                                    atoken.numInterestingWindows++;
                                    if (w.isDrawnLw()) {
                                        atoken.numDrawnWindows++;
                                        if (DEBUG_VISIBILITY || DEBUG_ORIENTATION) Slog.v(TAG,
                                                "tokenMayBeDrawn: " + atoken
                                                + " freezingScreen=" + atoken.freezingScreen
                                                + " mAppFreezing=" + w.mAppFreezing);
                                        tokenMayBeDrawn = true;
                                    }
                                }
                            } else if (w.isDrawnLw()) {
                                atoken.startingDisplayed = true;
                            }
                        }
                    } else if (w.mReadyToShow) {
                        w.performShowLocked();
                    }
                }

                changes |= mPolicy.finishAnimationLw();

                if (tokenMayBeDrawn) {
                    // See if any windows have been drawn, so they (and others
                    // associated with them) can now be shown.
                    final int NT = mTokenList.size();
                    for (i=0; i<NT; i++) {
                        AppWindowToken wtoken = mTokenList.get(i).appWindowToken;
                        if (wtoken == null) {
                            continue;
                        }
                        if (wtoken.freezingScreen) {
                            int numInteresting = wtoken.numInterestingWindows;
                            if (numInteresting > 0 && wtoken.numDrawnWindows >= numInteresting) {
                                if (DEBUG_VISIBILITY) Slog.v(TAG,
                                        "allDrawn: " + wtoken
                                        + " interesting=" + numInteresting
                                        + " drawn=" + wtoken.numDrawnWindows);
                                wtoken.showAllWindowsLocked();
                                unsetAppFreezingScreenLocked(wtoken, false, true);
                                orientationChangeComplete = true;
                            }
                        } else if (!wtoken.allDrawn) {
                            int numInteresting = wtoken.numInterestingWindows;
                            if (numInteresting > 0 && wtoken.numDrawnWindows >= numInteresting) {
                                if (DEBUG_VISIBILITY) Slog.v(TAG,
                                        "allDrawn: " + wtoken
                                        + " interesting=" + numInteresting
                                        + " drawn=" + wtoken.numDrawnWindows);
                                wtoken.allDrawn = true;
                                changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_ANIM;

                                // We can now show all of the drawn windows!
                                if (!mOpeningApps.contains(wtoken)) {
                                    wtoken.showAllWindowsLocked();
                                }
                            }
                        }
                    }
                }

                // If we are ready to perform an app transition, check through
                // all of the app tokens to be shown and see if they are ready
                // to go.
                if (mAppTransitionReady) {
                    int NN = mOpeningApps.size();
                    boolean goodToGo = true;
                    if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                            "Checking " + NN + " opening apps (frozen="
                            + mDisplayFrozen + " timeout="
                            + mAppTransitionTimeout + ")...");
                    if (!mDisplayFrozen && !mAppTransitionTimeout) {
                        // If the display isn't frozen, wait to do anything until
                        // all of the apps are ready.  Otherwise just go because
                        // we'll unfreeze the display when everyone is ready.
                        for (i=0; i<NN && goodToGo; i++) {
                            AppWindowToken wtoken = mOpeningApps.get(i);
                            if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                    "Check opening app" + wtoken + ": allDrawn="
                                    + wtoken.allDrawn + " startingDisplayed="
                                    + wtoken.startingDisplayed);
                            if (!wtoken.allDrawn && !wtoken.startingDisplayed
                                    && !wtoken.startingMoved) {
                                goodToGo = false;
                            }
                        }
                    }
                    if (goodToGo) {
                        if (DEBUG_APP_TRANSITIONS) Slog.v(TAG, "**** GOOD TO GO");
                        int transit = mNextAppTransition;
                        if (mSkipAppTransitionAnimation) {
                            transit = WindowManagerPolicy.TRANSIT_UNSET;
                        }
                        mNextAppTransition = WindowManagerPolicy.TRANSIT_UNSET;
                        mAppTransitionReady = false;
                        mAppTransitionRunning = true;
                        mAppTransitionTimeout = false;
                        mStartingIconInTransition = false;
                        mSkipAppTransitionAnimation = false;

                        mH.removeMessages(H.APP_TRANSITION_TIMEOUT);

                        // If there are applications waiting to come to the
                        // top of the stack, now is the time to move their windows.
                        // (Note that we don't do apps going to the bottom
                        // here -- we want to keep their windows in the old
                        // Z-order until the animation completes.)
                        if (mToTopApps.size() > 0) {
                            NN = mAppTokens.size();
                            for (i=0; i<NN; i++) {
                                AppWindowToken wtoken = mAppTokens.get(i);
                                if (wtoken.sendingToTop) {
                                    wtoken.sendingToTop = false;
                                    moveAppWindowsLocked(wtoken, NN, false);
                                }
                            }
                            mToTopApps.clear();
                        }

                        WindowState oldWallpaper = mWallpaperTarget;

                        adjustWallpaperWindowsLocked();
                        wallpaperMayChange = false;

                        // The top-most window will supply the layout params,
                        // and we will determine it below.
                        LayoutParams animLp = null;
                        AppWindowToken animToken = null;
                        int bestAnimLayer = -1;

                        if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                "New wallpaper target=" + mWallpaperTarget
                                + ", lower target=" + mLowerWallpaperTarget
                                + ", upper target=" + mUpperWallpaperTarget);
                        int foundWallpapers = 0;
                        // Do a first pass through the tokens for two
                        // things:
                        // (1) Determine if both the closing and opening
                        // app token sets are wallpaper targets, in which
                        // case special animations are needed
                        // (since the wallpaper needs to stay static
                        // behind them).
                        // (2) Find the layout params of the top-most
                        // application window in the tokens, which is
                        // what will control the animation theme.
                        final int NC = mClosingApps.size();
                        NN = NC + mOpeningApps.size();
                        for (i=0; i<NN; i++) {
                            AppWindowToken wtoken;
                            int mode;
                            if (i < NC) {
                                wtoken = mClosingApps.get(i);
                                mode = 1;
                            } else {
                                wtoken = mOpeningApps.get(i-NC);
                                mode = 2;
                            }
                            if (mLowerWallpaperTarget != null) {
                                if (mLowerWallpaperTarget.mAppToken == wtoken
                                        || mUpperWallpaperTarget.mAppToken == wtoken) {
                                    foundWallpapers |= mode;
                                }
                            }
                            if (wtoken.appFullscreen) {
                                WindowState ws = wtoken.findMainWindow();
                                if (ws != null) {
                                    // If this is a compatibility mode
                                    // window, we will always use its anim.
                                    if ((ws.mAttrs.flags&FLAG_COMPATIBLE_WINDOW) != 0) {
                                        animLp = ws.mAttrs;
                                        animToken = ws.mAppToken;
                                        bestAnimLayer = Integer.MAX_VALUE;
                                    } else if (ws.mLayer > bestAnimLayer) {
                                        animLp = ws.mAttrs;
                                        animToken = ws.mAppToken;
                                        bestAnimLayer = ws.mLayer;
                                    }
                                }
                            }
                        }

                        if (foundWallpapers == 3) {
                            if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                    "Wallpaper animation!");
                            switch (transit) {
                                case WindowManagerPolicy.TRANSIT_ACTIVITY_OPEN:
                                case WindowManagerPolicy.TRANSIT_TASK_OPEN:
                                case WindowManagerPolicy.TRANSIT_TASK_TO_FRONT:
                                    transit = WindowManagerPolicy.TRANSIT_WALLPAPER_INTRA_OPEN;
                                    break;
                                case WindowManagerPolicy.TRANSIT_ACTIVITY_CLOSE:
                                case WindowManagerPolicy.TRANSIT_TASK_CLOSE:
                                case WindowManagerPolicy.TRANSIT_TASK_TO_BACK:
                                    transit = WindowManagerPolicy.TRANSIT_WALLPAPER_INTRA_CLOSE;
                                    break;
                            }
                            if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                    "New transit: " + transit);
                        } else if (oldWallpaper != null) {
                            // We are transitioning from an activity with
                            // a wallpaper to one without.
                            transit = WindowManagerPolicy.TRANSIT_WALLPAPER_CLOSE;
                            if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                    "New transit away from wallpaper: " + transit);
                        } else if (mWallpaperTarget != null) {
                            // We are transitioning from an activity without
                            // a wallpaper to now showing the wallpaper
                            transit = WindowManagerPolicy.TRANSIT_WALLPAPER_OPEN;
                            if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                    "New transit into wallpaper: " + transit);
                        }

                        if ((transit&WindowManagerPolicy.TRANSIT_ENTER_MASK) != 0) {
                            mLastEnterAnimToken = animToken;
                            mLastEnterAnimParams = animLp;
                        } else if (mLastEnterAnimParams != null) {
                            animLp = mLastEnterAnimParams;
                            mLastEnterAnimToken = null;
                            mLastEnterAnimParams = null;
                        }

                        // If all closing windows are obscured, then there is
                        // no need to do an animation.  This is the case, for
                        // example, when this transition is being done behind
                        // the lock screen.
                        if (!mPolicy.allowAppAnimationsLw()) {
                            animLp = null;
                        }
                        
                        NN = mOpeningApps.size();
                        for (i=0; i<NN; i++) {
                            AppWindowToken wtoken = mOpeningApps.get(i);
                            if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                    "Now opening app" + wtoken);
                            wtoken.reportedVisible = false;
                            wtoken.inPendingTransaction = false;
                            wtoken.animation = null;
                            setTokenVisibilityLocked(wtoken, animLp, true, transit, false);
                            wtoken.updateReportedVisibilityLocked();
                            wtoken.waitingToShow = false;
                            wtoken.showAllWindowsLocked();
                        }
                        NN = mClosingApps.size();
                        for (i=0; i<NN; i++) {
                            AppWindowToken wtoken = mClosingApps.get(i);
                            if (DEBUG_APP_TRANSITIONS) Slog.v(TAG,
                                    "Now closing app" + wtoken);
                            wtoken.inPendingTransaction = false;
                            wtoken.animation = null;
                            setTokenVisibilityLocked(wtoken, animLp, false, transit, false);
                            wtoken.updateReportedVisibilityLocked();
                            wtoken.waitingToHide = false;
                            // Force the allDrawn flag, because we want to start
                            // this guy's animations regardless of whether it's
                            // gotten drawn.
                            wtoken.allDrawn = true;
                        }

                        mNextAppTransitionPackage = null;

                        mOpeningApps.clear();
                        mClosingApps.clear();

                        // This has changed the visibility of windows, so perform
                        // a new layout to get them all up-to-date.
                        changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_LAYOUT;
                        mLayoutNeeded = true;
                        if (!moveInputMethodWindowsIfNeededLocked(true)) {
                            assignLayersLocked();
                        }
                        updateFocusedWindowLocked(UPDATE_FOCUS_PLACING_SURFACES);
                        mFocusMayChange = false;
                    }
                }

                int adjResult = 0;

                if (!animating && mAppTransitionRunning) {
                    // We have finished the animation of an app transition.  To do
                    // this, we have delayed a lot of operations like showing and
                    // hiding apps, moving apps in Z-order, etc.  The app token list
                    // reflects the correct Z-order, but the window list may now
                    // be out of sync with it.  So here we will just rebuild the
                    // entire app window list.  Fun!
                    mAppTransitionRunning = false;
                    // Clear information about apps that were moving.
                    mToBottomApps.clear();

                    rebuildAppWindowListLocked();
                    changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_LAYOUT;
                    adjResult |= ADJUST_WALLPAPER_LAYERS_CHANGED;
                    moveInputMethodWindowsIfNeededLocked(false);
                    wallpaperMayChange = true;
                    // Since the window list has been rebuilt, focus might
                    // have to be recomputed since the actual order of windows
                    // might have changed again.
                    mFocusMayChange = true;
                }

                if (wallpaperForceHidingChanged && changes == 0 && !mAppTransitionReady) {
                    // At this point, there was a window with a wallpaper that
                    // was force hiding other windows behind it, but now it
                    // is going away.  This may be simple -- just animate
                    // away the wallpaper and its window -- or it may be
                    // hard -- the wallpaper now needs to be shown behind
                    // something that was hidden.
                    WindowState oldWallpaper = mWallpaperTarget;
                    if (mLowerWallpaperTarget != null
                            && mLowerWallpaperTarget.mAppToken != null) {
                        if (DEBUG_WALLPAPER) Slog.v(TAG,
                                "wallpaperForceHiding changed with lower="
                                + mLowerWallpaperTarget);
                        if (DEBUG_WALLPAPER) Slog.v(TAG,
                                "hidden=" + mLowerWallpaperTarget.mAppToken.hidden +
                                " hiddenRequested=" + mLowerWallpaperTarget.mAppToken.hiddenRequested);
                        if (mLowerWallpaperTarget.mAppToken.hidden) {
                            // The lower target has become hidden before we
                            // actually started the animation...  let's completely
                            // re-evaluate everything.
                            mLowerWallpaperTarget = mUpperWallpaperTarget = null;
                            changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_ANIM;
                        }
                    }
                    adjResult |= adjustWallpaperWindowsLocked();
                    wallpaperMayChange = false;
                    wallpaperForceHidingChanged = false;
                    if (DEBUG_WALLPAPER) Slog.v(TAG, "****** OLD: " + oldWallpaper
                            + " NEW: " + mWallpaperTarget
                            + " LOWER: " + mLowerWallpaperTarget);
                    if (mLowerWallpaperTarget == null) {
                        // Whoops, we don't need a special wallpaper animation.
                        // Clear them out.
                        forceHiding = false;
                        for (i=N-1; i>=0; i--) {
                            WindowState w = mWindows.get(i);
                            if (w.mSurface != null) {
                                final WindowManager.LayoutParams attrs = w.mAttrs;
                                if (mPolicy.doesForceHide(w, attrs) && w.isVisibleLw()) {
                                    if (DEBUG_FOCUS) Slog.i(TAG, "win=" + w + " force hides other windows");
                                    forceHiding = true;
                                } else if (mPolicy.canBeForceHidden(w, attrs)) {
                                    if (!w.mAnimating) {
                                        // We set the animation above so it
                                        // is not yet running.
                                        w.clearAnimation();
                                    }
                                }
                            }
                        }
                    }
                }

                if (wallpaperMayChange) {
                    if (DEBUG_WALLPAPER) Slog.v(TAG,
                            "Wallpaper may change!  Adjusting");
                    adjResult |= adjustWallpaperWindowsLocked();
                }

                if ((adjResult&ADJUST_WALLPAPER_LAYERS_CHANGED) != 0) {
                    if (DEBUG_WALLPAPER) Slog.v(TAG,
                            "Wallpaper layer changed: assigning layers + relayout");
                    changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_LAYOUT;
                    assignLayersLocked();
                } else if ((adjResult&ADJUST_WALLPAPER_VISIBILITY_CHANGED) != 0) {
                    if (DEBUG_WALLPAPER) Slog.v(TAG,
                            "Wallpaper visibility changed: relayout");
                    changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_LAYOUT;
                }

                if (mFocusMayChange) {
                    mFocusMayChange = false;
                    if (updateFocusedWindowLocked(UPDATE_FOCUS_PLACING_SURFACES)) {
                        changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_ANIM;
                        adjResult = 0;
                    }
                }

                if (mLayoutNeeded) {
                    changes |= PhoneWindowManager.FINISH_LAYOUT_REDO_LAYOUT;
                }

                if (DEBUG_APP_TRANSITIONS) Slog.v(TAG, "*** ANIM STEP: changes=0x"
                        + Integer.toHexString(changes));
                
                mInputMonitor.updateInputWindowsLw();
            } while (changes != 0);

            // THIRD LOOP: Update the surfaces of all windows.

            final boolean someoneLosingFocus = mLosingFocus.size() != 0;

            boolean obscured = false;
            boolean blurring = false;
            boolean dimming = false;
            boolean covered = false;
            boolean syswin = false;
            boolean backgroundFillerShown = false;

            final int N = mWindows.size();

            for (i=N-1; i>=0; i--) {
                WindowState w = mWindows.get(i);

                boolean displayed = false;
                final WindowManager.LayoutParams attrs = w.mAttrs;
                final int attrFlags = attrs.flags;

                if (w.mSurface != null) {
                    // XXX NOTE: The logic here could be improved.  We have
                    // the decision about whether to resize a window separated
                    // from whether to hide the surface.  This can cause us to
                    // resize a surface even if we are going to hide it.  You
                    // can see this by (1) holding device in landscape mode on
                    // home screen; (2) tapping browser icon (device will rotate
                    // to landscape; (3) tap home.  The wallpaper will be resized
                    // in step 2 but then immediately hidden, causing us to
                    // have to resize and then redraw it again in step 3.  It
                    // would be nice to figure out how to avoid this, but it is
                    // difficult because we do need to resize surfaces in some
                    // cases while they are hidden such as when first showing a
                    // window.
                    
                    w.computeShownFrameLocked();
                    if (localLOGV) Slog.v(
                            TAG, "Placing surface #" + i + " " + w.mSurface
                            + ": new=" + w.mShownFrame + ", old="
                            + w.mLastShownFrame);

                    boolean resize;
                    int width, height;
                    if ((w.mAttrs.flags & w.mAttrs.FLAG_SCALED) != 0) {
                        resize = w.mLastRequestedWidth != w.mRequestedWidth ||
                        w.mLastRequestedHeight != w.mRequestedHeight;
                        // for a scaled surface, we just want to use
                        // the requested size.
                        width  = w.mRequestedWidth;
                        height = w.mRequestedHeight;
                        w.mLastRequestedWidth = width;
                        w.mLastRequestedHeight = height;
                        w.mLastShownFrame.set(w.mShownFrame);
                        try {
                            if (SHOW_TRANSACTIONS) logSurface(w,
                                    "POS " + w.mShownFrame.left
                                    + ", " + w.mShownFrame.top, null);
                            w.mSurfaceX = w.mShownFrame.left;
                            w.mSurfaceY = w.mShownFrame.top;
                            w.mSurface.setPosition(w.mShownFrame.left, w.mShownFrame.top);
                        } catch (RuntimeException e) {
                            Slog.w(TAG, "Error positioning surface in " + w, e);
                            if (!recoveringMemory) {
                                reclaimSomeSurfaceMemoryLocked(w, "position");
                            }
                        }
                    } else {
                        resize = !w.mLastShownFrame.equals(w.mShownFrame);
                        width = w.mShownFrame.width();
                        height = w.mShownFrame.height();
                        w.mLastShownFrame.set(w.mShownFrame);
                    }

                    if (resize) {
                        if (width < 1) width = 1;
                        if (height < 1) height = 1;
                        if (w.mSurface != null) {
                            try {
                                if (SHOW_TRANSACTIONS) logSurface(w,
                                        "POS " + w.mShownFrame.left + ","
                                        + w.mShownFrame.top + " SIZE "
                                        + w.mShownFrame.width() + "x"
                                        + w.mShownFrame.height(), null);
                                w.mSurfaceResized = true;
                                w.mSurfaceW = width;
                                w.mSurfaceH = height;
                                w.mSurface.setSize(width, height);
                                w.mSurfaceX = w.mShownFrame.left;
                                w.mSurfaceY = w.mShownFrame.top;
                                w.mSurface.setPosition(w.mShownFrame.left,
                                        w.mShownFrame.top);
                            } catch (RuntimeException e) {
                                // If something goes wrong with the surface (such
                                // as running out of memory), don't take down the
                                // entire system.
                                Slog.e(TAG, "Failure updating surface of " + w
                                        + "size=(" + width + "x" + height
                                        + "), pos=(" + w.mShownFrame.left
                                        + "," + w.mShownFrame.top + ")", e);
                                if (!recoveringMemory) {
                                    reclaimSomeSurfaceMemoryLocked(w, "size");
                                }
                            }
                        }
                    }
                    if (!w.mAppFreezing && w.mLayoutSeq == mLayoutSeq) {
                        w.mContentInsetsChanged =
                            !w.mLastContentInsets.equals(w.mContentInsets);
                        w.mVisibleInsetsChanged =
                            !w.mLastVisibleInsets.equals(w.mVisibleInsets);
                        boolean configChanged =
                            w.mConfiguration != mCurConfiguration
                            && (w.mConfiguration == null
                                    || mCurConfiguration.diff(w.mConfiguration) != 0);
                        if (DEBUG_CONFIGURATION && configChanged) {
                            Slog.v(TAG, "Win " + w + " config changed: "
                                    + mCurConfiguration);
                        }
                        if (localLOGV) Slog.v(TAG, "Resizing " + w
                                + ": configChanged=" + configChanged
                                + " last=" + w.mLastFrame + " frame=" + w.mFrame);
                        if (!w.mLastFrame.equals(w.mFrame)
                                || w.mContentInsetsChanged
                                || w.mVisibleInsetsChanged
                                || w.mSurfaceResized
                                || configChanged) {
                            w.mLastFrame.set(w.mFrame);
                            w.mLastContentInsets.set(w.mContentInsets);
                            w.mLastVisibleInsets.set(w.mVisibleInsets);
                            // If the screen is currently frozen, then keep
                            // it frozen until this window draws at its new
                            // orientation.
                            if (mDisplayFrozen) {
                                if (DEBUG_ORIENTATION) Slog.v(TAG,
                                        "Resizing while display frozen: " + w);
                                w.mOrientationChanging = true;
                                if (!mWindowsFreezingScreen) {
                                    mWindowsFreezingScreen = true;
                                    // XXX should probably keep timeout from
                                    // when we first froze the display.
                                    mH.removeMessages(H.WINDOW_FREEZE_TIMEOUT);
                                    mH.sendMessageDelayed(mH.obtainMessage(
                                            H.WINDOW_FREEZE_TIMEOUT), 2000);
                                }
                            }
                            // If the orientation is changing, then we need to
                            // hold off on unfreezing the display until this
                            // window has been redrawn; to do that, we need
                            // to go through the process of getting informed
                            // by the application when it has finished drawing.
                            if (w.mOrientationChanging) {
                                if (DEBUG_ORIENTATION) Slog.v(TAG,
                                        "Orientation start waiting for draw in "
                                        + w + ", surface " + w.mSurface);
                                w.mDrawPending = true;
                                w.mCommitDrawPending = false;
                                w.mReadyToShow = false;
                                if (w.mAppToken != null) {
                                    w.mAppToken.allDrawn = false;
                                }
                            }
                            if (DEBUG_RESIZE || DEBUG_ORIENTATION) Slog.v(TAG,
                                    "Resizing window " + w + " to " + w.mFrame);
                            mResizingWindows.add(w);
                        } else if (w.mOrientationChanging) {
                            if (!w.mDrawPending && !w.mCommitDrawPending) {
                                if (DEBUG_ORIENTATION) Slog.v(TAG,
                                        "Orientation not waiting for draw in "
                                        + w + ", surface " + w.mSurface);
                                w.mOrientationChanging = false;
                            }
                        }
                    }

                    if (w.mAttachedHidden || !w.isReadyForDisplay()) {
                        if (!w.mLastHidden) {
                            //dump();
                            if (DEBUG_CONFIGURATION) Slog.v(TAG, "Window hiding: waitingToShow="
                                    + w.mRootToken.waitingToShow + " polvis="
                                    + w.mPolicyVisibility + " atthid="
                                    + w.mAttachedHidden + " tokhid="
                                    + w.mRootToken.hidden + " vis="
                                    + w.mViewVisibility);
                            w.mLastHidden = true;
                            if (SHOW_TRANSACTIONS) logSurface(w,
                                    "HIDE (performLayout)", null);
                            if (w.mSurface != null) {
                                w.mSurfaceShown = false;
                                try {
                                    w.mSurface.hide();
                                } catch (RuntimeException e) {
                                    Slog.w(TAG, "Exception hiding surface in " + w);
                                }
                            }
                        }
                        // If we are waiting for this window to handle an
                        // orientation change, well, it is hidden, so
                        // doesn't really matter.  Note that this does
                        // introduce a potential glitch if the window
                        // becomes unhidden before it has drawn for the
                        // new orientation.
                        if (w.mOrientationChanging) {
                            w.mOrientationChanging = false;
                            if (DEBUG_ORIENTATION) Slog.v(TAG,
                                    "Orientation change skips hidden " + w);
                        }
                    } else if (w.mLastLayer != w.mAnimLayer
                            || w.mLastAlpha != w.mShownAlpha
                            || w.mLastDsDx != w.mDsDx
                            || w.mLastDtDx != w.mDtDx
                            || w.mLastDsDy != w.mDsDy
                            || w.mLastDtDy != w.mDtDy
                            || w.mLastHScale != w.mHScale
                            || w.mLastVScale != w.mVScale
                            || w.mLastHidden) {
                        displayed = true;
                        w.mLastAlpha = w.mShownAlpha;
                        w.mLastLayer = w.mAnimLayer;
                        w.mLastDsDx = w.mDsDx;
                        w.mLastDtDx = w.mDtDx;
                        w.mLastDsDy = w.mDsDy;
                        w.mLastDtDy = w.mDtDy;
                        w.mLastHScale = w.mHScale;
                        w.mLastVScale = w.mVScale;
                        if (SHOW_TRANSACTIONS) logSurface(w,
                                "alpha=" + w.mShownAlpha + " layer=" + w.mAnimLayer
                                + " matrix=[" + (w.mDsDx*w.mHScale)
                                + "," + (w.mDtDx*w.mVScale)
                                + "][" + (w.mDsDy*w.mHScale)
                                + "," + (w.mDtDy*w.mVScale) + "]", null);
                        if (w.mSurface != null) {
                            try {
                                w.mSurfaceAlpha = w.mShownAlpha;
                                w.mSurface.setAlpha(w.mShownAlpha);
                                w.mSurfaceLayer = w.mAnimLayer;
                                w.mSurface.setLayer(w.mAnimLayer);
                                w.mSurface.setMatrix(
                                        w.mDsDx*w.mHScale, w.mDtDx*w.mVScale,
                                        w.mDsDy*w.mHScale, w.mDtDy*w.mVScale);
                            } catch (RuntimeException e) {
                                Slog.w(TAG, "Error updating surface in " + w, e);
                                if (!recoveringMemory) {
                                    reclaimSomeSurfaceMemoryLocked(w, "update");
                                }
                            }
                        }

                        if (w.mLastHidden && !w.mDrawPending
                                && !w.mCommitDrawPending
                                && !w.mReadyToShow) {
                            if (SHOW_TRANSACTIONS) logSurface(w,
                                    "SHOW (performLayout)", null);
                            if (DEBUG_VISIBILITY) Slog.v(TAG, "Showing " + w
                                    + " during relayout");
                            if (showSurfaceRobustlyLocked(w)) {
                                w.mHasDrawn = true;
                                w.mLastHidden = false;
                            } else {
                                w.mOrientationChanging = false;
                            }
                        }
                        if (w.mSurface != null) {
                            w.mToken.hasVisible = true;
                        }
                    } else {
                        displayed = true;
                    }

                    if (displayed) {
                        if (!covered) {
                            if (attrs.width == LayoutParams.MATCH_PARENT
                                    && attrs.height == LayoutParams.MATCH_PARENT) {
                                covered = true;
                            }
                        }
                        if (w.mOrientationChanging) {
                            if (w.mDrawPending || w.mCommitDrawPending) {
                                orientationChangeComplete = false;
                                if (DEBUG_ORIENTATION) Slog.v(TAG,
                                        "Orientation continue waiting for draw in " + w);
                            } else {
                                w.mOrientationChanging = false;
                                if (DEBUG_ORIENTATION) Slog.v(TAG,
                                        "Orientation change complete in " + w);
                            }
                        }
                        w.mToken.hasVisible = true;
                    }
                } else if (w.mOrientationChanging) {
                    if (DEBUG_ORIENTATION) Slog.v(TAG,
                            "Orientation change skips hidden " + w);
                    w.mOrientationChanging = false;
                }

                final boolean canBeSeen = w.isDisplayedLw();

                if (someoneLosingFocus && w == mCurrentFocus && canBeSeen) {
                    focusDisplayed = true;
                }

                final boolean obscuredChanged = w.mObscured != obscured;

                // Update effect.
                if (!(w.mObscured=obscured)) {
                    if (w.mSurface != null) {
                        if ((attrFlags&FLAG_KEEP_SCREEN_ON) != 0) {
                            holdScreen = w.mSession;
                        }
                        if (!syswin && w.mAttrs.screenBrightness >= 0
                                && screenBrightness < 0) {
                            screenBrightness = w.mAttrs.screenBrightness;
                        }
                        if (!syswin && w.mAttrs.buttonBrightness >= 0
                                && buttonBrightness < 0) {
                            buttonBrightness = w.mAttrs.buttonBrightness;
                        }
                        if (canBeSeen
                                && (attrs.type == WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG
                                 || attrs.type == WindowManager.LayoutParams.TYPE_KEYGUARD
                                 || attrs.type == WindowManager.LayoutParams.TYPE_SYSTEM_ERROR)) {
                            syswin = true;
                        }
                    }

                    boolean opaqueDrawn = canBeSeen && w.isOpaqueDrawn();
                    if (opaqueDrawn && w.isFullscreen(dw, dh)) {
                        // This window completely covers everything behind it,
                        // so we want to leave all of them as unblurred (for
                        // performance reasons).
                        obscured = true;
                    } else if (opaqueDrawn && w.needsBackgroundFiller(dw, dh)) {
                        if (SHOW_TRANSACTIONS) Slog.d(TAG, "showing background filler");
                        // This window is in compatibility mode, and needs background filler.
                        obscured = true;
                        if (mBackgroundFillerSurface == null) {
                            try {
                                mBackgroundFillerSurface = new Surface(mFxSession, 0,
                                        "BackGroundFiller",
                                        0, dw, dh,
                                        PixelFormat.OPAQUE,
                                        Surface.FX_SURFACE_NORMAL);
                            } catch (Exception e) {
                                Slog.e(TAG, "Exception creating filler surface", e);
                            }
                        }
                        try {
                            mBackgroundFillerSurface.setPosition(0, 0);
                            mBackgroundFillerSurface.setSize(dw, dh);
                            // Using the same layer as Dim because they will never be shown at the
                            // same time.
                            mBackgroundFillerSurface.setLayer(w.mAnimLayer - 1);
                            mBackgroundFillerSurface.show();
                        } catch (RuntimeException e) {
                            Slog.e(TAG, "Exception showing filler surface");
                        }
                        backgroundFillerShown = true;
                        mBackgroundFillerShown = true;
                    } else if (canBeSeen && !obscured &&
                            (attrFlags&FLAG_BLUR_BEHIND|FLAG_DIM_BEHIND) != 0) {
                        if (localLOGV) Slog.v(TAG, "Win " + w
                                + ": blurring=" + blurring
                                + " obscured=" + obscured
                                + " displayed=" + displayed);
                        if ((attrFlags&FLAG_DIM_BEHIND) != 0) {
                            if (!dimming) {
                                //Slog.i(TAG, "DIM BEHIND: " + w);
                                dimming = true;
                                if (mDimAnimator == null) {
                                    mDimAnimator = new DimAnimator(mFxSession);
                                }
                                mDimAnimator.show(dw, dh);
                                mDimAnimator.updateParameters(w, currentTime);
                            }
                        }
                        if ((attrFlags&FLAG_BLUR_BEHIND) != 0) {
                            if (!blurring) {
                                //Slog.i(TAG, "BLUR BEHIND: " + w);
                                blurring = true;
                                if (mBlurSurface == null) {
                                    if (SHOW_TRANSACTIONS) Slog.i(TAG, "  BLUR "
                                            + mBlurSurface + ": CREATE");
                                    try {
                                        mBlurSurface = new Surface(mFxSession, 0,
                                                "BlurSurface",
                                                -1, 16, 16,
                                                PixelFormat.OPAQUE,
                                                Surface.FX_SURFACE_BLUR);
                                    } catch (Exception e) {
                                        Slog.e(TAG, "Exception creating Blur surface", e);
                                    }
                                }
                                if (mBlurSurface != null) {
                                    if (SHOW_TRANSACTIONS) Slog.i(TAG, "  BLUR "
                                            + mBlurSurface + ": pos=(0,0) (" +
                                            dw + "x" + dh + "), layer=" + (w.mAnimLayer-1));
                                    mBlurSurface.setPosition(0, 0);
                                    mBlurSurface.setSize(dw, dh);
                                    mBlurSurface.setLayer(w.mAnimLayer-2);
                                    if (!mBlurShown) {
                                        try {
                                            if (SHOW_TRANSACTIONS) Slog.i(TAG, "  BLUR "
                                                    + mBlurSurface + ": SHOW");
                                            mBlurSurface.show();
                                        } catch (RuntimeException e) {
                                            Slog.w(TAG, "Failure showing blur surface", e);
                                        }
                                        mBlurShown = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (obscuredChanged && mWallpaperTarget == w) {
                    // This is the wallpaper target and its obscured state
                    // changed... make sure the current wallaper's visibility
                    // has been updated accordingly.
                    updateWallpaperVisibilityLocked();
                }
            }

            if (backgroundFillerShown == false && mBackgroundFillerShown) {
                mBackgroundFillerShown = false;
                if (SHOW_TRANSACTIONS) Slog.d(TAG, "hiding background filler");
                try {
                    mBackgroundFillerSurface.hide();
                } catch (RuntimeException e) {
                    Slog.e(TAG, "Exception hiding filler surface", e);
                }
            }

            if (mDimAnimator != null && mDimAnimator.mDimShown) {
                animating |= mDimAnimator.updateSurface(dimming, currentTime,
                        mDisplayFrozen || !mPolicy.isScreenOn());
            }

            if (!blurring && mBlurShown) {
                if (SHOW_TRANSACTIONS) Slog.i(TAG, "  BLUR " + mBlurSurface
                        + ": HIDE");
                try {
                    mBlurSurface.hide();
                } catch (IllegalArgumentException e) {
                    Slog.w(TAG, "Illegal argument exception hiding blur surface");
                }
                mBlurShown = false;
            }

            if (SHOW_TRANSACTIONS) Slog.i(TAG, "<<< CLOSE TRANSACTION");
        } catch (RuntimeException e) {
            Slog.e(TAG, "Unhandled exception in Window Manager", e);
        }

        mInputMonitor.updateInputWindowsLw();
        
        Surface.closeTransaction();

        if (mWatermark != null) {
            mWatermark.drawIfNeeded();
        }

        if (DEBUG_ORIENTATION && mDisplayFrozen) Slog.v(TAG,
                "With display frozen, orientationChangeComplete="
                + orientationChangeComplete);
        if (orientationChangeComplete) {
            if (mWindowsFreezingScreen) {
                mWindowsFreezingScreen = false;
                mH.removeMessages(H.WINDOW_FREEZE_TIMEOUT);
            }
            stopFreezingDisplayLocked();
        }

        i = mResizingWindows.size();
        if (i > 0) {
            do {
                i--;
                WindowState win = mResizingWindows.get(i);
                try {
                    if (DEBUG_RESIZE || DEBUG_ORIENTATION) Slog.v(TAG,
                            "Reporting new frame to " + win + ": " + win.mFrame);
                    int diff = 0;
                    boolean configChanged =
                        win.mConfiguration != mCurConfiguration
                        && (win.mConfiguration == null
                                || (diff=mCurConfiguration.diff(win.mConfiguration)) != 0);
                    if ((DEBUG_RESIZE || DEBUG_ORIENTATION || DEBUG_CONFIGURATION)
                            && configChanged) {
                        Slog.i(TAG, "Sending new config to window " + win + ": "
                                + win.mFrame.width() + "x" + win.mFrame.height()
                                + " / " + mCurConfiguration + " / 0x"
                                + Integer.toHexString(diff));
                    }
                    win.mConfiguration = mCurConfiguration;
                    win.mClient.resized(win.mFrame.width(),
                            win.mFrame.height(), win.mLastContentInsets,
                            win.mLastVisibleInsets, win.mDrawPending,
                            configChanged ? win.mConfiguration : null);
                    win.mContentInsetsChanged = false;
                    win.mVisibleInsetsChanged = false;
                    win.mSurfaceResized = false;
                } catch (RemoteException e) {
                    win.mOrientationChanging = false;
                }
            } while (i > 0);
            mResizingWindows.clear();
        }

        // Destroy the surface of any windows that are no longer visible.
        boolean wallpaperDestroyed = false;
        i = mDestroySurface.size();
        if (i > 0) {
            do {
                i--;
                WindowState win = mDestroySurface.get(i);
                win.mDestroying = false;
                if (mInputMethodWindow == win) {
                    mInputMethodWindow = null;
                }
                if (win == mWallpaperTarget) {
                    wallpaperDestroyed = true;
                }
                win.destroySurfaceLocked();
            } while (i > 0);
            mDestroySurface.clear();
        }

        // Time to remove any exiting tokens?
        for (i=mExitingTokens.size()-1; i>=0; i--) {
            WindowToken token = mExitingTokens.get(i);
            if (!token.hasVisible) {
                mExitingTokens.remove(i);
                if (token.windowType == TYPE_WALLPAPER) {
                    mWallpaperTokens.remove(token);
                }
            }
        }

        // Time to remove any exiting applications?
        for (i=mExitingAppTokens.size()-1; i>=0; i--) {
            AppWindowToken token = mExitingAppTokens.get(i);
            if (!token.hasVisible && !mClosingApps.contains(token)) {
                // Make sure there is no animation running on this token,
                // so any windows associated with it will be removed as
                // soon as their animations are complete
                token.animation = null;
                token.animating = false;
                mAppTokens.remove(token);
                mExitingAppTokens.remove(i);
                if (mLastEnterAnimToken == token) {
                    mLastEnterAnimToken = null;
                    mLastEnterAnimParams = null;
                }
            }
        }

        boolean needRelayout = false;

        if (!animating && mAppTransitionRunning) {
            // We have finished the animation of an app transition.  To do
            // this, we have delayed a lot of operations like showing and
            // hiding apps, moving apps in Z-order, etc.  The app token list
            // reflects the correct Z-order, but the window list may now
            // be out of sync with it.  So here we will just rebuild the
            // entire app window list.  Fun!
            mAppTransitionRunning = false;
            needRelayout = true;
            rebuildAppWindowListLocked();
            assignLayersLocked();
            // Clear information about apps that were moving.
            mToBottomApps.clear();
        }

        if (focusDisplayed) {
            mH.sendEmptyMessage(H.REPORT_LOSING_FOCUS);
        }
        if (wallpaperDestroyed) {
            needRelayout = adjustWallpaperWindowsLocked() != 0;
        }
        if (needRelayout) {
            requestAnimationLocked(0);
        } else if (animating) {
            requestAnimationLocked(currentTime+(1000/60)-SystemClock.uptimeMillis());
        }
        
        mInputMonitor.updateInputWindowsLw();
        
        if (DEBUG_FREEZE) Slog.v(TAG, "Layout: mDisplayFrozen=" + mDisplayFrozen
                + " holdScreen=" + holdScreen);
        if (!mDisplayFrozen) {
            setHoldScreenLocked(holdScreen != null);
            if (screenBrightness < 0 || screenBrightness > 1.0f) {
                mPowerManager.setScreenBrightnessOverride(-1);
            } else {
                mPowerManager.setScreenBrightnessOverride((int)
                        (screenBrightness * Power.BRIGHTNESS_ON));
            }
            if (buttonBrightness < 0 || buttonBrightness > 1.0f) {
                mPowerManager.setButtonBrightnessOverride(-1);
            } else {
                mPowerManager.setButtonBrightnessOverride((int)
                        (buttonBrightness * Power.BRIGHTNESS_ON));
            }
            if (holdScreen != mHoldingScreenOn) {
                mHoldingScreenOn = holdScreen;
                Message m = mH.obtainMessage(H.HOLD_SCREEN_CHANGED, holdScreen);
                mH.sendMessage(m);
            }
        }

        if (mTurnOnScreen) {
            if (DEBUG_VISIBILITY) Slog.v(TAG, "Turning screen on after layout!");
            mPowerManager.userActivity(SystemClock.uptimeMillis(), false,
                    LocalPowerManager.BUTTON_EVENT, true);
            mTurnOnScreen = false;
        }
        
        // Check to see if we are now in a state where the screen should
        // be enabled, because the window obscured flags have changed.
        enableScreenIfNeededLocked();
    }    
    
}    
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
```java

```
```java

```





