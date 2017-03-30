# Android系统应用框架篇：Activity启动流程(二)

作者: 郭孝星
邮箱: guoxiaoxingse@163.com
博客: http://blog.csdn.net/allenwells
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流
的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去
star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时
的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，
更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

我们接着上一篇文章来继续分析Activity的启动流程。

### 18 ActivityManagerService.activityPaused(IBinder token, Bundle icicle)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback{

    public final void activityPaused(IBinder token, Bundle icicle) {
        // Refuse possible leaked file descriptors
        if (icicle != null && icicle.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Bundle");
        }

        final long origId = Binder.clearCallingIdentity();
        mMainStack.activityPaused(token, icicle, false);
        Binder.restoreCallingIdentity(origId);
    }

}
```

IBinder token还是跟前面一样指向ActivityManagerService中与Launcher组件对应的一个ActivityRecord对象。该函数会调用ActivityStack.activityPaused()方法
进一步就处理ACTIVITY_PAUSED_TRANSACTION进程通信请求。

### 19 ActivityStack.activityPaused(IBinder token, Bundle icicle, boolean timeout)

```java
public class ActivityStack {

    final ActivityManagerService mService;

    final void activityPaused(IBinder token, Bundle icicle, boolean timeout) {
        if (DEBUG_PAUSE) Slog.v(
            TAG, "Activity paused: token=" + token + ", icicle=" + icicle
            + ", timeout=" + timeout);

        ActivityRecord r = null;

        synchronized (mService) {
            int index = indexOfTokenLocked(token);
            if (index >= 0) {
                //根据token查找mHistory栈中对应的ActivityRecord对象
                r = (ActivityRecord)mHistory.get(index);
                if (!timeout) {
                    r.icicle = icicle;
                    r.haveState = true;
                }
                //移除PAUSE_TIMEOUT_MSG消息，因为Launcher组件已经在规定的时间内完成ActivityManagerService给
                //它发送的终止通知了。
                mHandler.removeMessages(PAUSE_TIMEOUT_MSG, r);
                if (mPausingActivity == r) {
                    //此时r与mPausingActivity指向的都是Launcher对应的ActivityRecord对象。把Launcher置为
                    //ActivityState.PAUSED状态。
                    r.state = ActivityState.PAUSED;
                    //执行目标Activity的启动操作
                    completePauseLocked();
                } else {
                    EventLog.writeEvent(EventLogTags.AM_FAILED_TO_PAUSE,
                            System.identityHashCode(r), r.shortComponentName,
                            mPausingActivity != null
                                ? mPausingActivity.shortComponentName : "(none)");
                }
            }
        }
    }

}
```

该函数执行了以下操作：

```
1 根据token查找mHistory栈中对应的ActivityRecord对象。
2 移除PAUSE_TIMEOUT_MSG消息，因为Launcher组件已经在规定的时间内完成ActivityManagerService给它发送的终止通知了。
3 把Launcher置为ActivityState.PAUSED状态。
4 调用completePauseLocked()，执行目标Activity的启动操作。
```

### 20 ActivityStack.completePauseLocked()

```java
public class ActivityStack{

     private final void completePauseLocked() {
            ActivityRecord prev = mPausingActivity;
            if (DEBUG_PAUSE) Slog.v(TAG, "Complete pause: " + prev);

            if (prev != null) {
                if (prev.finishing) {
                    if (DEBUG_PAUSE) Slog.v(TAG, "Executing finish of activity: " + prev);
                    prev = finishCurrentActivityLocked(prev, FINISH_AFTER_VISIBLE);
                } else if (prev.app != null) {
                    if (DEBUG_PAUSE) Slog.v(TAG, "Enqueueing pending stop: " + prev);
                    if (prev.waitingVisible) {
                        prev.waitingVisible = false;
                        mWaitingVisibleActivities.remove(prev);
                        if (DEBUG_SWITCH || DEBUG_PAUSE) Slog.v(
                                TAG, "Complete pause, no longer waiting: " + prev);
                    }
                    if (prev.configDestroy) {
                        // The previous is being paused because the configuration
                        // is changing, which means it is actually stopping...
                        // To juggle the fact that we are also starting a new
                        // instance right now, we need to first completely stop
                        // the current instance before starting the new one.
                        if (DEBUG_PAUSE) Slog.v(TAG, "Destroying after pause: " + prev);
                        destroyActivityLocked(prev, true);
                    } else {
                        mStoppingActivities.add(prev);
                        if (mStoppingActivities.size() > 3) {
                            // If we already have a few activities waiting to stop,
                            // then give up on things going idle and start clearing
                            // them out.
                            if (DEBUG_PAUSE) Slog.v(TAG, "To many pending stops, forcing idle");
                            Message msg = Message.obtain();
                            msg.what = IDLE_NOW_MSG;
                            mHandler.sendMessage(msg);
                        }
                    }
                } else {
                    if (DEBUG_PAUSE) Slog.v(TAG, "App died during pause, not stopping: " + prev);
                    prev = null;
                }
                //mPausingActivity置为null，表示当前正在终止的Activity组件已经进入Paused状态了。
                mPausingActivity = null;
            }

            if (!mService.mSleeping && !mService.mShuttingDown) {
                //如果系统没有处于睡眠或关闭状态，则启动位于栈顶的Activity组件。
                resumeTopActivityLocked(prev);
            } else {
                if (mGoingToSleep.isHeld()) {
                    mGoingToSleep.release();
                }
                if (mService.mShuttingDown) {
                    mService.notifyAll();
                }
            }

            if (prev != null) {
                prev.resumeKeyDispatchingLocked();
            }

            if (prev.app != null && prev.cpuTimeAtResume > 0
                    && mService.mBatteryStatsService.isOnBattery()) {
                long diff = 0;
                synchronized (mService.mProcessStatsThread) {
                    diff = mService.mProcessStats.getCpuTimeForPid(prev.app.pid)
                            - prev.cpuTimeAtResume;
                }
                if (diff > 0) {
                    BatteryStatsImpl bsi = mService.mBatteryStatsService.getActiveStatistics();
                    synchronized (bsi) {
                        BatteryStatsImpl.Uid.Proc ps =
                                bsi.getProcessStatsLocked(prev.info.applicationInfo.uid,
                                prev.info.packageName);
                        if (ps != null) {
                            ps.addForegroundTimeLocked(diff);
                        }
                    }
                }
            }
            prev.cpuTimeAtResume = 0; // reset it
        }
}

```
mPausingActivity置为null，表示当前正在终止的Activity组件已经进入Paused状态了，调用resumeTopActivityLocked(prev)启动位于栈顶的Activity组件。

### 21 ActivityStack.resumeTopActivityLocked(ActivityRecord prev)

```java
public class ActivityStack{

      /**
         * Ensure that the top activity in the stack is resumed.
         *
         * @param prev The previously resumed activity, for when in the process
         * of pausing; can be null to call from elsewhere.
         *
         * @return Returns true if something is being resumed, or false if
         * nothing happened.
         */
        final boolean resumeTopActivityLocked(ActivityRecord prev) {

            //栈顶Activity，它指向即将启动的目标Activity组件
            // Find the first activity that is not finishing.
            ActivityRecord next = topRunningActivityLocked(null);

            // Remember how we'll process this pause/resume situation, and ensure
            // that the state is reset however we wind up proceeding.
            final boolean userLeaving = mUserLeaving;
            mUserLeaving = false;

            if (next == null) {
                // There are no more activities!  Let's just start up the
                // Launcher...
                if (mMainStack) {
                    return mService.startHomeActivityLocked();
                }
            }

            next.delayedResume = false;

            // If the top activity is the resumed one, nothing to do.
            if (mResumedActivity == next && next.state == ActivityState.RESUMED) {
                // Make sure we have executed any pending transitions, since there
                // should be nothing left to do at this point.
                mService.mWindowManager.executeAppTransition();
                mNoAnimActivities.clear();
                return false;
            }

            // If we are sleeping, and there is no resumed activity, and the top
            // activity is paused, well that is the state we want.
            if ((mService.mSleeping || mService.mShuttingDown)
                    && mLastPausedActivity == next && next.state == ActivityState.PAUSED) {
                // Make sure we have executed any pending transitions, since there
                // should be nothing left to do at this point.
                mService.mWindowManager.executeAppTransition();
                mNoAnimActivities.clear();
                return false;
            }

            // The activity may be waiting for stop, but that is no longer
            // appropriate for it.
            mStoppingActivities.remove(next);
            mWaitingVisibleActivities.remove(next);

            if (DEBUG_SWITCH) Slog.v(TAG, "Resuming " + next);

            //上一步中已经将mPausingActivity置为null
            // If we are currently pausing an activity, then don't do anything
            // until that is done.
            if (mPausingActivity != null) {
                if (DEBUG_SWITCH) Slog.v(TAG, "Skip resume: pausing=" + mPausingActivity);
                return false;
            }

            // Okay we are now going to start a switch, to 'next'.  We may first
            // have to pause the current activity, but this is an important point
            // where we have decided to go to 'next' so keep track of that.
            // XXX "App Redirected" dialog is getting too many false positives
            // at this point, so turn off for now.
            if (false) {
                if (mLastStartedActivity != null && !mLastStartedActivity.finishing) {
                    long now = SystemClock.uptimeMillis();
                    final boolean inTime = mLastStartedActivity.startTime != 0
                            && (mLastStartedActivity.startTime + START_WARN_TIME) >= now;
                    final int lastUid = mLastStartedActivity.info.applicationInfo.uid;
                    final int nextUid = next.info.applicationInfo.uid;
                    if (inTime && lastUid != nextUid
                            && lastUid != next.launchedFromUid
                            && mService.checkPermission(
                                    android.Manifest.permission.STOP_APP_SWITCHES,
                                    -1, next.launchedFromUid)
                            != PackageManager.PERMISSION_GRANTED) {
                        mService.showLaunchWarningLocked(mLastStartedActivity, next);
                    } else {
                        next.startTime = now;
                        mLastStartedActivity = next;
                    }
                } else {
                    next.startTime = SystemClock.uptimeMillis();
                    mLastStartedActivity = next;
                }
            }

            // We need to start pausing the current activity so the top one
            // can be resumed...
            if (mResumedActivity != null) {
                if (DEBUG_SWITCH) Slog.v(TAG, "Skip resume: need to start pausing");
                startPausingLocked(userLeaving, false);
                return true;
            }

            if (prev != null && prev != next) {
                if (!prev.waitingVisible && next != null && !next.nowVisible) {
                    prev.waitingVisible = true;
                    mWaitingVisibleActivities.add(prev);
                    if (DEBUG_SWITCH) Slog.v(
                            TAG, "Resuming top, waiting visible to hide: " + prev);
                } else {
                    // The next activity is already visible, so hide the previous
                    // activity's windows right now so we can show the new one ASAP.
                    // We only do this if the previous is finishing, which should mean
                    // it is on top of the one being resumed so hiding it quickly
                    // is good.  Otherwise, we want to do the normal route of allowing
                    // the resumed activity to be shown so we can decide if the
                    // previous should actually be hidden depending on whether the
                    // new one is found to be full-screen or not.
                    if (prev.finishing) {
                        mService.mWindowManager.setAppVisibility(prev, false);
                        if (DEBUG_SWITCH) Slog.v(TAG, "Not waiting for visible to hide: "
                                + prev + ", waitingVisible="
                                + (prev != null ? prev.waitingVisible : null)
                                + ", nowVisible=" + next.nowVisible);
                    } else {
                        if (DEBUG_SWITCH) Slog.v(TAG, "Previous already visible but still waiting to hide: "
                            + prev + ", waitingVisible="
                            + (prev != null ? prev.waitingVisible : null)
                            + ", nowVisible=" + next.nowVisible);
                    }
                }
            }

            // We are starting up the next activity, so tell the window manager
            // that the previous one will be hidden soon.  This way it can know
            // to ignore it when computing the desired screen orientation.
            if (prev != null) {
                if (prev.finishing) {
                    if (DEBUG_TRANSITION) Slog.v(TAG,
                            "Prepare close transition: prev=" + prev);
                    if (mNoAnimActivities.contains(prev)) {
                        mService.mWindowManager.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
                    } else {
                        mService.mWindowManager.prepareAppTransition(prev.task == next.task
                                ? WindowManagerPolicy.TRANSIT_ACTIVITY_CLOSE
                                : WindowManagerPolicy.TRANSIT_TASK_CLOSE);
                    }
                    mService.mWindowManager.setAppWillBeHidden(prev);
                    mService.mWindowManager.setAppVisibility(prev, false);
                } else {
                    if (DEBUG_TRANSITION) Slog.v(TAG,
                            "Prepare open transition: prev=" + prev);
                    if (mNoAnimActivities.contains(next)) {
                        mService.mWindowManager.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
                    } else {
                        mService.mWindowManager.prepareAppTransition(prev.task == next.task
                                ? WindowManagerPolicy.TRANSIT_ACTIVITY_OPEN
                                : WindowManagerPolicy.TRANSIT_TASK_OPEN);
                    }
                }
                if (false) {
                    mService.mWindowManager.setAppWillBeHidden(prev);
                    mService.mWindowManager.setAppVisibility(prev, false);
                }
            } else if (mHistory.size() > 1) {
                if (DEBUG_TRANSITION) Slog.v(TAG,
                        "Prepare open transition: no previous");
                if (mNoAnimActivities.contains(next)) {
                    mService.mWindowManager.prepareAppTransition(WindowManagerPolicy.TRANSIT_NONE);
                } else {
                    mService.mWindowManager.prepareAppTransition(WindowManagerPolicy.TRANSIT_ACTIVITY_OPEN);
                }
            }

            if (next.app != null && next.app.thread != null) {
                if (DEBUG_SWITCH) Slog.v(TAG, "Resume running: " + next);

                // This activity is now becoming visible.
                mService.mWindowManager.setAppVisibility(next, true);

                ActivityRecord lastResumedActivity = mResumedActivity;
                ActivityState lastState = next.state;

                mService.updateCpuStats();

                next.state = ActivityState.RESUMED;
                mResumedActivity = next;
                next.task.touchActiveTime();
                mService.updateLruProcessLocked(next.app, true, true);
                updateLRUListLocked(next);

                // Have the window manager re-evaluate the orientation of
                // the screen based on the new activity order.
                boolean updated = false;
                if (mMainStack) {
                    synchronized (mService) {
                        Configuration config = mService.mWindowManager.updateOrientationFromAppTokens(
                                mService.mConfiguration,
                                next.mayFreezeScreenLocked(next.app) ? next : null);
                        if (config != null) {
                            next.frozenBeforeDestroy = true;
                        }
                        updated = mService.updateConfigurationLocked(config, next);
                    }
                }
                if (!updated) {
                    // The configuration update wasn't able to keep the existing
                    // instance of the activity, and instead started a new one.
                    // We should be all done, but let's just make sure our activity
                    // is still at the top and schedule another run if something
                    // weird happened.
                    ActivityRecord nextNext = topRunningActivityLocked(null);
                    if (DEBUG_SWITCH) Slog.i(TAG,
                            "Activity config changed during resume: " + next
                            + ", new next: " + nextNext);
                    if (nextNext != next) {
                        // Do over!
                        mHandler.sendEmptyMessage(RESUME_TOP_ACTIVITY_MSG);
                    }
                    if (mMainStack) {
                        mService.setFocusedActivityLocked(next);
                    }
                    ensureActivitiesVisibleLocked(null, 0);
                    mService.mWindowManager.executeAppTransition();
                    mNoAnimActivities.clear();
                    return true;
                }

                try {
                    // Deliver all pending results.
                    ArrayList a = next.results;
                    if (a != null) {
                        final int N = a.size();
                        if (!next.finishing && N > 0) {
                            if (DEBUG_RESULTS) Slog.v(
                                    TAG, "Delivering results to " + next
                                    + ": " + a);
                            next.app.thread.scheduleSendResult(next, a);
                        }
                    }

                    if (next.newIntents != null) {
                        next.app.thread.scheduleNewIntent(next.newIntents, next);
                    }

                    EventLog.writeEvent(EventLogTags.AM_RESUME_ACTIVITY,
                            System.identityHashCode(next),
                            next.task.taskId, next.shortComponentName);

                    next.app.thread.scheduleResumeActivity(next,
                            mService.isNextTransitionForward());

                    pauseIfSleepingLocked();

                } catch (Exception e) {
                    // Whoops, need to restart this activity!
                    next.state = lastState;
                    mResumedActivity = lastResumedActivity;
                    Slog.i(TAG, "Restarting because process died: " + next);
                    if (!next.hasBeenLaunched) {
                        next.hasBeenLaunched = true;
                    } else {
                        if (SHOW_APP_STARTING_PREVIEW && mMainStack) {
                            mService.mWindowManager.setAppStartingWindow(
                                    next, next.packageName, next.theme,
                                    next.nonLocalizedLabel,
                                    next.labelRes, next.icon, null, true);
                        }
                    }
                    //执行目标Activity启动操作。
                    startSpecificActivityLocked(next, true, false);
                    return true;
                }

                // From this point on, if something goes wrong there is no way
                // to recover the activity.
                try {
                    next.visible = true;
                    completeResumeLocked(next);
                } catch (Exception e) {
                    // If any exception gets thrown, toss away this
                    // activity and try the next one.
                    Slog.w(TAG, "Exception thrown during resume of " + next, e);
                    requestFinishActivityLocked(next, Activity.RESULT_CANCELED, null,
                            "resume-exception");
                    return true;
                }

                // Didn't need to use the icicle, and it is now out of date.
                next.icicle = null;
                next.haveState = false;
                next.stopped = false;

            } else {
                // Whoops, need to restart this activity!
                if (!next.hasBeenLaunched) {
                    next.hasBeenLaunched = true;
                } else {
                    if (SHOW_APP_STARTING_PREVIEW) {
                        mService.mWindowManager.setAppStartingWindow(
                                next, next.packageName, next.theme,
                                next.nonLocalizedLabel,
                                next.labelRes, next.icon, null, true);
                    }
                    if (DEBUG_SWITCH) Slog.v(TAG, "Restarting: " + next);
                }
                startSpecificActivityLocked(next, true, true);
            }

            return true;
        }

}
```

在第10步中，ActivityManagerService已经尝试调用ActivityManagerService.resumeTopActivityLocked()来启动目标Activity组件了，但是
那个时候Launcher组件尚未进入Paused状态（即ActivityStack.mResumedActivity != null）所以先调用ActivityManagerService.startPausingLocked()
来终止Launcher组件。

接下来调用startSpecificActivityLocked(next, true, false)，执行目标Activity启动操作。


### 22 ActivityStack.startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig)

```java
public class ActivityStack{

    private final void startSpecificActivityLocked(ActivityRecord r,
            boolean andResume, boolean checkConfig) {
        // Is this activity's application already running?
        ProcessRecord app = mService.getProcessRecordLocked(r.processName,
                r.info.applicationInfo.uid);

        if (r.launchTime == 0) {
            r.launchTime = SystemClock.uptimeMillis();
            if (mInitialStartTime == 0) {
                mInitialStartTime = r.launchTime;
            }
        } else if (mInitialStartTime == 0) {
            mInitialStartTime = SystemClock.uptimeMillis();
        }

        if (app != null && app.thread != null) {
            try {
                //如果存在，则直接通知该应用进程将该Activity启动起来。
                realStartActivityLocked(r, app, andResume, checkConfig);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting activity "
                        + r.intent.getComponent().flattenToShortString(), e);
            }

            // If a dead object exception was thrown -- fall through to
            // restart the application.
        }

        //如果不存在，则用该Activity的用户ID和进程名称创建一个新的应用进程，再由该进程将该Activity启动起来。
        mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0,
                "activity", r.intent.getComponent(), false);
    }

}
```

在ActivityManagerService中，每个Activity组件都有一个用户ID和一个进程名称。

```
用户ID：安装该Activity时由PackageManagerService分配的。
进程名称：由Activity组件的android:process属性所决定的。
```
ActivityManagerService在启动Activity时会用它的用户ID和进程名称来检查系统中是否存在一个对应的应用进程。

如果存在，则直接通知该应用进程将该Activity启动起来。
如果不存在，则用该Activity的用户ID和进程名称创建一个新的应用进程，再由该进程将该Activity启动起来。

我们这里的目标Activity是第一次启动，所以它会调用startProcessLocked()为目标Activity创建一个新的进程。

### 23 ActivityManagerService.startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback{

    final ProcessRecord startProcessLocked(String processName,
            ApplicationInfo info, boolean knownToBeDead, int intentFlags,
            String hostingType, ComponentName hostingName, boolean allowWhileBooting) {

        //检查请求创建的应用进程是否已经存在，如果已经存在，则不再创建。
        ProcessRecord app = getProcessRecordLocked(processName, info.uid);
        // We don't have to do anything more if:
        // (1) There is an existing application record; and
        // (2) The caller doesn't think it is dead, OR there is no thread
        //     object attached to it so we know it couldn't have crashed; and
        // (3) There is a pid assigned to it, so it is either starting or
        //     already running.
        if (DEBUG_PROCESSES) Slog.v(TAG, "startProcess: name=" + processName
                + " app=" + app + " knownToBeDead=" + knownToBeDead
                + " thread=" + (app != null ? app.thread : null)
                + " pid=" + (app != null ? app.pid : -1));
        if (app != null && app.pid > 0) {
            if (!knownToBeDead || app.thread == null) {
                // We already have the app running, or are waiting for it to
                // come up (we have a pid but not yet its thread), so keep it.
                if (DEBUG_PROCESSES) Slog.v(TAG, "App already running: " + app);
                return app;
            } else {
                // An application record is attached to a previous process,
                // clean it up now.
                if (DEBUG_PROCESSES) Slog.v(TAG, "App died: " + app);
                handleAppDiedLocked(app, true);
            }
        }

        String hostingNameStr = hostingName != null
                ? hostingName.flattenToShortString() : null;

        if ((intentFlags&Intent.FLAG_FROM_BACKGROUND) != 0) {
            // If we are in the background, then check to see if this process
            // is bad.  If so, we will just silently fail.
            if (mBadProcesses.get(info.processName, info.uid) != null) {
                if (DEBUG_PROCESSES) Slog.v(TAG, "Bad process: " + info.uid
                        + "/" + info.processName);
                return null;
            }
        } else {
            // When the user is explicitly starting a process, then clear its
            // crash count so that we won't make it bad until they see at
            // least one crash dialog again, and make the process good again
            // if it had been bad.
            if (DEBUG_PROCESSES) Slog.v(TAG, "Clearing bad process: " + info.uid
                    + "/" + info.processName);
            mProcessCrashTimes.remove(info.processName, info.uid);
            if (mBadProcesses.get(info.processName, info.uid) != null) {
                EventLog.writeEvent(EventLogTags.AM_PROC_GOOD, info.uid,
                        info.processName);
                mBadProcesses.remove(info.processName, info.uid);
                if (app != null) {
                    app.bad = false;
                }
            }
        }

        if (app == null) {
            app = newProcessRecordLocked(null, info, processName);
            mProcessNames.put(processName, info.uid, app);
        } else {
            // If this is a new package in the process, add the package to the list
            app.addPackage(info.packageName);
        }

        // If the system is not ready yet, then hold off on starting this
        // process until it is.
        if (!mProcessesReady
                && !isAllowedWhileBooting(info)
                && !allowWhileBooting) {
            if (!mProcessesOnHold.contains(app)) {
                mProcessesOnHold.add(app);
            }
            if (DEBUG_PROCESSES) Slog.v(TAG, "System not ready, putting on hold: " + app);
            return app;
        }
        //如果不存在，则调用它的重载函数创建一个新的应用进程。
        startProcessLocked(app, hostingType, hostingNameStr);
        return (app.pid != 0) ? app : null;
    }

}

```

该函数会检查请求创建的应用进程是否已经存在，如果已经存在，则不再创建。如果不存在，则调用它的重载函数startProcessLocked(app, hostingType, hostingNameStr)创建一个新的应用进程。
具体实现：

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback{

      private final void startProcessLocked(ProcessRecord app,
                String hostingType, String hostingNameStr) {
            if (app.pid > 0 && app.pid != MY_PID) {
                synchronized (mPidsSelfLocked) {
                    mPidsSelfLocked.remove(app.pid);
                    mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);
                }
                app.pid = 0;
            }

            if (DEBUG_PROCESSES && mProcessesOnHold.contains(app)) Slog.v(TAG,
                    "startProcessLocked removing on hold: " + app);
            mProcessesOnHold.remove(app);

            updateCpuStats();

            System.arraycopy(mProcDeaths, 0, mProcDeaths, 1, mProcDeaths.length-1);
            mProcDeaths[0] = 0;

            try {
                //获取将要创建的应用进程的用户ID与用户组ID。
                int uid = app.info.uid;
                int[] gids = null;
                try {
                    gids = mContext.getPackageManager().getPackageGids(
                            app.info.packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(TAG, "Unable to retrieve gids", e);
                }
                if (mFactoryTest != SystemServer.FACTORY_TEST_OFF) {
                    if (mFactoryTest == SystemServer.FACTORY_TEST_LOW_LEVEL
                            && mTopComponent != null
                            && app.processName.equals(mTopComponent.getPackageName())) {
                        uid = 0;
                    }
                    if (mFactoryTest == SystemServer.FACTORY_TEST_HIGH_LEVEL
                            && (app.info.flags&ApplicationInfo.FLAG_FACTORY_TEST) != 0) {
                        uid = 0;
                    }
                }
                int debugFlags = 0;
                if ((app.info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    debugFlags |= Zygote.DEBUG_ENABLE_DEBUGGER;
                }
                // Run the app in safe mode if its manifest requests so or the
                // system is booted in safe mode.
                if ((app.info.flags & ApplicationInfo.FLAG_VM_SAFE_MODE) != 0 ||
                    Zygote.systemInSafeMode == true) {
                    debugFlags |= Zygote.DEBUG_ENABLE_SAFEMODE;
                }
                if ("1".equals(SystemProperties.get("debug.checkjni"))) {
                    debugFlags |= Zygote.DEBUG_ENABLE_CHECKJNI;
                }
                if ("1".equals(SystemProperties.get("debug.assert"))) {
                    debugFlags |= Zygote.DEBUG_ENABLE_ASSERT;
                }
                //创建一个新的应用进程。pid是一个大于0的ID
                int pid = Process.start("android.app.ActivityThread",
                        mSimpleProcessManagement ? app.processName : null, uid, uid,
                        gids, debugFlags, null);
                BatteryStatsImpl bs = app.batteryStats.getBatteryStats();
                synchronized (bs) {
                    if (bs.isOnBattery()) {
                        app.batteryStats.incStartsLocked();
                    }
                }

                EventLog.writeEvent(EventLogTags.AM_PROC_START, pid, uid,
                        app.processName, hostingType,
                        hostingNameStr != null ? hostingNameStr : "");

                if (app.persistent) {
                    Watchdog.getInstance().processStarted(app.processName, pid);
                }

                StringBuilder buf = mStringBuilder;
                buf.setLength(0);
                buf.append("Start proc ");
                buf.append(app.processName);
                buf.append(" for ");
                buf.append(hostingType);
                if (hostingNameStr != null) {
                    buf.append(" ");
                    buf.append(hostingNameStr);
                }
                buf.append(": pid=");
                buf.append(pid);
                buf.append(" uid=");
                buf.append(uid);
                buf.append(" gids={");
                if (gids != null) {
                    for (int gi=0; gi<gids.length; gi++) {
                        if (gi != 0) buf.append(", ");
                        buf.append(gids[gi]);

                    }
                }
                buf.append("}");
                Slog.i(TAG, buf.toString());
                if (pid == 0 || pid == MY_PID) {
                    // Processes are being emulated with threads.
                    app.pid = MY_PID;
                    app.removed = false;
                    mStartingProcesses.add(app);
                } else if (pid > 0) {
                    app.pid = pid;
                    app.removed = false;
                    synchronized (mPidsSelfLocked) {
                        //以pid为关键字将app指向的一个ProcessRecord对象保存在mPidsSelfLocked中
                        this.mPidsSelfLocked.put(pid, app);
                        //向ActivityManagerService所在线程发送一个PROC_START_TIMEOUT_MSG消息，
                        Message msg = mHandler.obtainMessage(PROC_START_TIMEOUT_MSG);
                        msg.obj = app;
                        //该消息要求PROC_START_TIMEOUT时间内被处理，也就是说新的进必须在PROC_START_TIMEOUT
                        //时间内完成启动，并向ActivityManagerService发送一个启动通知，以便ActivityManagerService
                        //去启动Activity组件，否则ActivityManagerService则任务进程创建超时，则无法启动Activity。
                        mHandler.sendMessageDelayed(msg, PROC_START_TIMEOUT);
                    }
                } else {
                    app.pid = 0;
                    RuntimeException e = new RuntimeException(
                            "Failure starting process " + app.processName
                            + ": returned pid=" + pid);
                    Slog.e(TAG, e.getMessage(), e);
                }
            } catch (RuntimeException e) {
                // XXX do better error recovery.
                app.pid = 0;
                Slog.e(TAG, "Failure starting process " + app.processName, e);
            }
        }

}
```

这个重载函数做了以下事情：

```
1 获取将要创建的应用进程的用户ID与用户组ID。
2 向ActivityManagerService所在线程发送一个PROC_START_TIMEOUT_MSG消息，该消息要求PROC_START_TIMEOUT时间内被处理，也就是说新的进必须在PROC_START_TIMEOUT,
时间内完成启动，并向ActivityManagerService发送一个启动通知，以便ActivityManagerService去启动Activity组件，否则ActivityManagerService则任务进程创建超时，则
无法启动Activity。
```

上述函数调用Process.start()来启动一个新的应用进程，指定该进程的进入函数为android.app.ActivityThread的静态成员函数main.

```
int pid = Process.start("android.app.ActivityThread",
            mSimpleProcessManagement ? app.processName : null, uid, uid,
            gids, debugFlags, null);
```

好了，我们接着看看进程是怎么创建的吧。

### 24 ActivityThread.main(String[] args)

```java
public final class ActivityThread {

    public static final void main(String[] args) {
        SamplingProfilerIntegration.start();

        Process.setArgV0("<pre-initialized>");

        //创建消息循环，并使当前进程进入该消息循环。
        Looper.prepareMainLooper();
        if (sMainThreadHandler == null) {
            sMainThreadHandler = new Handler();
        }

        //创建ActivityThread对象
        ActivityThread thread = new ActivityThread();
        //向ActivityManagerService发送一个启动完成通知
        thread.attach(false);

        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }

        Looper.loop();

        if (Process.supportsProcesses()) {
            throw new RuntimeException("Main thread loop unexpectedly exited");
        }

        thread.detach();
        String name = (thread.mInitialApplication != null)
            ? thread.mInitialApplication.getPackageName()
            : "<unknown>";
        Slog.i(TAG, "Main thread of " + name + " is now exiting");
    }

}
```

新的应用进程启动时主要做两件事情：

1 在进程中创建ActivityThread对象，创建ActivityThread对象时，会同时在其内部创建一个ApplicationThread对象，该对象是一个Binder本地对象，ActivityManagerService就是
通过它和应用进程通信的。

2 调用thread.attach(false)向ctivityManagerService发送启动完成通知。

3 调用Looper.prepareMainLooper()创建一个消息循环，并使当前进程进入到该消息循环中，

thread.attach(false)最终会去调用ActivityManagerProxy.attachApplication(IApplicationThread app)发送一个进程间通信请求。


### 25 ActivityManagerProxy.attachApplication(IApplicationThread app)

```java
public final class ActivityManagerProxy{

   public void attachApplication(IApplicationThread app) throws RemoteException{
       Parcel data = Parcel.obtain();
       Parcel reply = Parcel.obtain();
       data.writeInterfaceToken(IActivityManager.descriptor);
       data.writeStrongBinder(app.asBinder());
       mRemote.transact(ATTACH_APPLICATION_TRANSACTION, data, reply, 0);
       reply.readException();
       data.recycle();
       reply.recycle();
   }
}
```

将传递过来的参数包装成一个Parcel对象，并发起一个ATTACH_APPLICATION_TRANSACTION进程通信请求。


### 26 ActivityManagerService.attachApplication(IApplicationThread thread)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {

    public final void attachApplication(IApplicationThread thread) {
        synchronized (this) {
            int callingPid = Binder.getCallingPid();
            final long origId = Binder.clearCallingIdentity();
            attachApplicationLocked(thread, callingPid);
            Binder.restoreCallingIdentity(origId);
        }
    }
}
```
attachApplication(IApplicationThread thread)接收到了新应用进程发送过来的ATTACH_APPLICATION_TRANSACTION进程通信请求之后，它就知道
新的应用进程已经创建，接着调用ActivityManagerService.attachApplicationLocked(IApplicationThread thread, int pid)继续执行Activity
的启动操作。

### 27 ActivityManagerService.attachApplicationLocked(IApplicationThread thread, int pid)

```java
public final class ActivityManagerService extends ActivityManagerNative
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {

 private final boolean attachApplicationLocked(IApplicationThread thread,
            int pid) {

        // Find the application record that is being attached...  either via
        // the pid if we are running in multiple processes, or just pull the
        // next app record if we are emulating process with anonymous threads.
        ProcessRecord app;
        if (pid != MY_PID && pid >= 0) {
            synchronized (mPidsSelfLocked) {
                //通过新进程的pid取出对应的ProcessRecord对象，并保存在ProcessRecord app中。
                app = mPidsSelfLocked.get(pid);
            }
        } else if (mStartingProcesses.size() > 0) {
            app = mStartingProcesses.remove(0);
            app.setPid(pid);
        } else {
            app = null;
        }

        if (app == null) {
            Slog.w(TAG, "No pending application record for pid " + pid
                    + " (IApplicationThread " + thread + "); dropping process");
            EventLog.writeEvent(EventLogTags.AM_DROP_PROCESS, pid);
            if (pid > 0 && pid != MY_PID) {
                Process.killProcess(pid);
            } else {
                try {
                    thread.scheduleExit();
                } catch (Exception e) {
                    // Ignore exceptions.
                }
            }
            return false;
        }

        // If this application record is still attached to a previous
        // process, clean it up now.
        if (app.thread != null) {
            handleAppDiedLocked(app, true);
        }

        // Tell the process all about itself.

        if (localLOGV) Slog.v(
                TAG, "Binding process pid " + pid + " to record " + app);

        String processName = app.processName;
        try {
            thread.asBinder().linkToDeath(new AppDeathRecipient(
                    app, pid, thread), 0);
        } catch (RemoteException e) {
            app.resetPackageList();
            startProcessLocked(app, "link fail", processName);
            return false;
        }

        EventLog.writeEvent(EventLogTags.AM_PROC_BOUND, app.pid, app.processName);

        app.thread = thread;
        app.curAdj = app.setAdj = -100;
        app.curSchedGroup = Process.THREAD_GROUP_DEFAULT;
        app.setSchedGroup = Process.THREAD_GROUP_BG_NONINTERACTIVE;
        app.forcingToForeground = null;
        app.foregroundServices = false;
        app.debugging = false;

        //新进程已经在规定时间内创建，移除PROC_START_TIMEOUT_MSG消息
        mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);

        boolean normalMode = mProcessesReady || isAllowedWhileBooting(app.info);
        List providers = normalMode ? generateApplicationProvidersLocked(app) : null;

        if (!normalMode) {
            Slog.i(TAG, "Launching preboot mode app: " + app);
        }

        if (localLOGV) Slog.v(
            TAG, "New app record " + app
            + " thread=" + thread.asBinder() + " pid=" + pid);
        try {
            int testMode = IApplicationThread.DEBUG_OFF;
            if (mDebugApp != null && mDebugApp.equals(processName)) {
                testMode = mWaitForDebugger
                    ? IApplicationThread.DEBUG_WAIT
                    : IApplicationThread.DEBUG_ON;
                app.debugging = true;
                if (mDebugTransient) {
                    mDebugApp = mOrigDebugApp;
                    mWaitForDebugger = mOrigWaitForDebugger;
                }
            }

            // If the app is being launched for restore or full backup, set it up specially
            boolean isRestrictedBackupMode = false;
            if (mBackupTarget != null && mBackupAppName.equals(processName)) {
                isRestrictedBackupMode = (mBackupTarget.backupMode == BackupRecord.RESTORE)
                        || (mBackupTarget.backupMode == BackupRecord.BACKUP_FULL);
            }

            ensurePackageDexOpt(app.instrumentationInfo != null
                    ? app.instrumentationInfo.packageName
                    : app.info.packageName);
            if (app.instrumentationClass != null) {
                ensurePackageDexOpt(app.instrumentationClass.getPackageName());
            }
            if (DEBUG_CONFIGURATION) Slog.v(TAG, "Binding proc "
                    + processName + " with config " + mConfiguration);
            thread.bindApplication(processName, app.instrumentationInfo != null
                    ? app.instrumentationInfo : app.info, providers,
                    app.instrumentationClass, app.instrumentationProfileFile,
                    app.instrumentationArguments, app.instrumentationWatcher, testMode,
                    isRestrictedBackupMode || !normalMode,
                    mConfiguration, getCommonServicesLocked());
            updateLruProcessLocked(app, false, true);
            app.lastRequestedGc = app.lastLowMemory = SystemClock.uptimeMillis();
        } catch (Exception e) {
            // todo: Yikes!  What should we do?  For now we will try to
            // start another process, but that could easily get us in
            // an infinite loop of restarting processes...
            Slog.w(TAG, "Exception thrown during bind!", e);

            app.resetPackageList();
            startProcessLocked(app, "bind fail", processName);
            return false;
        }

        // Remove this record from the list of starting applications.
        mPersistentStartingProcesses.remove(app);
        if (DEBUG_PROCESSES && mProcessesOnHold.contains(app)) Slog.v(TAG,
                "Attach application locked removing on hold: " + app);
        mProcessesOnHold.remove(app);

        boolean badApp = false;
        boolean didSomething = false;

        //查看栈顶Activity（也就是我们要启动的目标Activity）是否将要在当前进程中启动。
        // See if the top visible activity is waiting to run in this process...
        ActivityRecord hr = mMainStack.topRunningActivityLocked(null);
        if (hr != null && normalMode) {
            //检查目标Activity的用户ID与进程名称与app所描述的应用进程的用户ID与进程名称是否一致。
            if (hr.app == null && app.info.uid == hr.info.applicationInfo.uid
                    && processName.equals(hr.processName)) {
                try {
                    //如果一致，则继续执行目标Activity启动操作。
                    if (mMainStack.realStartActivityLocked(hr, app, true, true)) {
                        didSomething = true;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Exception in new application when starting activity "
                          + hr.intent.getComponent().flattenToShortString(), e);
                    badApp = true;
                }
            } else {
                mMainStack.ensureActivitiesVisibleLocked(hr, null, processName, 0);
            }
        }

        // Find any services that should be running in this process...
        if (!badApp && mPendingServices.size() > 0) {
            ServiceRecord sr = null;
            try {
                for (int i=0; i<mPendingServices.size(); i++) {
                    sr = mPendingServices.get(i);
                    if (app.info.uid != sr.appInfo.uid
                            || !processName.equals(sr.processName)) {
                        continue;
                    }

                    mPendingServices.remove(i);
                    i--;
                    realStartServiceLocked(sr, app);
                    didSomething = true;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Exception in new application when starting service "
                      + sr.shortName, e);
                badApp = true;
            }
        }

        // Check if the next broadcast receiver is in this process...
        BroadcastRecord br = mPendingBroadcast;
        if (!badApp && br != null && br.curApp == app) {
            try {
                mPendingBroadcast = null;
                processCurBroadcastLocked(br, app);
                didSomething = true;
            } catch (Exception e) {
                Slog.w(TAG, "Exception in new application when starting receiver "
                      + br.curComponent.flattenToShortString(), e);
                badApp = true;
                logBroadcastReceiverDiscardLocked(br);
                finishReceiverLocked(br.receiver, br.resultCode, br.resultData,
                        br.resultExtras, br.resultAbort, true);
                scheduleBroadcastsLocked();
                // We need to reset the state if we fails to start the receiver.
                br.state = BroadcastRecord.IDLE;
            }
        }

        // Check whether the next backup agent is in this process...
        if (!badApp && mBackupTarget != null && mBackupTarget.appInfo.uid == app.info.uid) {
            if (DEBUG_BACKUP) Slog.v(TAG, "New app is backup target, launching agent for " + app);
            ensurePackageDexOpt(mBackupTarget.appInfo.packageName);
            try {
                thread.scheduleCreateBackupAgent(mBackupTarget.appInfo, mBackupTarget.backupMode);
            } catch (Exception e) {
                Slog.w(TAG, "Exception scheduling backup agent creation: ");
                e.printStackTrace();
            }
        }

        if (badApp) {
            // todo: Also need to kill application to deal with all
            // kinds of exceptions.
            handleAppDiedLocked(app, false);
            return false;
        }

        if (!didSomething) {
            updateOomAdjLocked();
        }

        return true;
    }

}
```
该函数做了以下事情：

1 通过新进程的pid取出对应的ProcessRecord对象，并保存在ProcessRecord app中。
2 新进程已经在规定时间内创建，移除PROC_START_TIMEOUT_MSG消息.
3 查看栈顶Activity（也就是我们要启动的目标Activity）是否将要在当前进程中启动。这个主要检查目标Activity的用户ID与进程名称与app所描述的应用进程的用户ID与进程名称是否一致。

如果一致，则继续调用mMainStack.realStartActivityLocked(hr, app, true, true)，执行目标Activity启动操作。

### 28 ActivityStack.realStartActivityLocked(ActivityRecord r, ProcessRecord app, boolean andResume, boolean checkConfig)

```java
public class ActivityStack{

  final boolean realStartActivityLocked(ActivityRecord r,
            ProcessRecord app, boolean andResume, boolean checkConfig)
            throws RemoteException {

        r.startFreezingScreenLocked(app, 0);
        mService.mWindowManager.setAppVisibility(r, true);

        // Have the window manager re-evaluate the orientation of
        // the screen based on the new activity order.  Note that
        // as a result of this, it can call back into the activity
        // manager with a new orientation.  We don't care about that,
        // because the activity is not currently running so we are
        // just restarting it anyway.
        if (checkConfig) {
            Configuration config = mService.mWindowManager.updateOrientationFromAppTokens(
                    mService.mConfiguration,
                    r.mayFreezeScreenLocked(app) ? r : null);
            mService.updateConfigurationLocked(config, r);
        }

        //表示它描述的Activity组件是在参数app描述的应用进程中启动。
        r.app = app;

        if (localLOGV) Slog.v(TAG, "Launching: " + r);

        int idx = app.activities.indexOf(r);
        if (idx < 0) {
            //将目标Activity组件添加到该应用进程中。
            app.activities.add(r);
        }
        mService.updateLruProcessLocked(app, true, true);

        try {
            if (app.thread == null) {
                throw new RemoteException();
            }
            List<ResultInfo> results = null;
            List<Intent> newIntents = null;
            if (andResume) {
                results = r.results;
                newIntents = r.newIntents;
            }
            if (DEBUG_SWITCH) Slog.v(TAG, "Launching: " + r
                    + " icicle=" + r.icicle
                    + " with results=" + results + " newIntents=" + newIntents
                    + " andResume=" + andResume);
            if (andResume) {
                EventLog.writeEvent(EventLogTags.AM_RESTART_ACTIVITY,
                        System.identityHashCode(r),
                        r.task.taskId, r.shortComponentName);
            }
            if (r.isHomeActivity) {
                mService.mHomeProcess = app;
            }
            mService.ensurePackageDexOpt(r.intent.getComponent().getPackageName());

            //app.thread是一个类型为ApplicationTHreadProxy的Binder代理对象，向我们刚才新创建的应用进程发送一个
            //进程间通信请求，通知前面创建的应用进程启动目标Activity组件，该Activity组件由ActivityRecord r来描述。
            app.thread.scheduleLaunchActivity(new Intent(r.intent), r,
                    System.identityHashCode(r),
                    r.info, r.icicle, results, newIntents, !andResume,
                    mService.isNextTransitionForward());

            if ((app.info.flags&ApplicationInfo.FLAG_CANT_SAVE_STATE) != 0) {
                // This may be a heavy-weight process!  Note that the package
                // manager will ensure that only activity can run in the main
                // process of the .apk, which is the only thing that will be
                // considered heavy-weight.
                if (app.processName.equals(app.info.packageName)) {
                    if (mService.mHeavyWeightProcess != null
                            && mService.mHeavyWeightProcess != app) {
                        Log.w(TAG, "Starting new heavy weight process " + app
                                + " when already running "
                                + mService.mHeavyWeightProcess);
                    }
                    mService.mHeavyWeightProcess = app;
                    Message msg = mService.mHandler.obtainMessage(
                            ActivityManagerService.POST_HEAVY_NOTIFICATION_MSG);
                    msg.obj = r;
                    mService.mHandler.sendMessage(msg);
                }
            }

        } catch (RemoteException e) {
            if (r.launchFailed) {
                // This is the second time we failed -- finish activity
                // and give up.
                Slog.e(TAG, "Second failure launching "
                      + r.intent.getComponent().flattenToShortString()
                      + ", giving up", e);
                mService.appDiedLocked(app, app.pid, app.thread);
                requestFinishActivityLocked(r, Activity.RESULT_CANCELED, null,
                        "2nd-crash");
                return false;
            }

            // This is the first time we failed -- restart process and
            // retry.
            app.activities.remove(r);
            throw e;
        }

        r.launchFailed = false;
        if (updateLRUListLocked(r)) {
            Slog.w(TAG, "Activity " + r
                  + " being launched, but already in LRU list");
        }

        if (andResume) {
            // As part of the process of launching, ActivityThread also performs
            // a resume.
            r.state = ActivityState.RESUMED;
            r.icicle = null;
            r.haveState = false;
            r.stopped = false;
            mResumedActivity = r;
            r.task.touchActiveTime();
            completeResumeLocked(r);
            pauseIfSleepingLocked();
        } else {
            // This activity is not starting in the resumed state... which
            // should look like we asked it to pause+stop (but remain visible),
            // and it has done so and reported back the current icicle and
            // other state.
            r.state = ActivityState.STOPPED;
            r.stopped = true;
        }

        // Launch the new version setup screen if needed.  We do this -after-
        // launching the initial activity (that is, home), so that it can have
        // a chance to initialize itself while in the background, making the
        // switch back to it faster and look better.
        if (mMainStack) {
            mService.startSetupActivityLocked();
        }

        return true;
    }
}
```

该函数主要做了以下事情：

```
1 将目标Activity添加到新创建的进程的Activity组件列表中。
2 调用 app.thread.scheduleLaunchActivity()，向我们刚才新创建的应用进程发送一个进程间通信请求，通知前面创建的应用进程启动目标Activity组件。
```

这里说一下这个app.thread，它是一个类型为ApplicationTHreadProxy的Binder代理对象。

### 29 ApplicationThreadProxy.scheduleLaunchActivity(Intent intent, IBinder token, int ident, ActivityInfo info, Bundle state, List<ResultInfo> pendingResults, List<Intent> pendingNewIntents, boolean notResumed, boolean isForward)

```java
class ApplicationThreadProxy implements IApplicationThread{

    public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
            ActivityInfo info, Bundle state, List<ResultInfo> pendingResults,
    		List<Intent> pendingNewIntents, boolean notResumed, boolean isForward)
    		throws RemoteException {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken(IApplicationThread.descriptor);
        intent.writeToParcel(data, 0);
        data.writeStrongBinder(token);
        data.writeInt(ident);
        info.writeToParcel(data, 0);
        data.writeBundle(state);
        data.writeTypedList(pendingResults);
        data.writeTypedList(pendingNewIntents);
        data.writeInt(notResumed ? 1 : 0);
        data.writeInt(isForward ? 1 : 0);
        mRemote.transact(SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION, data, null,
                IBinder.FLAG_ONEWAY);
        data.recycle();
    }
    
}
```

将传递过来的参数打包成一个Parcel对象，并发送一个类型为SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION进程间通信请求。

### 30 ActivityThread.scheduleRelaunchActivity(IBinder token, List<ResultInfo> pendingResults, List<Intent> pendingNewIntents, int configChanges, boolean notResumed, Configuration config)

```java
public final class ActivityThread{

    private final class ApplicationThread extends ApplicationThreadNative {
    
        // we use token to identify this activity without having to send the
        // activity itself back to the activity manager. (matters more with ipc)
        public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
                ActivityInfo info, Bundle state, List<ResultInfo> pendingResults,
                List<Intent> pendingNewIntents, boolean notResumed, boolean isForward) {
            ActivityClientRecord r = new ActivityClientRecord();

            r.token = token;
            r.ident = ident;
            r.intent = intent;
            r.activityInfo = info;
            r.state = state;

            r.pendingResults = pendingResults;
            r.pendingIntents = pendingNewIntents;

            r.startsNotResumed = notResumed;
            r.isForward = isForward;

            queueOrSendMessage(H.LAUNCH_ACTIVITY, r);
        }
    
    }
}
```
ApplicationThread的scheduleRelaunchActivity()用来处理ActivityManagerService发出的SCHEDULE_LAUNCH_ACTIVITY_TRANSACTION进程通信请求。
它主要把将要启动的目标Activity的信息封装成一个ActivityClientRecord对象，再以ActivityClientRecord对象为参数，调用queueOrSendMessage()函数
，向新创建应用进程的主线程消息队列发送一个类型为LAUNCH_ACTIVITY的消息。

### 31 ActivityThread.queueOrSendMessage(int what, Object obj)

```java
public final class ActivityThread{

    // if the thread hasn't started yet, we don't have the handler, so just
    // save the messages until we're ready.
    private final void queueOrSendMessage(int what, Object obj) {
        queueOrSendMessage(what, obj, 0, 0);
    }

    private final void queueOrSendMessage(int what, Object obj, int arg1) {
        queueOrSendMessage(what, obj, arg1, 0);
    }

    private final void queueOrSendMessage(int what, Object obj, int arg1, int arg2) {
        synchronized (this) {
            if (DEBUG_MESSAGES) Slog.v(
                TAG, "SCHEDULE " + what + " " + mH.codeToString(what)
                + ": " + arg1 + " / " + obj);
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            msg.arg1 = arg1;
            msg.arg2 = arg2;
            mH.sendMessage(msg);
        }
    }
    
}
```

将上一步传递过来的ActivityClientRecord对象封装成一个Message对象，并将消息LAUNCH_ACTIVITY发送。

### 32 H.handleMessage(Message msg)

```java
public final class H extends Handler{

  public void handleMessage(Message msg) {
            if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + msg.what);
            switch (msg.what) {
                case LAUNCH_ACTIVITY: {
                    //获取ActivityClientRecord对象
                    ActivityClientRecord r = (ActivityClientRecord)msg.obj;
                    //获取一个LoadedApk对象，并将它保存在ActivityClientRecord对象的成员变量packagInfo中。
                    r.packageInfo = getPackageInfoNoCheck(
                            r.activityInfo.applicationInfo);
                    handleLaunchActivity(r, null);
                } break;
                case RELAUNCH_ACTIVITY: {
                    ActivityClientRecord r = (ActivityClientRecord)msg.obj;
                    handleRelaunchActivity(r, msg.arg1);
                } break;
                case PAUSE_ACTIVITY:
                    handlePauseActivity((IBinder)msg.obj, false, msg.arg1 != 0, msg.arg2);
                    maybeSnapshot();
                    break;
                case PAUSE_ACTIVITY_FINISHING:
                    handlePauseActivity((IBinder)msg.obj, true, msg.arg1 != 0, msg.arg2);
                    break;
                case STOP_ACTIVITY_SHOW:
                    handleStopActivity((IBinder)msg.obj, true, msg.arg2);
                    break;
                case STOP_ACTIVITY_HIDE:
                    handleStopActivity((IBinder)msg.obj, false, msg.arg2);
                    break;
                case SHOW_WINDOW:
                    handleWindowVisibility((IBinder)msg.obj, true);
                    break;
                case HIDE_WINDOW:
                    handleWindowVisibility((IBinder)msg.obj, false);
                    break;
                case RESUME_ACTIVITY:
                    handleResumeActivity((IBinder)msg.obj, true,
                            msg.arg1 != 0);
                    break;
                case SEND_RESULT:
                    handleSendResult((ResultData)msg.obj);
                    break;
                case DESTROY_ACTIVITY:
                    handleDestroyActivity((IBinder)msg.obj, msg.arg1 != 0,
                            msg.arg2, false);
                    break;
                case BIND_APPLICATION:
                    AppBindData data = (AppBindData)msg.obj;
                    handleBindApplication(data);
                    break;
                case EXIT_APPLICATION:
                    if (mInitialApplication != null) {
                        mInitialApplication.onTerminate();
                    }
                    Looper.myLooper().quit();
                    break;
                case NEW_INTENT:
                    handleNewIntent((NewIntentData)msg.obj);
                    break;
                case RECEIVER:
                    handleReceiver((ReceiverData)msg.obj);
                    maybeSnapshot();
                    break;
                case CREATE_SERVICE:
                    handleCreateService((CreateServiceData)msg.obj);
                    break;
                case BIND_SERVICE:
                    handleBindService((BindServiceData)msg.obj);
                    break;
                case UNBIND_SERVICE:
                    handleUnbindService((BindServiceData)msg.obj);
                    break;
                case SERVICE_ARGS:
                    handleServiceArgs((ServiceArgsData)msg.obj);
                    break;
                case STOP_SERVICE:
                    handleStopService((IBinder)msg.obj);
                    maybeSnapshot();
                    break;
                case REQUEST_THUMBNAIL:
                    handleRequestThumbnail((IBinder)msg.obj);
                    break;
                case CONFIGURATION_CHANGED:
                    handleConfigurationChanged((Configuration)msg.obj);
                    break;
                case CLEAN_UP_CONTEXT:
                    ContextCleanupInfo cci = (ContextCleanupInfo)msg.obj;
                    cci.context.performFinalCleanup(cci.who, cci.what);
                    break;
                case GC_WHEN_IDLE:
                    scheduleGcIdler();
                    break;
                case DUMP_SERVICE:
                    handleDumpService((DumpServiceInfo)msg.obj);
                    break;
                case LOW_MEMORY:
                    handleLowMemory();
                    break;
                case ACTIVITY_CONFIGURATION_CHANGED:
                    handleActivityConfigurationChanged((IBinder)msg.obj);
                    break;
                case PROFILER_CONTROL:
                    handleProfilerControl(msg.arg1 != 0, (ProfilerControlData)msg.obj);
                    break;
                case CREATE_BACKUP_AGENT:
                    handleCreateBackupAgent((CreateBackupAgentData)msg.obj);
                    break;
                case DESTROY_BACKUP_AGENT:
                    handleDestroyBackupAgent((CreateBackupAgentData)msg.obj);
                    break;
                case SUICIDE:
                    Process.killProcess(Process.myPid());
                    break;
                case REMOVE_PROVIDER:
                    completeRemoveProvider((IContentProvider)msg.obj);
                    break;
                case ENABLE_JIT:
                    ensureJitEnabled();
                    break;
                case DISPATCH_PACKAGE_BROADCAST:
                    handleDispatchPackageBroadcast(msg.arg1, (String[])msg.obj);
                    break;
                case SCHEDULE_CRASH:
                    throw new RemoteServiceException((String)msg.obj);
            }
            if (DEBUG_MESSAGES) Slog.v(TAG, "<<< done: " + msg.what);
        }
        
}
```
上述方法做了两件事情：

1 获取ActivityClientRecord对象，获取一个LoadedApk对象，并将它保存在ActivityClientRecord对象的成员变量packagInfo中。

>LoadedApk对象：每个Android应用都打包在一个APK文件中，APK文件包含应用的所有资源，LoadedApk对象就是用来描述一个已经被加载的APK文件。

2 调用handleLaunchActivity(r, null)方法来处理LAUNCH_ACTIVITY消息。

### 33 ActivityThread.handleLaunchActivity(ActivityClientRecord r, Intent customIntent) 

```java
public final class ActivityThread {
    
   private final void handleLaunchActivity(ActivityClientRecord r, Intent customIntent) {
        // If we are getting ready to gc after going to the background, well
        // we are back active so skip it.
        unscheduleGcIdler();

        if (localLOGV) Slog.v(
            TAG, "Handling launch of " + r);
         //启动目标Activity组件
        Activity a = performLaunchActivity(r, customIntent);

        if (a != null) {
            r.createdConfig = new Configuration(mConfiguration);
            Bundle oldState = r.state;
            //将目标Activity组件状态设置为Resumed状态，表示他是系统当前激活的Activity
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

该函数主要做了两件事情：

```
1 调用performLaunchActivity(r, customIntent)启动目标Activity组件。
2 调用andleResumeActivity(r.token, false, r.isForward)将目标Activity组件状态设置为Resumed状态，表示他是系统当前激活的Activity
```

## 34 ActivityThread.performLaunchActivity(ActivityClientRecord r, Intent customIntent)

```java
public final class ActivityThread {

    private final Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
        // System.out.println("##### [" + System.currentTimeMillis() + "] ActivityThread.performLaunchActivity(" + r + ")");

        ActivityInfo aInfo = r.activityInfo;
        if (r.packageInfo == null) {
            r.packageInfo = getPackageInfo(aInfo.applicationInfo,
                    Context.CONTEXT_INCLUDE_CODE);
        }

        //获取Activity组件的包名以及类名
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

        //将目标Activity类文件加载到内存中，并且创建一个实例。
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
                //创建一个ContextImpl对象，它将作为Activity运行的上下文环境，通过它就可以访问特定的应用程序资源
                //，以及启动应用程序组件。
                ContextImpl appContext = new ContextImpl();
                appContext.init(r.packageInfo, r.token, this);
                appContext.setOuterContext(activity);
                CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());
                Configuration config = new Configuration(mConfiguration);
                if (DEBUG_CONFIGURATION) Slog.v(TAG, "Launching activity "
                        + r.activityInfo.name + " with config " + config);
                //使用ContextImpl对象和ActivityClientRecord对象来初始化Activity组件
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
                //Activity初始化完成后，调用mInstrumentation.callActivityOnCreate(activity, r.state)将Activity启动起来
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

这个函数正式开始Activity组件的初始化以及启动工作，我们来具体看看。

1 获取Activity组件的包名以及类名
2 将目标Activity类文件加载到内存中，并且创建一个实例。
3 创建一个ContextImpl对象，它将作为Activity运行的上下文环境，通过它就可以访问特定的应用程序资源，以及启动应用程序组件。使用ContextImpl对象和ActivityClientRecord对象
来初始化Activity组件。

4 Activity初始化完成后，调用mInstrumentation.callActivityOnCreate(activity, r.state)将Activity启动起来，我们来看看该函数的具体实现：

```java
public class Instrumentation {

    /**
     * Perform calling of an activity's {Activity#onCreate}
     * method.  The default implementation simply calls through to that method.
     * 
     * @param activity The activity being created.
     * @param icicle The previously frozen state (or null) to pass through to
     *               onCreate().
     */
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        if (mWaitingActivities != null) {
            synchronized (mSync) {
                final int N = mWaitingActivities.size();
                for (int i=0; i<N; i++) {
                    final ActivityWaiter aw = mWaitingActivities.get(i);
                    final Intent intent = aw.intent;
                    if (intent.filterEquals(activity.getIntent())) {
                        aw.activity = activity;
                        mMessageQueue.addIdleHandler(new ActivityGoing(aw));
                    }
                }
            }
        }
        
        activity.onCreate(icicle);
        
        if (mActivityMonitors != null) {
            synchronized (mSync) {
                final int N = mActivityMonitors.size();
                for (int i=0; i<N; i++) {
                    final ActivityMonitor am = mActivityMonitors.get(i);
                    am.match(activity, activity, activity.getIntent());
                }
            }
        }
    }    

}
```

可以看到该函数最终调用了Activity的onCreate()方法。

### 35 Activity.onCreate(Bundle savedInstanceState) 

```java
public class Activity extends ContextThemeWrapper
        implements LayoutInflater.Factory,
        Window.Callback, KeyEvent.Callback,
        OnCreateContextMenuListener, ComponentCallbacks {
        
    /**
     * Called when the activity is starting.  This is where most initialization
     * should go: calling { #setContentView(int)} to inflate the
     * activity's UI, using { #findViewById} to programmatically interact
     * with widgets in the UI, calling
     * { #managedQuery(android.net.Uri , String[], String, String[], String)} to retrieve
     * cursors for data being displayed, etc.
     * 
     * <p>You can call { #finish} from within this function, in
     * which case onDestroy() will be immediately called without any of the rest
     * of the activity lifecycle ({ #onStart}, { #onResume},
     * { #onPause}, etc) executing.
     * 
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     * 
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    protected void onCreate(Bundle savedInstanceState) {
        mVisibleFromClient = !mWindow.getWindowStyle().getBoolean(
                com.android.internal.R.styleable.Window_windowNoDisplay, false);
        mCalled = true;
    }
    
        /**
         * Finds a view that was identified by the id attribute from the XML that
         * was processed in {@link #onCreate}.
         *
         * @return The view if found or null otherwise.
         */
        public View findViewById(int id) {
            return getWindow().findViewById(id);
        }
    
        /**
         * Set the activity content from a layout resource.  The resource will be
         * inflated, adding all top-level views to the activity.
         * 
         * @param layoutResID Resource ID to be inflated.
         */
        public void setContentView(int layoutResID) {
            getWindow().setContentView(layoutResID);
        }
    
        /**
         * Set the activity content to an explicit view.  This view is placed
         * directly into the activity's view hierarchy.  It can itself be a complex
         * view hierarhcy.
         * 
         * @param view The desired content to display.
         */
        public void setContentView(View view) {
            getWindow().setContentView(view);
        }
    
        /**
         * Set the activity content to an explicit view.  This view is placed
         * directly into the activity's view hierarchy.  It can itself be a complex
         * view hierarhcy.
         * 
         * @param view The desired content to display.
         * @param params Layout parameters for the view.
         */
        public void setContentView(View view, ViewGroup.LayoutParams params) {
            getWindow().setContentView(view, params);
        }
    
        /**
         * Add an additional content view to the activity.  Added after any existing
         * ones in the activity -- existing views are NOT removed.
         * 
         * @param view The desired content to display.
         * @param params Layout parameters for the view.
         */
        public void addContentView(View view, ViewGroup.LayoutParams params) {
            getWindow().addContentView(view, params);
        }
}
```

全面人民万众瞩目，翘首以盼的onCreate()函数终于来到了我们面前。<img src="https://github.com/guoxiaoxing/emoji/raw/master/emoji/d_ku.png" width="30" height="30" align="bottom"/>

我们平时常见的Activity代码：

```java
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_application).setOnClickListener(this);
        findViewById(R.id.btn_system).setOnClickListener(this);
        findViewById(R.id.btn_program).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_application: {
                Intent intent = new Intent(MainActivity.this, ApplicationActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.btn_system: {
                Intent intent = new Intent(MainActivity.this, SystemActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.btn_program: {
                Intent intent = new Intent(MainActivity.this, ProgramActivity.class);
                startActivity(intent);
            }
            break;
        }
    }
}

```
onCreate()主要用来加载用户界面，以及对用户界面上的组件进行初始化，

