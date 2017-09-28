package com.guoxiaoxing.android.sdk.design._05_abstract_factory_pattern;

/**
 * 抽象工厂：指定生产轮胎、引擎、制动系统
 *
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午2:20
 */
public abstract class AbstractCarFactory {

    public abstract IBrake createBrake();
    public abstract IEngine createEngine();
    public abstract ITire createTire();
}
