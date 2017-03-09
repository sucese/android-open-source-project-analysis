# Android系统基础篇：源码下载与编译

作者: 郭孝星<br/>
邮箱: guoxiaoxingse@163.com<br/>
博客: https://guoxiaoxing.github.io/<br/>
简书: http://www.jianshu.com/users/66a47e04215b/latest_articles<br/>

**关于作者**

>郭孝星，非著名程序员，代码洁癖患者，爱编程，好吉他，喜烹饪，爱一切有趣的事物和人。

**关于文章**

>作者的文章会同时发布在Github、CSDN与简书上, 文章顶部也会附上文章的Github链接。如果文章中有什么疑问也欢迎发邮件与我交流, 对于交流的问
题, 请描述清楚问题并附上代码与日志, 一般都会给予回复。如果文章中有什么错误, 也欢迎斧正。如果你觉得本文章对你有所帮助, 也欢迎去star文
章, 关注文章的最新的动态。另外建议大家去Github上浏览文章，一方面文章的写作都是在Github上进行的，所以Github上的更新是最及时的，另一方
面感觉Github对Markdown的支持更好，文章的渲染也更加美观。

文章目录：https://github.com/guoxiaoxing/android-open-source-project-analysis

官方地址：https://source.android.com/index.html

清华大学开源软件镜像站：https://mirror.tuna.tsinghua.edu.cn/help/AOSP/

**源码版本**

[android-7.1.1_r1](https://source.android.com/source/build-numbers.html#source-code-tags-and-builds)

**电脑环境**

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/base/1/mac_os.png" width="700" height=""/>


## 创建区分大小写磁盘

打开磁盘工具

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/base/1/disk_tool_1.png" width="700" height=""/>

创建空白映像

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/base/1/disk_tool_2.png" width="700" height=""/>

设置区分大小写

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/base/1/disk_tool_3.png" width="700" height=""/>

## 下载repo工具

```
mkdir ~/bin
PATH=~/bin:$PATH
curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
chmod a+x ~/bin/repo
```

如果你没有翻墙，可以使用清华大学的repo镜像：https://mirrors.tuna.tsinghua.edu.cn/help/git-repo/

下载完成后将bin/repo打开，将里面的REPO_URL改成清华大学的镜像：

```
https://mirrors.tuna.tsinghua.edu.cn/git/git-repo/'
```

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/base/1/repo_download_1.png" width="700" height=""/>


## 下载源码

1 建立工作目录

```
mkdir WORKING_DIRECTORY
cd WORKING_DIRECTORY
```

2 初始化仓库

```
repo init -u https://aosp.tuna.tsinghua.edu.cn/platform/manifest
```


如果需要某个特定的Android版本，可以在后面指定版本号。

Android系统各版本号：https://source.android.com/source/build-numbers.html#source-code-tags-and-builds

```
repo init -u https://aosp.tuna.tsinghua.edu.cn/platform/manifest -b android-4.0.1_r1
```

同步源码树，开始下载源码，如果后续下载中断，也可以重复执行这个命令。

```
repo sync
```

源码的下载会经常中断，我们可以写一个脚本自动repo sync，保存成repo.sh，放到WORKING_DIRECTORY目录下，.repo.sh即可执行

```
#!/bin/bash   
#FileName  jkYishon.sh  
PATH=~/bin:$PATH   
repo init -u git://aosp.tuna.tsinghua.edu.cn/android/platform/manifest -b android-7.1.1_r1 
repo sync   
while [ $? = 1 ]; do   
echo "================sync failed, re-sync again ====="   
sleep 3   
repo sync   
done 
```

开始下载

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/base/1/repo_download_2.png" width="700" height=""/>

下载完成

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/base/1/repo_download_3.png" width="700" height=""/>
