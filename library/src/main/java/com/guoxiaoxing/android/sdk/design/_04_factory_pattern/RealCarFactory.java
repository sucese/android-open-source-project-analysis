package com.guoxiaoxing.android.sdk.design._04_factory_pattern;

/**
 * 具体工厂
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/27 下午6:33
 */
public class RealCarFactory extends AbstractCarFactory {

    @Override
    public AbstractAudiCar createProduct() {
        return new RealAudiCarA();
    }
}
