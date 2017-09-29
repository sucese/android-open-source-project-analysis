# Android编程思想篇：状态模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>当一个对象的内在状态改变时允许改变其行为，这个对象看起来像是改变其类。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/state_pattern_class.png"/>

状态模式与我们前面说的策略模式在结构上非常相似，但是本质却完全不一样，状态模式的行为是平行的，不可替换的，策略模式的行为是彼此独立的，可以替换的。

模式角色

- Context：用来操作状态的上下文环境
- State：抽象状态
- StateA/StateB：具体状态

应用场景

- 一个对象的行为取决于它的状态，并且在运行时可以根据状态改变它的行为。
- 代码里包含了大量与状态对象相关的判断语句，不同的条件下有不同的行为。

状态模式就是在不同的状态下对于同一行为的不同响应，这其实就将if-else用多态来实现的一个例子。

优点

- 状态模式将行为与一个具体的状态对象绑定，这样使得状态的判断与行为的执行更加可靠，也提升了程序的扩展性和可维护性。

缺点

- 状态的增加会增加类和对象的个数

## 模式实现

## 模式实践
