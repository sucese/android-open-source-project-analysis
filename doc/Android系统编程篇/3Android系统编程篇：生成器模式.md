# Android系统编程篇：生成器模式

作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流
的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去
star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时
的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

文章目录：https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md

建造者模式属于创建型模式的一种，它允许用户在不知道内部构建细节的情况下，可以更精细的控制对象的构造流程。该模式为了将构建复杂对象的
过程和它的部件解耦，使构建的过程和部件的表示隔离开来。

## 模式定义

>将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。

模式角色

```
Product：产品的抽象类
Builder：抽象Builder类，规范构建过程，一般由子类实现具体构建过程
ConcreteBuilder：具体的Builder类
Director：统一组装过程
```

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/2/design_pattern_proxy.png" width="700" height=""/>


使用场景

```
1 相同的方法，不同的执行顺序，产生不同的事件结果。
2 多个部件或者零件，都可以装配到同一个对象中，但产生的运行结果又不相同。
3 对象构建过程特别复杂，参数多，默认值也需要配置。
```
## 模式实现


## 模式实践