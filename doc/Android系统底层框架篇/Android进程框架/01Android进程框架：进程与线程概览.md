# Android系统应用框架篇：Binder进程通信系统

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，爱好广泛，技术栈主要涉及以下几个方面
>
>- Android/Linux
>- Java/Kotlin/JVM
>- Python
>- JavaScript/React/ReactNative
>- DataStructure/Algorithm
>
>文章首发于[Github](https://github.com/guoxiaoxing)，后续也会同步在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与
[CSDN](http://blog.csdn.net/allenwells)等博客平台上。文章中如果有什么问题，欢迎发邮件与我交流，邮件可发至guoxiaoxingse@163.com。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。


Android的系统服务运行在系统进程中，而即便是四大组件也有可能运行在不同的进程中，因为对于Android来说，一套高效简便的通信机制
显得十分重要。我们知道Linux内核本身提供了多套进程通信机制：

- Pipe（管道）
- Signal（信号）
- Message（消息队列）
- Share Memory（共享内存）
- Socket（套接字）

但是Android并没有采用这些方案，而是基于[OpenBinder](https://en.wikipedia.org/wiki/OpenBinder)实现了一套Binder方案，它
采用C/S模式，分为Client进程与Server进程，一个Server进程可以向多个Client进程提供服务，一个Client进程也可以向多个Server进程
请求服务，Client进程与Server进程之前的通信通过Binder驱动程序来完成。它们的关系如下：

从上图可以看出

用户空间：Client、Service与Service Manager
内核空间：Binder驱动程序

Client、Service与Service Manager都是通过调用open()、mmap()与ioctl()来访问Binder驱动程序来实现间接通信。