package com.guoxiaoxing.android.sdk.design._01_singleton_pattern;

import java.util.HashMap;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/20 下午4:14
 */
public class ContainerSingleton {

    private static HashMap<String, Object> map = new HashMap<>();

    private ContainerSingleton() {
    }

    public static void registerService(String key, Object service) {
        if (!map.containsKey(key)) {
            map.put(key, service);
        }
    }

    public static Object getService(String key) {
        return map.get(key);
    }
}
