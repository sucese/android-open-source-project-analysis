package com.guoxiaoxing.android.sdk.design._3_10_responsiblity_pattern;

import android.text.TextUtils;

// 处理器2
public class Handler2 extends Handler {

    @Override
    public void handleRequest(String condition) {
        if (TextUtils.equals(condition, "Handler2")) {
            // process request
        } else {
            // next handler
            next.handleRequest(condition);
        }
    }
}
