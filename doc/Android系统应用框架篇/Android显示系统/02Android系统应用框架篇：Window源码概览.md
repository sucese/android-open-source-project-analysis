# Android系统应用框架篇：Window源码概览

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作。技术栈主要涉及Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative，数据结构与算法等方面。热爱编程与吉他，喜欢有趣的事物和人。

**关于文章**

>作者的文章首发在[Github](https://github.com/guoxiaoxing)上，也会发在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与[CSDN](http://blog.csdn.net/allenwells)平台上，文章内容主要包含Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative, 数据结构与算法等方面的内容。如果有什么问题，也欢迎发邮件与我交流。

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