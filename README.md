# Android Open Source Project Analysis

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/android_7_nougat.jpg" width="1000"/>

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

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

**Android系统架构图**

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/android_system_structure.png"/>

文章更新中...

在正式阅读本系列文章之前，请先阅读导读相关内容，这会帮助你更加快捷的理解文章内容。

- [导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)

## Android系统应用框架篇

**Android交互框架**

- 1Android系统应用框架篇：Window源码概览
- 2Android系统应用框架篇：Window创建里程
- 3Android系统应用框架篇：Window内部机制
- 4Android系统应用框架篇：View源码概览
- 5Android系统应用框架篇：View工作原理
- 6Android系统应用框架篇：View事件体系

**Android组件框架**

- [1Android系统应用框架篇：Activity源码概览](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/1Android系统应用框架篇：Activity源码概览.md)
- [2Android系统应用框架篇：Activity生命周期](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/2Android系统应用框架篇：Activity生命周期.md)
- [3Android系统应用框架篇：Activity启动模式](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/3Android系统应用框架篇：Activity启动模式.md)
- [4Android系统应用框架篇：Activity启动标识](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/4Android系统应用框架篇：Activity启动标识.md)
- [5Android系统应用框架篇：Activity启动流程(一)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/5Android系统应用框架篇：Activity启动流程(一).md)
- [6Android系统应用框架篇：Activity启动流程(二)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/6Android系统应用框架篇：Activity启动流程(二).md)
- [7Android系统应用框架篇：Activity启动流程(三)](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/7Android系统应用框架篇：Activity启动流程(三).md)
- 8Android系统应用框架篇：Service源码概览
- 9Android系统应用框架篇：Service生命周期
- [10Android系统应用框架篇：Service启动流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android组件框架/10Android系统应用框架篇：Service启动流程.md)
- 11Android系统应用框架篇：Service绑定流程
- 12Android系统应用框架篇：Broadcast Receiver源码概览
- 13Android系统应用框架篇：Broadcast Receiver注册流程
- 14Android系统应用框架篇：Broadcast Receiver发送流程
- 15Android系统应用框架篇：Content Provider源码概览
- 16Android系统应用框架篇：Content Provider启动流程
- 17Android系统应用框架篇：Content Provider共享原理
- 18Android系统应用框架篇：Content Provider更新机制

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
- Android系统驱动篇：

**Android系统编程思想篇**

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