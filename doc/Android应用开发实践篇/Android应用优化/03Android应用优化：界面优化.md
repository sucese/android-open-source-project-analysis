# Android应用优化：界面优化

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

## 一 顿检测

我们可以利用BlockCanary去检查造成UI卡顿的地方，如下所示：

BlockCanary：https://github.com/markzhai/AndroidPerformanceMonitor

BlockCanary检查UI卡顿的原理如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/blockcanary_structure.png"/>

## 二 卡顿优化

Android界面优化主要解决界面卡顿的问题，Android系统每隔16ms就会发送一个VSYNC信号，触发UI渲染，如果绘制操作超过了16ms，就会引起掉帧，也就是会导致姐们卡顿。

导致界面卡顿的原因主要是过度绘制，绘制了多余的UI，开发者选项里有检测过度绘制的工具，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/performance/overdraw_level.png" width="250"/>

1. 移除不必要的backgroud。
2. 自定义View的时候clipReact减少重叠区域的绘制。
3. 利用<merge>等标签减少View的层级。
4. 利用<ViewStub>在需要的时候再去加载View。