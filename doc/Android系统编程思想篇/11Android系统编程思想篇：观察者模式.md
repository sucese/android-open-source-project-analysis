# Android编程思想篇：观察者模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>定义对象间一对多的依赖关系，使得每当这个对象状态方式改变时，所有依赖它的对象都可以得到通知并且自动更新。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/observer_pattern_class.png"/>

模式角色

- 抽象被观察者：抽象被观察者把所有观察者的引用保存在一个集合里，每个抽象被观察者都有任意数量的观察者，抽象被观察者提供一个接口，负责
添加和删除观察者。
- 具体被观察者：具体被观察者将有关状态存入具体观察者对象，在状态发生改变时，给注册过的观察者发出通知。
- 抽象观察者：观察者的抽象类，定义了更新接口，以便在得到通知后更新自己。
- 具体观察者：实现了抽象观察者的接口，得到通知后会去更新自己。

应用场景

- 关联行为场景
- 事件多级触发场景
- 跨系统的消息交换场景，例如：消息队列、事件总线等。

观察者模式主要的作用就是对象解耦，将观察者与被观察者完全隔离，只依赖于Observer与Observable的抽象。

优点

- 观察者与被观察者完全隔离，只依赖于Observer与Observable的抽象，可以灵活应对业务变化。
- 增强系统的灵活性和扩展性。

缺点

- 当被观察者包含很多个观察者时，可以会带来运行效率的问题，Java里的通知机制默认是顺序执行的，这种情况下，一个观察者卡顿就会影响
整体的效率。

## 模式实现

我们如果对某个网站感兴趣，回去订阅它的更新，这就是个典型的观察者模式。


## 模式实践
