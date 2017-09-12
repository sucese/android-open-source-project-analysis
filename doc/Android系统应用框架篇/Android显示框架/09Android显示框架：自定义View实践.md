# Android显示框架：自定义View实践

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

**文章目录**

- 一 Paint
- 二 Canvas
- 三 Path

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

>注：我们通常通过重写onDraw()方法来绘制界面上的内容，如果需要绘制前景内容则重写onDrawForeground(Canvas canvas) 方法。

## 一 Paint

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

### 第一组：颜色

给Paint设置颜色有两种方案

**public void setColor(@ColorInt int color)**
**public void setARGB(int a, int r, int g, int b) **

设置颜色

**public Shader setShader(Shader shader) **

设置着色器，着色器是图像领域的一个通用概念，它提供的是一套着色规则，具体由Shader的子类实现：

LinearGradient - 线性渐变


RadialGradient 
SweepGradient 
BitmapShader 
ComposeShader


### 第二组：效果
### 第三组：文字
### 第四组：初始化

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

**位图**

- **public void drawBitmap(@NonNull Bitmap bitmap, float left, float top, @Nullable Paint paint) - 绘制位图**
- **public void drawBitmapMesh(@NonNull Bitmap bitmap, int meshWidth, int meshHeight,
             @NonNull float[] verts, int vertOffset, @Nullable int[] colors, int colorOffset,
             @Nullable Paint paint) - 绘制拉伸位图**

第一个方法很简单，就是在指定的坐标处开始绘制位图。我们着重来看看第二个方法，这个方法不是很常用（可能是计算比较复杂的锅😓），但这并不影响它强大的功能。

drawBitmapMesh()方法将位图分为若干网格，然后对每个网格进行扭曲处理。我们先来看看这个方法的参数：

@NonNull Bitmap bitmap：源位图
int meshWidth：横向上将源位图划分成多少格
int meshHeight：纵向上将源位图划分成多少格
@NonNull float[] verts：网格顶点坐标数组，记录扭曲后图片各顶点的坐标，数组大小为 (meshWidth+1) * (meshHeight+1) * 2 + vertOffset
int vertOffset：记录verts数组从第几个数组元素开始扭曲
@Nullable int[] colors：设置网格顶点的颜色，该颜色会和位图对应像素的颜色叠加，数组大小为 (meshWidth+1) * (meshHeight+1) + colorOffset
int colorOffset：记录colors从几个数组元素开始取色
@Nullable Paint paint：画笔

我们来用drawBitmapMesh()方法实现一个水播放的效果。

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

## 2.2 位置转换

Canvas还提供了对象的位置变换的方法，其中包括：

- 旋转（Rotate）
- 缩放（Scale）
- 平移（Translate）
- 扭曲（Skew）

下面我们来分析一下Path类。

## 三 Path

>Path描述了绘制路径，用它可以完成很多复杂的图形绘制。

我们再来看看Path里的方法。

### 第一组：addXXX() - 添加完整的封闭图形

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

### 第二组：xxxTo() - 画线（直线或者曲线）

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

我们来用贝塞尔曲线实现一个波浪效果。

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

### 第三组：辅助设置和计算方法

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