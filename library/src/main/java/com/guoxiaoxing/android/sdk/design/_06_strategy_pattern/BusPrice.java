package com.guoxiaoxing.android.sdk.design._06_strategy_pattern;

/**
 * 具体策略：公交计价方式
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午3:48
 */
public class BusPrice implements IPrice {

    @Override
    public int price(int millage) {
        return millage / 20;
    }
}
