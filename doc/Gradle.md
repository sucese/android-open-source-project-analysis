我们的Android Project基于Gradle编译，我们来了解一下Gradle中对我们工作有用的特性。

>Gradle是一种基于Groovy的动态DSL，而Groovy则是一种基于JVM的动态语言。


Project

```
Project：每次构建都至少有一个Project来完成，每个build.gradle代表这一个Project，所以Gradle里的Project和Android Studio里的Project并不是一个概念。
```

Task

```、
Task：一个Task表示一个动作，在build.gradle里定义。
```

生命周期

一旦一个tasks被执行，那么它不会再次执行了，不包含依赖的Tasks总是优先执行，一次构建将会经历下列三个阶段：

```
1 初始化阶段：project实例在这儿创建，如果有多个模块，即有多个build.gradle文件，多个project将会被创建。
2 配置阶段：在该阶段，build.gradle脚本将会执行，为每个project创建和配置所有的tasks。
3 执行阶段：这一阶段，gradle会决定哪一个tasks会被执行，哪一个tasks会被执行完全依赖开始构建时传入的参数和当前所在的文件夹位置有关。
```

Android Studio项目结构

```
 MyApp
   ├── build.gradle
   ├── settings.gradle
   └── app
       ├── build.gradle
       ├── build
       ├── libs
       └── src
           └── main
               ├── java
               │   └── com.package.myapp
               └── res
                   ├── drawable
                   ├── layout
                   └── etc.
```