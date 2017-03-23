package com.guoxiaoxing.android.sdk.design.builder.simple;

/**
 * 如果系统中只需要一个具体建造者的话，可以省略掉抽象建造者。在具体建造者只有一个的情况下，如果抽象建造者角色已经被省略
 * 掉，那么还可以省略指挥者角色，让Builder角色扮演指挥者与建造者双重角色。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:05
 */
public class Product {

    public String board;
    public String display;
    public String os;

    class Builder {

        private Product product;

        public Builder() {
            product = new Product();
        }

        private String board;
        private String display;
        private String os;

        public void setBoard(String board) {
            product.board = board;
        }

        public void setDisplay(String display) {
            product.display = display;
        }

        public void setOs(String os) {
            product.os = os;
        }
    }

}
