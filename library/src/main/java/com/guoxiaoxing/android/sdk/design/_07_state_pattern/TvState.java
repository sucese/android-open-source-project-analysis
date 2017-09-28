package com.guoxiaoxing.android.sdk.design._07_state_pattern;

/**
 * 抽象状态：电视机的操作
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:15
 */
public interface TvState {

    void nextChannel();

    void prevChannel();

    void turnUp();

    void turnDown();
}
