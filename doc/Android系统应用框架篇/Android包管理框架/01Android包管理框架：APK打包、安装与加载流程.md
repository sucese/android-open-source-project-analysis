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

既然说到这里，我们再简单看一下Android是如何这些资源的，我们知道Android设备数以亿计、遍布全球，那么对于Android设备最大的一个问题就是适配。为了做好设备，Android将资源的组织方式划分为19个纬度，这一块的内容[Android资源官方文档](https://developer.android.com/guide/topics/resources/providing-resources.html)讲
的很清楚，这里也不再赘述。

我们可以利用资源的组织方式达到最佳的设备兼容性，那么问题来了，Android是如何查找到对应的资源的呢？🤔

Android会先获取自己的设备信息，然后根据设备信息去查找对应的资源，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/resource/android_resource_search_flow.png"/>

1. 淘汰与设备配置冲突的资源文件。
2. 选择优先级最高的限定符。（先从 MCC 开始，然后下移。）
3. 是否有资源目录包括此限定符？若无，返回到第 2 步，看看下一个限定符。若有，请继续执行第 4 步。
4. 淘汰不含此限定符的资源目录。在该示例中，系统会淘汰所有不含语言限定符的目录。
5. 返回并重复第 2 步、第 3 步和第 4 步，直到只剩下一个目录为止。

前面我们提到xml编写的Android资源文件都会编译成二进制格式的xml文件，资源的打包都是由AAPT工具来完成的，资源打包主要有以下操作：

1. 解析AndroidManifest.xml，获得应用程序的包名称，创建资源表。
2. 添加被引用资源包，被添加的资源会以一种资源ID的方式定义在R.java中。
3. 资源打包工具创建一个AaptAssets对象，收集当前需要编译的资源文件，收集到的资源保存在AaptAssets对象对象中。
4. 将上一步AaptAssets对象保存的资源，添加到资源表ResourceTable中去，用于最终生成资源描述文件resources.arsc。
5. 编译values类资源，这类资源包括数组、颜色、尺寸、字符串等值。
6. 给bag、style、array这类资源分配资源ID。
7. 编译xml资源文件，编译的流程分为：① 解析xml文件 ② 赋予属性名称资源ID ③ 解析属性值 ④ 将xml文件从文本格式转换为二进制格式，四步。
8. 生成资源索引表resources.arsc。

资源ID是一个4字节的无符整数，如下所示：

- 最高字节是Package ID表示命名空间，标明资源的来源，
- 次字节是Type ID，表示资源的类型，例如：anim、color、string等。
- 最低两个字节是Entry ID，表示资源在其所属资源类型中所出现的次序。

**关于Package ID**

>Android系统自己定义了两个Package ID，系统资源命名空间：0x01 和 应用资源命名空间：0x7f。

所有处于这两个值之间的空间都是合法的，我们可以从R.java文件中看到应用资源命令空间0x7f，如下所示：

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

首先解释一下什么是StringBlocks，StringBlocks描述的是一个字符串资源池，Android里每一个资源索引表resources.arsc都包含一个字符串资源池。

我们看看这两个native方法的底层实现，如下所示：


```java
static jint android_content_AssetManager_getStringBlockCount(JNIEnv* env, jobject clazz)
{
    AssetManager* am = assetManagerForJavaObject(env, clazz);
    if (am == NULL) {
        return 0;
    }
    return am->getResources().getTableCount();
}

static jlong android_content_AssetManager_getNativeStringBlock(JNIEnv* env, jobject clazz,
                                                           jint block)
{
    AssetManager* am = assetManagerForJavaObject(env, clazz);
    if (am == NULL) {
        return 0;
    }
    return reinterpret_cast<jlong>(am->getResources().getTableStringBlock(block));
}

const ResTable& AssetManager::getResources(bool required) const
{
    const ResTable* rt = getResTable(required);
    return *rt;
}
```
AssetManager的getResources()方法获取的是ResourceTable，正如它的名字那样，它是一个资源表。

👉 [ ResourceTable.cpp](https://android.googlesource.com/platform/frameworks/base.git/+/android-4.2.2_r1/tools/aapt/ResourceTable.cpp)

getNativeStringBlock()方法实际上就是将每一个资源包里面的resources.arsc的数据项值的字符串资源池数据块读取出来，并封装在C++层的StringPool对象中，然后Java层的makeStringBlocks()方法
再将该StringPool对象封装成Java层的StringBlock中。

如此，AssetManager和Resources对象的创建流程便分析完了，这两个对象构成了Android应用程序资源管理器的核心基础，APK资源的加载就是借由这两个对象来完成的。

### 3.3 资源的查找流程

前面我们分析了AssetManager和Resources对象的创建流程，AssetManager根据文件名来查找资源，Resouces根据资源ID查找资源，如果资源ID对应的是个文件，那么会Resouces先根据资源ID查找
出文件名，AssetManger再根据文件名查找出具体的资源。

整个流程还是比较简单的，具体序列图如下所示：

