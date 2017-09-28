# Android编程思想篇：抽象工厂模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>抽象工厂模式提供一个创建一系列相关或相互依赖对象的接口，而无需指定它们具体的类。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/Abstract_factory_pattern_class.png"/>

要理解抽象工厂模式，我们需要先理解两个概念：

- 产品等级结构：产品等级结构也即产品继承结构，例如一个抽象类是电视机，具体类就会是海尔电视机、长虹电视机、海信电视机等，那么抽象
电视机与具体电视机品牌就形成了一个产品等级结构。
- 产品族：产品族指的是由同一个工厂生产的，位于不同产品产品等级结构中的一组产品。例如：海尔电视机、海尔电冰箱、海尔空调等。

模式角色

- 抽象工厂：声明一组用于创建一种产品的方法，每种方法对应一种产品。
- 具体工厂：实现了抽象工厂里定义的产品方法，生成一组具体的产品，这些产品构成了一个产品种类，每一个产品都位于某个产品等级结构中。
- 抽象产品：为每种产品声明接口。
- 具体产品：实现抽象产品里的方法，定义具体的产品。

应用场景

优点

缺点

## 模式实现

有家奥迪汽车厂，它们主要生产奥迪A6、A7、A8系列的汽车。这类车型内部结构差异不大，一条生产线就可以进行生产。但是不同系列的车用的
配件不尽相同。例如：奥迪A6用的国产引擎而奥迪A8则用的进口引擎，奥迪A6用的普通轮胎而奥迪A8则用的进口轮胎。



## 模式实践
