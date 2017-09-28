# Android系统编程思想篇：工厂方法模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

工厂方法模式是创建型的模式的一种，该模式有着广泛的实践，比如我们常用的Activity的onCreate()方法都可以看做一个工厂方法。

## 模式定义

>工厂方法模式属于创建型模式，在该模式中，工厂父类负责定义创建产品对象的公共接口，而工厂子类负责生成具体的产品对象。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/factory_pattern_class.png"/>

模式角色

```
1 抽象工厂：工厂方法的核心。
2 具体工厂：实现了具体的业务逻辑。
3 抽象产品：产品的父类。
4 具体产品：产品的实现类
```

## 模式实现

我们来举个例子😁

有家奥迪汽车厂，它们主要生产奥迪A6、A7、A8系列的汽车。这类车型内部结构差异不大，一条生产线就可以进行生产。

抽象工厂

```java
/**
 * 抽象工厂
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/27 下午6:30
 */
public abstract class AbstractFactory {

    public abstract AbstractProduct createProduct();
}

```

具体工厂

```java
/**
 * 具体工厂
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/27 下午6:33
 */
public class RealFactory extends AbstractFactory {

    @Override
    public AbstractProduct createProduct() {
        return new RealProductA();
    }
}

```

抽象产品

```java
/**
 * 抽象产品
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/27 下午6:31
 */
public abstract class AbstractProduct {

    public abstract void method();
}

```

具体产品

```java
/**
 * 具体产品A
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/27 下午6:32
 */
public class RealProductA extends AbstractProduct {

    @Override
    public void method() {

    }
}

```

## 模式实践