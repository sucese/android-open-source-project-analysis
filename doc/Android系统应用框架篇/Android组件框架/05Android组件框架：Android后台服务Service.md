# Android组件框架：Android后台服务Service

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

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/service_create_structure.png" height="600"/>

1. ActivityManagerService通过Socket方式向Zygote进程请求生成（fork）新的进程用来承载Service。
2. Zygote进程调用fork()方法创建新的进程，并将ActivityThread相关资源加载到新进程。
3. 新进程创建完成以后，ActivityMangerService通过Binder方式向新生成的ActivityThread进程请求创建Service。
4. Service创建完成以后，ActivityThread启动Service。

Service启动流程序列图如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/service_start_sequence.png"/>

从整个序列图我们还可以看出，Service的启动流程涉及到4个进程，按颜色划分，如下所示：

- 启动者Activity所在进程
- 被启动者Service所在进程
- ActivityServiceManager所在进程（system_server进程）
- Zygote进程

Service的启动流程如下所示：

1. Activity所在进程进程采用Binder IPC向system_server进程发起startService请求；
2. system_server进程接收到请求后，向zygote进程发送创建进程的请求；
3. zygote进程fork出新的子进程Remote Service进程；
4. Remote Service进程，通过Binder IPC向sytem_server进程发起attachApplication请求；
5. system_server进程在收到请求后，进行一系列准备工作后，再通过binder IPC向remote Service进程发送scheduleCreateService请求；
6. Remote Service进程的binder线程在收到请求后，通过handler向主线程发送CREATE_SERVICE消息；
7. 主线程在收到Message后，通过发射机制创建目标Service，并回调Service.onCreate()方法。


## 三 Service绑定流程

Service绑定流程序列图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/service_bind_sequence.png"/>

1. ClientActivity组件向ActivityManagerService发送一个绑定ServerService组件的进程间通信请求。
2. ActivityManagerService发现用来运行ServerService组件与ClientActivity组件运行在同一个进程里，它
便直接通知该进程将该erverService组件启动起来。
3. 该erverService组件启动起来以后，ActivityManagerService就请求它返回一个Binder本地对象，以便
ClientActivity组件可以通过这个Binder对象与ServerService组件建立连接。
4. ActivityManagerService将从ServerService组件获得的Binder对象返回给调用者ClientActivity。
5. ClientActivity获得到ActivityManagerService发送给它的Binder对象后，它就可以通过这个BInder对象
获得ServerService组件的一个访问接口，从而获得ServerService的服务，这样便相当于ServerService组件
绑定在ClientActivity组件内部了。
