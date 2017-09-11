# Android显示框架：自定义View实践

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

前面我们讲解了Activity视图的创建与渲染流程：

- [04Android显示框架：Activity应用视图的创建流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/04Android显示框架：Activity应用视图的创建流程.md)
- [05Android显示框架：Activity应用视图的渲染流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/05Android显示框架：Activity应用视图的渲染流程.md)

今天我们来进行ui系列原理分析的的最后一篇文章，自定义View实践。前面的文章都是为了这篇文章做铺垫。

自定义View是开发中最常见的需求，图表等各种复杂的ui以及产品经理各种奇怪的需求😤都要通过自定义View来完成。

自定义View有三个关键点：

- 布局：决定View的摆放位置
- 绘制：决定View的具体内容
- 触摸反馈：决定View与用户的交互体验

一个简单的自定义View

```java
public class DrawView extends View {

    Paint paint = new Paint();

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(150, 150, 150, paint);
    }
}
```
它在屏幕上绘制了一个圆形，如图：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/draw_view_1.png" width="250" height="500"/>

在处理绘制的时候有以下几个关键点：

- 处理绘制需要重写绘制方法，常用的是onDraw()，当然我们也可以使用其他的绘制方法来处理遮盖关系。
- 完成绘制的是Canvas类，该类提供了绘制系列方法drawXXX()。裁剪系列方法clipXXX()以及几何变换方法translate()方法。
- 定制绘制的是Paint类，该类是绘制所用的画笔，可以实现特殊的绘制效果。

下面我们就这三个点分别分析。

## 一 onDraw

### 1.1 绘制视图

```java
public class View implements Drawable.Callback, KeyEvent.Callback,
        AccessibilityEventSource {
    
        //绘制视图
        protected void onDraw(Canvas canvas) {
            
        }
}
```

### 1.2 绘制前景

```java
public class View implements Drawable.Callback, KeyEvent.Callback,
        AccessibilityEventSource {
    
        //绘制前景视图
        public void onDrawForeground(Canvas canvas) {
            onDrawScrollIndicators(canvas);
            onDrawScrollBars(canvas);
    
            final Drawable foreground = mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
            if (foreground != null) {
                if (mForegroundInfo.mBoundsChanged) {
                    mForegroundInfo.mBoundsChanged = false;
                    final Rect selfBounds = mForegroundInfo.mSelfBounds;
                    final Rect overlayBounds = mForegroundInfo.mOverlayBounds;
    
                    if (mForegroundInfo.mInsidePadding) {
                        selfBounds.set(0, 0, getWidth(), getHeight());
                    } else {
                        selfBounds.set(getPaddingLeft(), getPaddingTop(),
                                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
                    }
    
                    final int ld = getLayoutDirection();
                    Gravity.apply(mForegroundInfo.mGravity, foreground.getIntrinsicWidth(),
                            foreground.getIntrinsicHeight(), selfBounds, overlayBounds, ld);
                    foreground.setBounds(overlayBounds);
                }
    
                foreground.draw(canvas);
            }
        }
}
```
## 二 Canvas

>Canvas实现了Android 2D图形的绘制，底层基于Skia实现。

### 2.1 界面绘制

Canvas提供了丰富的对象绘制方法，一般都以drawXXX()打头，绘制的对象包括：

- 弧线（Arcs）
- 颜色（Argb、Color）
- 位图（Bitmap）
- 圆（Circle）
- 点（Point）
- 线（Line）
- 矩形（Rect）
- 图片（Picture）
- 圆角矩形（RoundRect）
- 文本（Text）
- 顶点（Vertices）
- 路径（Path）

这里的方法大都很简单，我们来描述下期中比较复杂的方法。

**弧线**

```java
public void drawArc(float left, float top, float right, float bottom, float startAngle,
        float sweepAngle, boolean useCenter, @NonNull Paint paint) {
    native_drawArc(mNativeCanvasWrapper, left, top, right, bottom, startAngle, sweepAngle,
            useCenter, paint.getNativeInstance());
}
```

- float left, float top, float right, float bottom：左、上、右、下的坐标。
- float startAngle：弧形起始角度，Android坐标系x轴正右的方向是0度的位置，顺时针为正角度，逆时针为负角度。
- float sweepAngle：弧形划过的角度。
- boolean useCenter：是否连接到圆心。如果不连接到圆心就是弧形，如果连接到圆心，就是扇形。

例如

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/draw_view_2.png" width="250" height="500"/>

```java
paint.setStyle(Paint.Style.FILL);//填充模式
canvas.drawArc(200, 100, 800, 500, -110, 100, true, paint);
canvas.drawArc(200, 100, 800, 500, 20, 140, false, paint);
paint.setStyle(Paint.Style.STROKE);//画线模式
paint.setStrokeWidth(5);
canvas.drawArc(200, 100, 800, 500, 180, 60, false, paint);
```

**路径**

```java
public void drawPath(@NonNull Path path, @NonNull Paint paint) {
    if (path.isSimplePath && path.rects != null) {
        native_drawRegion(mNativeCanvasWrapper, path.rects.mNativeRegion, paint.getNativeInstance());
    } else {
        native_drawPath(mNativeCanvasWrapper, path.readOnlyNI(), paint.getNativeInstance());
    }
}
```

drawPath()可以绘制自定义图形，图形的路径用Path对象来描述。

Path对象可以描述很多图形，具体说来：

- 直线
- 二次曲线
- 三次曲线
- 圆
- 椭圆
- 弧形
- 矩形
- 圆角矩形

我们再来看看Path里的方法。

**第一组：addXXX() - 添加完整的封闭图形**

例如：addCircle(float x, float y, float radius, Direction dir)

```java
public void addCircle(float x, float y, float radius, Direction dir) {
    isSimplePath = false;
    native_addCircle(mNativePath, x, y, radius, dir.nativeInt);
}
```
该方法的参数含义：

- float x：圆心x轴坐标
- float y：圆心y轴坐标
- float radius：圆半径
- Direction dir：画圆的路径的方向，顺时针Direction.CN，逆时针Direction.CCN，它们在填充图形（Paint.Style 为 FILL 或  FILL_AND_STROKE）且图形出现相交的时候
用来判断填充范围。

其他的方法都是这个方法类似。

**第二组：xxxTo() - 画线（直线或者曲线）**

**直线**

```java
//从当前位置，向目标位置画一条直线，该方法使用相对于原点的绝对坐标
public void lineTo(float x, float y) {
    isSimplePath = false;
    native_lineTo(mNativePath, x, y);
}

//从当前位置，向目标位置画一条直线，该方法使用相对于当前位置的相对坐标
public void rLineTo(float dx, float dy) {
    isSimplePath = false;
    native_rLineTo(mNativePath, dx, dy);
}
```
>当前位置：当前位置指的是最后一次盗用Path的方法的终点位置，初始原点为(0, 0)

这里说到当前位置，我们再提一个方法Path.moveTo(float x, float y)，它可以移动当前位置到一个新的位置。

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/draw_view_3.png" width="250" height="500"/>

```java
paint.setStyle(Paint.Style.STROKE);
paint.setStrokeWidth(5);
path.lineTo(300, 400);// 由当前位置 (0, 0) 向 (300, 400) 画一条直线
path.rLineTo(400, 0);// 由当前位置 (300, 400) 向正右方400像素的位置画一条直线
canvas.drawPath(path, paint);
```
**贝塞尔曲线**

>贝塞尔曲线：贝塞尔曲线是几何上的一种曲线。它通过起点、控制点和终点来描述一条曲线，主要用于计算机图形学。简单来说，贝塞尔曲线就是讲
任意一条曲线转换为精确的数学公式。

在贝塞尔曲线中，有两类点：

- 数据点：一般指一条路径的起点与终点。
- 控制点：控制点决定了路径的弯曲轨迹，根据控制点的个数，贝塞尔曲线分为：一阶贝塞尔曲线（0个控制点），二阶贝塞尔曲线（1个控制点），三阶贝塞尔曲线（2个控制点）等。

一阶贝塞尔曲线

![](https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_1_formula.svg)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_1_demo.gif"/>


二阶贝塞尔曲线

![](https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_2_formula.svg)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_2_demo.gif"/>


三阶贝塞尔曲线

![](https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_3_formula.svg)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_3_demo.gif"/>

```java

//二次贝塞尔曲线，绝对坐标
public void quadTo(float x1, float y1, float x2, float y2) {
    isSimplePath = false;
    native_quadTo(mNativePath, x1, y1, x2, y2);
}

//二次贝塞尔曲线，相对坐标
public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
    isSimplePath = false;
    native_rQuadTo(mNativePath, dx1, dy1, dx2, dy2);
}

//三次贝塞尔曲线，绝对坐标
public void cubicTo(float x1, float y1, float x2, float y2,
                    float x3, float y3) {
    isSimplePath = false;
    native_cubicTo(mNativePath, x1, y1, x2, y2, x3, y3);
}

//三次贝塞尔曲线，相对坐标
public void rCubicTo(float x1, float y1, float x2, float y2,
                     float x3, float y3) {
    isSimplePath = false;
    native_rCubicTo(mNativePath, x1, y1, x2, y2, x3, y3);
}
```




**弧线**

```java

//画弧线
public void arcTo(float left, float top, float right, float bottom, float startAngle,
        float sweepAngle, boolean forceMoveTo) {
    isSimplePath = false;
    native_arcTo(mNativePath, left, top, right, bottom, startAngle, sweepAngle, forceMoveTo);
}
```
我们来看看这个方法的参数：

- float left, float top, float right, float bottom：左、上、右、下的坐标。
- float startAngle：弧形起始角度，Android坐标系x轴正右的方向是0度的位置，顺时针为正角度，逆时针为负角度。
- float sweepAngle：弧形划过的角度。
- boolean forceMoveTo)：是否留下移动的痕迹

>注：可以发现，这个方法与同样用来画弧线的方法Canvas.drawArc()少了个boolean useCenter参数，这是因为arcTo()方法只用来画弧线。

举例

```java

```


## 2.2 位置转换

Canvas还提供了对象的位置变换的方法，其中包括：

- 旋转（Rotate）
- 缩放（Scale）
- 平移（Translate）
- 扭曲（Skew）

### 绘制颜色

## 三 Paint

Paint有三种构造方法

```java
public class Paint {
      //空的构造方法
      public Paint() {
          this(0);
      }
  
      //传入flags来构造Paint，flags用来控制Paint的行为，例如：抗锯齿等
      public Paint(int flags) {
          mNativePaint = nInit();
          NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, mNativePaint);
          setFlags(flags | HIDDEN_DEFAULT_PAINT_FLAGS);
          // TODO: Turning off hinting has undesirable side effects, we need to
          //       revisit hinting once we add support for subpixel positioning
          // setHinting(DisplayMetrics.DENSITY_DEVICE >= DisplayMetrics.DENSITY_TV
          //        ? HINTING_OFF : HINTING_ON);
          mCompatScaling = mInvCompatScaling = 1;
          setTextLocales(LocaleList.getAdjustedDefault());
      }
  
      //传入另外一个Paint来构造新的Paint
      public Paint(Paint paint) {
          mNativePaint = nInitWithPaint(paint.getNativeInstance());
          NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, mNativePaint);
          setClassVariablesFrom(paint);
      }  
}
```
