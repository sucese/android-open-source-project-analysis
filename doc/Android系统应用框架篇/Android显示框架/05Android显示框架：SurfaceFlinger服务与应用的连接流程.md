# Android系统应用框架篇：SurfaceFlinger服务与应用的连接流程

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

>SurfaceFlinger运行在Android系统的System进程中，管理着Android系统的帧缓冲区（Frame Buffer），负责渲染整个系统的UI。

## SurfaceFlinger服务概述

Android应用与SurfaceFlinger服务运行在不同的进程中，它们通过Binder进程间通信机制来进行通信。Android应用在通知SurfaceFlinger服务绘制
自己的UI的时候，需要将UI元数据传递给SurfaceFlinger服务。当我们的应用界面很多时，这些元数据的量是非常大的，在这种情况下，通过Binder来传递
数据显然是不合适的，这个时候我们就需要借助匿名共享内存（Anonymous Shared Memory）来传递UI元数据。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/view/05/surface_flinger_structure.png"/>

这里简单介绍下匿名共享内存，后续会有专门的文章来分析这个内容。

应用并不会直接去使用匿名共享内存，匿名共享内存会被封住成一个SharedClient对象，每个SharedClient内部至多有31个SharedBufferStack，每个
SharedBufferStack都对应着应用中的一个Surface。所以最终SharedBufferStack存储了这些UI元数据，并在应用与SurfaceFlinger之间共享，原理
如下图所示：

>SharedBufferStack：SharedBufferStack用来存储UI元数据的缓冲区。

我们再来看看SharedBufferStack的结构

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/view/05/shared_buffer_stack_structure.png"/>

注：Buffer-1，Buffer-2是已经使用的缓冲区，后面的为空闲缓冲区。

注意这是一个Stack结构额数据，它内部有3个指针：

- queue_head：执行已经使用缓冲区的列表的头部
- tail：空闲缓冲区的尾部
- head：空闲缓冲区的头部

已经使用的缓冲区是GraphicBuffer来描述，它才是真正保存UI元数据的地方。我们再来说说空闲缓冲区，当Android应用需要更新一个Surface的
时候，它就会执行以下流程：

```
1 应用找到与该Surface对应的SharedBufferStack，并从它的空闲缓冲区尾部取出一个空闲的Buffer.
2 应用请求SurfaceFlinger为这个Buffer分配一个图形缓存区GraphicBuffer。
3 SurfaceFlinger分配好GraphicBuffer并将它返回给应用访问。
4 应用得到GraphicBuffer后就往它里面写入UI元数据。写完之后就将它插入已使用缓存栈的头部去。
5 SurfaceFlinger绘制完成该GraphicBuffer后，它又会重新变成空闲缓冲区。
```

所以你可以看到，应用端关心的是SharedBufferStack的空闲缓存区，它要去申请这些缓存区写入UI元数据，SurfaceFlinger端关心的
是已经使用的缓冲区，它去这些缓冲区读取UI元数据进行绘制。

为了方便对SharedBufferStack的访问，Android系统分别使用SharedBufferClient与SharedBufferServer来描述

## SurfaceFlinger服务与应用的连接流程

上文中提到SurfaceFlinger服务与应用运行在不同的进程中，应用需要与SurfaceFlinger服务建立连接，然后通过这个连接去请求SurfaceFlinger服务创建与渲染Surface。

我们以Android系统开机动画为例来分析服务的连接流程。

Android系统开机动画是由应用程序BootAnimation，它是一个由C++写的应用，实现在BootAnimation.cpp中。BootAnimation继承了Thread类

frameworks/base/cmds/bootanimation/BootAnimation.cpp

