package com.guoxiaoxing.android.sdk.design._3_5_observer_pattern;

// 监听者
public class Observer {

    public void setup() {
        Observable observable = new Observable();
        observable.setListener(new Listener() {
            @Override
            public void change() {
                // TODO 监听的对象发生改变
            }
        });
    }
}
