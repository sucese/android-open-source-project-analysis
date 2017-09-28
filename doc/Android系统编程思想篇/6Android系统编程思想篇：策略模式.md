# Android编程思想篇：外观模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>策略模式定义了一系列的算法，并将每个算法封装起来，并且使它们可以相互替换，策略模式让算法独立于使用它的客户端。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/stragety_pattern_class.png"/>

模式角色

- Context：用来操作策略的上下文环境
- Stragety：抽象策略
- StragetyA/StragetyB：具体策略

应用场景

- 针对同一问题有多种处理方式，仅仅是具体细节有差别时。
- 需要安全的封装多种同一类型的操作时。
- 出现同一抽象类的多个子类，而又需要用选择语句来选择具体子类时。

策略模式主要就是用来分离算法，在相同的行为抽象下有不同的具体策略实现，这个模式很好的实践了开闭原则，定义抽象，注入不同的实现，提高了程序的
扩展性。

优点

- 结构简单明了，使用简单直观。
- 耦合度较低，扩展方便。
- 操作封装更为彻底，数据更为安全。

缺点

- 随着策略的增多，子类变得庞大。

## 模式实现

我们平时都坐过公交、地铁，虽然我们都刷同一张一卡通，但是他们的计价方式是不一样。

## 模式实践
