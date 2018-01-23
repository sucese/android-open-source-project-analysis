# Android虚拟机框架：类文件结构

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

## 一 类文件基本结构

>Class文件是一组以8位字节为基础的单位的二进制流，各个数据项按严格的顺序紧密的排列在Class文件中，中间没有任何间隔。

这么说有点抽象，我们先来举一个简单的小例子。🤞

```java
public class TestClass {

    public int sum(int a, int b) {
        return a + b;
    }
}
```
编译生成Class文件，然后使用hexdump命令查看Class文件里的内容。

```
javac TestClass.java
hexdump TestClass.class
```
Class文件内容如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_1.png"/>

注：笔者用的二进制查看软件是iHex，可以去AppStore下载，Windows用户可以使用WinHex。

这是一份十六进制表示的二进制流，每个位排列紧密，都有其对应的含义，具体说来，如下所示：

注：下列表中四个段分别为 类型、名称、说明、数量

- u4	magic	识别Class文件格式，具体值为0xCAFEBABE	1
- u2	minor_version	Class文件格式副版本号	1
- u2	major_version	Class文件格式主版本号	1
- u2	constant_pool_count	常数表项个数	1
- cp_info	constant_pool	常数表，又称变长符号表	constant_pool_count-1
- u2	access_flags	Class的声明中使用的修改符掩码	1
- u2	this_class	常数表索引，索引内保存类名或接口名	1
- u2	super_class	常数表索引，索引内保存父类名	1
- u2	interfaces_count	超接口个数	1
- u2	interfaces	常数表索引，各超接口名称	interfaces_count
- u2	fields_count	类的域个数	1
- field_info	fields	域数据，包括属性名称索引	fields_count
- u2	methods_count	方法个数	1
- method_info	methods	方法数据，包括方法名称索引	methods_count
- u2	attributes_count	类附加属性个数	1
- attribute_info	attributes	类附加属性数据，包括源文件名称等	attributs_count

我们可以看着在上面这张表中有类似u2、attribute_info这样的类型，事实上Class文件采用一种类似于C语言结构体的伪结构struct来存储数据，这种结构有两种数据类型：

- 无符号数：基本数据类型，例如u1代表1个字节，u2代表2个字节，u4代表2个字节，u8代表8个字节。
- 表：由多个无符号数或者其他表作为数据项而构成的复合数据结构，用于描述有层次关系的复合数据结构，一般以"_info"结尾。

我们分别来看看上述的各个字段的具体含义已经对应数值。

注：这一块的内容可能有点枯燥，但是它是我们后续学习类加载机制，Android打包机制，以及学习插件化、热更新框架的基础，所以需要掌握。
但是也没必要都记住每个段的含义，你只需要有个整体性的认识即可，后续如果忘了具体的内容，可以再回来查阅。😁

### 1.1 魔数

具体含义

>魔数：1-4字节，用来确定这个文件是否为一个能被虚拟机接受的Class文件，它的值为0xCAFEBABE。

对应数值

>ca fe ba be

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_2.png"/>

### 1.2 版本号

具体含义

>版本号：5-6字节是次版本号，7-8字节是主版本号

对应数值

>5-6字节是次版本号0x0000（即0），7-8字节是主版本号0x0034（即52）.

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_2.png"/>

JDK版本号与数值的对应关系如下所示：

- JDK 1.8 = 52
- JDK 1.7 = 51
- JDK 1.6 = 50
- JDK 1.5 = 49
- JDK 1.4 = 48
- JDK 1.3 = 47
- JDK 1.2 = 46
- JDK 1.1 = 45

### 1.3 常量池计数/常量池

具体含义

>常量池计数：常量池中常量的数量不是固定的，因此常量池入口处会放置一项u2类型的数据，代表常量池容器计数。注意容器计数从1开始，索引为0代表不引用任何一个
常量池的项目。

对应数值

>9-10字节是常量池容器计数0x000f（即15）。说明常量池里有14个常量，从1-14.

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_3.png"/>

我们用javap命令分析一下字节码文件

```
javap -verbose TestClass.class
```

运行结果如下所示：

```j
Classfile /Users/guoxiaoxing/Github-app/android-open-source-project-analysis/demo/src/main/java/com/guoxiaoxing/android/framework/demo/native_framwork/vm/TestClass.class
  Last modified 2018-1-23; size 306 bytes
  MD5 checksum 86b5d89fa1de346213d812d5b6d3b5d7
  Compiled from "TestClass.java"
public class com.guoxiaoxing.android.framework.demo.native_framwork.vm.TestClass
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #3.#12         // java/lang/Object."<init>":()V
   #2 = Class              #13            // com/guoxiaoxing/android/framework/demo/native_framwork/vm/TestClass
   #3 = Class              #14            // java/lang/Object
   #4 = Utf8               <init>
   #5 = Utf8               ()V
   #6 = Utf8               Code
   #7 = Utf8               LineNumberTable
   #8 = Utf8               sum
   #9 = Utf8               (II)I
  #10 = Utf8               SourceFile
  #11 = Utf8               TestClass.java
  #12 = NameAndType        #4:#5          // "<init>":()V
  #13 = Utf8               com/guoxiaoxing/android/framework/demo/native_framwork/vm/TestClass
  #14 = Utf8               java/lang/Object
{
  public com.guoxiaoxing.android.framework.demo.native_framwork.vm.TestClass();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 10: 0

  public int sum(int, int);
    descriptor: (II)I
    flags: ACC_PUBLIC
    Code:
      stack=2, locals=3, args_size=3
         0: iload_1
         1: iload_2
         2: iadd
         3: ireturn
      LineNumberTable:
        line 13: 0
}
SourceFile: "TestClass.java"

```

如图所示，正如我们分析的那样，常量池里有14个常量。我们来看看这个TestClass全路径名的常量。

```
#13 = Utf8               com/guoxiaoxing/android/framework/demo/native_framwork/vm/TestClass
```

它的常量值如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_4.png"/>

常量池主要存放字面量与符号引用。

字面量包括：

- 文本字符串
- 声明为final的常量值等

符号引用包括：

- 类与接口的全限定名
- 字段的名称与描述符
- 方法的名称与描述符

常量池里的每个常量都用一个表来表示，表的结构如下所示：

```java
cp_info {
    //代表常量类型
    u1 tag;
    //代表存储的常量，不同的常量类型有不同的结构
    u1 info[];
}
```
目标一共有十四中常量类型，如下所示：

注：下表字段分别为 类型、标志（tag）、描述

- CONSTANT_Utf8_info	    1	UTF8编码的Unicode字符串
- CONSTANT_Integer_info	    3	整型字面量
- CONSTANT_Float_info	    4	浮点型字面量
- CONSTANT_Long_info	    5	长整型字面量
- CONSTANT_Double_info	    6	双精度浮点型字面量
- CONSTANT_Class_info	    7	类或接口的符号引用
- CONSTANT_String_info	    8	字符串类型字面量
- CONSTANT_Fieldref_info	9	字段的符号引用
- CONSTANT_Methodref_info	10	类中方法的符号引用
- CONSTANT_InterfaceMethodref_info	11	接口中方法的符号引用
- CONSTANT_NameAndType_info	12	字段或方法的部分符号引用

### 1.4 访问标志

具体含义

>访问标志：常量池之后就是访问标志，该标志用于识别一些类或则接口层次的访问信息。这些访问信息包括这个Class是类还是接口，是否定义Abstract类型等。

对应数值

>常量池之后就是访问标志，前两个字节代表访问标志。

从上面的分析中常量池最后一个常量是#14 = Utf8 java/lang/Object，所以它后面的两个字节就代表访问标志，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_5.png"/>

访问表示值与含义如下所示：

- ACC_PUBLIC	0x0001	是否为public
- ACC_FINAL	0x0010	是否为final
- ACC_SUPER	0x0020	JDK 1.0.2以后编译出来的类该标志位都为真
- ACC_INTERFACE	0x0200	是否为接口
- ACC_ABSTRACT	0x0400	是否为抽象的（接口和抽象类）
- ACC_SYNTHETIC	0x1000	表示这个代码并非由用户产生的
- ACC_ANNOTATION	0x2000	是否为注解
- ACC_ENUM	0x4000	是否为枚举

我们上面写了一个普通的Java类，ACC_PUBLIC位为真，又由于JDK 1.0.2以后编译出来的类ACC_SUPER标志位都为真，所以最终的值为：

```
0x0001 & 0x0020 = 0x0021
```

这个值就是上图中的值。

### 1.5 类索引、父类索引与接口索引

具体含义

>类索引（用来确定该类的全限定名）、父类索引（用来确定该类的父类的全限定名）是一个u2类型的数据（单个类、单继承），接口索引是一个u2类型的集合（多接口实现，用来描述该类实现了哪些接口）

对应数值

>类索引、父类索引与接口索引紧紧排列在访问标志之后。

类索引为0x0002，它的全限定名为com/guoxiaoxing/android/framework/demo/native_framwork/vm/TestClass。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_6.png"/>

父类索引为0x0003，它的全限定名为java/lang/Object。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_7.png"/>

接口索引的第一项是一个u2类型的数据表示接口计数器，表示实现接口的个数。这里没有实现任何接口，所以为0x0000。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_8.png"/>

### 1.6 字段表集合

具体含义

>字段表用来描述接口或者类里声明的变量、字段。包括类级变量以及实例级变量，但不包括方法内部声明的变量。

字段表结构如下所示：

```java
field_info {
    u2             access_flags;//访问标志位，例如private、public等
    u2             name_index;//字段的简单名称
    u2             descriptor_index;//方法的描述符，描述字段的数据类型，方法的参数列表和返回值
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```
access_flags取值如下所示：

- ACC_PUBLIC	0x0001	是否为 public; 
- ACC_PRIVATE	0x0002	是否为 private; 
- ACC_PROTECTED	0x0004	是否为 protected; 
- ACC_STATIC	0x0008	是否为 static;
- ACC_FINAL	0x0010	是否为 final; 
- ACC_VOLATILE	0x0040	是否为 volatile; 
- ACC_TRANSIENT	0x0080	是否为 transient; 
- ACC_SYNTHETIC	0x1000	是否为 synthetic;
- ACC_ENUM	0x4000	是否为enum.

descriptor_index里描述符的含义如下所示：

- B	byte
- C	char
- D	double
- F	float
- I	int
- J	long
- S	short
- Z	boolean
- V	void
- L	Object, 例如 Ljava/lang/Object


对应数值

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/TestClass_9.png"/>

根据这些描述符的含义，我们上面的写的方法。

```java
public int sum(int a, int b){
    return a + b;
}
```

access_flags为：

```
flags: ACC_PUBLIC
```

descriptor_index为：

```java
descriptor: (II)I

```

