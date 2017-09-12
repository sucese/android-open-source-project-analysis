package com.guoxiaoxing.android.framework.demo.application.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

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
