# Android组件框架：Android广播接收者Broadcast Receiver.

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

- 一 Service生命周期
- 二 Service启动流程
- 三 Service绑定流程

## 一 Service生命周期

## 二 Service启动流程

应用通过startService()或者bindService()方法去启动或者绑定Service的过程主要是通过ActivityManagerService来完成，Service启动的过程除了Service组件的创建
还包括Service所在进程（如果没有创建的话）的创建，具体流程如下图所示：

1. ActivityManagerService通过Socket方式向Zygote进程请求生成（fork）新的进程用来承载Service。
2. Zygote进程调用fork()方法创建新的进程，并将ActivityThread相关资源加载到新进程。
3. 新进程创建完成以后，ActivityMangerService通过Binder方式向新生成的ActivityThread进程请求创建Service。
4. Service创建完成以后，ActivityThread启动Service。


Service启动流程序列图如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/service_start_sequence.png" height="500"/>

1. 向ActivityManagerService发送一个启动Service组件的请求。
2. ActivityManagerService发现用来运行Service组件的进程不存在，它会先保存Service组件的信息，接着再创建一个新的应用进程。
3. 新的应用进程创建完成后，就会向ActivityManagerService发送一个启动完成的进程间通信请求，以便ActivityManagerService可
以继续执行启动Service组件的的操作。
4. ActivityManagerService将第2步保存的Service组件信息发送给新床架的应用进程，以便它可以将Service组件启动起来。

## 三 Service绑定流程

Service绑定流程序列图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/service_bind_sequence.png" height="500"/>

1. ClientActivity组件向ActivityManagerService发送一个绑定ServerService组件的进程间通信请求。
2. ActivityManagerService发现用来运行ServerService组件与ClientActivity组件运行在同一个进程里，它
便直接通知该进程将该erverService组件启动起来。
3. 该erverService组件启动起来以后，ActivityManagerService就请求它返回一个Binder本地对象，以便
ClientActivity组件可以通过这个Binder对象与ServerService组件建立连接。
4. ActivityManagerService将从ServerService组件获得的Binder对象返回给调用者ClientActivity。
5. ClientActivity获得到ActivityManagerService发送给它的Binder对象后，它就可以通过这个BInder对象
获得ServerService组件的一个访问接口，从而获得ServerService的服务，这样便相当于ServerService组件
绑定在ClientActivity组件内部了。
