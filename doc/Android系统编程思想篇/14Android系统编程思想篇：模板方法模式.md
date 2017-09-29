# Android编程思想篇：模板方法模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>定义一个操作中的算法的框架，而将一些步骤延迟到子类中，使得子类可以不改变一个算法的结构即可重定义该算法的某些步骤。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/template_method_pattern_class.png"/>

模式角色

- 抽象类：定义抽象方法。
- 具体类：实现抽象方法。

应用场景

- 多个子类有公有的方法，且逻辑基本相同。
- 重要、负责的算法，可以把核心算法设计为模板方法，周边的相关细节功能则由各个子类实现。
- 模板方法在重构项目时也非常有用，将相同的代码抽取到父类中，然后通过子类约束其行为。

模板方法模式体现的是流程封装。

优点

- 封装不变部分，扩展可变部分。
- 提取公共部分代码，便于维护。

缺点

- 模板方法会提高代码阅读的难度，让维护者难以理解。

## 模式实现

模板方法模式就像它的名字那样，它封装的是固定的流程。第一步该做什么，第二步该做什么等，这里举个计算机开机的例子。

## 模式实践
