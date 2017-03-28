# Android系统应用框架篇：Activity启动模式与标识位

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


|flag                              |vaule                 |meaning                                  |
|----------------------------------|:--------------------|:-----------------------------------------|
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_WRITE_URI_PERMISSION   |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |
|FLAG_GRANT_READ_URI_PERMISSION    |0x00000001            |                                         |


 // ---------------------------------------------------------------------
    // ---------------------------------------------------------------------
    // Intent flags (see mFlags variable).

    /**
     * If set, the recipient of this Intent will be granted permission to
     * perform read operations on the Uri in the Intent's data.
     */
    public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
    /**
     * If set, the recipient of this Intent will be granted permission to
     * perform write operations on the Uri in the Intent's data.
     */
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;
    /**
     * Can be set by the caller to indicate that this Intent is coming from
     * a background operation, not from direct user interaction.
     */
    public static final int FLAG_FROM_BACKGROUND = 0x00000004;
    /**
     * A flag you can enable for debugging: when set, log messages will be
     * printed during the resolution of this intent to show you what has
     * been found to create the final resolved list.
     */
    public static final int FLAG_DEBUG_LOG_RESOLUTION = 0x00000008;

    /**
     * If set, the new activity is not kept in the history stack.  As soon as
     * the user navigates away from it, the activity is finished.  This may also
     * be set with the {@link android.R.styleable#AndroidManifestActivity_noHistory
     * noHistory} attribute.
     */
    public static final int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
    /**
     * If set, the activity will not be launched if it is already running
     * at the top of the history stack.
     */
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    /**
     * If set, this activity will become the start of a new task on this
     * history stack.  A task (from the activity that started it to the
     * next task activity) defines an atomic group of activities that the
     * user can move to.  Tasks can be moved to the foreground and background;
     * all of the activities inside of a particular task always remain in
     * the same order.  See
     * <a href="{@docRoot}guide/topics/fundamentals.html#acttask">Application Fundamentals:
     * Activities and Tasks</a> for more details on tasks.
     *
     * <p>This flag is generally used by activities that want
     * to present a "launcher" style behavior: they give the user a list of
     * separate things that can be done, which otherwise run completely
     * independently of the activity launching them.
     *
     * <p>When using this flag, if a task is already running for the activity
     * you are now starting, then a new activity will not be started; instead,
     * the current task will simply be brought to the front of the screen with
     * the state it was last in.  See {@link #FLAG_ACTIVITY_MULTIPLE_TASK} for a flag
     * to disable this behavior.
     *
     * <p>This flag can not be used when the caller is requesting a result from
     * the activity being launched.
     */
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    /**
     * <strong>Do not use this flag unless you are implementing your own
     * top-level application launcher.</strong>  Used in conjunction with
     * {@link #FLAG_ACTIVITY_NEW_TASK} to disable the
     * behavior of bringing an existing task to the foreground.  When set,
     * a new task is <em>always</em> started to host the Activity for the
     * Intent, regardless of whether there is already an existing task running
     * the same thing.
     *
     * <p><strong>Because the default system does not include graphical task management,
     * you should not use this flag unless you provide some way for a user to
     * return back to the tasks you have launched.</strong>
     *
     * <p>This flag is ignored if
     * {@link #FLAG_ACTIVITY_NEW_TASK} is not set.
     *
     * <p>See <a href="{@docRoot}guide/topics/fundamentals.html#acttask">Application Fundamentals:
     * Activities and Tasks</a> for more details on tasks.
     */
    public static final int FLAG_ACTIVITY_MULTIPLE_TASK = 0x08000000;
    /**
     * If set, and the activity being launched is already running in the
     * current task, then instead of launching a new instance of that activity,
     * all of the other activities on top of it will be closed and this Intent
     * will be delivered to the (now on top) old activity as a new Intent.
     *
     * <p>For example, consider a task consisting of the activities: A, B, C, D.
     * If D calls startActivity() with an Intent that resolves to the component
     * of activity B, then C and D will be finished and B receive the given
     * Intent, resulting in the stack now being: A, B.
     *
     * <p>The currently running instance of activity B in the above example will
     * either receive the new intent you are starting here in its
     * onNewIntent() method, or be itself finished and restarted with the
     * new intent.  If it has declared its launch mode to be "multiple" (the
     * default) and you have not set {@link #FLAG_ACTIVITY_SINGLE_TOP} in
     * the same intent, then it will be finished and re-created; for all other
     * launch modes or if {@link #FLAG_ACTIVITY_SINGLE_TOP} is set then this
     * Intent will be delivered to the current instance's onNewIntent().
     *
     * <p>This launch mode can also be used to good effect in conjunction with
     * {@link #FLAG_ACTIVITY_NEW_TASK}: if used to start the root activity
     * of a task, it will bring any currently running instance of that task
     * to the foreground, and then clear it to its root state.  This is
     * especially useful, for example, when launching an activity from the
     * notification manager.
     *
     * <p>See <a href="{@docRoot}guide/topics/fundamentals.html#acttask">Application Fundamentals:
     * Activities and Tasks</a> for more details on tasks.
     */
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    /**
     * If set and this intent is being used to launch a new activity from an
     * existing one, then the reply target of the existing activity will be
     * transfered to the new activity.  This way the new activity can call
     * {@link android.app.Activity#setResult} and have that result sent back to
     * the reply target of the original activity.
     */
    public static final int FLAG_ACTIVITY_FORWARD_RESULT = 0x02000000;
    /**
     * If set and this intent is being used to launch a new activity from an
     * existing one, the current activity will not be counted as the top
     * activity for deciding whether the new intent should be delivered to
     * the top instead of starting a new one.  The previous activity will
     * be used as the top, with the assumption being that the current activity
     * will finish itself immediately.
     */
    public static final int FLAG_ACTIVITY_PREVIOUS_IS_TOP = 0x01000000;
    /**
     * If set, the new activity is not kept in the list of recently launched
     * activities.
     */
    public static final int FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = 0x00800000;
    /**
     * This flag is not normally set by application code, but set for you by
     * the system as described in the
     * {@link android.R.styleable#AndroidManifestActivity_launchMode
     * launchMode} documentation for the singleTask mode.
     */
    public static final int FLAG_ACTIVITY_BROUGHT_TO_FRONT = 0x00400000;
    /**
     * If set, and this activity is either being started in a new task or
     * bringing to the top an existing task, then it will be launched as
     * the front door of the task.  This will result in the application of
     * any affinities needed to have that task in the proper state (either
     * moving activities to or from it), or simply resetting that task to
     * its initial state if needed.
     */
    public static final int FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 0x00200000;
    /**
     * This flag is not normally set by application code, but set for you by
     * the system if this activity is being launched from history
     * (longpress home key).
     */
    public static final int FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000;
    /**
     * If set, this marks a point in the task's activity stack that should
     * be cleared when the task is reset.  That is, the next time the task
     * is brought to the foreground with
     * {@link #FLAG_ACTIVITY_RESET_TASK_IF_NEEDED} (typically as a result of
     * the user re-launching it from home), this activity and all on top of
     * it will be finished so that the user does not return to them, but
     * instead returns to whatever activity preceeded it.
     *
     * <p>This is useful for cases where you have a logical break in your
     * application.  For example, an e-mail application may have a command
     * to view an attachment, which launches an image view activity to
     * display it.  This activity should be part of the e-mail application's
     * task, since it is a part of the task the user is involved in.  However,
     * if the user leaves that task, and later selects the e-mail app from
     * home, we may like them to return to the conversation they were
     * viewing, not the picture attachment, since that is confusing.  By
     * setting this flag when launching the image viewer, that viewer and
     * any activities it starts will be removed the next time the user returns
     * to mail.
     */
    public static final int FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET = 0x00080000;
    /**
     * If set, this flag will prevent the normal {@link android.app.Activity#onUserLeaveHint}
     * callback from occurring on the current frontmost activity before it is
     * paused as the newly-started activity is brought to the front.
     *
     * <p>Typically, an activity can rely on that callback to indicate that an
     * explicit user action has caused their activity to be moved out of the
     * foreground. The callback marks an appropriate point in the activity's
     * lifecycle for it to dismiss any notifications that it intends to display
     * "until the user has seen them," such as a blinking LED.
     *
     * <p>If an activity is ever started via any non-user-driven events such as
     * phone-call receipt or an alarm handler, this flag should be passed to {@link
     * Context#startActivity Context.startActivity}, ensuring that the pausing
     * activity does not think the user has acknowledged its notification.
     */
    public static final int FLAG_ACTIVITY_NO_USER_ACTION = 0x00040000;
    /**
     * If set in an Intent passed to {@link Context#startActivity Context.startActivity()},
     * this flag will cause the launched activity to be brought to the front of its
     * task's history stack if it is already running.
     *
     * <p>For example, consider a task consisting of four activities: A, B, C, D.
     * If D calls startActivity() with an Intent that resolves to the component
     * of activity B, then B will be brought to the front of the history stack,
     * with this resulting order:  A, C, D, B.
     *
     * This flag will be ignored if {@link #FLAG_ACTIVITY_CLEAR_TOP} is also
     * specified.
     */
    public static final int FLAG_ACTIVITY_REORDER_TO_FRONT = 0X00020000;
    /**
     * If set in an Intent passed to {@link Context#startActivity Context.startActivity()},
     * this flag will prevent the system from applying an activity transition
     * animation to go to the next activity state.  This doesn't mean an
     * animation will never run -- if another activity change happens that doesn't
     * specify this flag before the activity started here is displayed, then
     * that transition will be used.  This this flag can be put to good use
     * when you are going to do a series of activity operations but the
     * animation seen by the user shouldn't be driven by the first activity
     * change but rather a later one.
     */
    public static final int FLAG_ACTIVITY_NO_ANIMATION = 0X00010000;
    /**
     * If set, when sending a broadcast only registered receivers will be
     * called -- no BroadcastReceiver components will be launched.
     */
    public static final int FLAG_RECEIVER_REGISTERED_ONLY = 0x40000000;
    /**
     * If set, when sending a broadcast the new broadcast will replace
     * any existing pending broadcast that matches it.  Matching is defined
     * by {@link Intent#filterEquals(Intent) Intent.filterEquals} returning
     * true for the intents of the two broadcasts.  When a match is found,
     * the new broadcast (and receivers associated with it) will replace the
     * existing one in the pending broadcast list, remaining at the same
     * position in the list.
     *
     * <p>This flag is most typically used with sticky broadcasts, which
     * only care about delivering the most recent values of the broadcast
     * to their receivers.
     */
    public static final int FLAG_RECEIVER_REPLACE_PENDING = 0x20000000;
    /**
     * If set, when sending a broadcast <i>before boot has completed</i> only
     * registered receivers will be called -- no BroadcastReceiver components
     * will be launched.  Sticky intent state will be recorded properly even
     * if no receivers wind up being called.  If {@link #FLAG_RECEIVER_REGISTERED_ONLY}
     * is specified in the broadcast intent, this flag is unnecessary.
     *
     * <p>This flag is only for use by system sevices as a convenience to
     * avoid having to implement a more complex mechanism around detection
     * of boot completion.
     *
     * @hide
     */
    public static final int FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT = 0x10000000;
    /**
     * Set when this broadcast is for a boot upgrade, a special mode that
     * allows the broadcast to be sent before the system is ready and launches
     * the app process with no providers running in it.
     * @hide
     */
    public static final int FLAG_RECEIVER_BOOT_UPGRADE = 0x08000000;
