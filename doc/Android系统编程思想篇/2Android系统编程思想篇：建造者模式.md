# Android系统编程思想篇：建造者模式

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，爱好广泛，技术栈主要涉及以下几个方面
>
>- Android/Linux
>- Java/Kotlin/JVM
>- Python
>- JavaScript/React/ReactNative
>- DataStructure/Algorithm
>
>文章首发于[Github](https://github.com/guoxiaoxing)，后续也会同步在[简书](http://www.jianshu.com/users/66a47e04215b/latest_articles)与
[CSDN](http://blog.csdn.net/allenwells)等博客平台上。文章中如果有什么问题，欢迎发邮件与我交流，邮件可发至guoxiaoxingse@163.com。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。


建造者模式属于创建型模式的一种，它允许用户在不知道内部构建细节的情况下，可以更精细的控制对象的构造流程。该模式为了将构建复杂对象的
过程和它的部件解耦，使构建的过程和部件的表示隔离开来。

## 模式定义

>将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。

模式角色

```
Product：产品的抽象类
Builder：抽象Builder类，规范构建过程，一般由子类实现具体构建过程
ConcreteBuilder：具体的Builder类
Director：统一组装过程
```

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/2/design_pattern_builder.png" width="700" height=""/>


使用场景

```
1 相同的方法，不同的执行顺序，产生不同的事件结果。
2 多个部件或者零件，都可以装配到同一个对象中，但产生的运行结果又不相同。
3 对象构建过程特别复杂，参数多，默认值也需要配置。
```

优点

```
1 客户端不必知道产品内部组成的细节，将产品本身与产品的创建过程解耦，使得相同的创建过程可以创建不同的产品对象。
2 每一个具体建造者都相对独立，而与其他的具体建造者无关，因此可以很方便地替换具体建造者或增加新的具体建造者，用户使用不同的具体建造者即可得到不同的产品对象。
3 可以更加精细地控制产品的创建过程，将复杂产品的创建步骤分解在不同的方法中，使得创建过程更加清晰，也更方便使用程序来控制创建过程。
4 增加新的具体建造者无须修改原有类库的代码，指挥者类针对抽象建造者类编程，方便系统扩展，符合“开闭原则”。
```

缺点

```
1 建造者模式所创建的产品一般具有较多的共同点，其组成部分相似，如果产品之间的差异性很大，则不适合使用建造者模式，因此其使用范围受到一定的限制。
2 如果产品的内部变化复杂，可能会导致需要定义很多具体建造者类来实现这种变化，导致系统变得很庞大。
```

另外，以下情况可以做模式简化处理

省略抽象建造者角色：如果系统中只需要一个具体建造者的话，可以省略掉抽象建造者。
省略指挥者角色：在具体建造者只有一个的情况下，如果抽象建造者角色已经被省略掉，那么还可以省略指挥者角色，让Builder角色扮演指挥者与建造者双重角色。

## 模式实现

我们先来看看完整的建造者模式的实现

抽象产品角色

```java
package com.guoxiaoxing.android.sdk.design.builder.whole;

/**
 * 抽象产品角色，定位一类产品的特性与行为。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:13
 */
public abstract class AbstractProduct {

    private String board;
    private String display;
    private String os;


    public void setBoard(String board) {
        this.board = board;
    }


    public void setDisplay(String display) {
        this.display = display;
    }


    public abstract void setOs(String os);
}
```

具体产品角色

```java
package com.guoxiaoxing.android.sdk.design.builder.whole;

/**
 * 具体产品角色，实现具体的产品特性与行为
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:06
 */
public class RealProduct extends AbstractProduct {

    @Override
    public void setOs(String os) {
        os = "mac osx";
    }

    /**
     * 设置参数内部类，构建的时候先构建参数，完成后再创建实例并装配参数
     */
    public static class RealProductParams {
        public String board;
        public String display;
        public String os;

        public void applyParams(RealProduct product) {
            product.setBoard(board);
            product.setDisplay(display);
            product.setOs(os);
        }
    }
}

```
抽象建造者角色

```java
package com.guoxiaoxing.android.sdk.design.builder.whole;

/**
 * 抽象建造者橘色，抽象构建行为，具体实现交由子类完成。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/22 下午6:27
 */
public abstract class AbstractBuilder {

    public abstract void setBoard(String board);

    public abstract void setDisplay(String display);

    public abstract void setOs(String os);
}

```

具体建造者角色

```java
package com.guoxiaoxing.android.sdk.design.builder.whole;

/**
 * 具体建造者橘色，完成具体构建行为。一般持有产品的引用，并在build()方法中返回该引用。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:11
 */
public class RealBuilder extends AbstractBuilder {

    private RealProduct.RealProductParams productParams;

    public RealBuilder() {
        productParams = new RealProduct.RealProductParams();
    }

    @Override
    public void setBoard(String board) {
        productParams.board = board;
    }

    @Override
    public void setDisplay(String display) {
        productParams.display = display;
    }

    @Override
    public void setOs(String os) {
        productParams.os = os;
    }

    public RealProduct build() {
        RealProduct product = new RealProduct();
        productParams.applyParams(product);
        return product;
    }
}

```

指挥者角色

```java
package com.guoxiaoxing.android.sdk.design.builder.whole;

/**
 * 指挥者角色，指挥多个建造者进行构建过程，当只有一个建造者时此角色可以省略。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:18
 */
public class Direactor {

    private AbstractBuilder builder;

    public void setBuilder(AbstractBuilder builder) {
        builder = builder;
    }

    private void buildProduct(String board, String display, String os) {
        builder.setBoard(board);
        builder.setDisplay(display);
        builder.setOs(os);
    }
}

```

日常编程中，我们的建造者通常只有一个，这个时候我们可以使用简化版的建造者模式

```java
package com.guoxiaoxing.android.sdk.design.builder.simple;

/**
 * 如果系统中只需要一个具体建造者的话，可以省略掉抽象建造者。在具体建造者只有一个的情况下，如果抽象建造者角色已经被省略
 * 掉，那么还可以省略指挥者角色，让Builder角色扮演指挥者与建造者双重角色。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:05
 */
public class Product {

    public String board;
    public String display;
    public String os;

    class Builder {

        private Product product;

        public Builder() {
            product = new Product();
        }

        private String board;
        private String display;
        private String os;

        public void setBoard(String board) {
            product.board = board;
        }

        public void setDisplay(String display) {
            product.display = display;
        }

        public void setOs(String os) {
            product.os = os;
        }
    }

}

```

## 模式实践

我们再来看看，我们平时见过的类库都有哪些建造者模式的具体实践。

最为常见的莫过于AlertDialog了吧，在AlertDialog创建的过程中

AlertDialog：内置Builder类，完成参数构建。

AlertController：AlertDialog的代理类，完成具体的构建过程，内置AlertParams类，由Builder类传递过来的参数都会设置到AlertParams中。

AlertDialog内部调用逻辑

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/2/ClusterCallInternal-app-AlertDialog.png" width="700" height=""/>

AlertController内部调用逻辑

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/program/2/ClusterCallInternal-app-AlertController.png" width="700" height=""/>

好，我们来看看AlertDialog的具体构建流程。

1 调用Builder，构建参数。

```java
public class Demo{
    AlertDialog dialog = new AlertDialog.Builder(context)
        .setTitle("标题")
        .setMessage("消息")
        .create();
}
```
2 调用create()方法，创建AlertDialog实例，同时创建AlertController实例，Builder里的参数传递到了AlertParams里。

```java
public class AlertDialog extends Dialog implements DialogInterface {

    protected AlertDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, com.android.internal.R.style.Theme_Dialog_Alert);
        setCancelable(cancelable);
        setOnCancelListener(cancelListener);
        mAlert = new AlertController(context, this, getWindow());
    }

    public AlertDialog create() {
        final AlertDialog dialog = new AlertDialog(P.mContext);
        P.apply(dialog.mAlert);
        dialog.setCancelable(P.mCancelable);
        dialog.setOnCancelListener(P.mOnCancelListener);
        if (P.mOnKeyListener != null) {
            dialog.setOnKeyListener(P.mOnKeyListener);
        }
        return dialog;
    }
    
}
```
3 调用apply()方法，执行Dialog的创建，设置按钮监听、文字等。

```java
public static class AlertParams {

    public void apply(AlertController dialog) {
        if (mCustomTitleView != null) {
            dialog.setCustomTitle(mCustomTitleView);
        } else {
            if (mTitle != null) {
                dialog.setTitle(mTitle);
            }
            if (mIcon != null) {
                dialog.setIcon(mIcon);
            }
            if (mIconId >= 0) {
                dialog.setIcon(mIconId);
            }
        }
        if (mMessage != null) {
            dialog.setMessage(mMessage);
        }
        if (mPositiveButtonText != null) {
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, mPositiveButtonText,
                    mPositiveButtonListener, null);
        }
        if (mNegativeButtonText != null) {
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, mNegativeButtonText,
                    mNegativeButtonListener, null);
        }
        if (mNeutralButtonText != null) {
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, mNeutralButtonText,
                    mNeutralButtonListener, null);
        }
        if (mForceInverseBackground) {
            dialog.setInverseBackgroundForced(true);
        }
        // For a list, the client can either supply an array of items or an
        // adapter or a cursor
        if ((mItems != null) || (mCursor != null) || (mAdapter != null)) {
            createListView(dialog);
        }
        if (mView != null) {
            if (mViewSpacingSpecified) {
                dialog.setView(mView, mViewSpacingLeft, mViewSpacingTop, mViewSpacingRight,
                        mViewSpacingBottom);
            } else {
                dialog.setView(mView);
            }
        }
        
        /*
        dialog.setCancelable(mCancelable);
        dialog.setOnCancelListener(mOnCancelListener);
        if (mOnKeyListener != null) {
            dialog.setOnKeyListener(mOnKeyListener);
        }
        */
    }
    
}
```
以上便是AlertDialog德创建过程，可以看出综合使用了代理模式与建造者模式，当create()方法被调用时才创建出了对象，当apply()方法被
调用的时候，才对对象进行属性设置。


事实上，Builder在创建时它只创建了AlertParams的实例，并没有去创建AlertDialog，这种写法也是懒加载的一种体现，有利于节省资源。

```java

public static class Builder {
    
    private final AlertController.AlertParams P;

    public Builder(@NonNull Context context, @StyleRes int themeResId) {
        P = new AlertController.AlertParams(new ContextThemeWrapper(
                context, resolveDialogTheme(context, themeResId)));
        mTheme = themeResId;
    }
}

```

以上便是Android系统中对建造者模式的应用，可以发现该实践中并没有抽象产品角色、指挥者角色，这是Android对建造者模式的一种简化，在后续的阅读源码过程中还会发现
很多这样的简化与变通，并不是刻板的按照标准实现来做，这也正是编程的魅力所在：因地制宜，灵活多变。

<img src="https://github.com/guoxiaoxing/emoji/raw/master/expression/expression9.jpg"/>

关于Dialog的进一步的内容以及WindowManager等相关原理，我们在这里不再进一步展开，后续的Android源码分析系列文章中会做进一步的详尽分析。