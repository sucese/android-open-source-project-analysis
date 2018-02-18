package com.guoxiaoxing.android.sdk.design._3_10_responsiblity_pattern;

// 处理器，定位行为和下一个处理器
public abstract class Handler {
    protected Handler next;

    public abstract void handleRequest(String condition);
}
