package com.guoxiaoxing.android.framework.demo.app_framwork.ui_framwork;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.guoxiaoxing.android.framework.demo.R;

public class WindowActivity extends AppCompatActivity implements View.OnClickListener {

    Button button;
    WindowManager windowManager;
    WindowManager.LayoutParams layoutParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_window);

        findViewById(R.id.btn_add_view).setOnClickListener(this);
        findViewById(R.id.btn_remove_view).setOnClickListener(this);

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT
                , WindowManager.LayoutParams.WRAP_CONTENT, 0, 0, PixelFormat.TRANSLUCENT);
        layoutParams.x = 100;
        layoutParams.y = 100;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;

        button = new Button(this);
        button.setText("Button");

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        if (windowManager != null) {
                            layoutParams.x = (int) event.getRawX();
                            layoutParams.y = (int) event.getRawY();
                            windowManager.updateViewLayout(button, layoutParams);
                        }
                        break;
                }

                return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add_view:
                addView();
                break;
            case R.id.btn_remove_view:
                removeView();
                break;
        }
    }


    private void addView() {
        if (windowManager != null) {
            windowManager.addView(button, layoutParams);
        }
    }

    private void removeView() {
        if (windowManager != null) {
            windowManager.removeView(button);
        }
    }
}
