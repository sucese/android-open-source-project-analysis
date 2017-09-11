package com.guoxiaoxing.android.framework.demo.application.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/10 上午10:49
 */
public class DrawView extends View {

    private Bitmap bitmap;
    private Matrix matrix = new Matrix();
    private Paint paint = new Paint();

    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.FILL);//填充模式
        canvas.drawArc(200, 100, 800, 500, -110, 100, true, paint);
        canvas.drawArc(200, 100, 800, 500, 20, 140, false, paint);
        paint.setStyle(Paint.Style.STROKE);//画线模式
        paint.setStrokeWidth(5);
        canvas.drawArc(200, 100, 800, 500, 180, 60, false, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, heightSpecSize);
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, heightSpecSize);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, heightSpecSize);
        }
    }

    private void init() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.timo);

        paint.setColor(Color.BLACK);
    }
}
