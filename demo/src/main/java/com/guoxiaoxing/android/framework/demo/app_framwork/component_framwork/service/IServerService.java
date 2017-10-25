package com.guoxiaoxing.android.framework.demo.app_framwork.component_framwork.service;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/5/3 下午5:26
 */
public interface IServerService {
    void startCounter(int count, ICounterCallback callback);
    void stopCounter();
}
