package com.guoxiaoxing.android.sdk.design.singleton;

import java.io.ObjectStreamException;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/20 下午3:53
 */
public class InnerClassSingleton {

    private InnerClassSingleton() {
    }

    public static InnerClassSingleton getInstance() {
        return InnerClassSingletonHolder.instance;
    }

    private static class InnerClassSingletonHolder {
        private static final InnerClassSingleton instance = new InnerClassSingleton();

        private Object readReslove() throws ObjectStreamException {
            return instance;
        }
    }
}
