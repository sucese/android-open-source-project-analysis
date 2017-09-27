# 03Android组件框架：Android布局渲染者LayoutInflater

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 LayoutInflater的创建

LayoutInflater在我们的日常开发中扮演者重要的角色，由于它被很好的封装在了Activity与Fragment中，所以很多时候我们不知道它的重要性。可谓是深藏功与名。

一 LayoutInflater的创建

回想一下我们平时如何调用LayoutInflater🤔

```java
LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
```
LayoutInflater创建序列图



通过上图我们可以知道在SystemServiceRegistry的静态代码块里注册各种ServiceFetcher，其中就包含了LAYOUT_INFLATER_SERVICE，并把他们保存中HashMap中。

```java
final class SystemServiceRegistry {
    
   private static final HashMap<Class<?>, String> SYSTEM_SERVICE_NAMES =
            new HashMap<Class<?>, String>();
   //保存各种注册的ServiceFetcher
   private static final HashMap<String, ServiceFetcher<?>> SYSTEM_SERVICE_FETCHERS =
            new HashMap<String, ServiceFetcher<?>>();
   
    static {
          //代码省略
          registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class,
                        new CachedServiceFetcher<LayoutInflater>() {
                    @Override
                    public LayoutInflater createService(ContextImpl ctx) {
                        return new PhoneLayoutInflater(ctx.getOuterContext());
                    }});
           //代码省略
    }
}
```