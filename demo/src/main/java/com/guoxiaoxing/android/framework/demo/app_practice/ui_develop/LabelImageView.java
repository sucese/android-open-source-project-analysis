package com.guoxiaoxing.android.framework.demo.app_practice.ui_develop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * 在图片右上角打上标签
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/14 上午10:36
 */
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
