# Android界面开发：View自定义实践概览

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。





在文章[02Android显示框架：Android应用视图的载体View](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/02Android显示框架：Android应用视图载体View.md)中我们理解了
View的测量、布局、绘制、触摸事件处理等内容，今天我们开始我们View自定义实践的内容。

View自定义是开发中最常见的需求，图表等各种复杂的ui以及产品经理各种奇怪的需求😤都要通过View自定义来完成。

View自定义有三个关键点：

- 布局：决定View的摆放位置
- 绘制：决定View的具体内容
- 交互：决定View与用户的交互体验

View自定义通常有哪些手段？🤔

- 继承View重写onDraw()方法，这种方式通常用来实现一些特殊的绘制效果。
- 继承ViewGroup实现一些特殊的Layout，这种方式通常用来实现一些系统之外的特殊的布局效果。
- 继承特定的View，例如ImageView、TextView，这种方式通常用于功能的扩展。

View自定义通常需要处理哪些问题？🤔

- 让View支持wrap_content以及padding，这个问题文章[02Android显示框架：Android应用视图的载体View](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/02Android显示框架：Android应用视图载体View.md)中已经做了详细的阐述。
- View带有滑嵌套时，需要处理好滑动冲突。
- View里的线程和动画需要及时的停止，另外View内部提供了postXXX()系列方法，无需再用Handler去做线程切换。

一个标准的自定义View模板

自定义属性

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <declare-styleable name="StandardView">
        <attr name="color" format="color" />
    </declare-styleable>

</resources>
```

自定义View


```java

```

