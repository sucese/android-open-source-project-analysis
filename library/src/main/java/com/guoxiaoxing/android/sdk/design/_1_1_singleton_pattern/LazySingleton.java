package com.guoxiaoxing.android.sdk.design._1_1_singleton_pattern;

import java.io.ObjectStreamException;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/20 上午9:38
 */
public class LazySingleton {

    private static LazySingleton instance;

    private LazySingleton() {
    }

    public synchronized static LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }

    private Object readReslove() throws ObjectStreamException {
        return instance;
    }
}
