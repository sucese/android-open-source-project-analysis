# Android系统应用框架篇：Service启动流程

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/view/01/android_ui_system.png"/>

从上图可以看出，Android的显示系统分为3层：

- UI框架层：负责管理窗口中View组件的布局与绘制以及响应用户输入事件
- WindowManagerService层：负责管理窗口Surface的布局与次序
- SurfaceFlinger层：将WindowManagerService管理的窗口按照一定的次序显示在屏幕上

接下来的文章将会从上到下依次分析整个Android的显示系统。


