# Android编程思想篇：外观模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>系统与内部的子模块通信需要通过一统一的对象进行，也就是子模块需要对外提供一个高层次的封装接口，屏蔽内部实现细节，供外部调用。

外观模式体现的也是面向接口编程的思想。

模式角色

```
Facade：系统对外的统一接口
SystemA, SystemB, SystemC: 系统子模块接口
```

应用场景

```
1 为一个系统的复杂子模块提供一个简单统一的接口，屏蔽具体实现细节，隔离变化。
2 在构建层次结构的系统时，用接口定义每层的入口，层与层之间通过接口通信，简化依赖关系。
```

优点

```
1 对客户程序隐藏系统细节，因而减少客户对于子系统的耦合，能够拥抱变卦。
2 外观类对于系统的接口封装，是的系统更容易使用。
```

缺点

```
1 外观类接口膨胀，由于子模块的接口都对外观类统一暴露，会是的外观类的API接口，增加了使用成本。
2 外观类没有遵循开闭原则，当业务发生变化时，可能需要直接使用修改外观类。
```

## 模式实现

一个笔记本电脑就是系统与子模块协调工作的典型情况。

键盘

```java
public interface Keyboard {

    void write();
}

```
```java
public class Keyboardimpl implements Keyboard {

    @Override
    public void write() {
        
    }
}

```

摄像头

```java
public interface Camera {
    
    void take();
}

```
```java
public class CameraImpl implements Camera {
    @Override
    public void take() {
        
    }
}

```

笔记本

```java

```

## 模式实践
