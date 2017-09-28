# Android系统编程思想篇：代理模式 

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

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
应用场景

```
1 不想直接访问某个对象
2 无法直接访问某个对象
```
按照用途我们可以把代理类型分为以下4种：

```
1 远程代理：为某个对象再不同的内存空间提供局部代理，是系统可以将Server部分隐藏，使得Client可以不必考虑Server的存在。
2 虚拟代理：使用一个代理对象表示一个十分消耗资源的对象并在真正需要它的时候进行创建。
3 保护代理：使用代理控制对原始对象的访问，常用来做对原始对象访问的权限控制。
4 智能引用：在访问原始对象时执行一些自己的附加操作，并对指向原始对象的引用进行计数。
```

值得一提的是，从代码实现角度，代理可以分为：静态代理与动态代理，静态代理在编码之前我们就知道真实主题角色，也就是知道我们被代理的对象，而动态代理则是在运行阶段通过反射
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