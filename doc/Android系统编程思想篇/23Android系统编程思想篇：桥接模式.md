# Android系统编程思想篇：桥接模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>桥接模式，结构型模式之一，它将抽象部分与实现部分相分离，使它们可以独立地进行变化。

模式角色

应用场景

```
1 如果一个系统需要在构件的抽象角色与具体角色之间增加更多的灵活性，避免在两个层次之间建静态的继承关系，可以通过桥接
模式在它们的抽象层建立一个关联关系。

2 对于那些不希望使用继承或者因为多层次继承而使得系统类的个数急剧增加的系统也可以使用桥接模式。

3 一个类存在两个独立变化的而维护，且这两个维度都需要扩展，也可以使用桥接模式。
```

优点

缺点


## 模式实现

## 模式实践