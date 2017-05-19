# Android系统应用框架篇：Service启动流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

从这篇文章开始，我们正式来分析Android的UI框架，包括Window、WindowManager、WindowManagerService等组件的实现原理。

>Window表示Android的窗口，它是一个抽象类，它的实现类是PhoneWindow，Android的所有视图都是通过Window来呈现的，Window
是View的直接管理者。

什么是窗口？

>Android系统中窗口指的是屏幕上一块用户绘制UI且可以相应用户操作的矩形区域，即独占一块Surface实例的显示区域，例如：Dialog、
Activity界面、Toast都是一个窗口。

什么是Surface？

>Android系统中Surface代表一块画布，应用可以通过Canvas或者OpenGL在上面绘制界面，然后通过SurfaceFlinger将Surface的内容
输出到FrameBuffer上，从而将界面显示给用户。

因此未来管理这些Surface的输出而提供了WindowMangerService。