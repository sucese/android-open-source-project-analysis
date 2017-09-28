package com.guoxiaoxing.android.sdk.design._08_responsiblilty_chain_pattern;

/**
 * 责任链：组长
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:49
 */
public class GroupLeader extends Leader {

    @Override
    public int limit() {
        return 1000;
    }

    @Override
    public void handle() {
        System.out.println("组长批复保险发票");
    }
}
