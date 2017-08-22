# Android Open Source Project Analysis

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/android_logo.png" width="1000"/>

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**写在前面**

>作者曾经看过很多关于Android源码分析的文章与书籍，这些前辈都写的很好，给Android技术的普及带来了很大的推动作用，随着Android技术的更
新，目前已经来到了Android 7.0 Nougat版本。所以笔者打算根据最新的源码从内核层到框架层再到应用层，从内核空间到用户空间，全面地去分析
Android系统内部的实现原理和设计思路，本系列的文章也会以一种独特的视角来做原理解析，来减轻大家在阅读源码时的枯燥感，好了，让我们开始吧。

**代码版本**

>[android-7.1.1_r1](https://source.android.com/source/build-numbers.html#source-code-tags-and-builds)

**分析思路**

>以某一个支线为起点，从上层往底层，不断地追溯，在各个模块、文件、方法之间来回跳转，反复地阅读，理清整个流程的逻辑。
同时带着思考去看源码，去揣测作者的用意，去理解代码的精妙之处，去思考代码可能存在的缺陷，去总结优秀的代码设计思想。

本系列文章由下至上，从内核层到框架层再到应用层，从内核空间到用户空间，全面的分析内部的实现原理和设计思路。在源码的分析过程中，还会穿插分析源码的
设计模式与编程思想（编程中的抽象、接口、六大原则以及23种设计模式），以下为后续文章的具体安排。

另外，在文章内容的安排上，一般会先去分析流程，再去分析流程中牵扯到的类的作用以及它们的实现细节。这种由线到点的方式会更加生动一些，也更有助于大家
理解。如果我们一上来就去讲这个类的作用与实现，难免有些枯燥，毕竟只有先去用它，才会想知道它是怎么实现的。

**Android系统架构图**

对Android系统的探索就像探索一个宝藏一样，每天分析一点，就完成了藏宝图的一角，直到有一天我们将整个宝藏的蓝图绘制完成。

官方藏宝图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/android_system_structure.png"/>

探索中的个人版藏宝图

文章更新中...

在正式阅读本系列文章之前，请先阅读导读相关内容，这会帮助你更加快捷的理解文章内容。

- [导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)

## Android系统应用框架篇

**Android显示框架**

- 01Android显示框架：Android显示框架概述
- 02Android显示框架：SufaceFlinger服务概述
- 03Android显示框架：SufaceFlinger服务启动流程分析
- 04Android显示框架：SufaceFlinger服务创建与绘制Surface流程
- 05Android显示框架：SufaceFlinger服务与应用的连接流程
- 06Android显示框架：SufaceFlinger服务渲染应用UI流程
- 07Android显示框架：WndowManagerService概述
- 08Android显示框架：WindowManagerService窗口大小与位置的计算
- 09Android显示框架：WindowManagerService窗口的组织方式
- 10Android显示框架：WindowManagerService窗口Z轴位置的计算流程
- 11Android显示框架：WindowManagerService中Activity窗口的显示与切换流程
- 12Android显示框架：WindowManagerService中Activity窗口动画的显示流程
- 13Android显示框架：Window概述
- 14Android显示框架：Window的创建、显示与销毁流程
- 15Android显示框架：View概述
- 16Android显示框架：View的创建、显示与销毁流程
- 17Android显示框架：View的测量、布局与绘制流程
- 18Android显示框架：View的事件分发与处理

**Android组件框架**

- [01Android组件框架：Android组件框架概述](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/01Android组件框架：组件框架概述.md)
- [02Android组件框架：Context家族](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/02Android组件框架：Context家族.md)
- [03Android组件框架：ActivityThread家族](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/03Android组件框架：ActivityThread家族.md)
- [04Android组件框架：ActivityManagerService家族](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/04Android组件框架：ActivityManagerService家族.md)
- [05Android组件框架：Activity源码概览](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/005Android组件框架：Activity源码概览.md)
- [06Android组件框架：Activity启动流程(一)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/06Android组件框架：Activity启动流程(一).md)
- [07Android组件框架：Activity启动流程(二)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/07Android组件框架：Activity启动流程(二).md)
- [08Android组件框架：Activity启动流程(三)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/08Android组件框架：Activity启动流程(三).md)
- [09Android组件框架：Service源码概览]()
- [10Android组件框架：Service启动流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/10Android组件框架：Service启动流程.md)
- [11Android组件框架：Service绑定流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/11Android组件框架：Service绑定流程.md)
- 12Android组件框架：Broadcast Receiver源码概览
- 13Android组件框架：Broadcast Receiver注册流程
- 14Android组件框架：Broadcast Receiver发送流程
- 15Android组件框架：Content Provider源码概览
- 16Android组件框架：Content Provider启动流程
- 17Android组件框架：Content Provider共享原理
- 18Android组件框架：Content Provider更新机制

## Android系统底层框架篇

>本篇章从Android源码的底层框架原理，例如：Binder机制、智能指针与虚拟机等。

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