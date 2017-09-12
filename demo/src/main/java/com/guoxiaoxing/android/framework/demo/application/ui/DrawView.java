package com.guoxiaoxing.android.framework.demo.application.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
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
    private Paint paint = new Paint();
    private Matrix matrix = new Matrix();
    private Path path = new Path();

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

        //绘制弧形
//        paint.setStyle(Paint.Style.FILL);//填充模式
//        canvas.drawArc(200, 100, 800, 500, -110, 100, true, paint);
//        canvas.drawArc(200, 100, 800, 500, 20, 140, false, paint);
//        paint.setStyle(Paint.Style.STROKE);//画线模式
//        paint.setStrokeWidth(5);
//        canvas.drawArc(200, 100, 800, 500, 180, 60, false, paint);

        //绘制心形
//        path.addArc(200, 200, 400, 400, -225, 225);
//        path.arcTo(400, 200, 600, 400, -180, 225, false);
//        path.lineTo(400, 542);
//        canvas.drawPath(path, paint);

        //绘制直线
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(5);
//        path.lineTo(300, 400);// 由当前位置 (0, 0) 向 (300, 400) 画一条直线
//        path.rLineTo(400, 0);// 由当前位置 (300, 400) 向正右方400像素的位置画一条直线
//        canvas.drawPath(path, paint);

        //二阶贝塞尔曲线
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(5);
//        path.quadTo(200, 200, 400, 0);
//        canvas.drawPath(path, paint);

        //三阶贝塞尔曲线
        paint.setStyle(Paint.Style.FILL);
        path.cubicTo(200, 200, 400, 0, 600, 200);
        canvas.drawPath(path, paint);

        //绘制弧线
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(5);
//        path.lineTo(300, 300);
//        path.arcTo(300, 300, 500, 500, -90, 90 ,true);//
//        canvas.drawPath(path, paint);
    }

    private void init() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.timo);

        paint.setColor(Color.BLACK);
    }
}
