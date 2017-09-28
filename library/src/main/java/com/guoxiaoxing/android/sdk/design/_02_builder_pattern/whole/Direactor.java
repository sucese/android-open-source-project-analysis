package com.guoxiaoxing.android.sdk.design._02_builder_pattern.whole;

/**
 * 指挥者角色，指挥多个建造者进行构建过程，当只有一个建造者时此角色可以省略。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:18
 */
public class Direactor {

    private AbstractBuilder builder;

    public void setBuilder(AbstractBuilder builder) {
        builder = builder;
    }

    private void buildProduct(String board, String display, String os) {
        builder.setBoard(board);
        builder.setDisplay(display);
        builder.setOs(os);
    }
}
