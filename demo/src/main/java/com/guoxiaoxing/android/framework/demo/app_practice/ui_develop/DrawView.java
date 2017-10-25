package com.guoxiaoxing.android.framework.demo.app_practice.ui_develop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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

    private Bitmap bitmapTimo;
    private Bitmap bitmapCat;
    private Paint paint1 = new Paint();
    private Paint paint2 = new Paint();
    private Paint paint3 = new Paint();
    private Paint paint4 = new Paint();
    private Matrix matrix = new Matrix();
    private Camera camera = new Camera();
    private Path path = new Path();
    private Rect rect = new Rect(250, 500, 750, 1000);
    private Rect rect1 = new Rect(0, 0, 500, 500);
    private Rect rect2 = new Rect(500, 500, 1000, 1000);

    float[] src = new float[]{100, 100, 500, 500};
    float[] dst = new float[]{100 + 20, 100 + 20, 500 - 100, 500 + 100};

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

        //绘制文字
//        canvas.drawText("Android", 80, 300, paint1);

        //绘制圆形
//        canvas.drawCircle(500, 500, 300, paint1);

        //绘制矩形
//        canvas.drawRect(0, 100, 900, 300, paint1);
//        canvas.drawRect(0, 400, 900, 600, paint2);
//        canvas.drawRect(0, 700, 900, 900, paint3);
//        canvas.drawRect(0, 1000, 900, 1200, paint4);

        //绘制弧形
//        paint1.setStyle(Paint.Style.FILL);//填充模式
//        canvas.drawArc(200, 100, 800, 500, -110, 100, true, paint1);
//        canvas.drawArc(200, 100, 800, 500, 20, 140, false, paint1);
//        paint1.setStyle(Paint.Style.STROKE);//画线模式
//        paint1.setStrokeWidth(5);
//        canvas.drawArc(200, 100, 800, 500, 180, 60, false, paint1);

        //绘制心形
//        path.addArc(200, 200, 400, 400, -225, 225);
//        path.arcTo(400, 200, 600, 400, -180, 225, false);
//        path.lineTo(400, 542);
//        canvas.drawPath(path, paint1);

        //绘制直线
//        paint1.setStyle(Paint.Style.STROKE);
//        paint1.setStrokeWidth(5);
//        path.lineTo(300, 400);// 由当前位置 (0, 0) 向 (300, 400) 画一条直线
//        path.rLineTo(400, 0);// 由当前位置 (300, 400) 向正右方400像素的位置画一条直线
//        canvas.drawPath(path, paint1);

        //绘制位图
//        canvas.drawBitmap(bitmapTimo, null, rect1, paint1);
//        canvas.drawBitmap(bitmapTimo, null, rect2, paint2);

        //二阶贝塞尔曲线
//        paint1.setStyle(Paint.Style.STROKE);
//        paint1.setStrokeWidth(5);
//        path.quadTo(200, 200, 400, 0);
//        canvas.drawPath(path, paint1);

        //三阶贝塞尔曲线
//        paint1.setStyle(Paint.Style.FILL);
//        path.cubicTo(200, 200, 400, 0, 600, 200);
//        canvas.drawPath(path, paint1);

        //绘制弧线
//        paint1.setStyle(Paint.Style.STROKE);
//        paint1.setStrokeWidth(5);
//        path.lineTo(300, 300);
//        path.arcTo(300, 300, 500, 500, -90, 90 ,true);//
//        canvas.drawPath(path, paint1);

        //范围裁切
//        canvas.save();//保存画布
//        canvas.clipRect(200, 200, 900, 900);
//        canvas.drawBitmap(bitmapTimo, 100, 100, paint1);
//        canvas.restore();//恢复画布
//
//        canvas.save();//保存画布
//        path.addCircle(500, 500, 300, Path.Direction.CW);
//        canvas.clipPath(path);
//        canvas.drawBitmap(bitmapTimo, 100, 100, paint1);
//        canvas.restore();//恢复画布

        //Canvas几何变换
//        canvas.save();//保存画布
//        canvas.skew(0, 0.5f);
//        canvas.drawBitmap(bitmapTimo, null, rect1, paint1);
//        canvas.restore();//恢复画布
//
//        canvas.save();//保存画布
//        canvas.rotate(45, 750, 750);
//        canvas.drawBitmap(bitmapTimo, null, rect2, paint1);
//        canvas.restore();//恢复画布

        //Matrix几何变换
//        canvas.save();//保存画布
//        matrix.preSkew(0, 0.5f);
//        canvas.concat(matrix);
//        canvas.drawBitmap(bitmapTimo, null, rect1, paint1);
//        canvas.restore();//恢复画布
//
//        canvas.save();//保存画布
//        matrix.reset();
//        matrix.preRotate(45, 750, 750);
//        canvas.concat(matrix);
//        canvas.drawBitmap(bitmapTimo, null, rect2, paint1);
//        canvas.restore();//恢复画布

//        canvas.save();//保存画布
//        matrix.setPolyToPoly(src, 0, dst, 0, 2);
//        canvas.concat(matrix);
//        canvas.drawBitmap(bitmapTimo, 0, 0, paint1);
//        canvas.restore();//恢复画布

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
    }

    private void init() {

        bitmapTimo = BitmapFactory.decodeResource(getResources(), R.drawable.timo);
        bitmapCat = BitmapFactory.decodeResource(getResources(), R.drawable.filter_bitmap);

        //线性渐变
//        Shader shader1 = new LinearGradient(0, 100, 200, 100, Color.RED, Color.BLUE, Shader.TileMode.CLAMP);
//        paint1.setShader(shader1);
//
//        Shader shader2 = new LinearGradient(0, 600, 200, 600, Color.RED, Color.BLUE, Shader.TileMode.MIRROR);
//        paint2.setShader(shader2);
//
//        Shader shader3 = new LinearGradient(0, 1100, 200, 1100, Color.RED, Color.BLUE, Shader.TileMode.REPEAT);
//        paint3.setShader(shader3);

        //辐射渐变
//        Shader shader1 = new RadialGradient(0, 100, 200, Color.RED, Color.BLUE, Shader.TileMode.CLAMP);
//        paint1.setShader(shader1);
//
//        Shader shader2 = new RadialGradient(0, 600, 200, Color.RED, Color.BLUE, Shader.TileMode.MIRROR);
//        paint2.setShader(shader2);
//
//        Shader shader3 = new RadialGradient(0, 1100, 200, Color.RED, Color.BLUE, Shader.TileMode.REPEAT);
//        paint3.setShader(shader3);

        //扫描渐变
//        Shader shader1 = new SweepGradient(200, 200, Color.RED, Color.BLUE);
//        paint1.setShader(shader1);

        //位图着色
//        Shader shader1 = new BitmapShader(bitmapTimo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
//        paint1.setShader(shader1);

        //组合着色
//        Shader shader1 = new BitmapShader(bitmapTimo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
//        Shader shader2 = new BitmapShader(bitmapCat, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
//        Shader shader = new ComposeShader(shader1, shader2, PorterDuff.Mode.SRC_OVER);
//        paint1.setShader(shader);

        //颜色过滤器
//        ColorFilter colorFilter1 = new LightingColorFilter(Color.RED, Color.BLUE);
//        paint2.setColorFilter(colorFilter1);

        //颜色抖动
//        paint1.setDither(false);
//        paint2.setDither(true);

        //双线性过滤
//        paint1.setFilterBitmap(false);
//        paint2.setFilterBitmap(true);

        //图形轮廓效果
//        //绘制圆角
//        PathEffect cornerPathEffect = new CornerPathEffect(20);
//        paint1.setStyle(Paint.Style.STROKE);
//        paint1.setStrokeWidth(5);
//        paint1.setPathEffect(cornerPathEffect);
//
//        //绘制尖角
//        PathEffect discretePathEffect = new DiscretePathEffect(20, 5);
//        paint2.setStyle(Paint.Style.STROKE);
//        paint2.setStrokeWidth(5);
//        paint2.setPathEffect(discretePathEffect);
//
//        //绘制虚线
//        PathEffect dashPathEffect = new DashPathEffect(new float[]{20,10, 5, 10}, 0);
//        paint3.setStyle(Paint.Style.STROKE);
//        paint3.setStrokeWidth(5);
//        paint3.setPathEffect(dashPathEffect);
//
//        //使用path来绘制虚线
//        Path path = new Path();//画一个三角来填充虚线
//        path.lineTo(40, 40);
//        path.lineTo(0, 40);
//        path.close();
//        PathEffect pathDashPathEffect = new PathDashPathEffect(path, 20, 0, PathDashPathEffect.Style.TRANSLATE);
//        paint4.setStyle(Paint.Style.STROKE);
//        paint4.setStrokeWidth(5);
//        paint4.setPathEffect(pathDashPathEffect);

//        //组合PathEffect，叠加应用
//        SumPathEffect sumPathEffect = new SumPathEffect(discretePathEffect, dashPathEffect);
//        paint4.setPathEffect(sumPathEffect);
//
//        //组合PathEffect，先后应用
//        ComposePathEffect composePathEffect = new ComposePathEffect(dashPathEffect, discretePathEffect);
//        paint4.setPathEffect(composePathEffect);

        //设置阴影图层,处于目标下层图层
//        paint1.setTextSize(200);
//        paint1.setShadowLayer(10, 0, 0, Color.RED);

        //设置遮罩图层,处于目标上层图层
        //关闭硬件加速
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        MaskFilter blurMaskFilter = new BlurMaskFilter(200, BlurMaskFilter.Blur.NORMAL);
//        paint2.setMaskFilter(blurMaskFilter);

//        MaskFilter embossMaskFilter = new EmbossMaskFilter(new float[]{0 ,1, 1}, 0.2f, 8, 10);
//        paint1.setMaskFilter(embossMaskFilter);
    }
}
