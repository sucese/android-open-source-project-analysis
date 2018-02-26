# Android组件管理框架：Android组件管理框架概述

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。


从这篇文章开始，我们正式开始去系统地分析Activity源码、原理、启动模式与启动流程等方面内容。这一系列文章可能是东半球最全面(捂脸:>逃~)的讲解Activity的文章。
<img src="https://github.com/guoxiaoxing/emoji/raw/master/emoji/d_doge.png" width="30" height="30" align="bottom"/>

## 继承体系

[点击查看高清SVG大图](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/1/UMLClassDiagram-app-ActivityGroup.png)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/1/UMLClassDiagram-app-ActivityGroup.png"/>

我们来介绍下上图中主要的类

- Context：抽象类，应用的全局运行环境。
- ContextWrapper：继承于Context，Context的代理类。ContextWrapper里的方法都最终调用Context里的方法来实现。
- ContextThemeWrapper：继承于ContextWrapper，可以进行主题修改。
- Activity：继承于ContextThemeWrapper，展示在用户面前的类，绘制UI，处理用户交互。
- ActivityGroup：继承于Activity，一个屏幕可以包含多个Activity。

## 内部结构

我们再来看看在Activity提供各种功能的内部模块。

- Instrumentation
- IBinder
- ActivityInfo
- ActivityThread
- SearchManager
- Window
- WindowManager


