# Android 高级开发工程师面试题集

## Java

### 描述一个类的加载过程？

Person person = new Person()

1. 查找Person.class，并加载到内存中。
2. 执行类里的静态代码块。
3. 在堆内存里开辟内存空间，并分配内存地址。
4. 在堆内存里建立对象的属性，并进行默认的初始化。
5. 对属性就行显示初始化。
6. 对对象进行构造代码块初始化。
7. 调用对象的构造函数进行初始化。
8. 将对象的地址赋值给person变量。

### Java对象的生命周期是什么？

### 描述一下类的加载机制？

### 描述一下GC的原理和回收策略？

### 接口和抽象类有什么区别？

### 内部类、静态内部类在业务中的应用场景是什么？

### synchronized与ReentrantLock有什么区别？

### volatile的原理是什么？

### 线程为什么阻塞，多线程并发如何实现？

### 线程如何关闭，如何避免线程内存泄漏？

### ThreadLocal的原理了解吗？

### wait和notify机制，手写一下生产者和消费者模型？

### 描述一下线程的几种状态？

### 死锁是如何发生的，如何避免死锁？

### ArrayList与LinkedList有什么区别，应用场景是什么？

### HashMap是如何解决hash碰撞的？

### HashMap原理。TreeMap、ConcurrentHashMap的实现原理是什么？

### SpareArray做了哪些优化？

### 了解Java注解的原理吗？

### String为什么要设计成不可变，String Buffer与String Builder有什么区别？

## Android

### 手画一下Android系统架构图，描述一下各个层次的作用？

### 描述一下Android的事件分发机制？

### 描述一下View的绘制原理？

### 了解APK的打包流程吗，描述一下？

### 了解APK的安装流程吗，描述一下？

### 当点击一个应用图标以后，都发生了什么，描述一下这个过程？

### BroadcastReceiver与LocalBroadcastReceiver有什么区别？

### Android Handler机制是做什么的，原理了解吗？

### Android Binder机制是做什么的，原理了解吗？

### 描述一下进程和Application的生命周期？

### Android的内存是如何管理的？

### Android有哪几种进程，是如何管理的？

###  OOM如何发生的，是否可以try catch


- SharePreference性能优化，进程同步。
- 动态布局
- SurfaceView
- 
- Bundle机制
- 权限管理系统
- 系统启动流程
- RecyclerView与ListView的区别
- Android事件的分发机制
- 进程的状态与调度
- 有序广播和标准广播的区别
- Service生命周期
- 数据库数据迁移问题
- 为什么设计Content Provider，进程共享和线程安全问题。
- Service与Activity的数据交互
- onStop做了网络请求，在onResume如何恢复
- Android进程分类
- Activity之间的通信方式
- Activity与Fragment之间的生命周期比较
- SQLite升级，增加字段里的语句
- 多线程读写文件的安全性
- App唤醒其他进程的方式
- 开启摄像头的步骤
- Activity上有Dialog按Hone键的生命周期变化
- 有几种Context对象，有何区别
- ViewPager的实现细节，懒加载如何实现
- 序列化，Android为什么引入Parcelable
- AIDL机制
- Android内进程的分配，能不能自己分配定额内存
- 后台保活

## 网络编程

- Https请求慢的解决办法，哪里用了对称加密，哪里用了非对称加密。
- TCP与UDP的区别
- WebSocket与Socket的区别

## 应用优化

- 冷启动时长优化
- 应用稳定性
- 应用保活
- 性能优化，保证应用不卡顿
- 内存优化、网络优化、布局优化、电量优化、业务优化
- View嵌套层级
- 多线程短短续传的原理
- ANR的原因，怎么分析，如何解决
- 内存泄漏的原因

## 工程实践

- App 热修复
- App 插件化
- App 模块化
- App 沙箱化
- MVP的实现

## 开源类库

- Fresco，图片加载机制
- LruCache & DiskLruCache
- Okhttp 网络缓存的实现
- RxJava实现原理
- EventBus的功能和原理，代替EventBus的方式

## 数据结构与算法

- 排序、快速排序、堆排序的实现，时间复杂度，空间复杂度
- 单链表反转，合并多个单链表
- 数组和链表的区别
- 树、B+树的介绍
- 二叉树的深度优先遍历和广度优先遍历
- 判断单链表是否连成环

## 设计模式

- 手写生成者与消费者模式
- 适配器模式、装饰者模式与外观模式的异同
