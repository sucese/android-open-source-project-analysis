# Android显示框架：Android应用视图的绘制流程

**关于作者**

>郭孝星，非著名程序员，主要从事Android平台基础架构与中间件方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提交Issue或者发邮件至guoxiaoxingse@163.com与我联系。

第一次阅览本系列文章，请参见[导读](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/导读.md)，更多文章请参见[文章目录](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/README.md)。

**文章目录**

在上篇文章[04Android显示框架：Activity应用视图的创建流程](https://github.com/guoxiaoxing/android-open-source-project-analysis/blob/master/doc/Android系统应用框架篇/Android显示框架/04Android显示框架：Activity应用视图的创建流程.md)
中我们分析了Activity应用视图的创建流程，这样我们便可以进行UI的绘制了。一个Android应用窗口里包含了很多UI元素，它们是以树形结构来组织的，即父子关系。在绘制UI的过程中，我们
要先确定父UI元素的大小及位置，再确定子UI元素的大小及位置，才能进行绘制。

View的绘制流程从ViewRoot.performTraversals()开始，整个流程分为三步：

1. measure：测量View的宽高
2. layout：确定View在父容器里的位置
3. draw：将View绘制在屏幕上

在上文创建View对象这一步中我们提到，Android应用窗口的顶层视图是一个类型为DecorView的UI元素，该顶层视图是由ViewRoot.performTraversals()方法来进行测量、布局与绘制
操作的。

这三个操作分别有

## 测量流程

>Measure过程决定了View的宽高，该过程完成后，通常都可以通过getMeasuredWith()/getMeasuredHeight()获得宽高。

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

## 布局流程

>Layout过程决定了View的父容器里四个坐标点的位置，该过程完成后可以通过getTop()、getBottom()、getLeft()和getRight()来拿到View四个顶点的位置，并可以通过
getWidth()/getHeigth()获得View的最终宽高。

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

## 绘制流程

>Draw过程最终将View绘制在屏幕上。

