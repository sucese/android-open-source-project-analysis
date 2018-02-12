## Android

- Android系统架构图
- SharePreference性能优化，进程同步。
- 动态布局
- SurfaceView
- BroadcastReceiver与LocalBroadcastReceiver区别
- Handler机制
- Binder机制
- Bundle机制
- Android事件传递机制
- App的启动流程
- APK的编译流程
- APK的安装流程
- 进程和Application的生命周期
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
- View的渲染原理
- Android进程分类
- Activity之间的通信方式
- Activity与Fragment之间的生命周期比较
- OOM如何发生的，是否可以try catch
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

## Java

- ArrayList与LinkedList的区别，应用场景。
- HashMap原理。TreeMap、ConcurrentHashMap原理，SpareArray源码实现
- ThreadLocal原理
- ClassLoader原理，类加载机制
- GC回收策略
- synchronized与ReentrantLock的区别
- volatile
- Java线程池、线程上限、线程池上限
- Java对象的生命周期
- 集合Set、Map都是如何防止Hash碰撞的
- JVM内存区域开线程影响哪块区域
- 死锁如何发生的，如何避免死锁
- JVM内存模型
- 并发集合
- 线程的状态与相关方法
- JNI如何调用Java层代码
- Java注解原理
- 内部类、静态内部类和匿名内部类，以及在项目中的应用
- String为什么要设计成不可变，String Buffer 与 String Builder
- 四种引用
- wait与notify
- 线程如何关闭，如何防止内存泄漏
- Object里的五个方法

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
- 树、B+树的介绍
- 二叉树的深度优先遍历和广度优先遍历
- 判断单链表是否连成环

## 设计模式

- 手写生成者与消费者模式
- 适配器模式、装饰者模式与外观模式的异同
