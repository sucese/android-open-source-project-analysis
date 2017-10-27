package com.guoxiaoxing.android.framework.demo.app_framwork.ui_framwork;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/10/26 下午5:17
 */
public class StandardView extends View {

    public StandardView(Context context) {
        super(context);
        setup(context, null);
    }

    public StandardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    public StandardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //TODO handle wrap_content and padding if necessary
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        //TODO do something if activity lifecycle changed if necessary
        //Activity onResume()
        if(visibility == VISIBLE){

        }
        //Activity onPause()
        else {

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        //TODO do something if activity lifecycle changed if necessary
        //Activity onResume()
        if (hasWindowFocus) {
        }
        //Activity onPause()
        else {
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
        //TODO handle the touch event if necessary
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //TODO release resources, thread, animation
    }

    private void setup(Context context, AttributeSet attrs) {

        //TODO init something like Paint and so on

        //TODO get attr
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StandardView);
            int color = typedArray.getColor(R.styleable.StandardView_color, Color.RED);
            typedArray.recycle();
        }
    }
}
