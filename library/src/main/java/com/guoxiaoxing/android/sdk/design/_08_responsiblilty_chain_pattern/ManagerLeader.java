package com.guoxiaoxing.android.sdk.design._08_responsiblilty_chain_pattern;

/**
 * 责任链：经理
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:50
 */
public class ManagerLeader extends Leader {

    @Override
    public int limit() {
        return 10000;
    }

    @Override
    public void handle() {
        System.out.println("经理批复保险发票");
    }
}
