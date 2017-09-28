# Android显示框架：自定义View实践之绘制篇

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

- 一 View
- 二 Paint
    - 2.1 颜色处理
    - 2.2 文字处理
    - 2.3 特殊处理
- 三 Canvas
    - 3.1 界面绘制
    - 3.2 范围裁切
    - 3.3 几何变换
- 四 Path
    -  4.1 添加图形
    -  4.3 画线（直线或曲线）
    -  4.3 辅助设置和计算

**文章源码**

- [DrawView](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/demo/src/main/java/com/guoxiaoxing/android/framework/demo/application/ui/DrawView.java)
- [WaveView](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/demo/src/main/java/com/guoxiaoxing/android/framework/demo/application/ui/bazier/WaveView.java)
- [RippleLayout](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/demo/src/main/java/com/guoxiaoxing/android/framework/demo/application/ui/RippleLayout.java)
- [LabelImageView](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/demo/src/main/java/com/guoxiaoxing/android/framework/demo/application/ui/LabelImageView.java)

本文还提供了三个综合性的完整实例来辅助理解。

- View绘制 - 图片标签效果实现
- Canvas绘图 - 水面涟漪效果实现
- 二阶贝塞尔曲线的应用 - 杯中倒水效果实现

<p>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/label_view.png" width="250" height="500"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/ripple.gif" width="260" height="500"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_wave.gif" width="260" height="500"/>
</p>

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

本篇文章我们来分析View绘制方面的实践。

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

- 处理绘制需要重写绘制方法，常用的是View的onDraw()，当然我们也可以使用其他的绘制方法来处理遮盖关系。
- 完成绘制的是Canvas类，该类提供了绘制系列方法drawXXX()。裁剪系列方法clipXXX()以及几何变换方法translate()方法，还有辅助绘制的Path与Matrix。
- 定制绘制的是Paint类，该类是绘制所用的画笔，可以实现特殊的绘制效果。

我们分别来看看这个关键的角色。

## 一 View

我们讨论的第一个问题就是View/ViewGroup的绘制顺序问题，绘制在View.draw()方法里调用的，具体的执行顺序是：

1. drawBackground()：绘制背景，不能重写。
2. onDraw()：绘制主体。
3. dispatchDraw()：绘制子View
4. onDrawForeground()：绘制滑动边缘渐变、滚动条和前景。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/view_draw_flow.png"/>

我们先从个小例子开始。

我们如果继承View来实现自定义View。View类的onDraw()是空实现，所以我们的绘制代码写在super.onDraw(canvas)的前面或者后面都没有关系，如下所示：

```java
public class DrawView extends View {
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制代码，写在super.onDraw(canvas)前后均可
    }
}
```

但是如果我们继承特定的控件，例如TextView。我们就需要去考虑TextView的绘制逻辑。

```java
public class DrawView extends TextView {
    @Override
    protected void onDraw(Canvas canvas) {

        //写在前面，DrawView的绘制会先于TextView的绘制，TextView绘制的内容可以会覆盖DrawView
        super.onDraw(canvas);
        //写在后面，DrawView的绘制会晚于TextView的绘制，DrawView绘制的内容可以会覆盖TextView
    }
}
```
- 写在前面，DrawView的绘制会先于TextView的绘制，TextView绘制的内容可以会覆盖DrawView
- 写在后面，DrawView的绘制会晚于TextView的绘制，DrawView绘制的内容可以会覆盖TextView

具体怎么做取决于你实际的需求，例如你如果想给TextView加个背景，就写在super.onDraw(canvas)前面，想给TextView前面加些点缀，就
写在super.onDraw(canvas)后面。

我们来写个例子理解下。

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/label_view.png" width="250" height="500"/>

```java
public class LabelImageView extends AppCompatImageView {

    /**
     * 梯形距离左上角的长度
     */
    private static final int LABEL_LENGTH = 100;
    /**
     * 梯形斜边的长度
     */
    private static final int LABEL_HYPOTENUSE_LENGTH = 100;

    private Paint textPaint;
    private Paint backgroundPaint;
    private Path pathText;
    private Path pathBackground;


    public LabelImageView(Context context) {
        super(context);
        init();
    }

    public LabelImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LabelImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //计算路径
        calculatePath(getMeasuredWidth(), getMeasuredHeight());
        canvas.drawPath(pathBackground, backgroundPaint);
        canvas.drawTextOnPath("Hot", pathText, 100, -20, textPaint);
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
    }

    /**
     * 计算路径              x1   x2
     * ................................    distance（标签离右上角的垂直距离）
     * .                      .    .  .
     * .                        .    .. y1
     * .                          .   .
     * .                            . .
     * .                              . y2    height(标签垂直高度)
     * .                              .
     * ................................
     */
    private void calculatePath(int measuredWidth, int measuredHeight) {

        int top = 185;
        int right = measuredWidth;

        float x1 = right - LABEL_LENGTH - LABEL_HYPOTENUSE_LENGTH;
        float x2 = right - LABEL_HYPOTENUSE_LENGTH;
        float y1 = top + LABEL_LENGTH;
        float y2 = top + LABEL_LENGTH + LABEL_HYPOTENUSE_LENGTH;

        pathText.reset();
        pathText.moveTo(x1, top);
        pathText.lineTo(right, y2);
        pathText.close();

        pathBackground.reset();
        pathBackground.moveTo(x1, top);
        pathBackground.lineTo(x2, top);
        pathBackground.lineTo(right, y1);
        pathBackground.lineTo(right, y2);
        pathBackground.close();
    }

    private void init() {
        pathText = new Path();
        pathBackground = new Path();

        textPaint = new Paint();
        textPaint.setTextSize(50);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.WHITE);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.RED);
        backgroundPaint.setStyle(Paint.Style.FILL);
    }
}
```

所以你可以看到，当我们继承了一个View，根据需求的不同可以选择性重写我们需要的方法，在super前插入代码和在super后插入代码，效果是不一样的。

- draw()：super.draw()之前，被背景盖住；super.draw()后，盖住前景；
- onDraw()：super.onDraw()之前，背景与主体内容之前；super.onDraw()之后，主体内容和子View之间；
- dispatchDraw()：super.dispatchDraw()之前，主体内容和子View之间；super.dispatchDraw()之后，子View和前景之间；
- onDrawForeground()：super.onDrawForeground()之前，子View和前景之间；super.onDrawForeground()之后，盖住前景；

## 二 Paint

>Paint：顾名思义，画笔，通过Paint可以对绘制行为进行控制。

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

### 2.1 颜色处理类

在Paint类中，处理颜色主要有三个方法。

- setShader(Shader shader)：用来处理颜色渐变
- setColorFilter(ColorFilter filter)：用来基于颜色进行过滤处理； 
- setXfermode(Xfermode xfermode) 用来处理源图像和 View 已有内容的关系

#### setShader(Shader shader)

>着色器是图像领域的一个通用概念，它提供的是一套着色规则。

```java
public Shader setShader(Shader shader) 
```
着色器具体由Shader的子类实现：

**LinearGradient - 线性渐变**

```java
public LinearGradient(float x0, float y0, float x1, float y1, int color0, int color1, TileMode tile)
```

- x0 y0 x1 y1：渐变的两个端点的位置 
- color0 color1 是端点的颜色 
- tile：端点范围之外的着色规则，类型是 TileMode。TileMode 一共有 3 个值可选： CLAMP, MIRROR 和 REPEAT。CLAMP 

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/shader_linear.png" width="250" height="500"/>

```java
//线性渐变
Shader shader1 = new LinearGradient(0, 100, 200, 100, Color.RED, Color.BLUE, Shader.TileMode.CLAMP);
paint1.setShader(shader1);

Shader shader2 = new LinearGradient(0, 600, 200, 600, Color.RED, Color.BLUE, Shader.TileMode.MIRROR);
paint2.setShader(shader2);

Shader shader3 = new LinearGradient(0, 1100, 200, 1100, Color.RED, Color.BLUE, Shader.TileMode.REPEAT);
paint3.setShader(shader3);

canvas.drawRect(0, 100, 1000, 500, paint1);
canvas.drawRect(0, 600, 1000, 1000, paint2);
canvas.drawRect(0, 1100, 1000, 1500, paint3);
```

**SweepGradient - 辐射渐变**

```java
public RadialGradient(float centerX, float centerY, float radius, int centerColor, int edgeColor, @NonNull TileMode tileMode) 
```

- centerX centerY：辐射中心的坐标 
- radius：辐射半径 
- centerColor：辐射中心的颜色 
- edgeColor：辐射边缘的颜色 
- tileMode：辐射范围之外的着色模式

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/shader_radial.png" width="250" height="500"/>


```java
//辐射渐变
Shader shader1 = new RadialGradient(0, 100, 200, Color.RED, Color.BLUE, Shader.TileMode.CLAMP);
paint1.setShader(shader1);

Shader shader2 = new RadialGradient(0, 600, 200, Color.RED, Color.BLUE, Shader.TileMode.MIRROR);
paint2.setShader(shader2);

Shader shader3 = new RadialGradient(0, 1100, 200, Color.RED, Color.BLUE, Shader.TileMode.REPEAT);
paint3.setShader(shader3);

canvas.drawRect(0, 100, 1000, 500, paint1);
canvas.drawRect(0, 600, 1000, 1000, paint2);
```
**BitmapShader - 位图着色** 

使用位图的像素来填充图形或者文字。

```java
 public BitmapShader(@NonNull Bitmap bitmap, TileMode tileX, TileMode tileY)
```
- bitmap：用来做模板的 Bitmap 对象 
- tileX：横向的 TileMode 
- tileY：纵向的 TileMode。

举例

BitmapShader是一个很有用的类，可以利用该类做各种各样的图片裁剪。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/shader_bitmap.png" width="250" height="500"/>

```java
//位图着色
Shader shader1 = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
paint1.setShader(shader1);

//绘制圆形
canvas.drawCircle(500, 500, 300, paint1);
```

**ComposeShader - 组合Shader**

ComposeShader可以将连个Shader组合在一起。

```java
public ComposeShader(Shader shaderA, Shader shaderB, PorterDuff.Mode mode) 
```

- shaderA, shaderB：两个相继使用的 Shader 
- mode: 两个 Shader 的叠加模式，即 shaderA 和 shaderB 应该怎样共同绘制。它的类型是PorterDuff.Mode。

PorterDuff.Mode用来指定两个Shader叠加时颜色的绘制策略，它有很多种策略，也就是以一种怎样的模式来与原图像进行合成，具体如下：

蓝色矩形为原图像，红色圆形为目标图像。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/porter_buff_mode_alpha.png"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/porter_duff_mode_blending.png"/>

更多细节可以参见[PorterDuff.Mode官方文档](https://developer.android.com/reference/android/graphics/PorterDuff.Mode.html)。

#### setColorFilter(ColorFilter filter)

>颜色过滤器可以将颜色按照一定的规则输出，常见于各种滤镜效果。

```java
public ColorFilter setColorFilter(ColorFilter filter) 
```
我们通常使用的是ColorFilter的三个子类：

**LightingColorFilter - 模拟光照效果**

```java
public LightingColorFilter(int mul, int add)
```
mul 和 add 都是和颜色值格式相同的 int 值，其中 mul 用来和目标像素相乘，add 用来和目标像素相加。

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/color_filter_lighting.png" width="250" height="500"/>

```java
//颜色过滤器
ColorFilter colorFilter1 = new LightingColorFilter(Color.RED, Color.BLUE);
paint2.setColorFilter(colorFilter1);

canvas.drawBitmap(bitmapTimo, null, rect1, paint1);
canvas.drawBitmap(bitmapTimo, null, rect2, paint2);
```

**PorterDuffColorFilter - 模拟颜色混合效果** 

```java
public PorterDuffColorFilter(@ColorInt int color, @NonNull PorterDuff.Mode mode) 
```
PorterDuffColorFilter指定一种颜色和PorterDuff.Mode来与源图像就行合成，也就是以一种怎样的模式来与原图像进行合成，我们在上面已经讲过这个内容。

举例

```java
//我们在使用Xfermode的时候也是使用它的子类PorterDuffXfermode
Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
canvas.drawBitmap(rectBitmap, 0, 0, paint); // 画方  
paint.setXfermode(xfermode); // 设置 Xfermode  
canvas.drawBitmap(circleBitmap, 0, 0, paint); // 画圆  
paint.setXfermode(null); // 用完及时清除 Xfermode  
```

**ColorMatrixColorFilter - 颜色矩阵过滤**

ColorMatrixColorFilter使用一个颜色矩阵ColorMatrix来对象图像进行处理。

```java
public ColorMatrixColorFilter(ColorMatrix matrix)
```
ColorMatrix是一个4x5的矩阵

```java
[ a, b, c, d, e,
  f, g, h, i, j,
  k, l, m, n, o,
  p, q, r, s, t ]
```
通过计算，ColorMatrix可以对要绘制的像素进行转换，如下：

```java
R’ = a*R + b*G + c*B + d*A + e;  
G’ = f*R + g*G + h*B + i*A + j;  
B’ = k*R + l*G + m*B + n*A + o;  
A’ = p*R + q*G + r*B + s*A + t;  
```

利用ColorMatrixColorFilter(可以实现很多炫酷的滤镜效果。

#### setXfermode(Xfermode xfermode)

Paint.setXfermode(Xfermode xfermode)方法，它也是一种混合图像的方法。

>Xfermode 指的是你要绘制的内容和 Canvas 的目标位置的内容应该怎样结合计算出最终的颜色。但通俗地说，其实就是要你以绘制的内容作为源图像，以View中已有的内
容作为目标图像，选取一个PorterDuff.Mode作为绘制内容的颜色处理方案。

**小结**

关于PorterDuff.Mode，我们已经提到

- ComposeShader：混合两个Shader
- PorterDuffColorFilter：增加一个单色的ColorFilter
- Xfermode：指定原图像与目标图像的混合模式

这三种以不同的方式来使用PorterDuff.Mode，但是原理都是一样的。

### 2.2 文字处理类

Paint里有大量方法来设置文字的绘制属性，事实上文字在Android底层是被当做图片来处理的。

- setTextSize(float textSize)：设置文字大小
-   setTypeface(Typeface typeface)：设置文字字体
- setFakeBoldText(boolean fakeBoldText)：是否使用伪粗体（并不是提到size，而是在运行时描粗的）
- setStrikeThruText(boolean strikeThruText)：是否添加删除线
- setUnderlineText(boolean underlineText)：是否添加下划线
- setTextSkewX(float skewX)：设置文字倾斜度
- setTextScaleX(float scaleX)：设置文字横向缩放
- setLetterSpacing(float letterSpacing)：设置文字间距
- setFontFeatureSettings(String settings)：使用CSS的font-feature-settings的方式来设置文字。
- setTextAlign(Paint.Align align)：设置文字对齐方式
- setTextLocale(Locale locale)：设置文字Local
- setHinting(int mode)：设置字体Hinting（微调），过向字体中加入 hinting 信息，让矢量字体在尺寸过小的时候得到针对性的修正，从而提高显示效果。
- setSubpixelText(boolean subpixelText)：设置次像素级抗锯齿，根据程序所运行的设备的屏幕类型，来进行针对性的次像素级的抗锯齿计算，从而达到更好的抗锯齿效果。

### 2.3 特殊效果类

#### setAntiAlias (boolean aa) 

设置抗锯齿，默认关闭，用来是图像的绘制更加圆润。我们还可以在初始化的时候设置Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);。

#### setStyle(Paint.Style style)

设置填充风格，

- FILL 模式，填充  
- STROKE 模式，画线  
- FILL_AND_STROKE 模式，填充 + 画线

如果是划线模式，我们针对线条还可以有多种设置。

setStrokeWidth(float width) - 设置线条粗细

setStrokeCap(Paint.Cap cap) - 设置线头的形状，默认为 BUTT

- UTT 平头
- ROUND 圆头
- SQUARE 方头

setStrokeJoin(Paint.Join join) - 设置拐角的形状。默认为 MITER

- MITER 尖角
- BEVEL 平角
- ROUND 圆角

setStrokeMiter(float miter)- 设置 MITER 型拐角的延长线的最大值

#### setDither(boolean dither)

设置图像的抖动。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/dither.png" width="250" height="500"/>

>抖动是指把图像从较高色彩深度（即可用的颜色数）向较低色彩深度的区域绘制时，在图像中有意地插入噪点，通过有规律地扰乱图像来让图像对于肉眼更加真实的做法。

当然这个效果旨在低位色的时候比较有用，例如，ARGB_4444 或者 RGB_565，不过现在Android默认的色彩深度都是32位的ARGB_8888，这个方法的效果没有那么明显。

#### setFilterBitmap(boolean filter)

设置是否使用双线性过滤来绘制 Bitmap 。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bitmap_filter.png" width="250" height="500"/>

图像在放大绘制的时候，默认使用的是最近邻插值过滤，这种算法简单，但会出现马赛克现象；而如果开启了双线性过滤，就可以让结果图像显得更加平滑。

#### etPathEffect(PathEffect effect)

设置图形的轮廓效果。Android有六种PathEffect：

- CornerPathEffect：将拐角绘制成圆角
- DiscretePathEffect：将线条进行随机偏离
- DashPathEffect：绘制虚线
- PathDashPathEffect：使用指定的Path来绘制虚线
- SumPathEffect：组合两个PathEffect，叠加应用。
- ComposePathEffect：组合两个PathEffect，叠加应用。

CornerPathEffect(float radius)

- float radius圆角半径

DiscretePathEffect(float segmentLength, float deviation)

- float segmentLength：用来拼接每个线段的长度，
- float deviation：偏离量

DashPathEffect(float[] intervals, float phase)

- float[] intervals：指定了虚线的格式，数组中元素必须为偶数（最少是 2 个），按照「画线长度、空白长度、画线长度、空白长度」……的顺序排列
- float phase：虚线的偏移量

PathDashPathEffect(Path shape, float advance, float phase, PathDashPathEffect.Style style)

- Path shape：用来绘制的Path
- float advance：两个相邻Path段起点间的间隔
- float phase：虚线的偏移量
- PathDashPathEffect.Style style：指定拐弯改变的时候 shape 的转换方式：TRANSLATE：位移、ROTATE：旋转、MORPH：变体
                                                       
SumPathEffect(PathEffect first, PathEffect second)

- PathEffect first：同时应用的PathEffect
- PathEffect second：同时应用的PathEffect

ComposePathEffect(PathEffect outerpe, PathEffect innerpe)

- PathEffect outerpe：后应用的PathEffect
- PathEffect innerpe：先应用用的PathEffect

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/path_effect.png" width="250" height="500"/>

```java
//图形轮廓效果
//绘制圆角
PathEffect cornerPathEffect = new CornerPathEffect(20);
paint1.setStyle(Paint.Style.STROKE);
paint1.setStrokeWidth(5);
paint1.setPathEffect(cornerPathEffect);

//绘制尖角
PathEffect discretePathEffect = new DiscretePathEffect(20, 5);
paint2.setStyle(Paint.Style.STROKE);
paint2.setStrokeWidth(5);
paint2.setPathEffect(discretePathEffect);

//绘制虚线
PathEffect dashPathEffect = new DashPathEffect(new float[]{20,10, 5, 10}, 0);
paint3.setStyle(Paint.Style.STROKE);
paint3.setStrokeWidth(5);
paint3.setPathEffect(dashPathEffect);

//使用path来绘制虚线
Path path = new Path();//画一个三角来填充虚线
path.lineTo(40, 40);
path.lineTo(0, 40);
path.close();
PathEffect pathDashPathEffect = new PathDashPathEffect(path, 40, 0, PathDashPathEffect.Style.TRANSLATE);
paint4.setStyle(Paint.Style.STROKE);
paint4.setStrokeWidth(5);
paint4.setPathEffect(pathDashPathEffect);
```
#### setShadowLayer(float radius, float dx, float dy, int shadowColor)

设置阴影图层，处于目标下层图层。

- float radius：阴影半径
- float dx：阴影偏移量
- float dy：阴影偏移量
- int shadowColor：阴影颜色

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/shadow_layer.png" width="250" height="500"/>

```java
paint1.setTextSize(200);
paint1.setShadowLayer(10, 0, 0, Color.RED);
canvas.drawText("Android", 80, 300 ,paint1);
```

>注：在硬件加速开启的情况下， setShadowLayer() 只支持文字的绘制，文字之外的绘制必须关闭硬件加速才能正常绘制阴影。如果 shadowColor 是半透明的，阴影的透明度就使用 shadowColor 自己
的透明度；而如果  shadowColor 是不透明的，阴影的透明度就使用 paint 的透明度。

#### setMaskFilter(MaskFilter maskfilter)

设置图层遮罩层，处于目标上层图层。

MaskFilter有两个子类：

- BlurMaskFilter：模糊效果
- BlurMaskFilter：浮雕效果

举例

模糊效果

- BlurMaskFilter.Blur.NORMAL
- BlurMaskFilter.Blur.SOLD
- BlurMaskFilter.Blur.INNER
- BlurMaskFilter.Blur.OUTTER

分别为：

<p>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/blur_mask_filter_style_normal.png" width="200"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/blur_mask_filter_style_sold.png" width="200"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/blur_mask_filter_style_inner.png" width="200"/>
<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/blur_mask_filter_style_outter.png" width="200"/>
</p>

```java
//设置遮罩图层,处于目标上层图层
//关闭硬件加速
setLayerType(View.LAYER_TYPE_SOFTWARE, null);
MaskFilter blurMaskFilter = new BlurMaskFilter(200, BlurMaskFilter.Blur.NORMAL);
paint2.setMaskFilter(blurMaskFilter);

canvas.drawBitmap(bitmapTimo, null, rect1, paint1);
canvas.drawBitmap(bitmapTimo, null, rect2, paint2);
```
>注：在硬件加速开启的情况下， setMaskFilter(MaskFilter maskfilter)只支持文字的绘制，文字之外的绘制必须关闭硬件加速才能正常绘制阴影。关闭硬件加速可以调用
View.setLayerType(View.LAYER_TYPE_SOFTWARE, null)或者在Activity标签里设置android:hardwareAccelerated="false"。

## 三 Canvas

>Canvas实现了Android 2D图形的绘制，底层基于Skia实现。

### 3.1 界面绘制

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
**位图**

- **public void drawBitmap(@NonNull Bitmap bitmap, float left, float top, @Nullable Paint paint) - 绘制位图**
- **public void drawBitmapMesh(@NonNull Bitmap bitmap, int meshWidth, int meshHeight,
             @NonNull float[] verts, int vertOffset, @Nullable int[] colors, int colorOffset,
             @Nullable Paint paint) - 绘制拉伸位图**

第一个方法很简单，就是在指定的坐标处开始绘制位图。我们着重来看看第二个方法，这个方法不是很常用（可能是计算比较复杂的锅😓），但这并不影响它强大的功能。

drawBitmapMesh()方法将位图分为若干网格，然后对每个网格进行扭曲处理。我们先来看看这个方法的参数：

- @NonNull Bitmap bitmap：源位图
- int meshWidth：横向上将源位图划分成多少格
- int meshHeight：纵向上将源位图划分成多少格
- @NonNull float[] verts：网格顶点坐标数组，记录扭曲后图片各顶点的坐标，数组大小为 (meshWidth+1) * (meshHeight+1) * 2 + vertOffset
- int vertOffset：记录verts数组从第几个数组元素开始扭曲
- @Nullable int[] colors：设置网格顶点的颜色，该颜色会和位图对应像素的颜色叠加，数组大小为 (meshWidth+1) * (meshHeight+1) + colorOffset
- int colorOffset：记录colors从几个数组元素开始取色
- @Nullable Paint paint：画笔

我们来用drawBitmapMesh()方法实现一个水面涟漪效果。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/ripple.gif" width="260" height="500"/>

举例

```java
/**
 * 利用Canvas.drawBitmapMeshC()方法对图像做扭曲处理，模拟水波效果。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/12 下午3:44
 */
public class RippleLayout extends FrameLayout {

    /**
     * 图片横向、纵向的格树
     */
    private final int MESH_WIDTH = 20;
    private final int MESH_HEIGHT = 20;

    /**
     * 图片顶点数
     */
    private final int VERTS_COUNT = (MESH_WIDTH + 1) * (MESH_HEIGHT + 1);

    /**
     * 原坐标数组
     */
    private final float[] originVerts = new float[VERTS_COUNT * 2];

    /**
     * 转换后的坐标数组
     */
    private final float[] targetVerts = new float[VERTS_COUNT * 2];

    /**
     * 当前空间的图像
     */
    private Bitmap bitmap;

    /**
     * 水波宽度的一半
     */
    private float rippleWidth = 100f;

    /**
     * 水波扩展的速度
     */
    private float rippleRadius = 15f;

    /**
     * 水波半径
     */
    private float rippleSpeed = 15f;

    /**
     * 水波动画是否在进行中
     */
    private boolean isRippling;

    public RippleLayout(@NonNull Context context) {
        super(context);
    }

    public RippleLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RippleLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isRippling && bitmap != null) {
            canvas.drawBitmapMesh(bitmap, MESH_WIDTH, MESH_HEIGHT, targetVerts, 0, null, 0, null);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                showRipple(ev.getX(), ev.getY());
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 显示水波动画
     *
     * @param originX 原点 x 坐标
     * @param originY 原点 y 坐标
     */
    public void showRipple(final float originX, final float originY) {
        if (isRippling) {
            return;
        }
        initData();
        if (bitmap == null) {
            return;
        }
        isRippling = true;
        //循环次数，通过控件对角线距离计算，确保水波纹完全消失
        int viewLength = (int) getLength(bitmap.getWidth(), bitmap.getHeight());
        final int count = (int) ((viewLength + rippleWidth) / rippleSpeed);
        Observable.interval(0, 10, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(count + 1)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        rippleRadius = aLong * rippleSpeed;
                        warp(originX, originY);
                        if (aLong == count) {
                            isRippling = false;
                        }
                    }
                });
    }

    /**
     * 初始化 Bitmap 及对应数组
     */
    private void initData() {
        bitmap = getCacheBitmapFromView(this);
        if (bitmap == null) {
            return;
        }
        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();
        int index = 0;
        for (int height = 0; height <= MESH_HEIGHT; height++) {
            float y = bitmapHeight * height / MESH_HEIGHT;
            for (int width = 0; width <= MESH_WIDTH; width++) {
                float x = bitmapWidth * width / MESH_WIDTH;
                originVerts[index * 2] = targetVerts[index * 2] = x;
                originVerts[index * 2 + 1] = targetVerts[index * 2 + 1] = y;
                index += 1;
            }
        }
    }

    /**
     * 图片转换
     *
     * @param originX 原点 x 坐标
     * @param originY 原点 y 坐标
     */
    private void warp(float originX, float originY) {
        for (int i = 0; i < VERTS_COUNT * 2; i += 2) {
            float staticX = originVerts[i];
            float staticY = originVerts[i + 1];
            float length = getLength(staticX - originX, staticY - originY);
            if (length > rippleRadius - rippleWidth && length < rippleRadius + rippleWidth) {
                PointF point = getRipplePoint(originX, originY, staticX, staticY);
                targetVerts[i] = point.x;
                targetVerts[i + 1] = point.y;
            } else {
                //复原
                targetVerts[i] = originVerts[i];
                targetVerts[i + 1] = originVerts[i + 1];
            }
        }
        invalidate();
    }

    /**
     * 获取水波的偏移坐标
     *
     * @param originX 原点 x 坐标
     * @param originY 原点 y 坐标
     * @param staticX 待偏移顶点的原 x 坐标
     * @param staticY 待偏移顶点的原 y 坐标
     * @return 偏移后坐标
     */
    private PointF getRipplePoint(float originX, float originY, float staticX, float staticY) {
        float length = getLength(staticX - originX, staticY - originY);
        //偏移点与原点间的角度
        float angle = (float) Math.atan(Math.abs((staticY - originY) / (staticX - originX)));
        //计算偏移距离
        float rate = (length - rippleRadius) / rippleWidth;
        float offset = (float) Math.cos(rate) * 10f;
        float offsetX = offset * (float) Math.cos(angle);
        float offsetY = offset * (float) Math.sin(angle);
        //计算偏移后的坐标
        float targetX;
        float targetY;
        if (length < rippleRadius + rippleWidth && length > rippleRadius) {
            //波峰外的偏移坐标
            if (staticX > originX) {
                targetX = staticX + offsetX;
            } else {
                targetX = staticX - offsetX;
            }
            if (staticY > originY) {
                targetY = staticY + offsetY;
            } else {
                targetY = staticY - offsetY;
            }
        } else {
            //波峰内的偏移坐标
            if (staticX > originY) {
                targetX = staticX - offsetX;
            } else {
                targetX = staticX + offsetX;
            }
            if (staticY > originY) {
                targetY = staticY - offsetY;
            } else {
                targetY = staticY + offsetY;
            }
        }
        return new PointF(targetX, targetY);
    }

    /**
     * 根据宽高，获取对角线距离
     *
     * @param width  宽
     * @param height 高
     * @return 距离
     */
    private float getLength(float width, float height) {
        return (float) Math.sqrt(width * width + height * height);
    }

    /**
     * 获取 View 的缓存视图
     *
     * @param view 对应的View
     * @return 对应View的缓存视图
     */
    private Bitmap getCacheBitmapFromView(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        final Bitmap drawingCache = view.getDrawingCache();
        Bitmap bitmap;
        if (drawingCache != null) {
            bitmap = Bitmap.createBitmap(drawingCache);
            view.setDrawingCacheEnabled(false);
        } else {
            bitmap = null;
        }
        return bitmap;
    }
}
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

### 3.2 范围裁切

Canvas里的范围裁切主要有两类方法：

- clipReact()：按路径裁切
- clipPath()：按坐标裁切

举例

clipReact

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/clip_rect.png" width="250" height="500"/>

clipPath

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/clip_path.png" width="250" height="500"/>

```java
//范围裁切
canvas.save();//保存画布
canvas.clipRect(200, 200, 900, 900);
canvas.drawBitmap(bitmapTimo, 100, 100, paint1);
canvas.restore();//恢复画布

canvas.save();//保存画布
path.addCircle(500, 500, 300, Path.Direction.CW);
canvas.clipPath(path);
canvas.drawBitmap(bitmapTimo, 100, 100, paint1);
canvas.restore();//恢复画布
```

### 3.3 几何变换

关于几何变换有三种实现方式：

- Canvas：常规几何变换
- Matrix：自定义几何变换
- Camera：三维变换

### Canvas常规几何变换

Canvas还提供了对象的位置变换的方法，其中包括：

- translate(float dx, float dy)：平移
- rotate(float degrees)：旋转，可以设置旋转圆点，默认在原点位置。
- scale(float sx, float sy)：缩放
- skew(float sx, float sy)：扭曲

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/canvas_rotate.png" width="250" height="500"/>

```java
canvas.save();//保存画布
canvas.skew(0, 0.5f);
canvas.drawBitmap(bitmapTimo, null, rect1, paint1);
canvas.restore();//恢复画布

canvas.save();//保存画布
canvas.rotate(45, 750, 750);
canvas.drawBitmap(bitmapTimo, null, rect2, paint1);
canvas.restore();//恢复画布
```
>注：1 为了不影响其他绘制操作，在进行变换之前需要调用canvas.save()保存画布，变换完成以后再调用canvas.restore()来恢复画布。
2 Canvas几何变换的顺序是相反的，例如我们在代码写了：canvas.skew(0, 0.5f); canvas.rotate(45, 750, 750); 它的实际调用顺序是canvas.rotate(45, 750, 750); -> canvas.skew(0, 0.5f)

#### Matrix自定义几何变换

Matrix也实现了Canvas里的四种常规变换，它的实现流程如下：

1. 创建 Matrix 对象；
2. 调用 Matrix 的 pre/postTranslate/Rotate/Scale/Skew() 方法来设置几何变换；
3. 使用 Canvas.setMatrix(matrix) 或 Canvas.concat(matrix) 来把几何变换应用到 Canvas。

>Canvas.concat(matrix)：用 Canvas 当前的变换矩阵和 Matrix 相乘，即基于 Canvas 当前的变换，叠加上 Matrix 中的变换。

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/canvas_rotate.png" width="250" height="500"/>

```java
//Matrix几何变换
canvas.save();//保存画布
matrix.preSkew(0, 0.5f);
canvas.concat(matrix);
canvas.drawBitmap(bitmapTimo, null, rect1, paint1);
canvas.restore();//恢复画布

canvas.save();//保存画布
matrix.reset();
matrix.preRotate(45, 750, 750);
canvas.concat(matrix);
canvas.drawBitmap(bitmapTimo, null, rect2, paint1);
canvas.restore();//恢复画布
```
Matrix除了四种基本的几何变换，还可以自定义几何变换。

- setPolyToPoly(float[] src, int srcIndex, float[] dst, int dstIndex, int pointCount)
- setRectToRect(RectF src, RectF dst, ScaleToFit stf)

这两个方法都是通过多点的映射的方式来直接设置变换，把指定的点移动到给出的位置，从而发生形变。

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/matrix_poly_to_poly.png" width="250" height="500"/>

```java
//Matrix几何变换
canvas.save();//保存画布
matrix.setPolyToPoly(src, 0, dst, 0, 2);
canvas.concat(matrix);
canvas.drawBitmap(bitmapTimo, 0, 0, paint1);
canvas.restore();//恢复画布
```
#### Camera三维变换

在讲解Camera的三维变换之前，我们需要先理解Camera的坐标系系统。

我们前面说过，Canvas使用的是二维坐标系。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/canvas_coordinate_system.png" width="350"/>

而Camera使用的是三维坐标系，这里偷个懒😊，借用凯哥的图来描述一下。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/camera_coordinate_system_1.gif"/>

关于Camera坐标系：

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/camera_tranlate_direction.png"/>

- 首先你要注意x、y、z轴的方向，z轴朝外是负轴。
- 在z的负轴上有个虚拟相机（就是图中的哪个黄点），它就是用来做投影的，setLocation(float x, float y, float z)方法移动的也就是它的位置。
- x、y、z轴旋转的方向也在上图中标出来了。

比如我们在Camera坐标系里做个X轴方向的旋转

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/camera_coordinate_system_2.gif"/>

Camera的三维变换包括：旋转、平移与移动相机。

旋转

- rotateX(deg)
- rotateY(deg)
- rotateZ(deg)
- rotate(x, y, z)

平移

- translate(float x, float y, float z)

移动相机

- setLocation(float x, float y, float z)

举例

旋转

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/camera_rotate.png" width="250" height="500"/>


```java
//Camera三维变换
canvas.save();//保存画布

camera.save();//保存camera
camera.rotateX(45);
canvas.translate(500, 750);//camera也是默认在原点(0, 0)位置，所以我们要把画布平移到图片中心(500, 750)
camera.applyToCanvas(canvas);
canvas.translate(-500, -750);//翻转完图片，再将画布从图片中心(500, 750)平移到原点(0, 0)
camera.restore();//恢复camera

canvas.drawBitmap(bitmapTimo, null, rect, paint1);
canvas.restore();//恢复画布
```

平移

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/camera_rotate.png" width="250" height="500"/>

```java
//Camera三维变换
canvas.save();//保存画布

camera.save();//保存camera
camera.translate(500, 500, 500);
canvas.translate(500, 750);//camera也是默认在原点(0, 0)位置，所以我们要把画布平移到图片中心(500, 750)
camera.applyToCanvas(canvas);
canvas.translate(-500, -750);//翻转完图片，再将画布从图片中心(500, 750)平移到原点(0, 0)
camera.restore();//恢复camera

canvas.drawBitmap(bitmapTimo, null, rect, paint1);
canvas.restore();//恢复画布
```

移动相机

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/camera_translate.png" width="250" height="500"/>

```java
//Camera三维变换
canvas.save();//保存画布

camera.save();//保存camera
camera.setLocation(0, 0, - 1000);//相机往前移动，图像变小
canvas.translate(500, 750);//camera也是默认在原点(0, 0)位置，所以我们要把画布平移到图片中心(500, 750)
camera.applyToCanvas(canvas);
canvas.translate(-500, -750);//翻转完图片，再将画布从图片中心(500, 750)平移到原点(0, 0)
camera.restore();//恢复camera

canvas.drawBitmap(bitmapTimo, null, rect, paint1);
canvas.restore();//恢复画布
```

## 四 Path

>Path描述了绘制路径，用它可以完成很多复杂的图形绘制。

我们再来看看Path里的方法。

### 4.1 添加图形

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

### 4.2 画线（直线或者曲线）

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

>贝塞尔曲线：贝塞尔曲线是几何上的一种曲线。它通过起点、控制点和终点来描述一条曲线，主要用于计算机图形学。简单来说，贝塞尔曲线就是将任意一条曲线转换为精确的数学公式。

在贝塞尔曲线中，有两类点：

- 数据点：一般指一条路径的起点与终点。
- 控制点：控制点决定了路径的弯曲轨迹，根据控制点的个数，贝塞尔曲线分为：一阶贝塞尔曲线（0个控制点），二阶贝塞尔曲线（1个控制点），三阶贝塞尔曲线（2个控制点）等。

一阶贝塞尔曲线

![](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/ui/bezier_cure_1_formula.svg)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_1_demo.gif"/>

B(t)为时间为t时的坐标，P0为起点，P1为终点。

二阶贝塞尔曲线

![](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/ui/bezier_cure_2_formula.svg)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_2_demo.gif"/>

三阶贝塞尔曲线

![](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/art/app/ui/bezier_cure_3_formula.svg)

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_cure_3_demo.gif"/>

贝塞尔曲线的模拟可以使用[bezier-curve](http://myst729.github.io/bezier-curve/)

我们再来看看Path类提供的关于贝塞尔曲线的方法。

```java

//二阶贝塞尔曲线，绝对坐标，(x1, y1)表示控制点，(x2, y2)表示终点
public void quadTo(float x1, float y1, float x2, float y2) {
    isSimplePath = false;
    native_quadTo(mNativePath, x1, y1, x2, y2);
}

//二阶贝塞尔曲线，相对坐标
public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
    isSimplePath = false;
    native_rQuadTo(mNativePath, dx1, dy1, dx2, dy2);
}

//三阶贝塞尔曲线，绝对坐标，(x1, y1)、(x2, y2)表示控制点，(x3, y3)表示终点
public void cubicTo(float x1, float y1, float x2, float y2,
                    float x3, float y3) {
    isSimplePath = false;
    native_cubicTo(mNativePath, x1, y1, x2, y2, x3, y3);
}

//三阶贝塞尔曲线，相对坐标
public void rCubicTo(float x1, float y1, float x2, float y2,
                     float x3, float y3) {
    isSimplePath = false;
    native_rCubicTo(mNativePath, x1, y1, x2, y2, x3, y3);
}
```

我们来用贝塞尔曲线实现一个杯中倒水效果。

举例

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/bezier_wave.gif" width="260" height="500"/>

```java
/**
 * 控制点的X坐标不断左右移动，形成波浪效果。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/11 下午6:11
 */
public class WaveView extends View {

    private static final String TAG = "WaveView";

    /**
     * 波浪从屏幕外开始，在屏幕外结束，这样效果更真实
     */
    private static final float EXTRA_DISTANCE = 200;

    private Path mPath;
    private Paint mPaint;

    /**
     * 控件宽高
     */
    private int mWidth;
    private int mHeight;

    /**
     * 控制点坐标
     */
    private float mControlX;
    private float mControlY;

    /**
     * 波浪峰值
     */
    private float mWaveY;

    /**
     * 是否移动控制点
     */
    private boolean mMoveControl = true;

    public WaveView(Context context) {
        super(context);
        init();
    }

    public WaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;

        mControlY = mHeight - mHeight / 8;
        mWaveY = mHeight - mHeight / 32;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //波浪从屏幕外开始，效果更真实
        mPath.moveTo(-EXTRA_DISTANCE, mWaveY);
        //二阶贝塞尔曲线
        mPath.quadTo(mControlX, mControlY, mWidth + EXTRA_DISTANCE, mWaveY);
        //闭合曲线
        mPath.lineTo(mWidth, mHeight);
        mPath.lineTo(0, mHeight);
        mPath.close();
        canvas.drawPath(mPath, mPaint);

        //mControlX坐标在 -EXTRA_DISTANCE ~ mWidth + EXTRA_DISTANCE 范围内，先自增再自减，左右移动
        //形成波浪效果
        if (mControlX <= -EXTRA_DISTANCE) {
            mMoveControl = true;
        } else if (mControlX >= mWidth + EXTRA_DISTANCE) {
            mMoveControl = false;
        }
        mControlX = mMoveControl ? mControlX + 20 : mControlX - 20;

        //水面不断上升
        if (mControlY >= 0) {
            mControlY -= 2;
            mWaveY -= 2;
        }

        Log.d(TAG, "mControlX: " + mControlX + " mControlY: " + mControlY + " mWaveY: " + mWaveY);

        mPath.reset();
        invalidate();
    }


    private void init() {
        mPath = new Path();
        mPaint = new Paint();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(Color.parseColor("#4CAF50"));
    }
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
- boolean forceMoveTo)：是否留下移动的痕迹file

>注：可以发现，这个方法与同样用来画弧线的方法Canvas.drawArc()少了个boolean useCenter参数，这是因为arcTo()方法只用来画弧线。

### 4.3 辅助设置和计算

**public void setFillType(FillType ft) - 设置填充方式**

方法用来设置填充方式，填充的方式有四种：

- WINDING：non-zero winding rule，非零环绕数原则，
- EVEN_ODD：even-odd rule，奇偶原则
- INVERSE_WINDING：WINDING的反转
- INVERSE_EVEN_ODD：EVEN_ODD的反转

>WINDING：non-zero winding rule，非零环绕数原则，该原则基于所有图形的绘制都有绘制方向（前面提到的Direction描述的顺时针与逆向时针），对于平面上的任意一点，向任意方向射出一条射线，射线遇到每个顺时针
的交点则加1，遇到逆时针的交点则减1，最后的结果如果不为0，则认为该点在图形内部，染色。如果结果为0，则认为该点在图形外部，不染色。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/fill_type_winding.jpg"/>

>EVEN_ODD：even-odd rule，奇偶原则，对于平面上的任意一点，向任意方向射出一条射线，这条射线与图形相交（不是相切）的次数为奇数则说明这个点在图形内部，则进行染色。若为偶数则认为在图形外部，不进行染色。
这是一中交叉染色的情况。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/fill_type_even_odd.jpg"/>