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
    public AbstractAudiCar createAudiCar(Class className) {
        AbstractAudiCar car = null;
        try {
            car = (AbstractAudiCar) Class.forName(className.getName()).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return car;
    }
}
