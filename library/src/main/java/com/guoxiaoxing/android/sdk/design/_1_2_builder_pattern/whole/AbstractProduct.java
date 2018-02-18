package com.guoxiaoxing.android.sdk.design._1_2_builder_pattern.whole;

/**
 * 抽象产品角色，定位一类产品的特性与行为。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:13
 */
public abstract class AbstractProduct {

    private String board;
    private String display;
    private String os;


    public void setBoard(String board) {
        this.board = board;
    }


    public void setDisplay(String display) {
        this.display = display;
    }


    public abstract void setOs(String os);
}
