作者: 郭孝星  
邮箱: guoxiaoxingse@163.com  
博客: http://blog.csdn.net/allenwells   
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles  

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

代理模式属于结构型设计模式的一种，在程序设计中十分常见。代理，顾名思义就是帮别人办事，科学上网中代理上网，上大学的时候上宿舍兄弟帮自己
带个饭都是代理。

## 模式定义

>为其他对象提供一种代理以控制对这个对象的访问。

模式角色

```
抽象主题：AbstractSubject，定义真实主题与代理共同的接口方法，它可以是抽象类，也可以是接口。
真实主题：RealSubject，实现了抽象主题，它其实是功能的真正实现者。
代理：ProxySubject，同样也实现了抽象主题，但是方法内部调用真实主题里的方法来实现的，以此来起到代理的作用。
```
使用场景

```
1 不想直接访问某个对象
2 无法直接访问某个对象
```

值得一提的是，代理也分为两种：静态代理与动态代理，静态代理在编码之前我们就知道真实主题角色，也就是知道我们被代理的对象，而动态代理则是在运行阶段通过反射
动态地生产被代理的对象。

## 模式实现

### 静态代理

抽象主题

```java
/**
 * 抽象主题角色，可以是抽象类，也可以是接口。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:17
 */
public abstract class AbstractSubject {

    public abstract void visit();
}

```

真实主题

```java
/**
 * 真实主题角色，实现了抽象主题。
 * 
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:18
 */
public class RealSubject extends AbstractSubject {

    @Override
    public void visit() {
        System.out.println("真实主题");
    }
}

```

代理

```java
/**
 * 代理类
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:22
 */
public class ProxySubject extends AbstractSubject {

    private RealSubject mRealSubject;

    public ProxySubject(RealSubject realSubject) {
        mRealSubject = realSubject;
    }

    @Override
    public void visit() {
        //调用真实主题里方法
        mRealSubject.visit();
    }
}

```

使用方法

```java
/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:25
 */
public class Client {

    public static void main(String[] args) {
        //创建真实角色对象
        RealSubject realSubject = new RealSubject();
        //创建代理角色对象
        ProxySubject proxySubject = new ProxySubject(realSubject);
        //调用代理角色方法
        proxySubject.visit();
    }
}

```
### 动态代理

要实现动态代理，只需要实现Java提供给我们的InvocationHandler接口即可。

## 模式实践