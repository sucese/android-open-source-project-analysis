# Android系统应用框架篇：WIndowManagerService窗口的组织方式

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**


Android系统的窗口与Activity类似，也是以堆栈的方式组织在WindowManagerService之中，其中Z轴位置较低的窗口位于Z轴位置较高的窗口下面。

WindowManagerService之中定义了三个类：

- WindowState：实现了WindowManagerPolicy.WindowState接口，它描述了Android系统中的窗口信息。
- WindowToken：窗口令牌，它描述了窗口类型、可见性等属性，这些属性的一组窗口具有相同的令牌。
- AppWindowToken：继承于WindowToken，用来描述应用的Activity窗口。

AppWindowToken相关操作

1 移除

```java
private void removeAppTokensLocked(List<IBinder> tokens) 
```

