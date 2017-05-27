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