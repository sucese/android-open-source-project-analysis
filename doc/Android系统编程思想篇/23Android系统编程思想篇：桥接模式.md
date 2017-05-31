# Android系统编程思想篇：桥接模式

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，爱好广泛，技术栈主要涉及以下几个方面
>
>- Android/Linux
>- Java/Kotlin/JVM
>- Python
>- JavaScript/React/ReactNative
>- DataStructure/Algorithm
>
>文章首发于[Github](https://github.com/guoxiaoxing)，后续也会同步在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与
[CSDN](http://blog.csdn.net/allenwells)等博客平台上。文章中如果有什么问题，欢迎发邮件与我交流，邮件可发至guoxiaoxingse@163.com。

## 模式定义

>桥接模式，结构型模式之一，它将抽象部分与实现部分相分离，使它们可以独立地进行变化。

使用场景

```
1 如果一个系统需要在构件的抽象角色与具体角色之间增加更多的灵活性，避免在两个层次之间建静态的继承关系，可以通过桥接
模式在它们的抽象层建立一个关联关系。

2 对于那些不希望使用继承或者因为多层次继承而使得系统类的个数急剧增加的系统也可以使用桥接模式。

3 一个类存在两个独立变化的而维护，且这两个维度都需要扩展，也可以使用桥接模式。
```

## 模式实现

## 模式实践