package com.guoxiaoxing.android.framework.demo.app_practice.performance_optimization;

import android.content.Context;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/12/21 下午1:57
 */
public class ContextManager {

    private static ContextManager instance;
    private Context mContext;

    public ContextManager(Context context) {
        mContext = context;
    }

    public static ContextManager getInstance(Context context) {
        if(instance == null){
            synchronized (ContextManager.class) {
                if (instance == null) {
                    instance = new ContextManager(context);
                }
            }
        }
        return instance;
    }
}
