# 03Android组件框架：Android布局渲染者LayoutInflater

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

>Instantiates a layout XML file into its corresponding {@link android.view.View}objects. 

LayoutInflater可以把xml布局文件里内容加载成一个View，LayoutInflater可以说是Android里的无名英雄，你经常用的到它，却体会不到它的好。因为隔壁的iOS兄弟是没有
这种东西的，他们只能用代码来写布局，需要应用跑起来才能看到效果。相比之下Android的开发者就幸福的多，但是大家有没有相关xml是如何转换成一个View的，今天我们就来分析
这个问题。

LayoutInflater也是通过Context获取，它也是系统服务的一种，被注册在ContextImpl的map里，然后通过LAYOUT_INFLATER_SERVICE来获取。

```java
LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
```
具体说来就是这样：

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
LayoutInflater是一个抽象类，它ed实现类是PhoneLayoutInflater。


```java

public abstract class LayoutInflater {

    public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        final Resources res = getContext().getResources();
        if (DEBUG) {
            Log.d(TAG, "INFLATING from resource: \"" + res.getResourceName(resource) + "\" ("
                    + Integer.toHexString(resource) + ")");
        }
        
        //获取xml资源解析器
        final XmlResourceParser parser = res.getLayout(resource);
        try {
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }

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
```

1. 解析merge标签，rInflate()方法会将merge下面的所有子View直接添加到根容器中，这里我们也理解了为什么merge标签可以达到简化布局的效果。
2. 不是merge标签那么直接调用createViewFromTag()方法解析成布局中的视图，这里的参数name就是要解析视图的类型，例如：ImageView。
3. 调用generateLayoutParams()f方法生成布局参数，如果attachToRoot为false，即不添加到根容器里，为View设置布局参数。
4. 调用rInflateChildren()方法解析当前View下面的所有子View。
5. 如果根容器不为空，且attachToRoot为true，则将解析出来的View添加到根容器中，如果根布局为空或者attachToRoot为false，那么解析出来的额View就是返回结果。返回解析出来的结果。