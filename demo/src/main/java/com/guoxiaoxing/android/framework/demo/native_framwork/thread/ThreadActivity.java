package com.guoxiaoxing.android.framework.demo.native_framwork.thread;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;

public class ThreadActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ThreadActivity";

    private ThreadLocal<Integer> mInteger = new ThreadLocal<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);

        findViewById(R.id.btn_add_thread_handler).setOnClickListener(this);

        mInteger.set(1);
        Log.d(TAG, "mInteger: " + mInteger.get());

        new Thread("Thread1") {
            @Override
            public void run() {
                mInteger.set(2);
                Log.d(TAG, "mInteger: " + mInteger.get());
            }
        }.start();

        new Thread("Thread2") {
            @Override
            public void run() {
                Log.d(TAG, "mInteger: " + mInteger.get());
            }
        }.start();
    }

    private void addHandlerThread() {
//        HandlerThread handlerThread = new HandlerThread("HandlerThread#1");
//        handlerThread.start();
//        Handler handler = new Handler(handlerThread.getLooper());
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "Thread.currentThread(): " + Thread.currentThread());
//            }
//        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "post runnable to message queue");
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_add_thread_handler:
                addHandlerThread();
                break;
        }
    }
}
