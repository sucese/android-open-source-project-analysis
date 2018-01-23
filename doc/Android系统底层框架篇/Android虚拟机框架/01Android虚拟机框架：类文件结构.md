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

```
0000000 ca fe ba be 00 00 00 34 00 0f 0a 00 03 00 0c 07
0000010 00 0d 07 00 0e 01 00 06 3c 69 6e 69 74 3e 01 00
0000020 03 28 29 56 01 00 04 43 6f 64 65 01 00 0f 4c 69
0000030 6e 65 4e 75 6d 62 65 72 54 61 62 6c 65 01 00 03
0000040 73 75 6d 01 00 05 28 49 49 29 49 01 00 0a 53 6f
0000050 75 72 63 65 46 69 6c 65 01 00 0e 54 65 73 74 43
0000060 6c 61 73 73 2e 6a 61 76 61 0c 00 04 00 05 01 00
0000070 43 63 6f 6d 2f 67 75 6f 78 69 61 6f 78 69 6e 67
0000080 2f 61 6e 64 72 6f 69 64 2f 66 72 61 6d 65 77 6f
0000090 72 6b 2f 64 65 6d 6f 2f 6e 61 74 69 76 65 5f 66
00000a0 72 61 6d 77 6f 72 6b 2f 76 6d 2f 54 65 73 74 43
00000b0 6c 61 73 73 01 00 10 6a 61 76 61 2f 6c 61 6e 67
00000c0 2f 4f 62 6a 65 63 74 00 21 00 02 00 03 00 00 00
00000d0 00 00 02 00 01 00 04 00 05 00 01 00 06 00 00 00
00000e0 1d 00 01 00 01 00 00 00 05 2a b7 00 01 b1 00 00
00000f0 00 01 00 07 00 00 00 06 00 01 00 00 00 0a 00 01
0000100 00 08 00 09 00 01 00 06 00 00 00 1c 00 02 00 03
0000110 00 00 00 04 1b 1c 60 ac 00 00 00 01 00 07 00 00
0000120 00 06 00 01 00 00 00 0d 00 01 00 0a 00 00 00 02
0000130 00 0b                                          
0000132
```

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

### 1.2 版本号

具体含义

>版本号：5-6字节是次版本号，7-8字节是主版本号

对应数值

>5-6字节是次版本号0x0000（即0），7-8字节是主版本号0x0034（即52）.

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

>常量池计数：常量池中常量的数量不是固定的，因此常量池入口处会防止一项u2类型的数据，代表常量池容器计数。注意容器计数从1开始，索引为0代表不引用任何一个
常量池的项目。

对应数值

如果所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/class_hex_structure_3.png"/>

9-10字节是常量池容器计数0x000f（即15）。说明常量池里有14个常量，从1-14.

我们用javap命令分析一下字节码文件

```
javap -verbose StandardClass.class
```

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/class_constant_pool.png"/>

如图所示，正如我们分析的那样，常量池里有14个常量。比方说我们看下第13个常量表示的类的全限定名，它对应的十六进制如下所示：

<img src="https://github.com/guoxiaoxing/java/raw/master/art/jvm/class_hex_structure_4.png"/>

常量池主要存放字面量与符号引用。字面量包括：

- 类与接口的全限定名
- 字段的名称与描述符
- 方法的名称与描述符

常量池中每一项常量都是一个表，目前共有14种表结构，如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/native/vm/constant_pool_data_type.png"/>

### 访问标志

>访问标志：常量池之后就是访问标志，该标志用于识别一些类或则接口层次的访问信息。

这些访问信息包括：

