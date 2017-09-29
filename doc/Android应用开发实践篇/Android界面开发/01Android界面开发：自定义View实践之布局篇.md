# Android界面开发：自定义View实践之布局篇

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

前面我们讲解了Activity视图的创建与渲染流程：

- [04Android显示框架：Activity应用视图的创建流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/04Android显示框架：Activity应用视图的创建流程.md)
- [05Android显示框架：Activity应用视图的渲染流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/05Android显示框架：Activity应用视图的渲染流程.md)

今天我们来进行ui系列原理分析的的最后一篇文章，自定义View实践。前面的文章都是为了这篇文章做铺垫。

自定义View是开发中最常见的需求，图表等各种复杂的ui以及产品经理各种奇怪的需求😤都要通过自定义View来完成。

自定义View有三个关键点：

- 布局：决定View的摆放位置
- 绘制：决定View的具体内容
- 触摸反馈：决定View与用户的交互体验

这篇文章我们就来分析关于自定义View的布局问题。要想彻底掌握自定义View的布局，就要理解View的布局实现，这个我们在前面的文章分析过源码，这里再来整体总结一下。



