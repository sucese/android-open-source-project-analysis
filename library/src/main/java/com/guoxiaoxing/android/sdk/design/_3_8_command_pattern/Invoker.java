package com.guoxiaoxing.android.sdk.design._3_8_command_pattern;

// 调用者
public class Invoker {

    private AbstractCommand mCommmand;

    public Invoker(AbstractCommand command) {
        mCommmand = command;
    }

    public void invoke() {
        mCommmand.command();
    }
}
