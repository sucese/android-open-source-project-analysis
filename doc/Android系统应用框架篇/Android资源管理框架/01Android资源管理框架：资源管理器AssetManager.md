# Android资源管理框架：资源管理器AssetManager

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

## 一 资源分类与适配

Android应用程序的设计也是代码与资源相分离，资源采用xml文件来描述，Android的资源文件可以分为两大类：

>assets：assets资源放在主工程assets目录下，它里面保存一些原始的文件，可以以任何方式来进行组织，这些文件最终会原封不动的
地被打包进APK文件中。

获取asset资源也十分简单，如下所示：

```java
InputStream is = getAssets.open("fileName");
```
>res：res资源放在主工程的res目录下，这类资源一般都会在编译阶段生成一个资源ID供我们使用。

res资源包含了我们开发中使用的各种资源，具体说来：

- animator
- anim
- color
- drawable
- layout
- menu
- raw
- values
- xml

这些资源的含义大家应该都很熟悉，这里就不再赘述。

上述9种类型的资源文件，除了raw类型资源，以及Bitmap文件的drawable类型资源之外，其它的资源文件均为文本格式的XML文件，它们在打包的过程中，会被编译成二进制格式的XML文件。这些二进制格式的XML文件分别有一个字符串资源池，用来保存文件中引
用到的每一个字符串，包括XML元素标签、属性名称、属性值，以及其它的一切文本值所使用到的字符串。这样原来在文本格式的XML文件中的每一个放置字符串的地方在二进制格式的XML文件中都被替换成一个索引到字符串资源池的整数值，这写整数值统一保存在
R.java类中，R.java会和其他源文件一起编译到APK中去。

我们接着来看Android是如何这些资源的，我们知道Android设备数以亿计、遍布全球，那么对于Android设备最大的一个问题就是适配。为了做好设备，Android将资源的组织方式划分为19个纬度，这一块的内容[Android资源官方文档](https://developer.android.com/guide/topics/resources/providing-resources.html)讲
的很清楚，这里也不再赘述。

我们可以利用资源的组织方式达到最佳的设备兼容性，那么问题来了，Android是如何查找到对应的资源的呢？🤔

Android会先获取自己的设备信息，然后根据设备信息去查找对应的资源，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/resource/android_resource_search_flow.png"/>

1. 淘汰与设备配置冲突的资源文件。
2. 选择优先级最高的限定符。（先从 MCC 开始，然后下移。）
3. 是否有资源目录包括此限定符？若无，返回到第 2 步，看看下一个限定符。若有，请继续执行第 4 步。
4. 淘汰不含此限定符的资源目录。在该示例中，系统会淘汰所有不含语言限定符的目录。
5. 返回并重复第 2 步、第 3 步和第 4 步，直到只剩下一个目录为止。
