package com.guoxiaoxing.android.sdk.design._07_state_pattern;

/**
 * 上下文环境：电视遥控器
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/28 下午4:19
 */
public class TvController implements TvState {

    private TvState tvState;

    public void setTvState(TvState tvState) {
        this.tvState = tvState;
    }

    @Override
    public void nextChannel() {
        tvState.nextChannel();
    }

    @Override
    public void prevChannel() {
        tvState.prevChannel();
    }

    @Override
    public void turnUp() {
        tvState.turnUp();
    }

    @Override
    public void turnDown() {
        tvState.turnDown();
    }
}
