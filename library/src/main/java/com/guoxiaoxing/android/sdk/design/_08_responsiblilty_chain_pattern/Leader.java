package com.guoxiaoxing.android.sdk.design._08_responsiblilty_chain_pattern;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:46
 */
public abstract class Leader {

    protected Leader nextHandler;

    public final void handlerRequest(int money) {
        if (money < limit()) {
            handle();
        } else {
            if (nextHandler != null) {
                nextHandler.handle();
            }
        }
    }

    public abstract int limit();
    public abstract void handle();
}
