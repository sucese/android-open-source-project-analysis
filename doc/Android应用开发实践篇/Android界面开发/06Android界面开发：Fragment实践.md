# Android界面开发：Fragment实践

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

- Fragment基本操作
- Fragment通信方式
- Fragment回退栈
- Fragment与ViewPager

我们为什么要选择Fragment代替Activity，实现单Activity+多Fragment或者多模块+多Fragment的架构？🤔

- Fragment相对于Activity，无需ActivityManagerService的跨进程通信，切换更加轻量级，响应速度更快，占用资源更少。
- Fragment相对于View，拥有更多的声明周期，可以管理Menu，持有Activity的引用，它可以将负责的业务逻辑解耦并可以进行组合使用，更利于模块化，并为app向pad平台扩展提供支持。

这便是我们使用Fragment两个最主要的原因。

## 一 Fragment基本操作

Fragment有七种基本操作。

- add：添加一个Fragment
- remove：移除一个Fragment
- replace：替换一个Fragment
- hide：隐藏一个Fragment
- show：显示一个Fragment
- detach：接除一个Fragment
- attach：关联一个Fragment

不同的操作对Fragment生命周期的影响是不一样的，我们可以根据自己的需要选择相应的方法。

add: add操作添加一个Fragment，会依次调用 onAttach, onCreate, onCreateView, onStart and onResume 等方法。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_add.png"/>

remove: remove操作移除一个Fragment，会依次调用nPause, onStop, onDestroyView, onDestroy and onDetach 等方法。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_remove.png"/>

replace: replace操作相当于remove+add，它同样会导致视图的重建。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_replace.png"/>

show：show操作会显示一个的视图，它只会触发onHiddenChange()方法。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_show.png"/>

hide：hide操作会隐藏一个的视图，它只会触发onHiddenChange()方法。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_hide.png"/>

attach: attach操作关联一个Fragment，会依次调用onCreateView, onStart and onResume 。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_attach.png"/>

detach: detach操作分离一个Fragment，会依次调用onPause, onStop and onDestroyView  等方法。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_detach.png"/>

popBackStack

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_pop_back.png"/>


关于使用Fragment操作的使用建议

- 如果Fragment视图被频繁的使用，或者一会要再次使用，建议使用show/hide方法，这样可以提升响应速度和性能。
- 如果Fragment占用大量资源，使用完成后，可以使用replace方法，这样可以及时的释放资源。

Fragment的复杂性或者不方便的地方，就是操作方法的调用和回退栈的管理。


一些其他建议

- Fragment的数据传递通过setArguments/getArguments进行，这样在Activity重启时，系统会帮你保存数据，这点和Activity很相似。
- Fragment的构造通过newInstance()静态方法进行，这样调用者只用关心参数，而无需关心传递的key值。


##

当配置发生改变、系统内存紧张等原因导致Activity重建。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/component/fragment_lifecycle_recreate.png"/>
