# Android系统应用框架篇：SurfaceFlinger渲染原理

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作。技术栈主要涉及Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative，数据结构与算法等方面。热爱编程与吉他，喜欢有趣的事物和人。

**关于文章**

>作者的文章首发在[Github](https://github.com/guoxiaoxing)上，也会发在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与[CSDN](http://blog.csdn.net/allenwells)平台上，文章内容主要包含Android/Linux, Java/Kotlin/JVM，Python, JavaScript/React/ReactNative, 数据结构与算法等方面的内容。如果有什么问题，也欢迎发邮件与我交流。

>SurfaceFlinger服务运行在Android系统的System进程中，它负责管理Android系统的帧缓冲区，绘制Android应用的UI。


Android应用与SurfaceFlinger服务的连接流程

http://blog.csdn.net/luoshengyang/article/details/7857163

Android应用与SurfaceFlinger共享UI元数据（SharedClient）的创建流程

http://blog.csdn.net/luoshengyang/article/details/7867340

Android应用请求SurfaceFlinger服务创建Surface流程

http://blog.csdn.net/luoshengyang/article/details/7884628

Android应用请求SurfaceFlinger服务绘制Surface流程

http://blog.csdn.net/luoshengyang/article/details/7932268