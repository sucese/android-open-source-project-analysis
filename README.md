# <img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/logo.png" alt="Android open source project analysis" width="80" height="80" align="bottom"/> Android open source project analysis

## 功能介绍

[![License](https://img.shields.io/github/license/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 
[![Stars](https://img.shields.io/github/stars/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 
[![Stars](https://img.shields.io/github/forks/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 
[![Forks](https://img.shields.io/github/issues/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

- [Git repositories on android](https://android.googlesource.com/)
- [Android Open Source Project](https://source.android.com/)

**代码版本**

- 细分版本：N6F26U	
- 分支：android-7.1.1_r28	
- 版本：Nougat	
- 支持设备：Nexus 6

**分析思路**

Android是一个庞大的系统，Android Framework只是对系统的一个封装，里面还牵扯到JNI、C++、Java虚拟机、Linux系统内核、指令集等。面对如此庞大的系统，我们得有一定的
章法去阅读源码，否则就会只见树木不见森林，陷入卷帙浩繁的细节与琐碎之中。

- 不要就记录那些API调用链，Android Framework中有很多复杂的API调用链，你去关注这些东西，一点用都没有。你需要学会的是跟踪调用链和梳理流程的技巧，思考一
下作者是怎么找到关键入口的，核心的实现在什么地方。
- 要有宏观思维，要善于思考，面对一个模块，你要去思考这个模块解决了什么问题，为什么这么解决，如果让我来写，我会怎么设计，不要陷入无穷无尽的细节之中。
- 要善于去粗存精，Android Framework也是人写的，有精华也有糟粕，并不是每行代码你都需要问个为什么，很多时候没有那么多为什么，只是当时那种情况下就那样设计了。但是
对于关键函数我们要去深究它的实现细节。

**写作风格**

和大家一样，笔者也是在前人的书籍和博客的基础上开始学习Android的底层实现的，站在前人的肩膀上会看的更远。但是这些书籍和博客有个问题在于，文章中罗列了大量的代码，这样
很容易把初学者带入到琐碎的细节之中，所以本系列文章在行文中更多的会以图文并茂的方式来分析问题，关键的地方才会去解析源码，力求为大家从宏观上理解Android的底层实现。

好了，让我们开始我们的寻宝之旅吧~😆

**Android系统架构图**

官方架构图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/aosp_structure.png"/>

在正式阅读本系列文章之前，请先阅读导读相关内容，这会帮助你更加快捷的理解文章内容。

- [导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)

## Android应用开发实践篇

**Android界面开发**

- [01Android界面开发：View自定义实践概览](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/01Android界面开发：View自定义实践概览.md)
- [02Android界面开发：View自定义实践布局篇](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/02Android界面开发：View自定义实践布局篇.md)
- [03Android界面开发：View自定义实践绘制篇](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/03Android界面开发：View自定义实践绘制篇.md)
- [04Android界面开发：View自定义实践交互篇](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/04Android界面开发：View自定义实践交互篇.md)

**Android兼容适配**

**Android混合编程**

- [01Android混合编程：WebView实践](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android混合编程/01Android混合编程：WebView实践.md)

**Android性能优化**

## Android系统应用框架篇

**Android显示框架**

- [01Android显示框架：Android显示框架概述](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/01Android显示框架：Android显示框架概述.md)
- [02Android显示框架：Android应用视图的载体View](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/02Android显示框架：Android应用视图载体View.md)
- [03Android显示框架：Android应用视图的管理者Window](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/03Android显示框架：Android应用视图管理者Window.md)
- [04Android显示框架：Android应用窗口的管理者WindowManager](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/04Android显示框架：Android应用窗口的管理者WindowManager.md)
- 05Android组件框架：Android布局渲染者LayoutInflater

**Android组件框架**

- [01Android组件框架：Android组件框架概述](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/01Android组件框架：组件框架概述.md)
- 02Android组件框架：Android组件管理者ActivityManager
- [03Android组件框架：Android视图容器Activity](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/03Android组件框架：Android视图容器Activity.md)
- [04Android组件框架：Android视图片段Fragment](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/04Android组件框架：Android视图片段Fragment.md)
- 05Android组件框架：Android后台服务Service
- 06Android组件框架：Android广播Broadcast Receiver
- 07Android组件框架：Android数据共享Content Provider

**Android动画框架**

**Android多媒体框架**

## Android系统底层框架篇

**Android进程框架**

- Android系统底层框架篇：进程与线程概览
- Android系统底层框架篇：Zygote与System进程启动流程
- Android系统底层框架篇：应用进程启动流程
- Android系统底层框架篇：应用消息处理机制
- Android系统底层框架篇：Binder进程通信机制

**Android内存框架**

- Android系统底层框架篇：Ashmem匿名共享内存系统
- Android系统基础篇：硬件抽象层
- Android系统基础篇：智能指针
- Android系统基础篇：ART/Dalvik 虚拟机
- Android系统驱动篇：Binder进程通信系统

## Android系统编程思想篇

- [1Android系统编程思想篇：单例模式](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统编程思想篇/1Android系统编程思想篇：单例模式.md)
- [2Android系统编程思想篇：建造者模式](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统编程思想篇/2Android系统编程思想篇：建造者模式.md)
- 3Android系统编程思想篇：原型模式
- 4Android系统编程思想篇：工厂模式
- 5Android系统编程思想篇：抽象工厂模式
- 6Android系统编程思想篇：策略模式
- 7Android系统编程思想篇：状态模式
- 8Android系统编程思想篇：责任链模式
- 9Android系统编程思想篇：解释器模式
- 10Android系统编程思想篇：命令模式
- 11Android系统编程思想篇：观察者模式
- 12Android系统编程思想篇：备忘录模式
- 13Android系统编程思想篇：迭代器模式
- 14Android系统编程思想篇：模板方法模式
- 15Android系统编程思想篇：访问者模式
- 16Android系统编程思想篇：中介者模式
- [17Android系统编程思想篇：代理模式](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统编程思想篇/17Android系统编程思想篇：代理模式.md)
- 18Android系统编程思想篇：组合模式
- 19Android系统编程思想篇：适配器模式
- 20Android系统编程思想篇：装饰模式
- 21Android系统编程思想篇：享元模式
- 22Android系统编程思想篇：外观模式
- 23Android系统编程思想篇：桥接模式
- 24Android系统编程思想篇：软件设计原则