# Android网络编程：基础理论

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

在Android的网络开发过程中，我们通常会使用像Okhttp、Retrofit这种高度封装的网络库，它们完全屏蔽了相关技术细节。但是掌握其中的原理对我们来
说是很重要的，要知其然，也要知其所以然。

网络编程通常会涉及以下几个角色：

- HTTP/HTTPS
- TCP/IP
- 客户端/服务端

怎么去理解它们的关系呢？🤔

>例如我们是双十一从马老板家买了部手机，这个时候我们就是客户端，马老板就是服务端。手机要通过快递公司的汽车运送到我们手中。TCP/IP就相当于汽车，但是光有汽车是不够的，还要对汽车
进行分类，不然都是一样的汽车就乱套了，而完成分类的就是HTTP/HTTPS了，HTTP/HTTPS会告诉这些汽车，你是负责送货的（GET），你是负责退货的（POST）等等。


MIME：文本标记，表示一种主要的对象类型和一个特定的子类型。
URI：统一资源描述符，在世界范围内唯一标识并定位信息资源。
URL：统一资源定位符，一台特点服务器上某资源的位置。事实上URI是一个更通用的概念，URL是URI的子集

我们更常用的还是URL，我们来看看它的构成：

协议://服务器位置/资源路径

https://github.com/guoxiaoxing

- 协议：指的是以一种怎样的方式访问资源，例如：https
- 服务器位置：指的是服务器的位置，例如：github.com
- 资源路径：指的是具体资源的路径，例如：/guoxiaoxing

在URL里还存在着一些保留字符

## HTTP/HTTPS

>[HTTP](https://zh.wikipedia.org/wiki/%E8%B6%85%E6%96%87%E6%9C%AC%E4%BC%A0%E8%BE%93%E5%8D%8F%E8%AE%AE)（HyperText Transfer Protocol）是一种用于分布式、协作式和超媒体信息系统的应用层协议[1]。HTTP是万维网的数据通信的基础。

HTTP应用程序是通过相互发送报文工作的，报文是HTTP应用程序之间发送的数据块，报文通常分为请求报文和响应报文两种，请求报文向服务器请求一个动作，响应报文将请求结果返回给客户端。

请求报文

```
GET / HTTP/1.1
Host: www.google.com
```

响应报文

```
HTTP/1.1 200 OK
Content-Length: 3059
Server: GWS/2.0
Date: Sat, 11 Jan 2003 02:44:04 GMT
Content-Type: text/html
Cache-control: private
Set-Cookie: PREF=ID=73d4aef52e57bae9:TM=1042253044:LM=1042253044:S=SMCc_HRPCQiqy
X9j; expires=Sun, 17-Jan-2038 19:14:07 GMT; path=/; domain=.google.com
Connection: keep-alive
```

报文通常由以下部分组成：

- 方法（method）：客户端希望服务器对资源执行的动作。例如：GET
- 请求URL（request url）：客户端要访问的资源URL。例如：www.google.com
- 版本（version）：报文所使用的HTTP版本。例如：HTTP/1.1
- 状态码（status code）：描述请求过程中发生的状况。例如：200
- 原因短语（reason phrase）：状态码的解释。例如：OK
- 首部（header）向请求报文或者响应报文中添加一些附加信息。例如：Content-Type: text/html
- 实体（body）：包含一个由任意数据组成的数据块。例如：

### 方法

>方法（method）：客户端希望服务器对资源执行的动作。

- GET	请求指定url的数据,请求体为空(例如打开网页)。
- POST	请求指定url的数据，同时传递参数(在请求体中)。
- HEAD	类似于get请求，只不过返回的响应体为空，用于获取响应头。
- PUT	从客户端向服务器传送的数据取代指定的文档的内容。
- DELETE	请求服务器删除指定的页面。
- CONNECT	HTTP/1.1协议中预留给能够将连接改为管道方式的代理服务器。
- OPTIONS	允许客户端查看服务器的性能。
- TRACE	回显服务器收到的请求，主要用于测试或诊断。

我们主要讨论我们最常用的两个GET/POST。

GET方法通常用于请求服务器发送某个资源。

POST方法通常用于向服务器提交数据。

GET与POST在本质上都是TCP连接，只是GET直接把参数写在请求行中，而POST把参数放在请求体中。关于这两个方法，要注意以下两点：

- HTTP协议本身并没有规定GET请求行的长度显示，但是浏览器和服务端有这个限制，浏览器支持的URL场地一般都2kb，服务器一般为64kb（可以设置）。
- GET中如果包含中文，需要进行编码URLEncoder.encode(params, "gbk")。

### 状态码

- 1xx：信息提示
- 2xx：成功
- 3xx：重定向
- 4xx：客户端错误
- 5xx：服务端错误

更多关于状态码的细节可以参见[HTTP状态码](https://zh.wikipedia.org/wiki/HTTP%E7%8A%B6%E6%80%81%E7%A0%81)。

### 首部

首部通常和方法配合工作，共同决定了客户端和服务器能做什么事情。

- 通用首部：客户端和服务端都可以使用的首部，提供一些通用的功能。例如：Date: Sat, 11 Jan 2003 02:44:04 GMT 提供了构建报文的时间。
- 请求首部：请求报文特有，它们为服务器提供一些额外的信息。例如：Accept: */* 告知服务器会接收与其请求相符的任意媒体类型。
- 响应首部：响应报文特有，它们为客户端提供一些额外的信息。例如：Server: GWS/2.0 告知客户端与其通信的服务端是GWS/2.0。
- 实体首部：实体中特有，为实体提供一些说明。例如：Content-Type: text/html 告知实体中的内容类型是text/html。
- 扩展首部：非标准首部，可以由开发者创建。

常见的通用首部

- Date	发送该消息的日期和时间(按照 RFC 7231 中定义的"超文本传输协议日期"格式来发送)	Date: Tue, 15 Nov 1994 08:12:31 GMT
- Host	服务器的域名(用于虚拟主机 )，以及服务器所监听的传输控制协议端口号。如果所请求的端口是对应的服务的标准端口，则端口号可被省略。

常见的请求首部

- Accept	能够接受的回应内容类型（Content-Types）。Accept: text/plain	
- Accept-Charset	能够接受的字符集	Accept-Charset: utf-8
- Accept-Encoding	能够接受的编码方式列表。	Accept-Encoding: gzip, deflate	
- Accept-Language	能够接受的回应内容的自然语言列表。	Accept-Language: en-US	
- Accept-Datetime	能够接受的按照时间来表示的版本	Accept-Datetime: Thu, 31 May 2007 20:35:00 GMT
- Authorization	 用于超文本传输协议的认证的认证信息	Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
- Cookie	之前由服务器通过 Set- Cookie （下文详述）发送的一个 超文本传输协议Cookie	Cookie: $Version=1; Skin=new;
- User-Agent	浏览器的浏览器身份标识字符串	User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0
- Upgrade	要求服务器升级到另一个协议。	Upgrade: HTTP/2.0, SHTTP/1.3, IRC/6.9, RTA/x11

常见的响应首部

- Server	服务器的名字	Server: Apache/2.4.1 (Unix)
- Status    用来说明当前这个超文本传输协议回应的 状态。Status: 200 OK
- Upgrade	要求客户端升级到另一个协议。	Upgrade: HTTP/2.0, SHTTP/1.3, IRC/6.9, RTA/x11
- Age	这个对象在代理缓存中存在的时间，以秒为单位	Age: 12
- Cache-Control	向从服务器直到客户端在内的所有缓存机制告知，它们是否可以缓存这个对象。其单位为秒	Cache-Control: max-age=3600	常设
- Connection	针对该连接所预期的选项 Connection: close
- Location	用来进行重定向，或者在创建了某个新资源时使用。	Location: http://www.w3.org/pub/WWW/People.html

常见的实体首部

- Content-Length	以 八位字节数组 （8位的字节）表示的请求体的长度	Content-Length: 348
- Content-Type	请求体的 多媒体类型 （用于POST和PUT请求中）	Content-Type: application/x-www-form-urlencoded	

更多关于首部的细节可以参见[HTTP首部](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5)。

## TCP/IP

>[TCP](https://zh.wikipedia.org/wiki/%E4%BC%A0%E8%BE%93%E6%8E%A7%E5%88%B6%E5%8D%8F%E8%AE%AE)（传输控制协议）是一种面向连接的、可靠的、基于字节流的传输层通信协议，


TCP用[三次握手](https://zh.wikipedia.org/wiki/%E4%BC%A0%E8%BE%93%E6%8E%A7%E5%88%B6%E5%8D%8F%E8%AE%AE#建立通路)（three-way handshake）过程创建一个连接。三次握手的流程如下所示：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/practice/network/three_way_handshake.png"/>

1. 客户端通过向服务器端发送一个SYN来创建一个主动打开，作为三路握手的一部分。客户端把这段连接的序号设定为随机数A。
2. 服务器端应当为一个合法的SYN回送一个SYN/ACK。ACK的确认码应为A+1，SYN/ACK包本身又有一个随机产生的序号B。
3. 最后，客户端再发送一个ACK。当服务端受到这个ACK的时候，就完成了三路握手，并进入了连接创建状态。此时包的序号被设定为收到的确认号A+1，而响应号则为B+1。

三次握手也是个老生常谈的概念，举个简单的例子说明一下。

>例如你小时候出去玩，经常玩忘了回家吃饭。你妈妈也经常过来喊你。如果你没有走远，在门口的小土堆上玩泥巴，你妈妈会喊："小新，回家吃饭了"。你听到后会回应："知道了，一会就回去"。妈妈听
到你的回应后又说："快点回来，饭要凉了"。这样你妈妈和你就完成了三次握手的过程。😁