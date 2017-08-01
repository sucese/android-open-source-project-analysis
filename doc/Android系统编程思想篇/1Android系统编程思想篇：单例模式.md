# Android系统编程思想篇：单例模式

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。


第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

单例模式可能是我们最常见的模式之一了，在单例模式中，我们要求一个系统只有一个全局对象存在，这样有利用我们去协调系统的整体行为。比如在某个服
务器程序中，该服务器的配置信息存放在一个文件中，这些配置数据由一个单例对象统一读取，然后服务进程中的其他对象再通过这个单例对象获取这些配置
信息。这种方式简化了在复杂环境下的配置管理。

## 模式定义

> 某个类有且只有一个实例，并且自行实例化，并向系统提供这个实例。


单例模式常见的使用场景

某个类的对象有且只有一个对象存在，这种场景分以下两种情况：

```
1 创建对象需要消耗大量资源，保证对象单一共享，减少资源消耗。
2 对象本身具有唯一性，不应该多次创建。
```

单例模式有哪些优缺点呢？

优点：

```
1 单例模式只有一个实例，减少类系统开销，避免了对资源的多重占用。
2 单例模式可以设置全局访问点，优化和共享资源访问。
```

缺点：

```
1 单例一般没有接口，后期做扩展比较困难，只能修改源码。
2 单例对象如果持有Context，容易引发内存泄漏，所以我们一般传递的是Application Context。
```
## 模式实现

单例模式实现要点：

```
1 构造函数不对外开放，一般为Private。
2 通过一个静态方法或者枚举类返回单例类对象。
3 确保单例类的对象有且只有一个，尤其要主要多线程环境下的线程安全问题，
3 确保单例类在反序列化时不会重新构建对象。
```

针对以上的实现要点，单例模式的实现一般说来有5种实现方式：

```
1 懒汉式单例
2 双层校验锁单例
3 容器单例
4 静态内部类单例
5 枚举单例
```

6种模式各有利弊，我们分别来看看这6种模式的具体实现方式以及它们的使用场景。


### 懒汉式单例

```java
public class LazySingleton {

    private static LazySingleton instance;

    private LazySingleton() {
    }

    public synchronized static LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }
}

```
优点：懒加载，使用的以后才会进行加载，节省资源。

缺点：每次调用时都会进行synchronized线程同步，消耗很多不必要的资源，实际上只需要在第一次创建对象的时候才会需要线程同步。

>注：synchronized，Java语言的关键字，当它用来修饰一个方法或者一个代码块的时候，能够保证在同一时刻最多只有一个线程执行该段代码。

### 双层校验锁单例

```java
public class DoubleCheckSingleton {
    private static DoubleCheckSingleton instance;

    private DoubleCheckSingleton() {
    }

    public static DoubleCheckSingleton getInstance() {
        //first check
        if (instance == null) {
            //double check
            synchronized (DoubleCheckSingleton.class) {
                instance = new DoubleCheckSingleton();
            }
        }
        return instance;
    }
}

```
优点：懒加载，同时由懒汉式单例扩展而来，只是在第一次创建实例的时候才会进行线程同步，节省了资源。

缺点：双层校验锁的方法由于Java内存模型的原因偶尔会失效。

我们来分析一下这个缺点。先来看看这句话"instance = new DoubleCheckSingleton()"，这段代码最终会被编译成多条汇编指令，大致如下：

1. 给DoubleCheckSingleton的实例分配内存空间
2. 调用DoubleCheckSingleton()构造函数
3. 将创建的instance对象指向分配的内存空间

假设有线程A和线程B，同时执行上述3条指令，由于JVM编译器允许处理器允许上述指令乱序执行，即上述第二条与第三条的顺序无法保证。想象以下这种情况：线程A异常执行过了第三条，此时
instance非空，线程B直接取走instance，因为instance虽然非空，但是并没有经过DoubleCheckSingleton()构造函数初始化，再使用时就会出错。

JDK1.5以后，通过volatile关键字来保证对象都是从主内存读取，来避免上述问题。

>注：volatile，java编程语言允许线程访问共享变量，为了确保共享变量能被准确和一致的更新，线程应该确保通过排他锁单独获得这个变量。Java语言提供了volatile，在某些情况下比锁更加方便。如果
一个字段被声明成volatile，java线程内存模型确保所有线程看到这个变量的值是一致的。

```java
public class DoubleCheckSingleton {
    private volatile static DoubleCheckSingleton instance;

    private DoubleCheckSingleton() {
    }

    public static DoubleCheckSingleton getInstance() {
        //first check
        if (instance == null) {
            //double check
            synchronized (DoubleCheckSingleton.class) {
                instance = new DoubleCheckSingleton();
            }
        }
        return instance;
    }
}

```

### 容器单例

```
public class ContainerSingleton {

    private static HashMap<String, Object> map = new HashMap<>();

    private ContainerSingleton() {
    }

    public static void registerService(String key, Object service) {
        if (!map.containsKey(key)) {
            map.put(key, service);
        }
    }

    public static Object getService(String key) {
        return map.get(key);
    }
}

```

优点：将多个单例类对象注入到统一的管理类中，使用统一的接口进行管理，隐藏类具体实现，降低了耦合度。

### 静态内部类单例

```java
public class InnerClassSingleton {
    
    private InnerClassSingleton() {
    }

    public static InnerClassSingleton getInstance() {
        return InnerClassSingletonHolder.instance;
    }

    private static class InnerClassSingletonHolder {
        private static final InnerClassSingleton instance = new InnerClassSingleton();
    }
}
```

优点：懒加载，无需线程同步，没有性能问题。

缺点：好像没什么缺点

### 枚举单例

```java
public enum EnumSingleton {
    INSTANCE;
}
```

当当当，是不是炒鸡简单，这也是它最大的优点。

优点：写法简单，枚举的创建都是就是线程安全的，这也省去了线程同步的问题。而且它还解决了上述几种写法都不能解决的问题："反序列化会导致重新创建实例"。

为什么反序列化会导致重新创建实例？

>通过序列化可以讲一个单例的实例写到磁盘，然后再读回来，从而有效的获取一个单例的实例，即使这个单例的构造函数是私有的，反序列化中类里有个钩子函数readResolve()，
该函数可以控制对象的反序列化。

所以我们在上述几种单例的实现种，如果要避免反序列化重新创建实例，需要加上以下代码：

```
private Object readReslove() throws ObjectStreamException {
    //将instance直接返回，不再重新生产实例
   return instance;
}
```

好了，5种单例的实现方式都讲完了，我们来总结一下，不管是哪种实现方式，要点都在于：

```
1 构造函数不对外开放，一般为Private。
2 通过一个静态方法或者枚举类返回单例类对象。
3 确保单例类的对象有且只有一个，尤其要主要多线程环境下的线程安全问题，
3 确保单例类在反序列化时不会重新构建对象。
```

以上5种从性能、便利等角度考虑，推荐内部类单例和枚举单例。

## 模式实践

了解了单例的常见实现，我们来看一下常见的开源项目中都是如何使用单例的。

Android-Universal-Image-Loader：https://github.com/nostra13/Android-Universal-Image-Loader

双层校验锁单例

```java
public class ImageLoader {
	private volatile static ImageLoader instance;

	/** Returns singleton class instance */
	public static ImageLoader getInstance() {
		if (instance == null) {
			synchronized (ImageLoader.class) {
				if (instance == null) {
					instance = new ImageLoader();
				}
			}
		}
		return instance;
	}
}
```