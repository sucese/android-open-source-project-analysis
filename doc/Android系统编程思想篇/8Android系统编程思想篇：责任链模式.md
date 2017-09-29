# Android编程思想篇：责任链模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>使多个对象都有机会处理请求，避免了请求的发送者与接受者的耦合关系，将这些对象连成一条链，并沿着这条链传递该请求。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/responsiblilty_chain_pattern_class.png"/>

模式角色

- AbstractHandler：抽象处理者
- RealHandler：具体处理者

应用场景

- 多个对象可以处理一个请求，需要动态决定哪个对象最终处理该请求
- 多个对象需要处理同一个请求

优点

- 将请求者与处理这的关系解耦，提交代码的灵活性。

缺点

- 对责任链中的处理者的遍历，在处理者比较多时会带来不小的性能问题，尤其是在递归中的调用。

## 模式实现

我们平时在公司报销发票是由领导审批的，但是不同等级的领导能够审批的额度是不一样的，这就是一个典型的责任链场景。

## 模式实践
