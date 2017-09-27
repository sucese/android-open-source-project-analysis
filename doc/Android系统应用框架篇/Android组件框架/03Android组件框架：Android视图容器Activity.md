# Android组件框架：Android视图容器Activity

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

本篇文章我们来分析Android的视图容器Activity，这也是我们在日常开发中接触的最多的一个组件。

**文章目录**

- 一 

😁一个简单的例子

```java
public class SimpleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
    }
}
```

基本上我们第一天接触Android的界面开发时就会看到这么一段代码，那么大家有没有思考过这段代码背后的调用逻辑是什么🤔，UI是如何呈现在Activity上的🤔
接下来我们就来一一分析这些问题。

