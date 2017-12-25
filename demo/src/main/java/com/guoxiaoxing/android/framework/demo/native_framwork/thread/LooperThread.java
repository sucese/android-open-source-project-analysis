package com.guoxiaoxing.android.framework.demo.native_framwork.thread;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;


/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/12/23 下午3:59
 */
public class LooperThread extends Thread {

    public Handler mHandler;

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // process incoming messages here
            }
        };
        Looper.loop();
    }
}
