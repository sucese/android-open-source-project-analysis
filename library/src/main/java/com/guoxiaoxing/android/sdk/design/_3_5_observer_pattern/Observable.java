package com.guoxiaoxing.android.sdk.design._3_5_observer_pattern;


// 被监听者
public class Observable {

    private Listener mListener;

    // 设置监听器
    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void onChange() {
        // 通知对象发生改变
        mListener.change();
    }
}
