# Android虚拟机框架：APK打包、安装与加载流程

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

## 一 APK打包流程

APK的打包流程如下图所示：

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

事实上，整个打包的过程中还有许多细节需要处理，例如NDK的处理、Proguard的处理、Render Script的处理等等，我们来看一张更加详细的图：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/apk_package_flow_detail.png"/>

流程虽然长，但是并不难理解，通过上面这两张图，相信读者对整个流程有了整体的把握，我们接着来看这些流程实现的细节。

### 1.1 资源的编译和打包

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

#### 1.1.1 资源ID

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

#### 1.1.2 资源索引

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

从这里可以看到resources.arsc索引表存在很多常量池，常量池的使用目的也很明显，就是提供资源的复用率，减少resources.arsc索引表的体积，提高索引效率。关于资源池在Java层
和C++层的实现，我们下面会讲。

### 1.2 代码的编译和打包

## 二 APK安装流程

我们来思考一下Android系统是如何安装一个APK文件的，从直观的流程上，当我们点击一个APK文件或者从应用商店下载一个APK文件，会弹起一个安装对话框，点击安装就可以安装应用。

那么这里面的流程是什么样的呢？🤔

首先很容易想到的是，Android根据文件类型MimeType来判断是否弹起安装页面，就行点击一个视频会调起视频播放器一样。

Android系统常见的文件类型如下所示：

👉 [MimeUtils.java](https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/libcore/net/MimeUtils.java)

- add("application/zip", "zip");
- add("application/vnd.android.package-archive", "apk");
- add("video/mp4", "mp4");
- add("video/3gpp", "3gpp");
- add("text/plain", "txt");
- add("image/gif", "gif");
- add("image/ico", "ico");
- add("image/jpeg", "jpeg");
- add("image/jpeg", "jpg");

这里面就有我们今天聊的APK文件，当点击APK文件时会调起安装界面，这个安装界面其实就是PackageInstallerActivity

```java
//点击APK文件，弹起对话框，询问是否安装此应用。
File apkFile;
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
context.startActivity(intent);
```

PackageInstallerActivity显示的是个对话框，当点击确定安装以后，会启动真正的安装界面，这个界面就是InstallAppProgress，它也是一个Activity，会显示安装的进度，

整个APK的安装流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/package/apk_install_structure.png" width="600"/>

1. 复制APK到/data/app目录下，解压并扫描安装包。
2. 资源管理器解析APK里的资源文件。
3. 解析AndroidManifest文件，并在/data/data/目录下创建对应的应用数据目录。
4. 然后对dex文件进行优化，并保存在dalvik-cache目录下。
5. 将AndroidManifest文件解析出的四大组件信息注册到PackageManagerService中。
5. 安装完成后，发送广播。

总体说来就两件事情拷贝APK和解析APK，解析APK主要是解析APK的应用配置文件AndroidManifest.xml，以便获得它的安装信息。在安装的过程中还会这个应用分配Linux用
户ID和Linux用户组ID（以便它可以在系统中获取合适的运行权限）。

**关于Linux用户ID与Linux用户组ID**

Linux用户ID与Linux用户组ID的分配与管理是由Settings类来完成的。

>Settings：该类用来管理应用程序的安装信息（APK包信息、Linux用户ID、Linux用户组ID等），Android系统在每次重启是都会将应用程序重新安装一遍，Settings就是保证在重新安装应用时可以恢复应用的信息。

在Android系统中，用户ID可以划分为以下层次：

- 小于FIRST_APPLICATION_UID：特权用户ID，用户应用程序不能直接使用，但是可以以一种sharedUserId的方式共享使用，例如想要修改系统时间就设置android:sharedUserId = "android.uid.system"。
- FIRST_APPLICATION_UID 至 FIRST_APPLICATION_UID + MAX_APPLICATION_UIDS：给用户应用程序使用，共有1000个。
- 大于FIRST_APPLICATION_UID + MAX_APPLICATION_UIDS：非法的Linxu用户ID。

以上便是对APK安装流程的整体概述，有了整体的把握，我们接着来看实现细节。APK安装流程序列图如下所示：

👉 点击图片查看高清大图

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/package/apk_install_sequence.png"/>

嗯，看起来有点长😤，但只要我们掌握核心逻辑和原理，再长的函数调用链都是纸老虎。😎

整个序列图按照颜色划分为三个进程：

- PackageInstaller进程：PackageInstaller事实上是一个应用，它负责APK安装以及卸载过程中与用户的交互流程。
- SystemServer进程：该进程主要运行的是系统服务，APK的安装、卸载和查询都由PackageManagerService负责，它也是Android核心系统服务的一种，在SystemServer里初始化系统服务的时候被启动。
- DefaultContainerService进程：DefaultContainerService也是一个单独的进程，它主要负责检查和复制设备上的文件，APK的复制就是由DefaultContainerService来完成的。

PackageInstaller和DefaultContainerService都比较简单，我们重点关注的是Android的包管理服务PackageManagerService。

## 2.1 APK解析流程

Android的应用程序是一个以".apk"为后缀名的归档文件，它在安装之前会先DefaultContainerService将自己复制到/data/app目录中去，拷贝完成以后
便开始解析APK。

这里提一下/data/app这个目录，Android不同的目录存放不同类型的应用，如下所示：

- /system/framwork：保存的是资源型的应用程序，它们用来打包资源文件。
- /system/app：保存系统自带的应用程序。
- /data/app：保存用户安装的应用程序。
- /data/app-private：保存受DRM保护的私有应用程序。
- /vendor/app：保存设备厂商提供的应用程序。

APK文件里包含了一个配置文件AndroidManifest.xml，Android应用程序的解析过程就是解析这个xml文件的过程。

从上面的序列图我们可以看出，APK解析是从PackageManagerService的scanPackageLI开始的，而该方法内部又调用的是scanPackageDirtyLI()方法，我们来看一下这个方法的实现。

```java
public class PackageManagerService extends IPackageManager.Stub {
    
       private PackageParser.Package scanPackageDirtyLI(PackageParser.Package pkg, int parseFlags,
               int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
           //...
           // writer
           synchronized (mPackages) {
               // 验证已注册的ContentProvider是否有其他同名，做冲突检测。
               if ((scanFlags & SCAN_NEW_INSTALL) != 0) {
                   final int N = pkg.providers.size();
                   int i;
                   for (i=0; i<N; i++) {
                       PackageParser.Provider p = pkg.providers.get(i);
                       if (p.info.authority != null) {
                           String names[] = p.info.authority.split(";");
                           for (int j = 0; j < names.length; j++) {
                               if (mProvidersByAuthority.containsKey(names[j])) {
                                   PackageParser.Provider other = mProvidersByAuthority.get(names[j]);
                                   final String otherPackageName =
                                           ((other != null && other.getComponentName() != null) ?
                                                   other.getComponentName().getPackageName() : "?");
                                   throw new PackageManagerException(
                                           INSTALL_FAILED_CONFLICTING_PROVIDER,
                                                   "Can't install because provider name " + names[j]
                                                   + " (in package " + pkg.applicationInfo.packageName
                                                   + ") is already used by " + otherPackageName);
                               }
                           }
                       }
                   }
               }
           }
         
           if (mPlatformPackage == pkg) {
              //...
           } else {
               // This is a normal package, need to make its data directory.
               dataPath = getDataPathForPackage(pkg.packageName, 0);
               if (dataPath.exists()) {
                   //...
               } else {
                   //invoke installer to do the actual installation
                   //这里创建了应用数据目录，用于存放用户数据
                   int ret = createDataDirsLI(pkgName, pkg.applicationInfo.uid,
                                              pkg.applicationInfo.seinfo);
                   //...
               }
             
           }
         
           // We also need to dexopt any apps that are dependent on this library.  Note that
           // if these fail, we should abort the install since installing the library will
           // result in some apps being broken.
           if (clientLibPkgs != null) {
               if ((scanFlags & SCAN_NO_DEX) == 0) {
                   for (int i = 0; i < clientLibPkgs.size(); i++) {
                       PackageParser.Package clientPkg = clientLibPkgs.get(i);
                       if (performDexOptLI(clientPkg, null /* instruction sets */, forceDex,
                               (scanFlags & SCAN_DEFER_DEX) != 0, false) == DEX_OPT_FAILED) {
                           throw new PackageManagerException(INSTALL_FAILED_DEXOPT,
                                   "scanPackageLI failed to dexopt clientLibPkgs");
                       }
                   }
               }
           }
         
           // writer
           synchronized (mPackages) {
               //...
               // 以下对四大组件进行注册
               int N = pkg.providers.size();
               StringBuilder r = null;
               int i;
               for (i=0; i<N; i++) {
                   PackageParser.Provider p = pkg.providers.get(i);
                   p.info.processName = fixProcessName(pkg.applicationInfo.processName,
                           p.info.processName, pkg.applicationInfo.uid);
                   //注册Content Provider
                   mProviders.addProvider(p);
                   //...
               }
               //...
           }
       }
       //...
   }
}
```
scanPackageDirtyLI是一个上千行的函数，它主要完成的工作如下所示：

1. 调用PackageParser的parsePackage()方法解析AndroidMainfest.xml文件，主要包括四大组件、权限信息、用户ID，其他use-feature、shared-userId、use-library等
信息，并保存到PackageManagerService相应的成员变量中。
2. 调用签名验证方法verifySignaturesLP()进行签名验证，验证失败的无法进行安装。
3. 调用createDataDirsDirtyLI()方法创建应用目录/data/data/package，同时将APK中提取的DEX文件保存到/data/dalvik-cache中。
4. 调用performDexOptLI()方法执行dexopt操作。

我们接着来看看APK里的信息是如何被解析出来的。

Apk的解析是PackageParser的parsePackage()函数来完成的，我们来看看它的实现。

```java
public class PackageParser {
    
     public Package parsePackage(File packageFile, int flags) throws PackageParserException {
         if (packageFile.isDirectory()) {
             return parseClusterPackage(packageFile, flags);
         } else {
             return parseMonolithicPackage(packageFile, flags);
         }
     }
     
     private Package parseClusterPackage(File packageDir, int flags) throws PackageParserException {
             //...
             
             //初始化AssetManager
             final AssetManager assets = new AssetManager();
             try {
                 //...
                 //解析Base APk，解析AndroidManifest.xml
                 final Package pkg = parseBaseApk(baseApk, assets, flags);
                 if (pkg == null) {
                     throw new PackageParserException(INSTALL_PARSE_FAILED_NOT_APK,
                             "Failed to parse base APK: " + baseApk);
                 }
     
                 //如果splitName不为空，则循环解析Split Apk
                 if (!ArrayUtils.isEmpty(lite.splitNames)) {
                     final int num = lite.splitNames.length;
                     pkg.splitNames = lite.splitNames;
                     pkg.splitCodePaths = lite.splitCodePaths;
                     pkg.splitRevisionCodes = lite.splitRevisionCodes;
                     pkg.splitFlags = new int[num];
                     pkg.splitPrivateFlags = new int[num];
     
                     for (int i = 0; i < num; i++) {
                         //解析
                         parseSplitApk(pkg, i, assets, flags);
                     }
                 }
     
                 pkg.setCodePath(packageDir.getAbsolutePath());
                 pkg.setUse32bitAbi(lite.use32bitAbi);
                 return pkg;
             } finally {
                 IoUtils.closeQuietly(assets);
             }
         }
}

```

注：Split APK是Google为解决65535上限以及APK越来越大的问题而提出的一种方案，它可以将一个庞大的APK按照屏幕密度、ABI等形式拆分成多个独立的APK，这些APK共享相同的data、cache目录。
共享相同的进程，共享相同的包名。它们还可以使用各自的资源，并且继承了Base APK里的资源。更多细节可以查阅官方文档[Build Multiple APKs](https://developer.android.com/studio/build/configure-apk-splits.html)。

该方法调用parseBaseApk()去解析AndroidManifest.xml，AndroidManifest.xml也是xml文件，当然也使用XmlResourceParser来解析。这个解析相应标签并保存到PackageManagerService对应的成员变量中去。
此处就不再展开。

通过上面的讲解，我们理解了APK的计息流程，APK解包以后，里面有个DEX文件，我们前面也说过PackageManagerService的performPackageLI()方法去执行dexopt操作
，我们接着来分析它是如何实现的。

## 2.2 DEX的dexopt流程

我们先来了解一下什么dexopt操作，dexopt操作实际上对DEX文件在执行前进行一些优化，但是不同的虚拟机操作有所不同。

- Davlik：将dex文件优化生成odex文件，这个odex文件的后缀也是dex，保存在/data/dalvik-cache目录下。
- ART：将dex文件翻译生成oat文件

从上面的序列图我们可以看出，Installer.java通过Socket方式请求守护进程installd完成dexopt操作。

```java
public final class Installer {  
    public int dexopt(String apkPath, int uid, boolean isPublic) {  
        StringBuilder builder = new StringBuilder("dexopt");  
        builder.append(' ');  
        builder.append(apkPath);  
        builder.append(' ');  
        builder.append(uid);  
        builder.append(isPublic ? " 1" : " 0");  
        return execute(builder.toString());  
    }  
}
```
守护进程调用Command.c里的dexopt()方法执行dexopt操作，如果你对dexopt的C++层的实现感兴趣可以异步：

👉 [Android ART运行时无缝替换Dalvik虚拟机的过程分析](http://blog.csdn.net/luoshengyang/article/details/18006645)

APK安装完成以后会在桌面生成一个快捷图标，点击图标就可以启动应用了。

预校验问题

## 三 APK加载流程

我们前面说过APK可以分为代码与资源两部分，那么在加载APK时也会涉及代码的加载和资源的加载，代码的加载事实上对应的就是Android应用进程的创建流程，关于这一块的内容我们在文章[01Android进程框架：进程的创建、启动与调度流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统底层框架篇/Android进程框架/01Android进程框架：进程的创建、启动与调度流程.md)已经分析过，本篇文章
我们着重来分析资源的加载流程。

我们知道在代码中我们通常会通过getResource()去获取Resources对象，Resource对象是应用进程内的一个全局对象，它用来访问应用的资源。除了Resources对象我们还可以通过getAsset()获取
AssetManger来读取指定文件路径下的文件。Resource与AssetManger这对兄弟就构造了资源访问框架的基础。

那么AssetManager对象与Resources对象在哪里创建的呢？🤔

### 3.1 AssetManager的创建流程

我们知道每个启动的应用都需要先创建一个应用上下文Context，Context的实际实现类是ContextImpl，ContextImpl在创建的时候创建了Resources对象和AssetManager对象。

AssetManager对象创建序列图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/resource/asset_manager_create_sequence.png"/>

我们可以发现在整个流程AssetManager在Java和C++层都有一个实现，那么它们俩有什么关系呢？🤔

>事实上实际的功能都是由C++层的AssetManag来完成的。每个Java层的AssetManager对象都一个long型的成员变量mObject，用来保存C++层
AssetManager对象的地址，通过这个变量将Java层的AssetManager对象与C++层的AssetManager对象关联起来。

```java
public final class AssetManager implements AutoCloseable {
    // 通过这个变量将Java层的AssetManager对象与C++层的AssetManager对象关联起来。
    private long mObject;
}
```

从上述序列图中我们可以看出，最终调用Asset的构造函数来创建Asset对象，如下所示：

```java
public final class AssetManager implements AutoCloseable {
      public AssetManager() {
          synchronized (this) {
              if (DEBUG_REFS) {
                  mNumRefs = 0;
                  incRefsLocked(this.hashCode());
              }
              init(false);
              if (localLOGV) Log.v(TAG, "New asset manager: " + this);
              //创建系统的AssetManager
              ensureSystemAssets();
          }
      }
  
      private static void ensureSystemAssets() {
          synchronized (sSync) {
              if (sSystem == null) {
                  AssetManager system = new AssetManager(true);
                  system.makeStringBlocks(null);
                  sSystem = system;
              }
          }
      }
      
      private AssetManager(boolean isSystem) {
          if (DEBUG_REFS) {
              synchronized (this) {
                  mNumRefs = 0;
                  incRefsLocked(this.hashCode());
              }
          }
          init(true);
          if (localLOGV) Log.v(TAG, "New asset manager: " + this);
      }
      
    private native final void init(boolean isSystem);
}
```

可以看到构造函数会先调用native方法init()去构造初始化AssetManager对象，可以发现它还调用了ensureSystemAssets()方法去创建系统AssetManager，为什么还会有个系统AssetManager呢？🤔

>这是因为Android应用程序不仅要访问自己的资源，还需要访问系统的资源，系统的资源放在/system/framework/framework-res.apk文件中，它在应用进程中是通过一个单独的Resources对象（Resources.sSystem）
和一个单独的AssetManger（AssetManager.sSystem）对象来管理的。

我们接着来看native方法init()的实现，它实际上是调用android_util_AssetManager.cpp类的android_content_AssetManager_init()方法，如下所示；

👉 [android_util_AssetManager.cpp](https://android.googlesource.com/platform/frameworks/base.git/+/android-4.3_r2.1/core/jni/android_util_AssetManager.cpp)

```java
static void android_content_AssetManager_init(JNIEnv* env, jobject clazz, jboolean isSystem)
{
    if (isSystem) {
        verifySystemIdmaps();
    }
    //构建AssetManager对象
    AssetManager* am = new AssetManager();
    if (am == NULL) {
        jniThrowException(env, "java/lang/OutOfMemoryError", "");
        return;
    }

    //添加默认的资源路径，也就是系统资源的路径
    am->addDefaultAssets();

    ALOGV("Created AssetManager %p for Java object %p\n", am, clazz);
    env->SetLongField(clazz, gAssetManagerOffsets.mObject, reinterpret_cast<jlong>(am));
}
```
我们接着来看看AssetManger.cpp的ddDefaultAssets()方法。

👉 [AssetManager.cpp](https://android.googlesource.com/platform/frameworks/base/+/master/libs/androidfw/AssetManager.cpp)

```java
static const char* kSystemAssets = "framework/framework-res.apk";

bool AssetManager::addDefaultAssets()
{
    const char* root = getenv("ANDROID_ROOT");
    LOG_ALWAYS_FATAL_IF(root == NULL, "ANDROID_ROOT not set");

    String8 path(root);
    path.appendPath(kSystemAssets);

    return addAssetPath(path, NULL, false /* appAsLib */, true /* isSystemAsset */);
}
```
ANDROID_ROOT指的就是/sysetm目录，全局变量kSystemAssets指向的是"framework/framework-res.apk"，所以拼接以后就是我们前面说的系统资源的存放目录"/system/framework/framework-res.apk"

拼接好path后作为参数传入addAssetPath()方法，注意Java层的addAssetPath()方法实际调用的也是底层的此方法，如下所示：

```java

static const char* kAppZipName = NULL; //"classes.jar";

bool AssetManager::addAssetPath(
        const String8& path, int32_t* cookie, bool appAsLib, bool isSystemAsset)
{
    AutoMutex _l(mLock);

    asset_path ap;

    String8 realPath(path);
    //kAppZipName如果不为NULL，一般将会被设置为classes.jar
    if (kAppZipName) {
        realPath.appendPath(kAppZipName);
    }
    
    //检查传入的path是一个文件还是一个目录，两者都不是的时候直接返回
    ap.type = ::getFileType(realPath.string());
    if (ap.type == kFileTypeRegular) {
        ap.path = realPath;
    } else {
        ap.path = path;
        ap.type = ::getFileType(path.string());
        if (ap.type != kFileTypeDirectory && ap.type != kFileTypeRegular) {
            ALOGW("Asset path %s is neither a directory nor file (type=%d).",
                 path.string(), (int)ap.type);
            return false;
        }
    }

    //资源路径mAssetPaths是否已经添加过参数path描述的一个APK的文件路径，如果
    //已经添加过，则不再往下处理。直接将path保存在输出参数cookie中
    for (size_t i=0; i<mAssetPaths.size(); i++) {
        if (mAssetPaths[i].path == ap.path) {
            if (cookie) {
                *cookie = static_cast<int32_t>(i+1);
            }
            return true;
        }
    }

    ALOGV("In %p Asset %s path: %s", this,
         ap.type == kFileTypeDirectory ? "dir" : "zip", ap.path.string());

    ap.isSystemAsset = isSystemAsset;
    //path所描述的APK资源路径没有被添加过，则添加到mAssetPaths中。
    mAssetPaths.add(ap);

    //...

    return true;
```

该方法的实现也很简单，就是把path描述的APK资源路径加入到资源目录数组mAssetsPath中去，mAssetsPath是AssetManger.cpp的成员变量，AssetManger.cpp有三个
比较重要的成员变量：

- mAssetsPath：资源存放目录。
- mResources：资源索引表。
- mConfig：设备的本地配置信息，包括设备大小，国家地区、语音等配置信息。

有了这些变量AssetManger就可以正常的工作了。AssetManger对象也就创建完成了。

ResroucesManager的createResroucesImpl()方法会先调用createAssetManager()方法创建AssetManger对象，然后再调用ResourcesImpl的构造方法创建ResourcesImpl对象。

### 3.1 Resources的创建流程

Resources对象的创建序列图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/resource/resource_create_sequence.png"/>

ResourcesImpl的构造方法如下所示：

```java
public class ResourcesImpl {
   public ResourcesImpl(@NonNull AssetManager assets, @Nullable DisplayMetrics metrics,
            @Nullable Configuration config, @NonNull DisplayAdjustments displayAdjustments) {
        mAssets = assets;
        mMetrics.setToDefaults();
        mDisplayAdjustments = displayAdjustments;
        updateConfiguration(config, metrics, displayAdjustments.getCompatibilityInfo());
        mAssets.ensureStringBlocks();
    }
}
```
在这个方法里有两个重要的函数：

- updateConfiguration(config, metrics, displayAdjustments.getCompatibilityInfo())：首先是根据参数config和metrics来更新设备的当前配置信息，例如，屏幕大小和密码、国家地区和语言、键盘
配置情况等，接着再调用成员变量mAssets所指向的一个Java层的AssetManager对象的成员函数setConfiguration来将这些配置信息设置到与之关联的C++层的AssetManger。
- ensureStringBlocks()：读取

我们重点来看看ensureStringBlocks()的实现。

```java
public final class AssetManager implements AutoCloseable {
    
    @NonNull
    final StringBlock[] ensureStringBlocks() {
        synchronized (this) {
            if (mStringBlocks == null) {
                //读取字符串资源池，sSystem.mStringBlocks表示系统资源索引表的字符串常量池
                //前面我们已经创建的了系统资源的AssetManger sSystem，所以系统资源字符串资源池已经读取完毕。
                makeStringBlocks(sSystem.mStringBlocks);
            }
            return mStringBlocks;
        }
    }

    //seed表示是否要将系统资源索引表里的字符串资源池也一起拷贝出来
    /*package*/ final void makeStringBlocks(StringBlock[] seed) {
        //系统资源索引表个数
        final int seedNum = (seed != null) ? seed.length : 0;
        //总的资源索引表个数
        final int num = getStringBlockCount();
        mStringBlocks = new StringBlock[num];
        if (localLOGV) Log.v(TAG, "Making string blocks for " + this
                + ": " + num);
        for (int i=0; i<num; i++) {
            if (i < seedNum) {
                //系统预加载资源的时候，已经解析过framework-res.apk中的resources.arsc
                mStringBlocks[i] = seed[i];
            } else {
                //调用getNativeStringBlock(i)方法读取字符串资源池
                mStringBlocks[i] = new StringBlock(getNativeStringBlock(i), true);
            }
        }
    }
    
    private native final int getStringBlockCount();
    private native final long getNativeStringBlock(int block);
}
```

首先解释一下什么是StringBlocks，StringBlocks描述的是一个字符串资源池，Android里每一个资源索引表resources.arsc都包含一个字符串资源池。 getStringBlockCount()
方法返回的也就是这种资源池的个数。

上面我们已经说了resources.arsc的文件格式，接下来就会调用native方法getNativeStringBlock()去解析resources.arsc文件的内容，获取字符串
常量池，getNativeStringBlock()方法实际上就是将每一个资源包里面的resources.arsc的数据项值的字符串资源池数据块读取出来，并封装在C++层的StringPool对象中，然后Java层的makeStringBlocks()方法
再将该StringPool对象封装成Java层的StringBlock中。

关于C++层的具体实现，可以参考罗哥的这两篇博客：

- [resources.arsc的数据格式](http://blog.csdn.net/luoshengyang/article/details/8744683)
- [resources.arsc的解析流程](http://blog.csdn.net/luoshengyang/article/details/8806798)

如此，AssetManager和Resources对象的创建流程便分析完了，这两个对象构成了Android应用程序资源管理器的核心基础，资源的加载就是借由这两个对象来完成的。

### 3.3 资源的查找与解析流程

前面我们分析了AssetManager和Resources对象的创建流程，AssetManager根据文件名来查找资源，Resouces根据资源ID查找资源，如果资源ID对应的是个文件，那么会Resouces先根据资源ID查找
出文件名，AssetManger再根据文件名查找出具体的资源。

整个流程还是比较简单的，我们以layout.xml文件的查找流程为例来说明一下，具体序列图如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/LayoutInflater_inflate_sequence.png"/>

我们先来看看总的调度方法inflate()，这个也是我们最常用的

```java
public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot)
```
这个方法有三个参数：

int resource：布局ID，也就是要解析的xml布局文件，boolean attachToRoot表示是否要添加到父布局root中去。这里面还有个关键的参数root。它用来表示根布局，这个就很常见的，我们在用
这个方法的时候，有时候给root赋值了，有时候直接给了null（给null的时候IDE会有警告提示），这个root到底有什么作用呢？🤔

它主要有两个方面的作用：

- 当attachToRoot == true且root ！= null时，新解析出来的View会被add到root中去，然后将root作为结果返回。
- 当attachToRoot == false且root ！= null时，新解析的View会直接作为结果返回，而且root会为新解析的View生成LayoutParams并设置到该View中去。
- 当attachToRoot == false且root == null时，新解析的View会直接作为结果返回。

注意第二条和第三条是由区别的，你可以去写个例子试一下，当root为null时，新解析出来的View没有LayoutParams参数，这时候你设置的layout_width和layout_height是不生效的。

说到这里，有人可能有疑问了，Activity里的布局应该也是LayoutInflater加载的，我也没做什么处理，但是我设置的layout_width和layout_heigh参数都是可以生效的，这是为什么？🤔

>这是因为Activity内部做了处理，我们知道Activity的setContentView()方法，实际上调用的PhoneWindow的setContentView()方法。它调用的时候将Activity的顶级DecorView（FrameLayout）
作为root传了进去，mLayoutInflater.inflate(layoutResID, mContentParent)实际调用的是inflate(resource, root, root != null)，所以在调用Activity的setContentView()方法时
可以将解析出的View添加到顶级DecorView中，我们设置的layout_width和layout_height参数也可以生效。

具体代码如下：

```java
@Override
public void setContentView(int layoutResID) {
    if (mContentParent == null) {
        installDecor();
    } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
        mContentParent.removeAllViews();
    }

    if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
        final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                getContext());
        transitionTo(newScene);
    } else {
        
        mLayoutInflater.inflate(layoutResID, mContentParent);
    }
    mContentParent.requestApplyInsets();
    final Callback cb = getCallback();
    if (cb != null && !isDestroyed()) {
        cb.onContentChanged();
    }
    mContentParentExplicitlySet = true;
}
```

了解了inflate()方法各个参数的含义，我们正式来分析它的实现。

```java

public abstract class LayoutInflater {

    public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        final Resources res = getContext().getResources();
        if (DEBUG) {
            Log.d(TAG, "INFLATING from resource: \"" + res.getResourceName(resource) + "\" ("
                    + Integer.toHexString(resource) + ")");
        }
        
        //获取xml资源解析器XmlResourceParser
        final XmlResourceParser parser = res.getLayout(resource);
        try {
            return inflate(parser, root, attachToRoot);//解析View
        } finally {
            parser.close();
        }
    }
}
```

可以发现在该方法里，主要完成了两件事情：

1. 获取xml资源解析器XmlResourceParser。
2. 解析View

我们先来看看XmlResourceParser是如何获取的。

从上面的序列图可以看出，调用了Resources的getLayout(resource)去获取对应的XmlResourceParser。getLayout(resource)又去调用了Resources的loadXmlResourceParser()
方法来完成XmlResourceParser的加载，如下所示：

```java
public class Resources {
    
     XmlResourceParser loadXmlResourceParser(@AnyRes int id, @NonNull String type)
             throws NotFoundException {
         final TypedValue value = obtainTempTypedValue();
         try {
             final ResourcesImpl impl = mResourcesImpl;
             //1. 获取xml布局资源，并保存在TypedValue中。
             impl.getValue(id, value, true);
             if (value.type == TypedValue.TYPE_STRING) {
                 //2. 加载对应的loadXmlResourceParser解析器。
                 return impl.loadXmlResourceParser(value.string.toString(), id,
                         value.assetCookie, type);
             }
             throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id)
                     + " type #0x" + Integer.toHexString(value.type) + " is not valid");
         } finally {
             releaseTempTypedValue(value);
         }
     }   
}
```

可以发现这个方法又被分成了两步：

1. 获取xml布局资源，并保存在TypedValue中。
2. 加载对应的loadXmlResourceParser解析器。

从上面的序列图可以看出，资源的获取涉及到resources.arsc的解析过程，这个我们已经在**Resources的创建流程**简单聊过，这里就不再赘述。通过
getValue()方法获取到xml资源以后，就会调用ResourcesImpl的loadXmlResourceParser()方法对该布局资源进行解析，以便得到一个UI布局视图。

我们来看看它的实现。

#### 3.3.1 获取XmlResourceParser

```java
public class ResourcesImpl {
    
    XmlResourceParser loadXmlResourceParser(@NonNull String file, @AnyRes int id, int assetCookie,
               @NonNull String type)
               throws NotFoundException {
           if (id != 0) {
               try {
                   synchronized (mCachedXmlBlocks) {
                       //... 从缓存中查找xml资源
   
                       // Not in the cache, create a new block and put it at
                       // the next slot in the cache.
                       final XmlBlock block = mAssets.openXmlBlockAsset(assetCookie, file);
                       if (block != null) {
                           final int pos = (mLastCachedXmlBlockIndex + 1) % num;
                           mLastCachedXmlBlockIndex = pos;
                           final XmlBlock oldBlock = cachedXmlBlocks[pos];
                           if (oldBlock != null) {
                               oldBlock.close();
                           }
                           cachedXmlBlockCookies[pos] = assetCookie;
                           cachedXmlBlockFiles[pos] = file;
                           cachedXmlBlocks[pos] = block;
                           return block.newParser();
                       }
                   }
               } catch (Exception e) {
                   final NotFoundException rnf = new NotFoundException("File " + file
                           + " from xml type " + type + " resource ID #0x" + Integer.toHexString(id));
                   rnf.initCause(e);
                   throw rnf;
               }
           }
   
           throw new NotFoundException("File " + file + " from xml type " + type + " resource ID #0x"
                   + Integer.toHexString(id));
       } 
}
```

我们先来看看这个方法的四个形参：

- String file：xml文件的路径
- int id：xml文件的资源ID
- int assetCookie：xml文件的资源缓存
- String type：资源类型

ResourcesImpl会缓存最近解析的4个xml资源，如果不在缓存里则调用AssetManger的openXmlBlockAsset()方法创建一个XmlBlock。XmlBlock是已编译的xml文件的一个包装类。

AssetManger的openXmlBlockAsset()方法的实现如下所示：

```java
public final class AssetManager implements AutoCloseable {
   /*package*/ final XmlBlock openXmlBlockAsset(int cookie, String fileName)
       throws IOException {
       synchronized (this) {
           //...
           long xmlBlock = openXmlAssetNative(cookie, fileName);
           if (xmlBlock != 0) {
               XmlBlock res = new XmlBlock(this, xmlBlock);
               incRefsLocked(res.hashCode());
               return res;
           }
       }
       //...
   } 
}
```

可以看出该方法会调用native方法openXmlAssetNative()去代开fileName指定的xml文件，成功打开该文件后，会得到C++层的ResXMLTree对象的地址xmlBlock，然后将xmlBlock封装进
XmlBlock中返回给调用者。ResXMLTreed对象会存放打开后xml资源的内容。

上述序列图里的AssetManger.cpp的方法的具体实现也就是一个打开资源文件的过程，资源文件一般存放在APK中，APK是一个zip包，所以最终会调用openAssetFromZipLocked()方法打开xml文件。

XmlBlock封装完成后，会调用XmlBlock对象的newParser()方法去构建一个XmlResourceParser对象，实现如下所示：

```java
final class XmlBlock {
    public XmlResourceParser newParser() {
        synchronized (this) {
            //mNative指向的是C++层的ResXMLTree对象的地址
            if (mNative != 0) {
                return new Parser(nativeCreateParseState(mNative), this);
            }
            return null;
        }
    }
    
    private static final native long nativeCreateParseState(long obj);
}
```

mNative指向的是C++层的ResXMLTree对象的地址，native方法nativeCreateParseState()根据这个地址找到ResXMLTree对象，利用ResXMLTree对象对象构建一个ResXMLParser对象，并将ResXMLParser对象
的地址封装进Java层的Parser对象中，构建一个Parser对象。所以他们的对应关系如下所示：

- XmlBlock <--> ResXMLTree
- Parser <--> ResXMLParser

就是建立了Java层与C++层的对应关系，实际的实现还是由C++层完成。

等获取了XmlResourceParser对象以后就可以调用inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) 方法进行View的解析了，在解析View时
，会先去调用rInflate()方法解析View树，然后再调用createViewFromTag()方法创建具体的View，我们来详细的看一看。

#### 3.3.2 解析View树

1. 解析merge标签，rInflate()方法会将merge下面的所有子View直接添加到根容器中，这里我们也理解了为什么merge标签可以达到简化布局的效果。
2. 不是merge标签那么直接调用createViewFromTag()方法解析成布局中的视图，这里的参数name就是要解析视图的类型，例如：ImageView。
3. 调用generateLayoutParams()f方法生成布局参数，如果attachToRoot为false，即不添加到根容器里，为View设置布局参数。
4. 调用rInflateChildren()方法解析当前View下面的所有子View。
5. 如果根容器不为空，且attachToRoot为true，则将解析出来的View添加到根容器中，如果根布局为空或者attachToRoot为false，那么解析出来的额View就是返回结果。返回解析出来的结果。

接下来，我们分别看下View树解析以及View的解析。

```java
public abstract class LayoutInflater {
    
    public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
           synchronized (mConstructorArgs) {
               Trace.traceBegin(Trace.TRACE_TAG_VIEW, "inflate");
   
               final Context inflaterContext = mContext;
               final AttributeSet attrs = Xml.asAttributeSet(parser);
               
               //Context对象
               Context lastContext = (Context) mConstructorArgs[0];
               mConstructorArgs[0] = inflaterContext;
               
               //存储根视图
               View result = root;
   
               try {
                   // 获取根元素
                   int type;
                   while ((type = parser.next()) != XmlPullParser.START_TAG &&
                           type != XmlPullParser.END_DOCUMENT) {
                       // Empty
                   }
   
                   if (type != XmlPullParser.START_TAG) {
                       throw new InflateException(parser.getPositionDescription()
                               + ": No start tag found!");
                   }
   
                   final String name = parser.getName();
                   
                   if (DEBUG) {
                       System.out.println("**************************");
                       System.out.println("Creating root view: "
                               + name);
                       System.out.println("**************************");
                   }
   
                   //1. 解析merge标签，rInflate()方法会将merge下面的所有子View直接添加到根容器中，这里
                   //我们也理解了为什么merge标签可以达到简化布局的效果。
                   if (TAG_MERGE.equals(name)) {
                       if (root == null || !attachToRoot) {
                           throw new InflateException("<merge /> can be used only with a valid "
                                   + "ViewGroup root and attachToRoot=true");
                       }
   
                       rInflate(parser, root, inflaterContext, attrs, false);
                   } else {
                       //2. 不是merge标签那么直接调用createViewFromTag()方法解析成布局中的视图，这里的参数name就是要解析视图的类型，例如：ImageView
                       final View temp = createViewFromTag(root, name, inflaterContext, attrs);
   
                       ViewGroup.LayoutParams params = null;
   
                       if (root != null) {
                           if (DEBUG) {
                               System.out.println("Creating params from root: " +
                                       root);
                           }
                           //3. 调用generateLayoutParams()f方法生成布局参数，如果attachToRoot为false，即不添加到根容器里，为View设置布局参数
                           params = root.generateLayoutParams(attrs);
                           if (!attachToRoot) {
                               // Set the layout params for temp if we are not
                               // attaching. (If we are, we use addView, below)
                               temp.setLayoutParams(params);
                           }
                       }
   
                       if (DEBUG) {
                           System.out.println("-----> start inflating children");
                       }
   
                       //4. 调用rInflateChildren()方法解析当前View下面的所有子View
                       rInflateChildren(parser, temp, attrs, true);
   
                       if (DEBUG) {
                           System.out.println("-----> done inflating children");
                       }
   
                       //如果根容器不为空，且attachToRoot为true，则将解析出来的View添加到根容器中
                       if (root != null && attachToRoot) {
                           root.addView(temp, params);
                       }
   
                       //如果根布局为空或者attachToRoot为false，那么解析出来的额View就是返回结果
                       if (root == null || !attachToRoot) {
                           result = temp;
                       }
                   }
   
               } catch (XmlPullParserException e) {
                   final InflateException ie = new InflateException(e.getMessage(), e);
                   ie.setStackTrace(EMPTY_STACK_TRACE);
                   throw ie;
               } catch (Exception e) {
                   final InflateException ie = new InflateException(parser.getPositionDescription()
                           + ": " + e.getMessage(), e);
                   ie.setStackTrace(EMPTY_STACK_TRACE);
                   throw ie;
               } finally {
                   // Don't retain static reference on context.
                   mConstructorArgs[0] = lastContext;
                   mConstructorArgs[1] = null;
   
                   Trace.traceEnd(Trace.TRACE_TAG_VIEW);
               }
   
               return result;
           }
     }
}
```

上面我们已经提到View树的解析是有rInflate()方法来完成的，我们接着来看看View树是如何被解析的。

```java
public abstract class LayoutInflater {
    
    void rInflate(XmlPullParser parser, View parent, Context context,
            AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {

        //1. 获取树的深度，执行深度优先遍历
        final int depth = parser.getDepth();
        int type;

        //2. 逐个进行元素解析
        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            final String name = parser.getName();
            
            if (TAG_REQUEST_FOCUS.equals(name)) {
                //3. 解析添加ad:focusable="true"的元素，并获取View焦点。
                parseRequestFocus(parser, parent);
            } else if (TAG_TAG.equals(name)) {
                //4. 解析View的tag。
                parseViewTag(parser, parent, attrs);
            } else if (TAG_INCLUDE.equals(name)) {
                //5. 解析include标签，注意include标签不能作为根元素。
                if (parser.getDepth() == 0) {
                    throw new InflateException("<include /> cannot be the root element");
                }
                parseInclude(parser, context, parent, attrs);
            } else if (TAG_MERGE.equals(name)) {
                //merge标签必须为根元素
                throw new InflateException("<merge /> must be the root element");
            } else {
                //6. 根据元素名进行解析，生成View。
                final View view = createViewFromTag(parent, name, context, attrs);
                final ViewGroup viewGroup = (ViewGroup) parent;
                final ViewGroup.LayoutParams params = viewGroup.generateLayoutParams(attrs);
                //7. 递归调用解析该View里的所有子View，也是深度优先遍历，rInflateChildren内部调用的也是rInflate()方
                //法，只是传入了新的parent View
                rInflateChildren(parser, view, attrs, true);
                //8. 将解析出来的View添加到它的父View中。
                viewGroup.addView(view, params);
            }
        }

        if (finishInflate) {
            //9. 回调根容器的onFinishInflate()方法，这个方法我们应该很熟悉。
            parent.onFinishInflate();
        }
    }
    
    //rInflateChildren内部调用的也是rInflate()方法，只是传入了新的parent View
    final void rInflateChildren(XmlPullParser parser, View parent, AttributeSet attrs,
            boolean finishInflate) throws XmlPullParserException, IOException {
        rInflate(parser, parent, parent.getContext(), attrs, finishInflate);
    }

}
```

上述方法描述了整个View树的解析流程，我们来概括一下：

1. 获取树的深度，执行深度优先遍历.
2. 逐个进行元素解析。
3. 解析添加ad:focusable="true"的元素，并获取View焦点。
4. 解析View的tag。
5. 解析include标签，注意include标签不能作为根元素，而merge必须作为根元素。
6. 根据元素名进行解析，生成View。
7. 递归调用解析该View里的所有子View，也是深度优先遍历，rInflateChildren内部调用的也是rInflate()方法，只是传入了新的parent View。
8. 将解析出来的View添加到它的父View中。
9. 回调根容器的onFinishInflate()方法，这个方法我们应该很熟悉。

你可以看到，负责解析单个View的正是createViewFromTag()方法，我们再来分析下这个方法。

#### 3.3.3 创建View

```java
public abstract class LayoutInflater {

        View createViewFromTag(View parent, String name, Context context, AttributeSet attrs,
                boolean ignoreThemeAttr) {
            
            //1. 解析view标签。注意是小写view，这个不太常用，下面会说。
            if (name.equals("view")) {
                name = attrs.getAttributeValue(null, "class");
            }
    
            //2. 如果标签与主题相关，则需要将context与themeResId包裹成ContextThemeWrapper。
            if (!ignoreThemeAttr) {
                final TypedArray ta = context.obtainStyledAttributes(attrs, ATTRS_THEME);
                final int themeResId = ta.getResourceId(0, 0);
                if (themeResId != 0) {
                    context = new ContextThemeWrapper(context, themeResId);
                }
                ta.recycle();
            }
    
            //3. BlinkLayout是一种会闪烁的布局，被包裹的内容会一直闪烁，像QQ消息那样。
            if (name.equals(TAG_1995)) {
                // Let's party like it's 1995!
                return new BlinkLayout(context, attrs);
            }
    
            try {
                View view;
                
                //4. 用户可以设置LayoutInflater的Factory来进行View的解析，但是默认情况下
                //这些Factory都是为空的。
                if (mFactory2 != null) {
                    view = mFactory2.onCreateView(parent, name, context, attrs);
                } else if (mFactory != null) {
                    view = mFactory.onCreateView(name, context, attrs);
                } else {
                    view = null;
                }
                if (view == null && mPrivateFactory != null) {
                    view = mPrivateFactory.onCreateView(parent, name, context, attrs);
                }
    
                //5. 默认情况下没有Factory，而是通过onCreateView()方法对内置View进行解析，createView()
                //方法进行自定义View的解析。
                if (view == null) {
                    final Object lastContext = mConstructorArgs[0];
                    mConstructorArgs[0] = context;
                    try {
                        //这里有个小技巧，因为我们在使用自定义View的时候是需要在xml指定全路径的，例如：
                        //com.guoxiaoxing.CustomView，那么这里就有个.了，可以利用这一点判定是内置View
                        //还是自定义View，Google的工程师很机智的。😎
                        if (-1 == name.indexOf('.')) {
                            //内置View解析
                            view = onCreateView(parent, name, attrs);
                        } else {
                            //自定义View解析
                            view = createView(name, null, attrs);
                        }
                    } finally {
                        mConstructorArgs[0] = lastContext;
                    }
                }
    
                return view;
            } catch (InflateException e) {
                throw e;
    
            } catch (ClassNotFoundException e) {
                final InflateException ie = new InflateException(attrs.getPositionDescription()
                        + ": Error inflating class " + name, e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
    
            } catch (Exception e) {
                final InflateException ie = new InflateException(attrs.getPositionDescription()
                        + ": Error inflating class " + name, e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            }
        }
}
```

单个View的解析流程也很简单，我们来梳理一下：

1. 解析View标签。
2. 如果标签与主题相关，则需要将context与themeResId包裹成ContextThemeWrapper。
3. BlinkLayout是一种会闪烁的布局，被包裹的内容会一直闪烁，像QQ消息那样。
4. 用户可以设置LayoutInflater的Factory来进行View的解析，但是默认情况下这些Factory都是为空的。
5. 默认情况下没有Factory，而是通过onCreateView()方法对内置View进行解析，createView()方法进行自定义View的解析。这里有个小技巧，因为
我们在使用自定义View的时候是需要在xml指定全路径的，例如：com.guoxiaoxing.CustomView，那么这里就有个.了，可以利用这一点判定是内置View
还是自定义View，Google的工程师很机智的。😎

**关于view标签**

```xml
<view
    class="RelativeLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```
在使用时，相当于所有控件标签的父类一样，可以设置class属性，这个属性会决定view这个节点会是什么控件。

**关于BlinkLayout**

这个也是个冷门的控件，本质上是一个FrameLayout，被它包裹的控件会像电脑版的QQ小企鹅那样一直闪烁。

```xml
<blink
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="这个控件会一直闪烁"/>

</blink>
```

**关于onCreateView()与createView()**

这两个方法在本质上都是一样的，只是onCreateView()会给内置的View前面加一个前缀，例如：android.widget，方便开发者在写内置View的时候，不用谢全路径名。
前面我们也提到了LayoutInflater是一个抽象类，我们实际使用的PhoneLayoutInflater，这个类的实现很简单，它重写了LayoutInflater的onCreatView()方法，该
方法就是做了一个给内置View加前缀的事情。

```java
public class PhoneLayoutInflater extends LayoutInflater {
    private static final String[] sClassPrefixList = {
        "android.widget.",
        "android.webkit.",
        "android.app."
    };

    @Override protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        
        //循环遍历三种前缀，尝试创建View
        for (String prefix : sClassPrefixList) {
            try {
                View view = createView(name, prefix, attrs);
                if (view != null) {
                    return view;
                }
            } catch (ClassNotFoundException e) {
                // In this case we want to let the base class take a crack
                // at it.
            }
        }
        return super.onCreateView(name, attrs);
    }
    public LayoutInflater cloneInContext(Context newContext) {
        return new PhoneLayoutInflater(this, newContext);
    }
}
```
这样一来，真正的View构建还是在createView()方法里完成的，createView()主要根据完整的类的路径名利用反射机制构建View对象，我们具体来
看看createView()方法的实现。

```java
public abstract class LayoutInflater {
    
    public final View createView(String name, String prefix, AttributeSet attrs)
                throws ClassNotFoundException, InflateException {
        
            //1. 从缓存中读取构造函数。
            Constructor<? extends View> constructor = sConstructorMap.get(name);
            if (constructor != null && !verifyClassLoader(constructor)) {
                constructor = null;
                sConstructorMap.remove(name);
            }
            Class<? extends View> clazz = null;
    
            try {
                Trace.traceBegin(Trace.TRACE_TAG_VIEW, name);
    
                if (constructor == null) {
                    // Class not found in the cache, see if it's real, and try to add it
                    
                    //2. 没有在缓存中查找到构造函数，则构造完整的路径名，并加装该类。
                    clazz = mContext.getClassLoader().loadClass(
                            prefix != null ? (prefix + name) : name).asSubclass(View.class);
                    
                    if (mFilter != null && clazz != null) {
                        boolean allowed = mFilter.onLoadClass(clazz);
                        if (!allowed) {
                            failNotAllowed(name, prefix, attrs);
                        }
                    }
                    //3. 从Class对象中获取构造函数，并在sConstructorMap做下缓存，方便下次使用。
                    constructor = clazz.getConstructor(mConstructorSignature);
                    constructor.setAccessible(true);
                    sConstructorMap.put(name, constructor);
                } else {
                    //4. 如果sConstructorMap中有当前View构造函数的缓存，则直接使用。
                    if (mFilter != null) {
                        // Have we seen this name before?
                        Boolean allowedState = mFilterMap.get(name);
                        if (allowedState == null) {
                            // New class -- remember whether it is allowed
                            clazz = mContext.getClassLoader().loadClass(
                                    prefix != null ? (prefix + name) : name).asSubclass(View.class);
                            
                            boolean allowed = clazz != null && mFilter.onLoadClass(clazz);
                            mFilterMap.put(name, allowed);
                            if (!allowed) {
                                failNotAllowed(name, prefix, attrs);
                            }
                        } else if (allowedState.equals(Boolean.FALSE)) {
                            failNotAllowed(name, prefix, attrs);
                        }
                    }
                }
    
                Object[] args = mConstructorArgs;
                args[1] = attrs;
    
                //5. 利用构造函数，构建View对象。
                final View view = constructor.newInstance(args);
                if (view instanceof ViewStub) {
                    // Use the same context when inflating ViewStub later.
                    final ViewStub viewStub = (ViewStub) view;
                    viewStub.setLayoutInflater(cloneInContext((Context) args[0]));
                }
                return view;
    
            } catch (NoSuchMethodException e) {
                final InflateException ie = new InflateException(attrs.getPositionDescription()
                        + ": Error inflating class " + (prefix != null ? (prefix + name) : name), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
    
            } catch (ClassCastException e) {
                // If loaded class is not a View subclass
                final InflateException ie = new InflateException(attrs.getPositionDescription()
                        + ": Class is not a View " + (prefix != null ? (prefix + name) : name), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } catch (ClassNotFoundException e) {
                // If loadClass fails, we should propagate the exception.
                throw e;
            } catch (Exception e) {
                final InflateException ie = new InflateException(
                        attrs.getPositionDescription() + ": Error inflating class "
                                + (clazz == null ? "<unknown>" : clazz.getName()), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } finally {
                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
            }
        }   
}
```

好了，讲到这里一个布局xml资源文件的查找和解析流程就分析完了。


