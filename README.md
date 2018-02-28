# <img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/logo.png" alt="Android open source project analysis" width="80" height="80" align="bottom"/> Android open source project analysis

## 功能介绍

[![License](https://img.shields.io/github/license/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 
[![Stars](https://img.shields.io/github/stars/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 
[![Stars](https://img.shields.io/github/forks/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 
[![Forks](https://img.shields.io/github/issues/guoxiaoxing/android-open-source-project-analysis.svg)](https://jitpack.io/#guoxiaoxing/android-open-source-project-analysis) 

- [点击进入GitBook阅读](https://www.gitbook.com/book/guoxiaoxing/android-open-source-project-analysis/details)

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

- 不要去记录那些API调用链，绘制一个序列图理清思路即可，Android Framework中有很多复杂的API调用链，你去关注这些东西，用处不大。你需要学会的是跟踪调用链和梳理流程的
技巧，思考一下作者是怎么找到关键入口的，核心的实现在什么地方。
- 要善于思考，要多问为什么，面对一个模块，你要去思考这个模块解决了什么问题，这个问题的本质是什么，为什么这么解决，如果让我来写，我会怎么设计。事实上不管是是计算机还是
手机，从CPU、到内存、到操作系统、到应用层，看似纷繁复杂，但问题的本质无非就是这么几种：时间片怎么分配？线程/进程怎么调度？通信的机制是什么？只是在不同的场景下加了具体
的优化，但问题的本质没有改变，我们要善于抓住本质。
- 要善于去粗存精，Android Framework也是人写的，有精华也有糟粕，并不是每行代码你都需要问个为什么，很多时候没有那么多为什么，只是当时那种情况下就那样设计了。但是
对于关键函数我们要去深究它的实现细节。

**写作风格**

和大家一样，笔者也是在前人的书籍和博客的基础上开始学习Android的底层实现的，站在前人的肩膀上会看的更远。但是这些书籍和博客有个问题在于，文章中罗列了大量的代码，这样
很容易把初学者带入到琐碎的细节之中，所以本系列文章在行文中更多的会以图文并茂和提纲总结的方式来分析问题，关键的地方才会去解析源码，力求让大家从宏观上理解Android的底
层实现。另外，基本上一个主题对应一篇文章，所以文章会比较长，但是文章会有详细的标题划分和提纲总结，可以有的放矢，阅读自己需要的内容。

好了，让我们开始我们的寻宝之旅吧~😆

**Android系统架构图**

Android系统架构图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/android_system_structure.png"/>

从上到下依次分为六层：

- 应用框架层
- 进程通信层
- 系统服务层
- Android运行时层
- 硬件抽象层
- Linux内核层

在正式阅读本系列文章之前，请先阅读导读相关内容，这会帮助你更加快捷的理解文章内容。

- [导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)

## Android系统应用框架篇

**Android窗口管理框架**

- [01Android窗口管理框架：Android窗口管理框架概述](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android窗口管理框架/01Android窗口管理框架：Android窗口管理框架概述.md)
- [02Android窗口管理框架：Android应用视图的载体View](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android窗口管理框架/02Android窗口管理框架：Android应用视图载体View.md)
- [03Android窗口管理框架：Android应用视图的管理者Window](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android窗口管理框架/03Android窗口管理框架：Android应用视图管理者Window.md)
- [04Android窗口管理框架：Android应用窗口管理服务WindowServiceManager](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android窗口管理框架/04Android窗口管理框架：Android应用窗口管理服务WindowServiceManager.md)
- [05Android窗口管理框架：Android布局解析者LayoutInflater](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android窗口管理框架/05Android窗口管理框架：Android布局解析者LayoutInflater.md)
- [06Android窗口管理框架：Android列表控件RecyclerView](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android窗口管理框架/06Android窗口管理框架：Android列表控件RecyclerView.md)

**Android组件管理框架**

- [01Android组件管理框架：Android组件管理框架概述](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/01Android组件管理框架：组件管理框架概述.md)
- [02Android组件管理框架：Android组件管理服务ActivityServiceManager](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/02Android组件管理框架：Android组件管理服务ActivityServiceManager.md)
- [03Android组件管理框架：Android视图容器Activity](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/03Android组件管理框架：Android视图容器Activity.md)
- [04Android组件管理框架：Android视图片段Fragment](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/04Android组件管理框架：Android视图片段Fragment.md)
- [05Android组件管理框架：Android后台服务Service](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/05Android组件管理框架：Android后台服务Service.md)
- [06Android组件管理框架：Android内容提供者ContentProvider](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/06Android组件管理框架：Android内容提供者ContentProvider.md)
- [07Android组件管理框架：Android广播接收者BroadcastReceiver](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/07Android组件管理框架：Android广播接收者BroadcastReceiver.md)
- [08Android组件管理框架：Android应用上下文Context](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件管理框架/08Android组件管理框架：Android应用上下文Context.md)

**Android包管理框架**

- [01Android包管理框架：APK的打包流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android包管理框架/01Android包管理框架：APK的打包流程.md)
- [02Android包管理框架：APK的安装流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android包管理框架/02Android包管理框架：APK的安装流程.md)
- [03Android包管理框架：APK的加载流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android包管理框架/03Android包管理框架：APK的加载流程.md)

**Android资源管理框架**

- [01Android资源管理框架：资源管理器AssetManager](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android资源管理管理框架/01Android资源管理框架：资源管理器AssetManager.md)

## Android系统底层框架篇

**Android进程框架**

- [01Android进程框架：进程的创建、启动与调度流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android进程框架/01Android进程框架：进程的创建、启动与调度流程.md)
- [02Android进程框架：线程与线程池](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android进程框架/02Android进程框架：线程与线程池.md)
- [03Android进程框架：线程通信的桥梁Handler](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android进程框架/03Android进程框架：线程通信的桥梁Handler.md)
- [04Android进程框架：进程通信的桥梁Binder](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android进程框架/04Android进程框架：进程通信的桥梁Binder.md)
- [05Android进程框架：进程通信的桥梁Socket](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android进程框架/05Android进程框架：进程通信的桥梁Socket.md)

**Android内存框架**

- [01Android内存框架：内存管理系统](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android内存框架/01Android内存框架：内存管理系统.md)
- [02Android内存框架：Ashmem匿名共享内存系统](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android内存框架/02Android内存框架：Ashmem匿名共享内存系统.md)

**Android虚拟机框架**

- [01Android虚拟机框架：Java类加载机制](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android虚拟机框架/01Android虚拟机框架：Java类加载机制.md)

## Android应用开发实践篇

**Android界面开发**

- [01Android界面开发：View自定义实践概览](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/01Android界面开发：View自定义实践概览.md)
- [02Android界面开发：View自定义实践布局篇](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/02Android界面开发：View自定义实践布局篇.md)
- [03Android界面开发：View自定义实践绘制篇](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/03Android界面开发：View自定义实践绘制篇.md)
- [04Android界面开发：View自定义实践交互篇](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android界面开发/04Android界面开发：View自定义实践交互篇.md)

**Android应用优化**

- [01Android应用优化：兼容适配实践指南](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android应用优化/01Android应用优化：兼容适配实践指南.md)
- [02Android应用优化：性能优化实践指南](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/Android应用优化/02Android应用优化：性能优化实践指南.md)

**其他**

- [01Android混合编程：WebView实践](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/其他/01Android混合编程：WebView实践.md)
- [02Android网络编程：网络编程实践](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android应用开发实践篇/其他/02Android网络编程：网络编程实践.md)

## Android系统软件设计篇

- [01Android系统软件设计篇：软件设计原则](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统软件设计篇/01Android系统软件设计篇：软件设计原则.md)
- [02Android系统软件设计篇：设计模式](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统软件设计篇/02Android系统软件设计篇：设计模式.md)

有兴趣参与此项目的小伙伴可以扫码入群，本群主要讨论Android Framework、主流开源框架以及Android工程化相关技术，本群不是一个读者群，希望大家每个人都能成为项目的参与者。如果微信群提示满额了可以
加我微信 allenwells 邀请入群。

<img src="https://user-gold-cdn.xitu.io/2018/2/26/161d0b402e9616a6?w=674&h=896&f=jpeg&s=118219" width="300"/>

后续的技术文章和资料也会做成PDF发送给读者，为了方便群文件管理，这里也建立了一个QQ群。

<img src="https://user-gold-cdn.xitu.io/2018/2/26/161d0b15a98f790d?w=540&h=740&f=jpeg&s=90598" width="300"/>

**关于此项目后续的计划**

一个人的力量是有限的，后续此项目会做成一个团体项目，在Github建立新的工作组和新的Repo，设计新的名字和Logo，每篇文章会在文章头部注明作者的名字和Github账号。希望有更多的小伙伴参与进来，不仅仅
是学习源码原理，也可以提升自己的技术品牌。
