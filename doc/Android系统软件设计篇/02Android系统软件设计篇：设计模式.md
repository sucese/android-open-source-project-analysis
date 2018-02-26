# Android系统编程思想：设计模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

提到设计模式，大家并不陌生，我们之前在分析Android源码的时候也有提及，但都比较零散，不成系统。今天的这篇文章就来系统的总结一下23种
设计模式的模式定义与实现方式，让读者有一个整体上的模式。

什么是设计模式？🤔

> 通俗来讲，设计模式就是针对某一种特殊场景而给出的标准解决方案，它是前辈们的经验性总结，也是实现软件工程化的基础，良好的设计模式应用
可以是我们的软件变得更加健壮可维护。

设计模式按照类型划分可以分为三大类，如下所示：

- 创建型设计模式：如同它的名字那样，它是用来解耦对象的实例化过程。
- 组合型设计模式：将类和对象按照一定规则组合成一个更加强大的结构体。
- 行为型设计模式：定义类和对象的交互行为。

23种设计模式划分如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/design_pattern.png"/>

👉 注：23种设计模式很多小伙伴都烂熟于心，但是真正编程实践的时候未必会想的起来，这其实是一个潜移默化的过程，在看设计模式的时候，尽量多动手写一写，其中
手写（不借助IDE）的效果最佳，可以加深理解，理解的深了，编程的时候自然就可以想的到去应用。

## 一 创建型设计模式

> 创建型设计模式主要用来解耦对象的实例化过程，控制实例的生成。

创建型设计模式一共有六种，如下所示：

### 1.1 单例模式

模式定义

> 当系统中只需要一个实例或者一个全局访问点的时候可以使用单例模式。

- 优点：节省系统创建对象的资源，提高了系统效率，提供了统一的访问入口，可以严格控制用户对该对象的访问。
- 确定：只有一个对象，积累的职责过重，违背了单一职责原则。构造方法为private，无法继承，扩展性较差。

单例模式的实现由很多种，如下所示：

1. 懒汉式单例
2. 双层校验锁单例
3. 容器单例
4. 静态内部类单例
5. 枚举单例

其中静态内部类单例和枚举单例都是单例模式最佳的实现，但是出于便利性的考量，双层校验锁的实现应用的更为广泛，如下所示：

```java
public class DoubleCheckSingleton {
    
    // volatile关键字保证了：① instance实例对于所有线程都是可见的 ② 禁止了instance 
    // 操作指令重排序。
    private volatile static DoubleCheckSingleton instance;

    private DoubleCheckSingleton() {
    }

    public static DoubleCheckSingleton getInstance() {
        // 第一次校验，防止不必要的同步。
        if (instance == null) {
            // synchronized关键字加锁，保证每次只有一个线程执行对象初始化操作
            synchronized (DoubleCheckSingleton.class) {
                // 第二次校验，进行判空，如果为空则执行初始化
                if(instance == null){
                     instance = new DoubleCheckSingleton();
                }
            }
        }
        return instance;
    }
}
```
关于双层校验锁单例为何能实现JVM单例，它的要点在于两次判空和synchronized、volatile关键字，具体原理已经写在上方的注释里，这里
我们单独说一下volatile关键字。

说明我们说到，volatile关键字禁止了instance 操作指令重排序，我们来解释一下，我们知道instance = new DoubleCheckSingleton()这个操作
在汇编指令里大致会做三件事情：

1. 给我们知道instance分配内存。
2. 调用DoubleCheckSingleton()构造方法。
3. 将构造的对象赋值给instance。

但是在真正执行的时候，Java编译器是允许指令乱序执行的（编译优化），所以上述3步的顺序得不到保证，有可能是132，试想一下，如果线程A没有执行第2步，先执行了
第3步，而恰在此时，线程B取走了instance对象，在使用instance对象时就会有问题，双层校验锁单例失败，而volatile关键字可以禁止指令重排序从而解决这个问题。

单例模式的另一个问题就是多进程的情况下的失败问题，因为JVN里的单例是基于一个虚拟机进程的，这个时候通常的做法就是让这个单例支持跨进程调用，这个在
Android里一般用AIDL实现。

### 1.2 建造者模式

模式定义

> 封装一个复杂对象的构建过程，可以按照流程来构建对象。

- 优点：它可以将一个复杂对象的构建与表示相分离，同一个构建过程，可以构成出不同的产品，简化了投建逻辑。
- 缺点：如果构建流程特别复杂，就是导致这个构建系统过于庞大，不利于控制。

建造者模式的实现，也十分简单，如下所示：

```java
public class Product {

    private String board;
    private String display;
    private String os;

    public String getBoard() {
        return board;
    }

    public String getDisplay() {
        return display;
    }

    public String getOs() {
        return os;
    }

    private Product(Builder builder) {
        // 进行构建
        this.board = builder.board;
        this.display = builder.display;
        this.os = builder.os;
    }

    public static class Builder {
        // 建造者模式还可以设置默认值
        private String board = "default value";
        private String display = "default value";
        private String os = "default value";

        public void setBoard(String board) {
            this.board = board;
        }

        public void setDisplay(String display) {
            this.display = display;
        }

        public void setOs(String os) {
            this.os = os;
        }


        public Product build() {
            return new Product(this);
        }
    }
}
```

### 1.3 原型模式

模式定义

> 当某个对象的数据结构或者构建过程特别复杂，频繁的构建势必会消耗系统性能，这个时候我们采用原型模式对原有的
对象进行克隆，构建新的对象。

- 优点：直接克隆原有实例生成新的实例，免去了复杂的构建过程，节省了系统资源。
- 缺点：


实现原型模式也很简单，主需要声明实现loneable接口，然后覆写Object的clone()方法接口。

```java
public class Person implements Cloneable{

    public int age;
    public String name;

    @Override
    public Person clone() throws CloneNotSupportedException {
        return (Person) super.clone();
    }
}
```

原型模式要注意深拷贝和浅拷贝的问题，Object的clone()方法默认是钱拷贝，即对于引用对象拷贝的地址而不是值，所以要实现
深拷贝，在clone()方法里对于引用对象也有调用一下clone()方法，并且引用对象也要实现Cloneable接口和覆写clone()方法。

接下来我们继续看看三种工厂模式，如下所示：

- 简单工厂模式：根据传入的参数决定实例化哪个对象。
- 工厂模式：工厂模式定义了一个创建对象的接口，由子类进行对象的初始化，即工厂模式将子类的初始化推迟到了子类里。
- 抽象工厂模式：抽象工厂模式和工厂模式很相似，只是它利用接口或者抽象类定义了一个产品族，例如定义一个拨号产品族，只定义功能，不
关心实现，具体实现交由Android、iOS等操作系统自己完成。

### 1.4 简单工厂模式

模式定义

> 根据传入的参数决定实例化哪个对象。

简单工厂模式是工厂模式的简化版本，无需定义抽象工厂，通常还可以利用反射来生成对象，简化操作，如下所示：

```java
// 简单工厂
public class SimpleFactory {

    public static <T extends AbstractProduct> T create(Class<T> clasz) {
        AbstractProduct product = null;
        try {
            product = (AbstractProduct) Class.forName(clasz.getName()).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return (T) product;
    }
}
```

- 优点：
- 缺点：

### 1.5 工厂模式

模式定义

> 工厂模式定义了一个创建对象的接口，由子类进行对象的初始化，即工厂模式将子类的初始化推迟到了子类里。抽象工厂模式

- 优点：工厂模式符合开闭原则，当需要增加一个新产品时，只需要增加一个具体产品类和一个具体工厂类，无需修改原有的系统，外界也无需
知道具体的产品类的实现。
- 缺点：每次增加新产品的时候都会增加产品类和工厂类，势必会让系统越来越庞大。

工厂模式的实现也很简单，就是定义一个抽象类或者接口工厂，在子类工厂中决定实例化具体的类。

```java
// 抽象工厂
public abstract class AbstractFactory {
    public abstract AbstractProduct create();
}

// 具体工厂
public class ConcretetFactory {

    public static AbstractProduct create() {
        return new ConcreteProductA();
//        return new ConcreteProductB();
    }
}

// 抽象产品
public class AbstractProduct {
}

// 具体产品A
public class ConcreteProductA extends AbstractProduct {
}

// 具体产品B
public class ConcreteProductB extends AbstractProduct {
}

```

### 1.6 抽象工厂模式

模式定义

> 抽象工厂模式和工厂模式很相似，只是它利用接口或者抽象类定义了一个产品族，例如定义一个拨号产品族，只定义功能，不
 关心实现，具体实现交由Android、iOS等操作系统自己完成。

- 优点：
- 缺点：

实现如下所示：

```java
// 抽象产品A
public abstract class AbstractProductA {
}

// 抽象产品B
public abstract class AbstractProductB {
}

// 具体产品A1
public class ConcreteProductA1 extends AbstractProductA{
}

// 具体产品A2
public class ConcreteProductA2 extends AbstractProductA {
}

// 具体产品B1
public class ConcreteProductB1 extends AbstractProductB {
}

// 具体产品B2
public class ConcreteProductB2 extends AbstractProductB {
}

// 抽象工厂
public abstract class AbstractFactory {
    
    public abstract AbstractProductA createProductA();
    public abstract AbstractProductB createProductB();
}

// 具体工厂
public class ConcreteFactory extends AbstractFactory {
    
    @Override
    public AbstractProductA createProductA() {
        return new ConcreteProductA1();
    }

    @Override
    public AbstractProductB createProductB() {
        return new ConcreteProductB1();
    }
}
```

## 二 组合型设计模式

### 2.1 适配器模式

模式定义

> 适配器模式可以将一个类的接口，转换成客户端期望的另一个接口，让两个原本不兼容的接口可以无缝对接。

- 优点：
- 缺点：

```java
// 目标接口
public interface TargetInterface {
    int getFive();
}

// 被适配对象
public class Adaptee {

    public int getTen() {
        return 10;
    }
}

// 适配器
public class Adapter extends Adaptee implements TargetInterface {

    @Override
    public int getFive() {
        return 5;
    }
}
```

### 2.2 组合模式

模式定义

> 将对象组成树形结构以表示整体-部分的层次结构，使得用户对单个对象和组合对象的使用具有一致性。

应用场景

- 表示对象部分-整体的层次结构。
- 从一个整体中能够独立出部分模块或者功能的场景。

### 2.3 装饰模式

模式定义

> 动态的为对象增加一些额外的功能。

应用场景

- 需要透明且动态的扩展类的功能时。

```java
// 抽象组件类
public abstract class AbstractComponent {
    
    protected abstract void operation();
}

// 具体组件类
public class ConcreteComponent extends AbstractComponent {
    @Override
    protected void operation() {
        
    }
}

// 抽象装饰类
public abstract class AbstractDecorator extends AbstractComponent {

    private AbstractComponent mComponent;

    public AbstractDecorator(AbstractComponent component) {
        mComponent = component;
    }

    @Override
    protected void operation() {
        mComponent.operation();
    }
}

// 具体装饰类
public class ConcreteDecorator extends AbstractDecorator {

    public ConcreteDecorator(AbstractComponent component) {
        super(component);
    }

    @Override
    protected void operation() {
        operationA();
        super.operation();
        operationB();
    }

    private void operationA() {

    }

    private void operationB() {

    }
}
```

### 2.4 外观模式

模式定义

> 要求一个字系统的外部与其内部的通信都通过一个统一的而对象进行。

应用场景

- 子系统在迭代的过程中可以会不断变化，甚至被替代掉，给一个统一的访问接口，避免子系统的改变影响到外部的调用者。
- 当需要构建层次结构型的系统时，为各层子系统提供访问的接口进行通信，避免直接产生依赖。

```java
// 列表接口
public interface List {
    
    void add();
    void remove();
    
}

// 数组列表
public class ArrayList implements List {
    @Override
    public void add() {

    }

    @Override
    public void remove() {

    }
}

// 链表列表
public class LinkedList implements List {
    @Override
    public void add() {

    }

    @Override
    public void remove() {

    }
}
```

### 2.5 桥接模式

模式定义

> 将抽象部分和实现部分相互分离，使它们可以独立变化。

应用场景

- 如果一个系统需要在抽象部分和实现部分增加更多的灵活性，避免两种变化的时候相互影响。
- 如果不希望使用继承而增加系统的复杂度，可以考虑使用桥接模式。
- 一个类存在两个独立变化的纬度，且这两个纬度都希望进行扩展。

### 2.6 享元模式

模式定义

> 使用共享对象可以有效的支持大量的细粒度对象。

应用场景

- 系统中存在着大量的相似对象。
- 细粒度的对象都具有较接近的外部状态，而且内部状态与外部环境无关。
- 需要缓冲池的场景。

### 2.7 代理模式

模式定义

> 为其他对象提供一个代理以提供对这个对象的访问。 

应用场景

- 当无法或者不想直接访问某个对象时，可以通过一个代理对象进行访问。

代理模式按照代理类运行前是否存在还可以分为静态代理和动态代理，如下所示：

静态代理

```java
// 被代理接口，定义要实现的功能。
public interface Subject {

    void visit();
}

// 被代理类，完成实际的功能。
public class ConcreteSubject implements Subject {

    @Override
    public void visit() {
        System.out.println("visit");
    }
}

// 静态代理类，与被代理类实现同一套接口
public class StaticProxy implements Subject {

    private Subject mSubject;

    public StaticProxy(Subject subject) {
        mSubject = subject;
    }

    @Override
    public void visit() {
        mSubject.visit();
    }
}
```

动态代理

```java
// 被代理接口，定义要实现的功能。
public interface Subject {

    void visit();
}

// 被代理类，完成实际的功能。
public class ConcreteSubject implements Subject {

    @Override
    public void visit() {
        System.out.println("visit");
    }
}

// 动态代理类，实现InvocationHandler接口。
public class DynamicProxy implements InvocationHandler {

    private Subject mSubject;

    public DynamicProxy(Subject subject) {
        mSubject = subject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        System.out.println("函数执行前自定义操作");
        // 调用被代理类的方法时会调用该方法
        method.invoke(mSubject, args);
        System.out.println("函数执行后自定义操作");
        return null;
    }
}

public class Client {

    public static void main(String[] args) {

        Subject subject = new ConcreteSubject();
        DynamicProxy proxy = new DynamicProxy(subject);

        // 动态生成代理类
        Subject proxySubject = (Subject) Proxy.newProxyInstance(DynamicProxy.class.getClassLoader()
                , subject.getClass().getInterfaces()
                , proxy);
        proxySubject.visit();

    }
}
```

## 三 行为型设计模式

### 3.1 模板模式

模式定义

> 定义一个操作的算法框架，而将具体实现延迟到子类中进行，使得子类在不改变整体算法框架的基础上，可以自定义算法实现。

应用场景

- 多个子类有公有的方法，并且逻辑基本相同时。
- 重要复杂的算法可以把核心算法设计为模板方法，具体细节则由子类实现
- 重构代码时，把相同的代码抽取到父类中，然后通过钩子函数约束其行为。

### 3.2 解释器模式

模式定义

> 给定一个语音，定义它的文法的一种表示，并定义一个解释器。

应用场景

- 如果某个简单的语音需要解释执行并且可以将该语言中的语句表示为一个抽象语法树时可以使用解释器模式。
- 在某些特定领域不断出现的问题是，可以将该领域的问题转船为一种语法规则下的语句，然后构建解释器来解释该语句。

### 3.3 策略模式

模式定义

> 策略模式定义了一系列算法，并将算法封装起来可以互相替换，策略模式让算法与使用它的客户端解耦，可以独立变化。

应用场景

- 针对同一类型的问题有多种处理方式，仅仅是具体的行为有差别时。
- 需要安全的封装多种同一类型的操作时。
- 出现同一抽象类的多个字类，而又需要使用if-else来选择子类时。

策略模式的实现也非常简单，依赖于接口或者抽象类，如下所示：

```java
// 策略接口，定义功能。
public interface IStrategy {
    void method();
}

// 策略A
public class StrategyA implements IStrategy {
    @Override
    public void method() {

    }
}

// 策略B
public class StrategyB implements IStrategy {
    @Override
    public void method() {
        
    }
}
```

### 3.4 状态模式

模式定义

> 允许一个对象在内部状态改变时改变它的行为。

应用场景

- 一个对象的行为取决于它的状态，并且它在运行时根据状态改变它的行为。
- 代码中包含大量与对象状态相关的判断语句。

状态模式的核心是根据不同的状态对应不同的操作，如下所示：

```java
// 操作接口
public interface TVState {
    void nextChannel();

    void lastChannel();
}

// 开机状态
public class PowerOnState implements TVState {

    @Override
    public void nextChannel() {
        // next channel 
    }

    @Override
    public void lastChannel() {
        // last channel
    }
}

// 关机状态
public class PowerOffChannel implements TVState {
    @Override
    public void nextChannel() {
        // do nothing
    }

    @Override
    public void lastChannel() {
        // do nothing
    }
}
```

### 3.5 观察者模式

模式定义

> 定义对象间一对多的依赖关系，每当这个对象发生改变时，其他对象都能收到通知并更新自己。

应用场景

- 关联行为场景
- 事件多级触发场景
- 跨系统的消息交换场景，例如消息队列，事件总线的处理机制。

观察者模式的实现如下所示：

```java
// 监听接口，通知被监听对象发生改变
public interface Listener {

    void change();
}

// 被监听者
public class Observable {

    private Listener mListener;

    // 设置监听器
    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void onChange() {
        // 通知对象发生改变
        mListener.change();
    }
}

// 监听者
public class Observer {

    public void setup() {
        Observable observable = new Observable();
        observable.setListener(new Listener() {
            @Override
            public void change() {
                // TODO 监听的对象发生改变
            }
        });
    }
}

```

### 3.6 备忘录模式

模式定义

> 在不破坏封装的前提下，保存一个对象的内部状态，并在该对象之外保存这个状态，以便可以将该对象恢复到保存时的状态。

应用场景

- 需要保存某个对象在某一时刻的状态。
- 外界向访问对象的状态，但是又不想直接暴露接口给外部，这时候可以将对象状态保存下来，间接的暴露给外部。

### 3.7 中介者模式

模式定义

> 中介者模式定义了一系列对象间的交互方式，使得这些对象像话作用而又不耦合在一起。

应用场景

- 当对象间有很多的交互操作，而且一个对象的行为依赖于其他对象时，可以利用中介者模式解决紧耦合的问题。

### 3.8 命令模式

模式定义

> 将一个请求封装成一个对象，可以将不同的请求参数化，可以对请求就行排队、日志记录以及撤销等操作。

应用场景

- 需要抽象待执行的动作，然后以参数的形式提供出来。
- 在不同的时刻，指定和排列请求。
- 需要支持撤销操作。
- 需要支持日志功能，这样当系统崩溃时，可以重做一遍。
- 需要支持事务操作。

命令模式的实现如下所示：

```java
// 接收命令
public class Receiver {

    public void action() {
        // TODO 真正执行命令具体逻辑
    }
}

// 抽象命令
public interface AbstractCommand {
    
    // 执行命令
    void command();
}

// 具体命令
public class ConcreteCommand implements AbstractCommand {

    private Receiver mReceiver;

    public ConcreteCommand(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    public void command() {
        mReceiver.action();
    }
}

// 调用者
public class Invoker {

    private AbstractCommand mCommmand;

    public Invoker(AbstractCommand command) {
        mCommmand = command;
    }

    public void invoke() {
        mCommmand.command();
    }
}
```

### 3.9 访问者模式

模式定义

> 封装一些作用于某些数据结构中的各元素的操作，它可以在不改变数据结构的前提下赋予这些元素新的操作。

应用场景

- 对象结构比较稳定，但是需要在对象结构的基础上定义新的操作。
- 需要对同一个类的不同对象执行不不同的操作，但是不希望增加操作的时候改变这些类。

### 3.10 责任链模式

模式定义

> 将请求的发送者和接收者进行解耦，使得多个对象都有机会处理该请求，将这些对象串成一条链，并沿着这条链子处理请求，直到有对象处理它为止。

应用场景

- 多个对象可以处理同一个请求，但是具体由哪个对象处理在运行时动态决定。
- 在请求处理者不明确的情况下向多个对象中的一个提交请求。
- 需要动态指定一组对象的处理请求。


责任链模式的实现主要在于处理器的迭代，要么使用循环迭代，要么使用链表后继，如下所示：

```java
// 处理器，定位行为和下一个处理器
public abstract class Handler {
    protected Handler next;

    public abstract void handleRequest(String condition);
}

// 处理器1
public class Handler1 extends Handler {
    @Override
    public void handleRequest(String condition) {
        if (TextUtils.equals(condition, "Handler1")) {
            // process request
        } else {
            // next handler
            next.handleRequest(condition);
        }
    }
}

// 处理器2
public class Handler2 extends Handler {
    
    @Override
    public void handleRequest(String condition) {
        if (TextUtils.equals(condition, "Handler2")) {
            // process request
        } else {
            // next handler
            next.handleRequest(condition);
        }
    }
}
```

### 3.11 迭代器模式

模式定义

> 提供一个方法顺序访问一个容器内的元素，而又不暴露该对象的内部表示。

应用场景

- 遍历一个容器时。





