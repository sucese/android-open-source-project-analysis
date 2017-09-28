package com.guoxiaoxing.android.sdk.design._08_responsiblilty_chain_pattern;

/**
 * 责任链：老板
 *
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:51
 */
public class BossLeader extends Leader {

    @Override
    public int limit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void handle() {
        System.out.println("老板批复保险发票");
    }
}
