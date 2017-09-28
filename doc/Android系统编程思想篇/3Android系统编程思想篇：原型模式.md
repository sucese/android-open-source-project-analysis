# Android系统编程思想篇：原型模式

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 模式定义

>原型模式属于创建型模式的一种，原型模式的特点在于通过复制一个已经存在的实例来返回新的实例，而不是去创建新额实例，被复制的实例被称为原型，这个原型是可以定制的。


<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/prototype_design_pattern.png"/>


模式角色

```
1 Client：客户端对象
2 Prototype：抽象类或者接口，声明其具备clone能力，
3 ConcretePrototype：具体的原型类。
```

使用场景

```
1 创建对象需要消耗很多资源。
2 创建对象需要非常繁琐的数据准备或者访问权限。
3 一个对象需要提供给其他对象访问，而且各个调用者都要修改其值。
```

## 模式实现

在介绍具体实现之前，我们先来了解两个概念。

Cloneable接口

```
Cloneable接口表面该对象允许克隆。如果一个类实现了Cloneable，Object的clone()方法就会返回该对象的逐域拷贝，否则就会抛出CloneNotSupportedException异常。
由此我们便可以无需调用构造函数就可以创建对象。    
```

```java
public class Object{

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object {@code x}, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be {@code true}, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be {@code true}, this is not an absolute requirement.
     * <p>
     * By convention, the returned object should be obtained by calling
     * {@code super.clone}.  If a class and all of its superclasses (except
     * {@code Object}) obey this convention, it will be the case that
     * {@code x.clone().getClass() == x.getClass()}.
     * <p>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by {@code super.clone} before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by {@code super.clone}
     * need to be modified.
     * <p>
     * The method {@code clone} for class {@code Object} performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface {@code Cloneable}, then a
     * {@code CloneNotSupportedException} is thrown. Note that all arrays
     * are considered to implement the interface {@code Cloneable} and that
     * the return type of the {@code clone} method of an array type {@code T[]}
     * is {@code T[]} where T is any reference or primitive type.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p>
     * The class {@code Object} does not itself implement the interface
     * {@code Cloneable}, so calling the {@code clone} method on an object
     * whose class is {@code Object} will result in throwing an
     * exception at run time.
     *
     * @return     a clone of this instance.
     * @exception  CloneNotSupportedException  if the object's class does not
     *               support the {@code Cloneable} interface. Subclasses
     *               that override the {@code clone} method can also
     *               throw this exception to indicate that an instance cannot
     *               be cloned.
     * @see java.lang.Cloneable
     */
    protected Object clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable)) {
            throw new CloneNotSupportedException("Class " + getClass().getName() +
                                                 " doesn't implement Cloneable");
        }

        return internalClone();
    }

    /*
     * Native helper method for cloning.
     */
    private native Object internalClone(); 
}
```


浅拷贝与深拷贝

浅拷贝

```
浅拷贝：也称为影子拷贝，这种拷贝并不是将源对象的所有字段都构造了一份吗，知识将副本对象的也指向源对象的地址，两者公用一个地址，对其中一个对象的修改也同时会修改另一个对象。
```

深拷贝

```
深拷贝：
```




## 模式实践