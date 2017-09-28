package com.guoxiaoxing.android.sdk.design._06_strategy_pattern;

/**
 * 具体策略：地铁计价方式
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午3:49
 */
public class SubwayPrice implements IPrice {

    @Override
    public int price(int millage) {
        return millage / 10;
    }
}
