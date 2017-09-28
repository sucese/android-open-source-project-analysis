package com.guoxiaoxing.android.sdk.design._01_singleton_pattern;

import java.io.ObjectStreamException;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/20 上午11:24
 */
public class DoubleCheckSingleton {
    private static volatile DoubleCheckSingleton instance;

    private DoubleCheckSingleton() {
    }

    public static DoubleCheckSingleton getInstance() {
        //first check
        if (instance == null) {
            //double check
            synchronized (DoubleCheckSingleton.class) {
                instance = new DoubleCheckSingleton();
            }
        }
        return instance;
    }

    private Object readReslove() throws ObjectStreamException {
        return instance;
    }
}
