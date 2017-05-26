# Android系统应用框架篇：Window的创建、显示与销毁流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作。技术栈主要涉及Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative，数据结构与算法等方面。热爱编程与吉他，喜欢有趣的事物和人。

**关于文章**

>作者的文章首发在[Github](https://github.com/guoxiaoxing)上，也会发在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与[CSDN](http://blog.csdn.net/allenwells)平台上，文章内容主要包含Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative, 数据结构与算法等方面的内容。如果有什么问题，也欢迎发邮件与我交流。

Window的添加与移除都是由WindowManagerService来管理。

## 添加窗口

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
    
                //为所有的窗口分配最终额排列次序
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

这个函数主要做了3件事情：

```
1 检查窗口权限，没有权限的客户端不能添加窗口
2 根据客户端的attrs.token取出已经注册的WindowToken
3 WindowMangerService要为添加的窗口创建一个WindowState对象，这个对象维护了窗口的所有状体信息
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

## 移除窗口

我们接着来看移除窗口的实现

```java
public class WindowManagerService extends IWindowManager.Stub
        implements Watchdog.Monitor {
    
    public void removeWindow(Session session, IWindow client) {
            synchronized(mWindowMap) {
                //根据session与client描述当前窗口的WindowState
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null) {
                    return;
                }
                //根据Session与WindowState移除该窗口
                removeWindowLocked(session, win);
            }
        }
    
        public void removeWindowLocked(Session session, WindowState win) {
    
            if (localLOGV || DEBUG_FOCUS) Slog.v(
                TAG, "Remove " + win + " client="
                + Integer.toHexString(System.identityHashCode(
                    win.mClient.asBinder()))
                + ", surface=" + win.mSurface);
    
            final long origId = Binder.clearCallingIdentity();
            
            win.disposeInputChannel();
    
            if (DEBUG_APP_TRANSITIONS) Slog.v(
                    TAG, "Remove " + win + ": mSurface=" + win.mSurface
                    + " mExiting=" + win.mExiting
                    + " isAnimating=" + win.isAnimating()
                    + " app-animation="
                    + (win.mAppToken != null ? win.mAppToken.animation : null)
                    + " inPendingTransaction="
                    + (win.mAppToken != null ? win.mAppToken.inPendingTransaction : false)
                    + " mDisplayFrozen=" + mDisplayFrozen);
            // Visibility of the removed window. Will be used later to update orientation later on.
            boolean wasVisible = false;
            
            //1 检查移除窗口前是否有退出动画，如果有，则等播放完动画再移除该窗口
            // First, see if we need to run an animation.  If we do, we have
            // to hold off on removing the window until the animation is done.
            // If the display is frozen, just remove immediately, since the
            // animation wouldn't be seen.
            if (win.mSurface != null && !mDisplayFrozen && mPolicy.isScreenOn()) {
                // If we are not currently running the exit animation, we
                // need to see about starting one.
                if (wasVisible=win.isWinVisibleLw()) {
    
                    int transit = WindowManagerPolicy.TRANSIT_EXIT;
                    if (win.getAttrs().type == TYPE_APPLICATION_STARTING) {
                        transit = WindowManagerPolicy.TRANSIT_PREVIEW_DONE;
                    }
                    //播放动画
                    // Try starting an animation.
                    if (applyAnimationLocked(win, transit, false)) {
                        win.mExiting = true;
                    }
                }
                //当前动画正在播放，需要等待动画播放完毕
                if (win.mExiting || win.isAnimating()) {
                    // The exit animation is running... wait for it!
                    //Slog.i(TAG, "*** Running exit animation...");
                    win.mExiting = true;
                    win.mRemoveOnExit = true;
                    mLayoutNeeded = true;
                    updateFocusedWindowLocked(UPDATE_FOCUS_WILL_PLACE_SURFACES);
                    performLayoutAndPlaceSurfacesLocked();
                    if (win.mAppToken != null) {
                        win.mAppToken.updateReportedVisibilityLocked();
                    }
                    //dump();
                    Binder.restoreCallingIdentity(origId);
                    return;
                }
            }
            2 移除当前窗口
            removeWindowInnerLocked(session, win);
            // Removing a visible window will effect the computed orientation
            // So just update orientation if needed.
            if (wasVisible && computeForcedAppOrientationLocked()
                    != mForcedAppOrientation
                    && updateOrientationFromAppTokensLocked()) {
                mH.sendEmptyMessage(H.SEND_NEW_CONFIGURATION);
            }
            //更新窗口焦点
            updateFocusedWindowLocked(UPDATE_FOCUS_NORMAL);
            Binder.restoreCallingIdentity(origId);
        }
    
        private void removeWindowInnerLocked(Session session, WindowState win) {
            win.mRemoved = true;
    
            if (mInputMethodTarget == win) {
                moveInputMethodWindowsIfNeededLocked(false);
            }
    
            if (false) {
                RuntimeException e = new RuntimeException("here");
                e.fillInStackTrace();
                Slog.w(TAG, "Removing window " + win, e);
            }
    
            mPolicy.removeWindowLw(win);
            win.removeLocked();
    
            mWindowMap.remove(win.mClient.asBinder());
            mWindows.remove(win);
            mWindowsChanged = true;
            if (DEBUG_WINDOW_MOVEMENT) Slog.v(TAG, "Final remove of window: " + win);
    
            if (mInputMethodWindow == win) {
                mInputMethodWindow = null;
            } else if (win.mAttrs.type == TYPE_INPUT_METHOD_DIALOG) {
                mInputMethodDialogs.remove(win);
            }
    
            final WindowToken token = win.mToken;
            final AppWindowToken atoken = win.mAppToken;
            token.windows.remove(win);
            if (atoken != null) {
                atoken.allAppWindows.remove(win);
            }
            if (localLOGV) Slog.v(
                    TAG, "**** Removing window " + win + ": count="
                    + token.windows.size());
            if (token.windows.size() == 0) {
                if (!token.explicit) {
                    mTokenMap.remove(token.token);
                    mTokenList.remove(token);
                } else if (atoken != null) {
                    atoken.firstWindowDrawn = false;
                }
            }
    
            if (atoken != null) {
                if (atoken.startingWindow == win) {
                    atoken.startingWindow = null;
                } else if (atoken.allAppWindows.size() == 0 && atoken.startingData != null) {
                    // If this is the last window and we had requested a starting
                    // transition window, well there is no point now.
                    atoken.startingData = null;
                } else if (atoken.allAppWindows.size() == 1 && atoken.startingView != null) {
                    // If this is the last window except for a starting transition
                    // window, we need to get rid of the starting transition.
                    if (DEBUG_STARTING_WINDOW) {
                        Slog.v(TAG, "Schedule remove starting " + token
                                + ": no more real windows");
                    }
                    Message m = mH.obtainMessage(H.REMOVE_STARTING, atoken);
                    mH.sendMessage(m);
                }
            }
    
            if (win.mAttrs.type == TYPE_WALLPAPER) {
                mLastWallpaperTimeoutTime = 0;
                adjustWallpaperWindowsLocked();
            } else if ((win.mAttrs.flags&FLAG_SHOW_WALLPAPER) != 0) {
                adjustWallpaperWindowsLocked();
            }
    
            if (!mInLayout) {
                assignLayersLocked();
                mLayoutNeeded = true;
                performLayoutAndPlaceSurfacesLocked();
                if (win.mAppToken != null) {
                    win.mAppToken.updateReportedVisibilityLocked();
                }
            }
            
            mInputMonitor.updateInputWindowsLw();
        }

}
    
```