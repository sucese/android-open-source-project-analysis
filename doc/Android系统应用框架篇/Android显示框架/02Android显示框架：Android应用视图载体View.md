# Android显示框架：Android应用视图的载体View

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

- 一 View生命周期
- 二 View的测量、布局与绘制流程

> This class represents the basic building block for user interface components. A Viewoccupies a rectangular area on the screen and is 
responsible for drawing and event handling.

View是屏幕上的一块矩形区域，负责界面的绘制与触摸事件的处理。

## 一 View生命周期

在View中有诸多回调方法，它们在View的不同生命周期阶段调用，常用的有以下方法。

我们写一个简单的自定义View来观察View与Activity的生命周期变化。

```java
public class CustomView extends View {

    private static final String TAG = "View";

    public CustomView(Context context) {
        super(context);
        Log.d(TAG, "CustomView()");
    }

    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "CustomView()");
    }

    public CustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "CustomView()");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "View onFinishInflate()");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(TAG, "View onMeasure()");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d(TAG, "View onLayout() left = " + left + " top = " + top + " right = " + right + " bottom = " + bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "View onSizeChanged() w = " + w + " h = " + h + " oldw = " + oldw + " oldh = " + oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "View onDraw()");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "View onKeyDown() event = " + event.getAction());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "View onKeyUp() event = " + event.getAction());
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "View onTouchEvent() event =  " + event.getAction());
        return super.onTouchEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        Log.d(TAG, "View onFocusChanged() gainFocus = " + gainFocus);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        Log.d(TAG, "View onWindowFocusChanged() hasWindowFocus = " + hasWindowFocus);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "View onAttachedToWindow()");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "View onDetachedFromWindow()");
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Log.d(TAG, "View onVisibilityChanged() visibility = " + visibility);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        Log.d(TAG, "View onWindowVisibilityChanged() visibility = " + visibility);
    }
```

```java
public class ViewActivity extends AppCompatActivity {

    private static final String TAG = "View";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Activity onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Activity onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity onDestroy()");
    }
}
```

Activity与View的生命周期变化一目了然。

Activity create

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/view_lifecycle_create.png"/>

Activity pause

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/view_lifecycle_pause.png"/>

Activity resume

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/measure_sequence.png"/>

Activity destory

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/view_lifecycle_resume.png"/>


<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/view_lifecycle.png"/>

## 二 View的测量、布局与绘制流程

在上篇文章[04Android显示框架：Activity应用视图的创建流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/04Android显示框架：Activity应用视图的创建流程.md)
中我们分析了Activity应用视图的创建流程，这样我们便可以进行UI的绘制了。一个Android应用窗口里包含了很多UI元素，它们是以树形结构来组织的，即父子关系。在绘制UI的过程中，我们
要先确定父UI元素的大小及位置，再确定子UI元素的大小及位置，才能进行绘制。

View的绘制流程从ViewRoot.performTraversals()开始，整个流程分为三步：

1. measure：测量View的宽高
2. layout：确定View在父容器里的位置
3. draw：将View绘制在屏幕上

在上文创建View对象这一步中我们提到，Android应用窗口的顶层视图是一个类型为DecorView的UI元素，该顶层视图是由ViewRoot.performTraversals()方法来进行测量、布局与绘制操作。

### 2.1 测量流程

>Measure过程决定了View的宽高，该过程完成后，通常都可以通过getMeasuredWith()/getMeasuredHeight()获得宽高。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/measure_sequence.png" height="500"/>

在介绍测量流程之前，我们先来介绍下MeasureSpec，它用来描述宽高。它是一个32位int值。

- 高2位：SpecMode，测量模式
- 低30位：SpecSize，在特定测量模式下的大小

测量模式有三种：

```java
public static class MeasureSpec {
    
    private static final int MODE_SHIFT = 30;
    private static final int MODE_MASK  = 0x3 << MODE_SHIFT;
    
    //父容器不对View做任何限制，需要多打给多大，这种情况一般用于系统内部，表示一种测量的状态
    public static final int UNSPECIFIED = 0 << MODE_SHIFT;
    
    //父容器已经检测出View所需要的精确大小，这个时候View的最终大小就是SpecSize所指定的值，它对应LayoutParams中的match_parent和具体数值这两种模式
    public static final int EXACTLY     = 1 << MODE_SHIFT;
    
    //父容器指定了一个可用大小SpecSize，View的大小不能大于这个值，它对应于LayoutParams中的wrap_content
    public static final int AT_MOST     = 2 << MODE_SHIFT;  
}
```
为什么会这么描述这三种模式的含义呢，这一点可以从ViewRoot.getRootMeasureSpec()方法中看出来，这一组MeasureSpec计算完成后传给了measure()方法。

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
    
        private int getRootMeasureSpec(int windowSize, int rootDimension) {
            int measureSpec;
            switch (rootDimension) {
    
            case ViewGroup.LayoutParams.MATCH_PARENT:
                // Window can't resize. Force root view to be windowSize.
                measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.EXACTLY);
                break;
            case ViewGroup.LayoutParams.WRAP_CONTENT:
                // Window can resize. Set max size for root view.
                measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.AT_MOST);
                break;
            default:
                // Window wants to be an exact size. Force root view to be that size.
                measureSpec = MeasureSpec.makeMeasureSpec(rootDimension, MeasureSpec.EXACTLY);
                break;
            }
            return measureSpec;
        }
}
```
我们再来看看View的MeasureSpec是如果获取的。

```java
public abstract class ViewGroup extends View implements ViewParent, ViewManager {
    
     public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
            int specMode = MeasureSpec.getMode(spec);
            int specSize = MeasureSpec.getSize(spec);
    
            int size = Math.max(0, specSize - padding);
    
            int resultSize = 0;
            int resultMode = 0;
    
            switch (specMode) {
            // Parent has imposed an exact size on us
            case MeasureSpec.EXACTLY:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size. So be it.
                    resultSize = size;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
    
            // Parent has imposed a maximum size on us
            case MeasureSpec.AT_MOST:
                if (childDimension >= 0) {
                    // Child wants a specific size... so be it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size, but our size is not fixed.
                    // Constrain child to not be bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
    
            // Parent asked to see how big we want to be
            case MeasureSpec.UNSPECIFIED:
                if (childDimension >= 0) {
                    // Child wants a specific size... let him have it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size... find out how big it should
                    // be
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size.... find out how
                    // big it should be
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
            }
            return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
        }
        
}
```
该方法用来获取子View的MeasureSpec，由参数我们就可以知道子View的MeasureSpec由父容器的spec，父容器中已占用的的空间大小
padding，以及子View自身大小childDimension共同来决定的。

通过上述方法，我们可以总结出普通View的MeasureSpec的创建规则。

- 当View采用固定宽高的时候，不管父容器的MeasureSpec是什么，resultSize都是指定的宽高，resultMode都是MeasureSpec.EXACTLY。
- 当View的宽高是match_parent，当父容器是MeasureSpec.EXACTLY，则View也是MeasureSpec.EXACTLY，并且其大小就是父容器的剩余空间。当父容器是MeasureSpec.AT_MOST
则View也是MeasureSpec.AT_MOST，并且大小不会超过父容器的剩余空间。
- 当View的宽高是wrap_content时，不管父容器的模式是MeasureSpec.EXACTLY还是MeasureSpec.AT_MOST，View的模式总是MeasureSpec.AT_MOST，并且大小都不会超过芙蓉的剩余空间。


了解了MeasureSpec的概念之后，我就就可以开始分析测量流程了。

- 对于顶级View（DecorView）其MeasureSpec由窗口的尺寸和自身的LayoutParams共同确定的。
- 对于普通View其MeasureSpec由父容器的Measure和自身的LayoutParams共同确定的。

对于我们来说，测量流程中最熟悉的方法要属onMeasure()方法了，我们经常在自定义View时重写这个方法。我们来看几个不同组件的onMeasure()方法的实现。

**关键点1：View.onMeasure(int widthMeasureSpec, int heightMeasureSpec)**

```java
public class View implements Drawable.Callback, KeyEvent.Callback, AccessibilityEventSource {
    
       protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
           setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                   getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
       }
       
       //measureSpec指的是View测量后的大小
       public static int getDefaultSize(int size, int measureSpec) {
           int result = size;
           int specMode = MeasureSpec.getMode(measureSpec);
           int specSize =  MeasureSpec.getSize(measureSpec);
   
           switch (specMode) {
           case MeasureSpec.UNSPECIFIED:
               result = size;
               break;
           case MeasureSpec.AT_MOST:
           case MeasureSpec.EXACTLY:
               result = specSize;
               break;
           }
           return result;
       }
       
       //mMinHeight对应于android:minHeight，它可能为0，如果View指定了背景，则suggestedMinHeight为max(mMinHeight, bgMinHeight)
       protected int getSuggestedMinimumHeight() {
           int suggestedMinHeight = mMinHeight;
   
           if (mBGDrawable != null) {
               final int bgMinHeight = mBGDrawable.getMinimumHeight();
               if (suggestedMinHeight < bgMinHeight) {
                   suggestedMinHeight = bgMinHeight;
               }
           }
   
           return suggestedMinHeight;
       }
}
```
View的onMeasure()方法实现比较简单，从它的实现可以看出，View的宽高由specSize来决定，这里我们思考一个问题：如果继承了View却没有重写其onMeasure()方法
会发生什么？

答案是：在布局里使用wrap_content就相当于是match_parent。

从上面的描述我们可以知道当使用wrap_content时，View的specMode是MeasureSpec.AT_MOST，这种情况下它的宽高是specSize，即parentSize，也就是总是会填充
当前父容器的剩余空间，这就相当于match_parent。

**关键点2：FrameLayout.onMeasure(int widthMeasureSpec, int heightMeasureSpec)** 

View.onMeasure()方法的具体实现一般是由其子类来完成的，对于应用窗口的顶级视图DecorView来说，它继承于FrameLayout，我们来看看FrameLayout.onMeasure()
方法的实现。

```java
public class FrameLayout extends ViewGroup {
    
       @Override
       protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
           final int count = getChildCount();
   
           int maxHeight = 0;
           int maxWidth = 0;
   
           // Find rightmost and bottommost child
           for (int i = 0; i < count; i++) {
               final View child = getChildAt(i);
               if (mMeasureAllChildren || child.getVisibility() != GONE) {
                   measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                   maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                   maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
               }
           }
   
           // Account for padding too
           maxWidth += mPaddingLeft + mPaddingRight + mForegroundPaddingLeft + mForegroundPaddingRight;
           maxHeight += mPaddingTop + mPaddingBottom + mForegroundPaddingTop + mForegroundPaddingBottom;
   
           // Check against our minimum height and width
           maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
           maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
   
           // Check against our foreground's minimum height and width
           final Drawable drawable = getForeground();
           if (drawable != null) {
               maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
               maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
           }
   
           setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
                   resolveSize(maxHeight, heightMeasureSpec));
       }
       
      public static int resolveSize(int size, int measureSpec) {
           int result = size;
           int specMode = MeasureSpec.getMode(measureSpec);
           int specSize =  MeasureSpec.getSize(measureSpec);
           switch (specMode) {
           case MeasureSpec.UNSPECIFIED:
               result = size;
               break;
           case MeasureSpec.AT_MOST:
               result = Math.min(size, specSize);
               break;
           case MeasureSpec.EXACTLY:
               result = specSize;
               break;
           }
           return result;
       }
}
```

可以看到该方法主要做了以下事情：

1. 调用measureChildWithMargins()去测量每一个子View的大小，找到最大高度和宽度保存在maxWidth/maxHeigth中。
2. 将上一步计算的maxWidth/maxHeigth加上padding值，mPaddingLeft，mPaddingRight，mPaddingTop ，mPaddingBottom表示当前内容区域的左右上下四条边分别到当前视图的左右上下四条边的距离，
mForegroundPaddingLeft ，mForegroundPaddingRight，mForegroundPaddingTop ，mForegroundPaddingBottom表示当前视图的各个子视图所围成的区域的左右上下四条边到当前视图前景区域的
左右上下四条边的距离，经过计算获得最终宽高。
3. 当前视图是否设置有最小宽度和高度。如果设置有的话，并且它们比前面计算得到的宽度maxWidth和高度maxHeight还要大，那么就将它们作为当前视图的宽度和高度值。
4. 当前视图是否设置有前景图。如果设置有的话，并且它们比前面计算得到的宽度maxWidth和高度maxHeight还要大，那么就将它们作为当前视图的宽度和高度值。
5. 经过以上的计算，就得到了正确的宽高，先调用resolveSize()方法，获取MeasureSpec，接着调用父类的setMeasuredDimension()方法将它们作为当前视图的大小。

我们再来看看resolveSize(int size, int measureSpec)方法是如果获取MeasureSpec的？

这个方法的两个参数：int size：前面计算出的最大宽/高，int measureSpec父视图指定的MeasureSpec，它们按照：

- MeasureSpec.UNSPECIFIED: 取size
- MeasureSpec.AT_MOST: 取size, specSize的最小值
- MeasureSpec.EXACTLY: 取specSize

来生成最后的大小。

以上便是Measure的整个流程，该流程完成以后，我们可以通过getMeasuredWidth()与getMeasuredHeight()来获得View的宽高。但是在某些情况下，系统需要经过多次Measure才能确定
最终的宽高，因此在onMeasure()方法中拿到的宽高很可能是不正确的，比较好的做法是在onLayout()方法中获取View的宽高。

### 2.2 布局流程

>Layout过程决定了View的父容器里四个坐标点的位置，该过程完成后可以通过getTop()、getBottom()、getLeft()和getRight()来拿到View四个顶点的位置，并可以通过
getWidth()/getHeigth()获得View的最终宽高。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/layout_sequence.png" height="500"/>

从上面的序列图可知，View.layout()方法会首先调用setFrame()方法来设置当前视图的位置与大小，设置完成之后，如果当前视图的大小或者位置发生了变化，则调用onLayout()重新布局。

**关键点1：View.invalidate()**

```java
public class View implements Drawable.Callback, KeyEvent.Callback, AccessibilityEventSource {
    
    public void invalidate() {
        if (ViewDebug.TRACE_HIERARCHY) {
            ViewDebug.trace(this, ViewDebug.HierarchyTraceType.INVALIDATE);
        }

        //检查mPrivateFlags的DRAWN位与HAS_BOUNDS是否被置1，说明上一次请求执行的UI绘制已经完成了，这个时候才能执行新的UI绘制操作
        if ((mPrivateFlags & (DRAWN | HAS_BOUNDS)) == (DRAWN | HAS_BOUNDS)) {
            //将mPrivateFlags的DRAWN位与HAS_BOUNDS是否被置0
            mPrivateFlags &= ~DRAWN & ~DRAWING_CACHE_VALID;
            final ViewParent p = mParent;
            final AttachInfo ai = mAttachInfo;
            if (p != null && ai != null) {
                final Rect r = ai.mTmpInvalRect;
                r.set(0, 0, mRight - mLeft, mBottom - mTop);
                // Don't call invalidate -- we don't want to internally scroll
                // our own bounds
                p.invalidateChild(this, r);
            }
        }
    }
}
```
该方法检查mPrivateFlags的DRAWN位与HAS_BOUNDS是否被置1，说明上一次请求执行的UI绘制已经完成了，这个时候才能执行新的UI绘制操作，在执行新的UI绘制操作之前，还会将
这两个标志位置0，然后调用ViewParent.invalidateChild()方法来完成绘制操作，这个ViewParent指向的是ViewRoot对象。

**关键点2：FrameLayout.onLayout(boolean changed, int left, int top, int right, int bottom)** 

onLayout的实现依赖于具体的布局，所以View/ViewGroup并没有实现这个方法，我们来看看FrameLayout的实现。

```java
public class FrameLayout extends ViewGroup {
    
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int count = getChildCount();
    
            final int parentLeft = mPaddingLeft + mForegroundPaddingLeft;
            final int parentRight = right - left - mPaddingRight - mForegroundPaddingRight;
    
            final int parentTop = mPaddingTop + mForegroundPaddingTop;
            final int parentBottom = bottom - top - mPaddingBottom - mForegroundPaddingBottom;
    
            mForegroundBoundsChanged = true;
            
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    
                    final int width = child.getMeasuredWidth();
                    final int height = child.getMeasuredHeight();
    
                    int childLeft = parentLeft;
                    int childTop = parentTop;
    
                    final int gravity = lp.gravity;
    
                    if (gravity != -1) {
                        final int horizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
    
                        switch (horizontalGravity) {
                            case Gravity.LEFT:
                                childLeft = parentLeft + lp.leftMargin;
                                break;
                            case Gravity.CENTER_HORIZONTAL:
                                childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                                        lp.leftMargin - lp.rightMargin;
                                break;
                            case Gravity.RIGHT:
                                childLeft = parentRight - width - lp.rightMargin;
                                break;
                            default:
                                childLeft = parentLeft + lp.leftMargin;
                        }
    
                        switch (verticalGravity) {
                            case Gravity.TOP:
                                childTop = parentTop + lp.topMargin;
                                break;
                            case Gravity.CENTER_VERTICAL:
                                childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                                        lp.topMargin - lp.bottomMargin;
                                break;
                            case Gravity.BOTTOM:
                                childTop = parentBottom - height - lp.bottomMargin;
                                break;
                            default:
                                childTop = parentTop + lp.topMargin;
                        }
                    }
    
                    child.layout(childLeft, childTop, childLeft + width, childTop + height);
                }
            }
        }
}
```
我们先来解释一下这个函数里的变量的含义。

- int left, int top, int right, int bottom: 描述的是当前视图的外边距，即它与父窗口的边距。
- mPaddingLeft，mPaddingTop，mPaddingRight，mPaddingBottom: 描述的当前视图的内边距。

通过这些参数，我们就可以得到当前视图的子视图所能布局在的区域。

接着，该方法就会遍历它的每一个子View，并获取它的左上角的坐标位置：childLeft，childTop。这两个位置信息会根据gravity来进行计算。
最后会调用子View的layout()方法循环布局操作，直到所有的布局都完成为止。

### 2.3 绘制流程

>Draw过程最终将View绘制在屏幕上。

<img src="https://github.com/guoxiaoxing/android-open-source-project-analysis/raw/master/art/app/ui/draw_sequence.png" height="500"/>

绘制从ViewRoot.draw()开始，它首先会创建一块画布，接着再在画布上绘制Android上的UI，再把画布的内容交给SurfaceFlinger服务来渲染。

**关键点1：ViewRoot.draw(boolean fullRedrawNeeded)**

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks {
    
    private void draw(boolean fullRedrawNeeded) {
            //surface用来操作应用窗口的绘图表面
            Surface surface = mSurface;
            if (surface == null || !surface.isValid()) {
                return;
            }
    
            if (!sFirstDrawComplete) {
                synchronized (sFirstDrawHandlers) {
                    sFirstDrawComplete = true;
                    for (int i=0; i<sFirstDrawHandlers.size(); i++) {
                        post(sFirstDrawHandlers.get(i));
                    }
                }
            }
            
            scrollToRectOrFocus(null, false);
    
            if (mAttachInfo.mViewScrollChanged) {
                mAttachInfo.mViewScrollChanged = false;
                mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
            }
    
            int yoff;
            //计算窗口是否处于滚动状态
            final boolean scrolling = mScroller != null && mScroller.computeScrollOffset();
            if (scrolling) {
                yoff = mScroller.getCurrY();
            } else {
                yoff = mScrollY;
            }
            if (mCurScrollY != yoff) {
                mCurScrollY = yoff;
                fullRedrawNeeded = true;
            }
            //描述窗口是否正在请求大小缩放
            float appScale = mAttachInfo.mApplicationScale;
            boolean scalingRequired = mAttachInfo.mScalingRequired;
    
            //描述窗口的脏区域，即需要重新绘制的区域
            Rect dirty = mDirty;
            if (mSurfaceHolder != null) {
                // The app owns the surface, we won't draw.
                dirty.setEmpty();
                return;
            }
            
            //用来描述是否需要用OpenGL接口来绘制UI，当应用窗口flag等于WindowManager.LayoutParams.MEMORY_TYPE_GPU
            //则表示需要用OpenGL接口来绘制UI
            if (mUseGL) {
                if (!dirty.isEmpty()) {
                    Canvas canvas = mGlCanvas;
                    if (mGL != null && canvas != null) {
                        mGL.glDisable(GL_SCISSOR_TEST);
                        mGL.glClearColor(0, 0, 0, 0);
                        mGL.glClear(GL_COLOR_BUFFER_BIT);
                        mGL.glEnable(GL_SCISSOR_TEST);
    
                        mAttachInfo.mDrawingTime = SystemClock.uptimeMillis();
                        mAttachInfo.mIgnoreDirtyState = true;
                        mView.mPrivateFlags |= View.DRAWN;
    
                        int saveCount = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                        try {
                            canvas.translate(0, -yoff);
                            if (mTranslator != null) {
                                mTranslator.translateCanvas(canvas);
                            }
                            canvas.setScreenDensity(scalingRequired
                                    ? DisplayMetrics.DENSITY_DEVICE : 0);
                            mView.draw(canvas);
                            if (Config.DEBUG && ViewDebug.consistencyCheckEnabled) {
                                mView.dispatchConsistencyCheck(ViewDebug.CONSISTENCY_DRAWING);
                            }
                        } finally {
                            canvas.restoreToCount(saveCount);
                        }
    
                        mAttachInfo.mIgnoreDirtyState = false;
    
                        mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
                        checkEglErrors();
    
                        if (SHOW_FPS || Config.DEBUG && ViewDebug.showFps) {
                            int now = (int)SystemClock.elapsedRealtime();
                            if (sDrawTime != 0) {
                                nativeShowFPS(canvas, now - sDrawTime);
                            }
                            sDrawTime = now;
                        }
                    }
                }
                
                //如果窗口处于滚动状态，则应用窗口需要马上进行下一次全部重绘，调用scheduleTraversals()方法
                if (scrolling) {
                    mFullRedrawNeeded = true;
                    scheduleTraversals();
                }
                return;
            }
    
            //是否需要全部重绘，如果是则将窗口的脏区域设置为整个窗口区域，表示整个窗口曲云都需要重绘
            if (fullRedrawNeeded) {
                mAttachInfo.mIgnoreDirtyState = true;
                dirty.union(0, 0, (int) (mWidth * appScale + 0.5f), (int) (mHeight * appScale + 0.5f));
            }
    
            if (DEBUG_ORIENTATION || DEBUG_DRAW) {
                Log.v(TAG, "Draw " + mView + "/"
                        + mWindowAttributes.getTitle()
                        + ": dirty={" + dirty.left + "," + dirty.top
                        + "," + dirty.right + "," + dirty.bottom + "} surface="
                        + surface + " surface.isValid()=" + surface.isValid() + ", appScale:" +
                        appScale + ", width=" + mWidth + ", height=" + mHeight);
            }
    
            if (!dirty.isEmpty() || mIsAnimating) {
                Canvas canvas;
                try {
                    int left = dirty.left;
                    int top = dirty.top;
                    int right = dirty.right;
                    int bottom = dirty.bottom;
                    //调用Surface.lockCanvas()来创建画布
                    canvas = surface.lockCanvas(dirty);
    
                    if (left != dirty.left || top != dirty.top || right != dirty.right ||
                            bottom != dirty.bottom) {
                        mAttachInfo.mIgnoreDirtyState = true;
                    }
    
                    // TODO: Do this in native
                    canvas.setDensity(mDensity);
                } catch (Surface.OutOfResourcesException e) {
                    Log.e(TAG, "OutOfResourcesException locking surface", e);
                    // TODO: we should ask the window manager to do something!
                    // for now we just do nothing
                    return;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "IllegalArgumentException locking surface", e);
                    // TODO: we should ask the window manager to do something!
                    // for now we just do nothing
                    return;
                }
    
                try {
                    if (!dirty.isEmpty() || mIsAnimating) {
                        long startTime = 0L;
    
                        if (DEBUG_ORIENTATION || DEBUG_DRAW) {
                            Log.v(TAG, "Surface " + surface + " drawing to bitmap w="
                                    + canvas.getWidth() + ", h=" + canvas.getHeight());
                            //canvas.drawARGB(255, 255, 0, 0);
                        }
    
                        if (Config.DEBUG && ViewDebug.profileDrawing) {
                            startTime = SystemClock.elapsedRealtime();
                        }
    
                        // If this bitmap's format includes an alpha channel, we
                        // need to clear it before drawing so that the child will
                        // properly re-composite its drawing on a transparent
                        // background. This automatically respects the clip/dirty region
                        // or
                        // If we are applying an offset, we need to clear the area
                        // where the offset doesn't appear to avoid having garbage
                        // left in the blank areas.
                        if (!canvas.isOpaque() || yoff != 0) {
                            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        }
    
                        dirty.setEmpty();
                        mIsAnimating = false;
                        mAttachInfo.mDrawingTime = SystemClock.uptimeMillis();
                        mView.mPrivateFlags |= View.DRAWN;
    
                        if (DEBUG_DRAW) {
                            Context cxt = mView.getContext();
                            Log.i(TAG, "Drawing: package:" + cxt.getPackageName() +
                                    ", metrics=" + cxt.getResources().getDisplayMetrics() +
                                    ", compatibilityInfo=" + cxt.getResources().getCompatibilityInfo());
                        }
                        int saveCount = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                        try {
                            canvas.translate(0, -yoff);
                            if (mTranslator != null) {
                                mTranslator.translateCanvas(canvas);
                            }
                            canvas.setScreenDensity(scalingRequired
                                    ? DisplayMetrics.DENSITY_DEVICE : 0);
                            mView.draw(canvas);
                        } finally {
                            mAttachInfo.mIgnoreDirtyState = false;
                            canvas.restoreToCount(saveCount);
                        }
    
                        if (Config.DEBUG && ViewDebug.consistencyCheckEnabled) {
                            mView.dispatchConsistencyCheck(ViewDebug.CONSISTENCY_DRAWING);
                        }
    
                        if (SHOW_FPS || Config.DEBUG && ViewDebug.showFps) {
                            int now = (int)SystemClock.elapsedRealtime();
                            if (sDrawTime != 0) {
                                nativeShowFPS(canvas, now - sDrawTime);
                            }
                            sDrawTime = now;
                        }
    
                        if (Config.DEBUG && ViewDebug.profileDrawing) {
                            EventLog.writeEvent(60000, SystemClock.elapsedRealtime() - startTime);
                        }
                    }
    
                } finally {
                    //UI绘制完成后，调用urface.unlockCanvasAndPost(canvas)S来请求SurfaceFlinger进行UI的渲染
                    surface.unlockCanvasAndPost(canvas);
                }
            }
    
            if (LOCAL_LOGV) {
                Log.v(TAG, "Surface " + surface + " unlockCanvasAndPost");
            }
    
            if (scrolling) {
                mFullRedrawNeeded = true;
                scheduleTraversals();
            }
        }
}
```

这个函数主要做了以下事情：

1. 调用Scroller.computeScrollOffset()方法计算应用是否处于滑动状态，并获得应用窗口在Y轴上的即时滑动位置yoff。
2. 根据AttachInfo里描述的数据，判断窗口是否需要缩放。
3. 根据成员变量React mDirty的描述来判断窗口脏区域的大小，脏区域指的是需要全部重绘的窗口区域。
4. 根据成员变量boolean mUserGL判断是否需要用OpenGL接口来绘制UI，当应用窗口flag等于WindowManager.LayoutParams.MEMORY_TYPE_GPU则表示需要用OpenGL接口来绘制UI.
5. 如果不是用OpenGL来绘制，则用Surface来绘制，先调用Surface.lockCanvas()来创建画布，UI绘制完成后，再调用urface.unlockCanvasAndPost(canvas)S来请求SurfaceFlinger进行UI的渲染

注：这里的Surface对象对应了C++层里的Surface对象，真正的功能在C++层，关于C++层的实现，我们会在后续的文章进一步分析。

**关键点2：View.draw(Canvas canvas)**

```java
public class View implements Drawable.Callback, KeyEvent.Callback, AccessibilityEventSource {
    
    public void draw(Canvas canvas) {
            if (ViewDebug.TRACE_HIERARCHY) {
                ViewDebug.trace(this, ViewDebug.HierarchyTraceType.DRAW);
            }
    
            final int privateFlags = mPrivateFlags;
            //dirtyOpaque用来描述当前绘制，它有两种情况：1 检查DIRTY_OPAQUE为是否为1，如果是则说明当前视图某个子视图请求了一个不透明的UI绘制操作，此时当前
            //视图会被子视图覆盖 2 如果mAttachInfo.mIgnoreDirtyState = true则表示忽略该标志位
            final boolean dirtyOpaque = (privateFlags & DIRTY_MASK) == DIRTY_OPAQUE &&
                    (mAttachInfo == null || !mAttachInfo.mIgnoreDirtyState);
            
            //将DIRTY_MASK与DRAWN置为1，表示开始绘制
            mPrivateFlags = (privateFlags & ~DIRTY_MASK) | DRAWN;
           
    
            /*
             * Draw traversal performs several drawing steps which must be executed
             * in the appropriate order:
             *
             *      1. Draw the background
             *      2. If necessary, save the canvas' layers to prepare for fading
             *      3. Draw view's content
             *      4. Draw children
             *      5. If necessary, draw the fading edges and restore layers
             *      6. Draw decorations (scrollbars for instance)
             */
    
            // Step 1, draw the background, if needed
            int saveCount;
    
            if (!dirtyOpaque) {
                //绘制当前视图的背景
                final Drawable background = mBGDrawable;
                if (background != null) {
                    final int scrollX = mScrollX;
                    final int scrollY = mScrollY;
    
                    if (mBackgroundSizeChanged) {
                        background.setBounds(0, 0,  mRight - mLeft, mBottom - mTop);
                        mBackgroundSizeChanged = false;
                    }
    
                    if ((scrollX | scrollY) == 0) {
                        background.draw(canvas);
                    } else {
                        canvas.translate(scrollX, scrollY);
                        background.draw(canvas);
                        canvas.translate(-scrollX, -scrollY);
                    }
                }
            }
    
            //检查是否可以跳过第2步和第5步，也就是绘制变量，FADING_EDGE_HORIZONTAL == 1表示处于水平
            //滑动状态，则需要绘制水平边框渐变效果，FADING_EDGE_VERTICAL == 1表示处于垂直滑动状态，则
            //需要绘制垂直边框渐变效果。
            // skip step 2 & 5 if possible (common case)
            final int viewFlags = mViewFlags;
            boolean horizontalEdges = (viewFlags & FADING_EDGE_HORIZONTAL) != 0;
            boolean verticalEdges = (viewFlags & FADING_EDGE_VERTICAL) != 0;
            if (!verticalEdges && !horizontalEdges) {
                
                //窗口内容不透明才开始绘制，透明的时候就无需绘制了
                // Step 3, draw the content
                if (!dirtyOpaque) onDraw(canvas);
    
                // Step 4, draw the children
                dispatchDraw(canvas);
    
                // Step 6, draw decorations (scrollbars)
                onDrawScrollBars(canvas);
    
                // we're done...
                return;
            }
    
            /*
             * Here we do the full fledged routine...
             * (this is an uncommon case where speed matters less,
             * this is why we repeat some of the tests that have been
             * done above)
             */
            //检查失修需要保存参数canvas所描述的一块画布的堆栈状态，并且创建额外的图层来绘制当前视图
            //在滑动时的边框渐变效果
            boolean drawTop = false;
            boolean drawBottom = false;
            boolean drawLeft = false;
            boolean drawRight = false;
    
            float topFadeStrength = 0.0f;
            float bottomFadeStrength = 0.0f;
            float leftFadeStrength = 0.0f;
            float rightFadeStrength = 0.0f;
    
            // Step 2, save the canvas' layers
            int paddingLeft = mPaddingLeft;
            int paddingTop = mPaddingTop;
    
            final boolean offsetRequired = isPaddingOffsetRequired();
            if (offsetRequired) {
                paddingLeft += getLeftPaddingOffset();
                paddingTop += getTopPaddingOffset();
            }
    
            //表示当前视图可以用来绘制的内容区域，这个区域已经将内置的和扩展的内边距排除之外
            int left = mScrollX + paddingLeft;
            int right = left + mRight - mLeft - mPaddingRight - paddingLeft;
            int top = mScrollY + paddingTop;
            int bottom = top + mBottom - mTop - mPaddingBottom - paddingTop;
    
            if (offsetRequired) {
                right += getRightPaddingOffset();
                bottom += getBottomPaddingOffset();
            }
    
            final ScrollabilityCache scrollabilityCache = mScrollCache;
            int length = scrollabilityCache.fadingEdgeLength;
    
            // clip the fade length if top and bottom fades overlap
            // overlapping fades produce odd-looking artifacts
            if (verticalEdges && (top + length > bottom - length)) {
                length = (bottom - top) / 2;
            }
    
            // also clip horizontal fades if necessary
            if (horizontalEdges && (left + length > right - length)) {
                length = (right - left) / 2;
            }
    
            if (verticalEdges) {
                topFadeStrength = Math.max(0.0f, Math.min(1.0f, getTopFadingEdgeStrength()));
                drawTop = topFadeStrength >= 0.0f;
                bottomFadeStrength = Math.max(0.0f, Math.min(1.0f, getBottomFadingEdgeStrength()));
                drawBottom = bottomFadeStrength >= 0.0f;
            }
    
            if (horizontalEdges) {
                leftFadeStrength = Math.max(0.0f, Math.min(1.0f, getLeftFadingEdgeStrength()));
                drawLeft = leftFadeStrength >= 0.0f;
                rightFadeStrength = Math.max(0.0f, Math.min(1.0f, getRightFadingEdgeStrength()));
                drawRight = rightFadeStrength >= 0.0f;
            }
    
            saveCount = canvas.getSaveCount();
    
            int solidColor = getSolidColor();
            if (solidColor == 0) {
                final int flags = Canvas.HAS_ALPHA_LAYER_SAVE_FLAG;
    
                if (drawTop) {
                    canvas.saveLayer(left, top, right, top + length, null, flags);
                }
    
                if (drawBottom) {
                    canvas.saveLayer(left, bottom - length, right, bottom, null, flags);
                }
    
                if (drawLeft) {
                    canvas.saveLayer(left, top, left + length, bottom, null, flags);
                }
    
                if (drawRight) {
                    canvas.saveLayer(right - length, top, right, bottom, null, flags);
                }
            } else {
                scrollabilityCache.setFadeColor(solidColor);
            }
    
            // Step 3, draw the content
            if (!dirtyOpaque) onDraw(canvas);
    
            // Step 4, draw the children
            dispatchDraw(canvas);
    
            //绘制当前视图的上下左右边框的渐变效果
            // Step 5, draw the fade effect and restore layers
            final Paint p = scrollabilityCache.paint;
            final Matrix matrix = scrollabilityCache.matrix;
            final Shader fade = scrollabilityCache.shader;
            final float fadeHeight = scrollabilityCache.fadingEdgeLength;
    
            if (drawTop) {
                matrix.setScale(1, fadeHeight * topFadeStrength);
                matrix.postTranslate(left, top);
                fade.setLocalMatrix(matrix);
                canvas.drawRect(left, top, right, top + length, p);
            }
    
            if (drawBottom) {
                matrix.setScale(1, fadeHeight * bottomFadeStrength);
                matrix.postRotate(180);
                matrix.postTranslate(left, bottom);
                fade.setLocalMatrix(matrix);
                canvas.drawRect(left, bottom - length, right, bottom, p);
            }
    
            if (drawLeft) {
                matrix.setScale(1, fadeHeight * leftFadeStrength);
                matrix.postRotate(-90);
                matrix.postTranslate(left, top);
                fade.setLocalMatrix(matrix);
                canvas.drawRect(left, top, left + length, bottom, p);
            }
    
            if (drawRight) {
                matrix.setScale(1, fadeHeight * rightFadeStrength);
                matrix.postRotate(90);
                matrix.postTranslate(right, top);
                fade.setLocalMatrix(matrix);
                canvas.drawRect(right - length, top, right, bottom, p);
            }
    
            canvas.restoreToCount(saveCount);
    
            //绘制当前视图的滚动条
            // Step 6, draw decorations (scrollbars)
            onDrawScrollBars(canvas);
        }
}
```
该方法主要完成了以下事情：

1. 绘制当前视图的背景
2. 保存当前画布的状态，并且在当前画布创建额外的突出，以便接下来可以绘制视图在滑动时的边框渐变效果。
3. 绘制当前视图的内容
4. 绘制当前视图的子视图
5. 绘制当前视图在滑动时的边框渐变效果
6. 绘制当前视图的滚动条

**关键点2：ViewGroup.dispatchDraw(Canvas canvas)**

dispatchDraw用来循环绘制子View视图。

```java
public abstract class ViewGroup extends View implements ViewParent, ViewManager {
    
    protected void dispatchDraw(Canvas canvas) {
            //当前视图的子视图个数
            final int count = mChildrenCount;
            final View[] children = mChildren;
            int flags = mGroupFlags;
    
            if ((flags & FLAG_RUN_ANIMATION) != 0 && canAnimate()) {
                final boolean cache = (mGroupFlags & FLAG_ANIMATION_CACHE) == FLAG_ANIMATION_CACHE;
    
                for (int i = 0; i < count; i++) {
                    final View child = children[i];
                    if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                        final LayoutParams params = child.getLayoutParams();
                        attachLayoutAnimationParameters(child, params, i, count);
                        bindLayoutAnimation(child);
                        if (cache) {
                            child.setDrawingCacheEnabled(true);
                            child.buildDrawingCache(true);
                        }
                    }
                }
    
                final LayoutAnimationController controller = mLayoutAnimationController;
                if (controller.willOverlap()) {
                    mGroupFlags |= FLAG_OPTIMIZE_INVALIDATE;
                }
    
                controller.start();
    
                //检查是否需要显示动画，即FLAG_RUN_ANIMATION == 1
                mGroupFlags &= ~FLAG_RUN_ANIMATION;
                mGroupFlags &= ~FLAG_ANIMATION_DONE;
    
                if (cache) {
                    mGroupFlags |= FLAG_CHILDREN_DRAWN_WITH_CACHE;
                }
                
                //通知动画监听者动画开始显示了
                if (mAnimationListener != null) {
                    mAnimationListener.onAnimationStart(controller.getAnimation());
                }
            }
    
            int saveCount = 0;
            //如果CLIP_TO_PADDING_MASK != 1，则说明参数canvas描述的是画布的剪裁区域，该剪裁区域不包含当前视图组的内边距
            final boolean clipToPadding = (flags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK;
            if (clipToPadding) {
                saveCount = canvas.save();
                //裁剪画布
                canvas.clipRect(mScrollX + mPaddingLeft, mScrollY + mPaddingTop,
                        mScrollX + mRight - mLeft - mPaddingRight,
                        mScrollY + mBottom - mTop - mPaddingBottom);
    
            }
    
            // We will draw our child's animation, let's reset the flag
            mPrivateFlags &= ~DRAW_ANIMATION;
            mGroupFlags &= ~FLAG_INVALIDATE_REQUIRED;
    
            boolean more = false;
            final long drawingTime = getDrawingTime();
    
            //如果FLAG_USE_CHILD_DRAWING_ORDER == 0，则说明子视图按照它们在children数组里顺序进行绘制
            //否则需要调用getChildDrawingOrder来判断绘制顺序
            if ((flags & FLAG_USE_CHILD_DRAWING_ORDER) == 0) {
                for (int i = 0; i < count; i++) {
                    final View child = children[i];
                    //如果子视图可见，则开始绘制子视图
                    if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null) {
                        more |= drawChild(canvas, child, drawingTime);
                    }
                }
            } else {
                for (int i = 0; i < count; i++) {
                    final View child = children[getChildDrawingOrder(count, i)];
                    if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null) {
                        more |= drawChild(canvas, child, drawingTime);
                    }
                }
            }
    
            //mDisappearingChildren用来保存哪些正在消失的子视图，正在消失的子视图也是需要绘制的
            // Draw any disappearing views that have animations
            if (mDisappearingChildren != null) {
                final ArrayList<View> disappearingChildren = mDisappearingChildren;
                final int disappearingCount = disappearingChildren.size() - 1;
                // Go backwards -- we may delete as animations finish
                for (int i = disappearingCount; i >= 0; i--) {
                    final View child = disappearingChildren.get(i);
                    more |= drawChild(canvas, child, drawingTime);
                }
            }
    
            if (clipToPadding) {
                canvas.restoreToCount(saveCount);
            }
    
            // mGroupFlags might have been updated by drawChild()
            flags = mGroupFlags;
    
            //如果FLAG_INVALIDATE_REQUIRED == 1，则说明需要进行重新绘制
            if ((flags & FLAG_INVALIDATE_REQUIRED) == FLAG_INVALIDATE_REQUIRED) {
                invalidate();
            }
    
            //通知动画监听者，动画已经结束
            if ((flags & FLAG_ANIMATION_DONE) == 0 && (flags & FLAG_NOTIFY_ANIMATION_LISTENER) == 0 &&
                    mLayoutAnimationController.isDone() && !more) {
                // We want to erase the drawing cache and notify the listener after the
                // next frame is drawn because one extra invalidate() is caused by
                // drawChild() after the animation is over
                mGroupFlags |= FLAG_NOTIFY_ANIMATION_LISTENER;
                final Runnable end = new Runnable() {
                   public void run() {
                       notifyAnimationListener();
                   }
                };
                post(end);
            }
        }
}
```
dispatchDraw用来循环绘制子View视图，它主要做了以下事情：

1. 检查是否需要显示动画，即FLAG_RUN_ANIMATION == 1，则开始显示动画，并通知动画监听者动画已经开始。
2. 如果FLAG_USE_CHILD_DRAWING_ORDER == 0，则说明子视图按照它们在children数组里顺序进行绘制否则需要调用getChildDrawingOrder来判断绘制顺序，最终调用drawChild()来完成
子视图的绘制。
3. 判断是否需要进行重绘以及通知动画监听者动画已经结束。

**关键点3：ViewGroup.drawChild(Canvas canvas, View child, long drawingTime)**

ViewGroup.drawChild(Canvas canvas, View child, long drawingTime)用来完成子视图的绘制。

```java
public abstract class ViewGroup extends View implements ViewParent, ViewManager {
    
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            //表示子视图child是否还在显示动画
            boolean more = false;
    
            //获取子视图的绘制区域以及标志位
            final int cl = child.mLeft;
            final int ct = child.mTop;
            final int cr = child.mRight;
            final int cb = child.mBottom;
    
            final int flags = mGroupFlags;
    
            if ((flags & FLAG_CLEAR_TRANSFORMATION) == FLAG_CLEAR_TRANSFORMATION) {
                if (mChildTransformation != null) {
                    mChildTransformation.clear();
                }
                mGroupFlags &= ~FLAG_CLEAR_TRANSFORMATION;
            }
    
            //获取子视图的变换矩阵transformToApply
            Transformation transformToApply = null;
            //获取子视图的动画
            final Animation a = child.getAnimation();
            boolean concatMatrix = false;
    
            if (a != null) {
                if (mInvalidateRegion == null) {
                    mInvalidateRegion = new RectF();
                }
                final RectF region = mInvalidateRegion;
    
                final boolean initialized = a.isInitialized();
                if (!initialized) {
                    a.initialize(cr - cl, cb - ct, getWidth(), getHeight());
                    a.initializeInvalidateRegion(0, 0, cr - cl, cb - ct);
                    child.onAnimationStart();
                }
    
                if (mChildTransformation == null) {
                    mChildTransformation = new Transformation();
                }
                //如果子视图需要播放动画，则调用getTransformation开始执行动画，如果动画还需要继续执行，则more == true，并且返回子视图的
                //变化矩阵mChildTransformation
                more = a.getTransformation(drawingTime, mChildTransformation);
                transformToApply = mChildTransformation;
    
                concatMatrix = a.willChangeTransformationMatrix();
    
                if (more) {
                    if (!a.willChangeBounds()) {
                        if ((flags & (FLAG_OPTIMIZE_INVALIDATE | FLAG_ANIMATION_DONE)) ==
                                FLAG_OPTIMIZE_INVALIDATE) {
                            mGroupFlags |= FLAG_INVALIDATE_REQUIRED;
                        } else if ((flags & FLAG_INVALIDATE_REQUIRED) == 0) {
                            // The child need to draw an animation, potentially offscreen, so
                            // make sure we do not cancel invalidate requests
                            mPrivateFlags |= DRAW_ANIMATION;
                            invalidate(cl, ct, cr, cb);
                        }
                    } else {
                        a.getInvalidateRegion(0, 0, cr - cl, cb - ct, region, transformToApply);
    
                        // The child need to draw an animation, potentially offscreen, so
                        // make sure we do not cancel invalidate requests
                        mPrivateFlags |= DRAW_ANIMATION;
    
                        final int left = cl + (int) region.left;
                        final int top = ct + (int) region.top;
                        invalidate(left, top, left + (int) region.width(), top + (int) region.height());
                    }
                }
            } 
            //如果FLAG_SUPPORT_STATIC_TRANSFORMATIONS == 1，调用getChildStaticTransformation()方法检查子视图是否被设置一个
            //变换矩阵，如果设置了，即hasTransform == true，则mChildTransformation就是子视图需要的变换矩阵
            else if ((flags & FLAG_SUPPORT_STATIC_TRANSFORMATIONS) ==
                    FLAG_SUPPORT_STATIC_TRANSFORMATIONS) {
                if (mChildTransformation == null) {
                    mChildTransformation = new Transformation();
                }
                final boolean hasTransform = getChildStaticTransformation(child, mChildTransformation);
                if (hasTransform) {
                    final int transformType = mChildTransformation.getTransformationType();
                    transformToApply = transformType != Transformation.TYPE_IDENTITY ?
                            mChildTransformation : null;
                    concatMatrix = (transformType & Transformation.TYPE_MATRIX) != 0;
                }
            }
    
            //设置mPrivateFlags的DRAWN标志位为1，标明它要开始绘制了。
            // Sets the flag as early as possible to allow draw() implementations
            // to call invalidate() successfully when doing animations
            child.mPrivateFlags |= DRAWN;
    
            if (!concatMatrix && canvas.quickReject(cl, ct, cr, cb, Canvas.EdgeType.BW) &&
                    (child.mPrivateFlags & DRAW_ANIMATION) == 0) {
                return more;
            }
            
            //调用computeScroll()计算子视图的滑动位置
            child.computeScroll();
    
            final int sx = child.mScrollX;
            final int sy = child.mScrollY;
    
            boolean scalingRequired = false;
            Bitmap cache = null;
            //如果FLAG_CHILDREN_DRAWN_WITH_CACHE或者FLAG_CHILDREN_DRAWN_WITH_CACHE为1，则表示它采用缓冲的方式进行
            //绘制，它将自己的UI缓冲在一个Bitmap里，可以调用getDrawingCache()方法来获得这个Bitmap。
            if ((flags & FLAG_CHILDREN_DRAWN_WITH_CACHE) == FLAG_CHILDREN_DRAWN_WITH_CACHE ||
                    (flags & FLAG_ALWAYS_DRAWN_WITH_CACHE) == FLAG_ALWAYS_DRAWN_WITH_CACHE) {
                cache = child.getDrawingCache(true);
                if (mAttachInfo != null) scalingRequired = mAttachInfo.mScalingRequired;
            }
    
            final boolean hasNoCache = cache == null;
    
            //设置子视图child的偏移、Alpha通道以及裁剪区域
            final int restoreTo = canvas.save();
            if (hasNoCache) {
                canvas.translate(cl - sx, ct - sy);
            } else {
                canvas.translate(cl, ct);
                if (scalingRequired) {
                    // mAttachInfo cannot be null, otherwise scalingRequired == false
                    final float scale = 1.0f / mAttachInfo.mApplicationScale;
                    canvas.scale(scale, scale);
                }
            }
    
            float alpha = 1.0f;
    
            if (transformToApply != null) {
                if (concatMatrix) {
                    int transX = 0;
                    int transY = 0;
                    if (hasNoCache) {
                        transX = -sx;
                        transY = -sy;
                    }
                    // Undo the scroll translation, apply the transformation matrix,
                    // then redo the scroll translate to get the correct result.
                    canvas.translate(-transX, -transY);
                    canvas.concat(transformToApply.getMatrix());
                    canvas.translate(transX, transY);
                    mGroupFlags |= FLAG_CLEAR_TRANSFORMATION;
                }
    
                alpha = transformToApply.getAlpha();
                if (alpha < 1.0f) {
                    mGroupFlags |= FLAG_CLEAR_TRANSFORMATION;
                }
    
                if (alpha < 1.0f && hasNoCache) {
                    final int multipliedAlpha = (int) (255 * alpha);
                    if (!child.onSetAlpha(multipliedAlpha)) {
                        canvas.saveLayerAlpha(sx, sy, sx + cr - cl, sy + cb - ct, multipliedAlpha,
                                Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG);
                    } else {
                        child.mPrivateFlags |= ALPHA_SET;
                    }
                }
            } else if ((child.mPrivateFlags & ALPHA_SET) == ALPHA_SET) {
                child.onSetAlpha(255);
            }
    
            //如果FLAG_CLIP_CHILDREN == 1，则需要设置子视图的裁剪区域
            if ((flags & FLAG_CLIP_CHILDREN) == FLAG_CLIP_CHILDREN) {
                if (hasNoCache) {
                    canvas.clipRect(sx, sy, sx + (cr - cl), sy + (cb - ct));
                } else {
                    if (!scalingRequired) {
                        canvas.clipRect(0, 0, cr - cl, cb - ct);
                    } else {
                        canvas.clipRect(0, 0, cache.getWidth(), cache.getHeight());
                    }
                }
            }
    
            //绘制子视图的UI
            if (hasNoCache) {
                // Fast path for layouts with no backgrounds
                if ((child.mPrivateFlags & SKIP_DRAW) == SKIP_DRAW) {
                    if (ViewDebug.TRACE_HIERARCHY) {
                        ViewDebug.trace(this, ViewDebug.HierarchyTraceType.DRAW);
                    }
                    child.mPrivateFlags &= ~DIRTY_MASK;
                    child.dispatchDraw(canvas);
                } else {
                    child.draw(canvas);
                }
            } else {
                final Paint cachePaint = mCachePaint;
                if (alpha < 1.0f) {
                    cachePaint.setAlpha((int) (alpha * 255));
                    mGroupFlags |= FLAG_ALPHA_LOWER_THAN_ONE;
                } else if  ((flags & FLAG_ALPHA_LOWER_THAN_ONE) == FLAG_ALPHA_LOWER_THAN_ONE) {
                    cachePaint.setAlpha(255);
                    mGroupFlags &= ~FLAG_ALPHA_LOWER_THAN_ONE;
                }
                if (Config.DEBUG && ViewDebug.profileDrawing) {
                    EventLog.writeEvent(60003, hashCode());
                }
                canvas.drawBitmap(cache, 0.0f, 0.0f, cachePaint);
            }
    
            //恢复画布的堆栈状态，以便在绘制完当前子视图的UI后，可以继续绘制其他子视图的UI
            canvas.restoreToCount(restoreTo);
    
            if (a != null && !more) {
                child.onSetAlpha(255);
                finishAnimatingView(child, a);
            }
    
            return more;
        }
}
```

ViewGroup.drawChild(Canvas canvas, View child, long drawingTime)用来完成子视图的绘制，它主要完成了以下事情：

1 获取子视图的绘制区域以及标志位
2 获取子视图的变换矩阵transformToApply，这个分两种情况：

- 如果子视图需要播放动画，则调用getTransformation开始执行动画，如果动画还需要继续执行，则more == true，并且返回子视图的变化矩阵mChildTransformation
- 如果FLAG_SUPPORT_STATIC_TRANSFORMATIONS == 1，调用getChildStaticTransformation()方法检查子视图是否被设置一个变换矩阵，如果设置了，即hasTransform == true，则mChildTransformation就是子视图需要的变换矩阵

3 如果FLAG_CHILDREN_DRAWN_WITH_CACHE或者FLAG_CHILDREN_DRAWN_WITH_CACHE为1，则表示它采用缓冲的方式进行绘制，它将自己的UI缓冲在一个Bitmap里，可以调用getDrawingCache()方法来获得这个Bitmap。
4 设置子视图child的偏移、Alpha通道以及裁剪区域。

5 绘制子视图的UI，这分为两种情况：

- 如果以非缓冲的方式来绘制，如果SKIP_DRAW == 1，则说明需要跳过当前子视图而去绘制它自己的子视图，否则先绘制它的视图，再绘制它的子视图。绘制自身通过draw()函数来
完成，绘制它的子视图则通过dispatchDraw()来完成的。
- 如果是以缓冲的方式来绘制，这种情况只需要将上一次的缓冲的Bitmap对象cache绘制到画布canvas上

6 恢复画布的堆栈状态，以便在绘制完当前子视图的UI后，可以继续绘制其他子视图的UI。

**总结**

至此，Android应用程序窗口的渲染流程就分析完了，我们再来总结一下。

1. 渲染Android应用视图的渲染流程，测量流程用来确定视图的大小、布局流程用来确定视图的位置、绘制流程最终将视图绘制在应用窗口上。
2. Android应用程序窗口UI首先是使用Skia图形库API来绘制在一块画布上，实际地是绘制在这块画布里面的一个图形缓冲区中，这个图形缓冲区最终会被交给SurfaceFlinger服
务，而SurfaceFlinger服务再使用OpenGL图形库API来将这个图形缓冲区渲染到硬件帧缓冲区中。