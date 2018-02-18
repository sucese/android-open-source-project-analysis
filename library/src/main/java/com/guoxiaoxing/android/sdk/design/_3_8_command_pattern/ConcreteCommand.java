package com.guoxiaoxing.android.sdk.design._3_8_command_pattern;

// 具体命令
public class ConcreteCommand implements AbstractCommand {

    private Receiver mReceiver;

    public ConcreteCommand(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    public void command() {
        mReceiver.action();
    }
}
