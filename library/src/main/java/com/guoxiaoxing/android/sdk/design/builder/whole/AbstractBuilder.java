package com.guoxiaoxing.android.sdk.design.builder.whole;

/**
 * 抽象建造者橘色，抽象构建行为，具体实现交由子类完成。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/22 下午6:27
 */
public abstract class AbstractBuilder {

    public abstract void setBoard(String board);

    public abstract void setDisplay(String display);

    public abstract void setOs(String os);
}
