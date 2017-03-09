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

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/understand.png"/>

### 演示文稿

演示文稿是用 Keynote 来做的。

[Keynote](http://xclient.info/s/keynote.html)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/office_for_mac.png"/>

### 文章写作

文章都是用 Markdown 来写的，工具用的是MWeb，一款很强大的 Markdown 编辑工具。

[MWeb for Mac](http://www.mweb.im/)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/nweb_for_mac.png"/>

### 图例绘制

流程图、类图、时序图、系统架构图等各种图例采用EdrawMax来绘制，Visio 也比较好用，可惜 Mac 下没有。

[EdrawMax](http://xclient.info/s/edraw-max.html)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/EdrawMax.png"/>

### 图片处理

Gif 图的制作用的是VideoGIF。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/VideoGIF.png"/>

[VideoGIF](http://xclient.info/s/videogif.html)

## 书籍篇

站在前辈的肩膀上，我们能看的更远，进步的更快。以下是学习框架与源码一些不错的书籍。

[Android 源码设计模式解析与实战](https://item.jd.com/11793928.html)：何红辉，关爱民 著

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/android_source_code_design_pattern.png"/>

[Android系统源代码情景分析](https://item.jd.com/11838754.html)：罗升阳 著

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/android_source_code_scenario_analysis.png"/>

[Android开发艺术探索](https://item.jd.com/11760209.html)：任玉刚 著

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/android_develop_art_explore.png"/>

好了，以上就是阅读 AOSP 需要做的全部工作了，如果你已经对源码产生了兴趣，那么不妨去[章节](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/章节.md)
里看看后续的写作安排吧。