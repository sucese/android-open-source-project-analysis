# Android系统应用框架篇：Service启动流程

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

在Java层，Service Manager的代理对象是ServiceManagerProxy，它实现了IServiceManager接口，如下图所示：

从上图我们可以了解：

- Binder：该类用于实现一个Java服务，它实现了IBinder接口，它内部有个int mObject变量，它指向C++层中的一个Binder本地对象。这样我们就完成了
一个Java服务与C++层中的一个Binder本地对象的关联，即可以通过C++层中的本地对象来实现Java层的服务。
- ServiceManagerNative：继承与BInder，实现了IServiceManager接口，它内部还有一个ServiceManagerProxy子类。


## ServiceManager的Java代理对象的创建

ServiceManager的Java代理对象是由ServiceManager开始创建的，它调用ServiceManagerNative.asInterface(BinderInternal.getContextObject()方法来创建
代理对象。

```java
public final class ServiceManager {
    
    private static IServiceManager sServiceManager;
    
    private static IServiceManager getIServiceManager() {
        if (sServiceManager != null) {
            return sServiceManager;
        }

        // Find the service manager
        sServiceManager = ServiceManagerNative.asInterface(BinderInternal.getContextObject());
        return sServiceManager;
    }
}
```
这个方法分为两步：

1. 首先调用BinderInternal.getContextObject()创建一个句柄值等于0的Java服务代理对象。
2. 然后调用ServiceManagerNative.asInterface(BinderInternal.getContextObject())方法将该Java服务代理对象封装成一个ServiceManagerProxy对象，并
保存在静态变量sServiceManager中。

>句柄是一种对象的标识符，拥有句柄就可以对对象进行操作。

我们分别来看，

```java
public class BinderInternal {
    /**
     * Return the global "context object" of the system.  This is usually
     * an implementation of IServiceManager, which you can use to find
     * other services.
     */
    public static final native IBinder getContextObject();
}
```