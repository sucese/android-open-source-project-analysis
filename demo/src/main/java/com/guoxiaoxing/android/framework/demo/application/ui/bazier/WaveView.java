package com.guoxiaoxing.android.framework.demo.application.ui.bazier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

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
