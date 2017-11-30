package com.guoxiaoxing.android.framework.demo.app_framwork.ui_framwork;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;

import com.guoxiaoxing.android.framework.demo.R;

public class ViewActivity extends AppCompatActivity {

    private static final String TAG = "ViewActivity";

    private CustomView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        view = (CustomView) findViewById(R.id.view);

        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            @Override
            public void onDraw() {

            }
        });

        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

            }
        });
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();

            Log.d(TAG, "view.getMeasuredWidth(): " + width);
            Log.d(TAG, "view.getMeasuredHeight(): " + height);
        }
    }
}
