# Android包管理框架：APK的打包流程

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

- 一 资源的编译和打包
- 二 代码的编译和打包

Android的包文件APK分为两个部分：代码和资源，所以打包方面也分为资源打包和代码打包两个方面，这篇文章就来分析资源和代码的编译打包原理。

APK整体的的打包流程如下图所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/apk_package_flow.png"/>

具体说来：

1. 通过AAPT工具进行资源文件（包括AndroidManifest.xml、布局文件、各种xml资源等）的打包，生成R.java文件。
2. 通过AIDL工具处理AIDL文件，生成相应的Java文件。
3. 通过Javac工具编译项目源码，生成Class文件。
4. 通过DX工具将所有的Class文件转换成DEX文件，该过程主要完成Java字节码转换成Dalvik字节码，压缩常量池以及清除冗余信息等工作。
5. 通过ApkBuilder工具将资源文件、DEX文件打包生成APK文件。
6. 利用KeyStore对生成的APK文件进行签名。
7. 如果是正式版的APK，还会利用ZipAlign工具进行对齐处理，对齐的过程就是将APK文件中所有的资源文件举例文件的起始距离都偏移4字节的整数倍，这样通过内存映射访问APK文件
的速度会更快。

上述流程都是Android Studio在编译时调用各种编译命令自动完成的，具体说来，如下所示：

1. 创建Android工程。

> android  create project \
    -n packageTest2 \
    -a MainActivity \
    -k com.package.test2 \
    -t android-23 \
    -p ./PackageTest2
    
2. 编译R文件

> aapt  package \
   -f \
   -J ./gen \
   -M ./AndroidManifest.xml \
   -S ./res/ \
   -I /Users/RadAsm/Library/AndroidSDK/sdk/platforms/android-23/android.jar
 
3. 编译源代码文件

> javac -source 1.6 \
   -target 1.6 \
   -cp /Users/RadAsm/Library/AndroidSDK/sdk/platforms/android-23/android.jar \
   ./src/com/packtest/test1/MainActivity.java ./src/com/packtest/test1/R.java \
   -d ./gen/classes

4. 编译DEX文件

>  dx --dex \
    --verbose \
    --output ./gen/dex/packtest1.dex 
    ./gen/classes/

5. 生成APK文件

> aapt package 
     -f \
     -J ./gen \
     -M ./AndroidManifest.xml \
     -S ./res/ \
     -I /Users/RadAsm/Library/AndroidSDK/sdk/platforms/android-23/android.jar \
     -F ./output/res.apk
  
6. APK文件对齐
 
> zipalign -v -p 4 packagetest_unsigned.apk packagetest_aligned_unsigned.apk

7. APK签名

> apksigner sign --ks my-release-key.jks my-app.apk

以上便是APK打包的整个流程，我们再来总结一下：

1. 除了assets和res/raw资源被原装不动地打包进APK之外，其它的资源都会被编译或者处理；
2. 除了assets资源之外，其它的资源都会被赋予一个资源ID；
3. 打包工具负责编译和打包资源，编译完成之后，会生成一个resources.arsc文件和一个R.java，前者保存的是一个资源索引表，后者定义了各个资源ID常量。
4. 应用程序配置文件AndroidManifest.xml同样会被编译成二进制的XML文件，然后再打包到APK里面去。
5. 应用程序在运行时通过AssetManager来访问资源，或通过资源ID来访问，或通过文件名来访问。

理解了整体的流程，我们再来看看具体的细节。

## 一 资源的编译和打包

在分析资源的编译和打包之前，我们先来了解一下Android程序包里有哪些资源。

我们知道Android应用程序的设计也是代码与资源相分离的，Android的资源文件可以分为两大类：

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

前面我们提到xml编写的Android资源文件都会编译成二进制格式的xml文件，资源的打包都是由AAPT工具来完成的，资源打包主要有以下流程：

1. 解析AndroidManifest.xml，获得应用程序的包名称，创建资源表。
2. 添加被引用资源包，被添加的资源会以一种资源ID的方式定义在R.java中。
3. 资源打包工具创建一个AaptAssets对象，收集当前需要编译的资源文件，收集到的资源保存在AaptAssets对象对象中。
4. 将上一步AaptAssets对象保存的资源，添加到资源表ResourceTable中去，用于最终生成资源描述文件resources.arsc。
5. 编译values类资源，这类资源包括数组、颜色、尺寸、字符串等值。
6. 给bag、style、array这类资源分配资源ID。
7. 编译xml资源文件，编译的流程分为：① 解析xml文件 ② 赋予属性名称资源ID ③ 解析属性值 ④ 将xml文件从文本格式转换为二进制格式，四步。
8. 生成资源索引表resources.arsc。

### 1.1 资源ID

每个Android项目里都有有一个R.java文件，如下所示：

```java
public final class R {
     //...
     public static final class anim {
        public static final int abc_fade_in=0x7f010000;
     }
     public static final class attr {
         public static final int actionBarDivider=0x7f020000;
     }
     public static final class string {
          public static final int actionBarDivider=0x7f020000;
     }
     //...
}
```

每个资源项后的整数就是资源ID，资源ID是一个4字节的无符整数，如下所示：

- 最高字节是Package ID表示命名空间，标明资源的来源，Android系统自己定义了两个Package ID，系统资源命名空间：0x01 和 应用资源命名空间：0x7f。
- 次字节是Type ID，表示资源的类型，例如：anim、color、string等。
- 最低两个字节是Entry ID，表示资源在其所属资源类型中所出现的次序。

### 1.2 资源索引

上面提到，最终生成的是资源索引表resources.arsc，Android正是利用这个索引表根据资源ID进行资源的查找，为不同语言、不同地区、不同设备提供相对应的最佳资源。查找和通过Resources和
AssetManger来完成的，这个我们下面会讲。

resources.arsc是一个编译后的二进制文件，在Android Stduio里打开以后是这样的，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/resource/resources_arsc_file.png"/>

可以看到resources.arsc里存放l各类资源的索引参数和配置信息。

resources.arsc的文件格式如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/resource/resources_arsc_structure.png"/>

注：整个文件都是有一系列chuck（块）构成的，chuck是整个文件的划分单位，每个模块都是一个chuck，chuck最前面是一个ResChunk_header的结构体，用来描述整个chunk的信息，如下所示：

更多关于索引表格式的细节，可以查阅源码：

👉 [ResourceTypes.h](https://android.googlesource.com/platform/frameworks/base/+/56a2301/include/androidfw/ResourceTypes.h)

resources.arsc索引表从上至下文件格式依次为：

- 文件头：数据结构用ResTable_header来描述，用来描述整个文件的信息，包括文件头大小，文件大小，资源包Package的数量等信息。
- 全局字符串池：存放所有的字符串，所以资源复用这些字符串，字符串里存放的是资源文件的路径名和资源值等信息。全局字符串池分为资源类型（type）字符串池和
- 资源包：会有多个（例如：系统资源包、应用资源包）。

资源包也被划分为以下几个部分：

- 包头：描述资源包相关信息。
- 资源类型字符串池：存放资源的类型。
- 资源名称字符串池：存放资源的名称。
- 配置列表：存放语音、位置等手机配置信息，用来作为查找资源的标准。

从这里可以看到resources.arsc索引表存在很多常量池，常量池的使用目的也很明显，就是提供资源的复用率，减少resources.arsc索引表的体积，提高索引效率。

## 二 代码的编译和打包






## 附录

Android打包流程详图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/apk_package_flow_detail.png"/>

