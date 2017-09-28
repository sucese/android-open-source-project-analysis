package com.guoxiaoxing.android.sdk.design._07_state_pattern;

/**
 * 具体状态：电视开机状态
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:16
 */
public class PowerOnState implements TvState {

    @Override
    public void nextChannel() {
        System.out.println("下一频道");
    }

    @Override
    public void prevChannel() {
        System.out.println("上一频道");
    }

    @Override
    public void turnUp() {
        System.out.println("开大音量");
    }

    @Override
    public void turnDown() {
        System.out.println("关小音量");
    }
}
