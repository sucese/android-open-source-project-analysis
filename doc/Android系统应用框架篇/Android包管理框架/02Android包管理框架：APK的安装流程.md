# Android包管理框架：APK的安装流程

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

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

## 一 APK解析流程

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

## 二 DEX的dexopt流程

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




