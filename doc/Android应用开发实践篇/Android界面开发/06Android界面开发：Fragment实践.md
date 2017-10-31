# Android界面开发：Fragment实践

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

我们为什么要选择Fragment代替Activity，实现单Activity+多Fragment或者多模块+多Fragment的架构？🤔

- Fragment相对于Activity，无需ActivityManagerService的跨进程通信，切换更加轻量级，响应速度更快，占用资源更少。
- Fragment相对于View，拥有更多的声明周期，可以管理Menu，持有Activity的引用，它可以将负责的业务逻辑解耦并可以进行组合使用，更利于模块化，并为app向pad平台扩展提供支持。

这便是我们使用Fragment两个最主要的原因。
