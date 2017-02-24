# Android 框架层源码分析

作者: 郭孝星<br/>
邮箱: guoxiaoxingse@163.com<br/>
博客: https://guoxiaoxing.github.io/<br/>
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles<br/>

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好音乐，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章都会同时发布在个人博客和简书博客上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问欢迎发邮件与我交流, 对于交流的
问题, 请描述清楚并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文章,
关注文章的最新的动态。

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/android_7_nougat.jpg"/>

写在前面

>之前看过很多Android框架层的源码以及分析源码的书籍，但都是零零散散的不成系统，作为一个 Android 开发人员，
自上而下的 Android 底层源码的功力还是很重要的，从今天开始我就对整个 Andriod 框架层的源码和原理做个系统的
分析，虽然 对于 Android 源码的分析，很多前辈都写过，但之所以会有这个系列的文章，一来 Android 的源码也在
不断的变化目前已经到 Android 7.0 Nougat，本系列文章会针对最新的代码进行分析，二来就是源码分析的过程是比
较枯燥的，本系列的文章会一种独特的视角来减轻大家在阅读源码时的负担，好了，让我们开始吧。

这是本系列博客的第一篇文章，后续的每篇文章都会配上详尽的类图、时序图、示例代码，每大章节还是设立导读PPT。

Android 的源码是多名伟大工程师智慧的结晶，不可谓博大而精深，所以我们在学习之前，要掌握相关的基本技术，欲工
其事，必先利其器，我们需要掌握以下的技术。

代码版本

>本系列文章针对的是最新的 Android 7.0 Nougat 版本源码。

核心思路

>以某一个支线为起点，从上层往底层，不断地追溯，在各个模块、文件、方法之间来回跳转，反复地阅读，理清整个流程的逻辑。
同时带着思考去看源码，去揣测作者的用意，去理解代码的精妙之处，去思考代码可能存在的缺陷，去总结优秀的代码设计思想。

## 基础篇

阅读 AOSP（Android Open Source Projec）之前你需要掌握的技术有：

- Java
    * AOSP 的主要语言，当然是应该掌握的。
- Linux
    * Android 是基于 Linux 内核开发的，如何你需要涉及到内核或者驱动的开发，你需要掌握 Linux 相关技术。
- Make
    * AOSP 是用 Make 来编译的，因此要了解常用的 Make 语法。
- Git
    * 这是程序员必备技能了吧。
- C++
    * AOSP 一些性能敏感的模块底层都是由C++完成的，当然如果你如果不需要关注底层实现，也可以跳过这一段，可以更多的
    去关注框架层。
- 设计模式
    * AOSP里大量的框架都是用了谋者设计模式，比方说观察者模式、工厂模式、复合模式等，如果对设计模式不够了解的会
    看的云里雾里。
- 熟练的 Android App 开发技能


Android 源码

https://android.googlesource.com/

https://github.com/android

https://source.android.com/source/index.html

## 工具篇

本系列的文章基于的环境是MacOS，但是所使用的工具软件多数都是跨平台的，所以对其他平台的小伙伴也没有影响。

### 代码阅读

如果在 Windows 下直接上 SourceInsight 就可以了，Mac 下可以用 Understand，功能和 SourceInsight 一样强大。

[Understand 4.0.849 代码阅读分析软件](http://xclient.info/s/understand.html)

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/understand.png"/>

### 演示文稿

演示文稿还是用 Office 来做了，没有用 Keynote，担心有些同学看不了。

[Office for Mac 2016 ](http://xclient.info/s/office-for-mac-2016.html)

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/office_for_mac.png"/>

### 文章写作

文章都是用 Markdown 来写的，工具用的是MWeb，一款很强大的 Markdown 编辑工具。

[MWeb for Mac](http://www.mweb.im/)

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/nweb_for_mac.png"/>

### 图例绘制

流程图、类图、时序图、系统架构图等各种图例采用EdrawMax来绘制，Visio 也比较好用，可惜 Mac 下没有。

[EdrawMax](http://xclient.info/s/edraw-max.html)

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/EdrawMax.png"/>

### 图片处理

Gif 图的制作用的是VideoGIF。

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/VideoGIF.png"/>

[VideoGIF](http://xclient.info/s/videogif.html)

## 书籍篇

站在前辈的肩膀上，我们能看的更远，进步的更快。以下是学习框架与源码一些不错的书籍，

[Android 源码设计模式解析与实战](https://item.jd.com/11793928.html)：何红辉，关爱民 著

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/android_source_code_design_pattern.png"/>

[Android系统源代码情景分析](https://item.jd.com/11838754.html)：罗升阳 著

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/android_source_code_scenario_analysis.png"/>

[Android开发艺术探索](https://item.jd.com/11760209.html)：任玉刚 著

<img src="https://github.com/guoxiaoxing/android-framework-source-code-analysis/raw/master/art/android_develop_art_explore.png"/>

好了，以上就是阅读 AOSP 需要做的全部工作，如果你已经对源码产生了兴趣，那么就去[章节](https://github.com/guoxiaoxing/android-framework-source-code-analysis/blob/master/doc/章节.md)
里看看后续的写作安排吧。



