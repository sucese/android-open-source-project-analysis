package com.guoxiaoxing.android.sdk.design._05_abstract_factory_pattern;

/**
 * 具体工厂：奥迪A8工厂
 *
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午3:18
 */
public class RealCarFactoryA8 extends AbstractCarFactory {

    @Override
    public IBrake createBrake() {
        return new BrakeA8();
    }

    @Override
    public IEngine createEngine() {
        return new EngineA8();
    }

    @Override
    public ITire createTire() {
        return new TireA8();
    }
}
