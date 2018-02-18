package com.guoxiaoxing.android.sdk.design._1_4_factory_pattern;

// 简单工厂
public class SimpleFactory {

    public static <T extends AbstractProduct> T create(Class<T> clasz) {
        AbstractProduct product = null;
        try {
            product = (AbstractProduct) Class.forName(clasz.getName()).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return (T) product;
    }
}
