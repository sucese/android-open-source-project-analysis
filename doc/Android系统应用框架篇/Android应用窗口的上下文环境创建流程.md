# Android系统应用框架篇：SurfaceFlinger服务启动流程分析

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

Android应用在运行的过程中需要访问一些特定的资源和类，这些特定的资源或者类构成了Android应用运行的上下文环境，即Context。Context是一个抽象类，ContextImpl继承了Context，
并实现它的抽象方法。

因此，每个Activity组件关联的是ContextImpl对象，它们的类图关系如下：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/Context_class.png"/>