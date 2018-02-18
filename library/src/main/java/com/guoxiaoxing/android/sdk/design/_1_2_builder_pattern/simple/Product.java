package com.guoxiaoxing.android.sdk.design._1_2_builder_pattern.simple;

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

    private String board;
    private String display;
    private String os;

    /**
     * get config which the user set
     * @return String
     */
    public String getBoard() {
        return board;
    }

    public String getDisplay() {
        return display;
    }

    public String getOs() {
        return os;
    }

    private Product(Builder builder) {
        this.board = builder.board;
        this.display = builder.display;
        this.os = builder.os;
    }

    public static class Builder {
        private String board = "default value";
        private String display = "default value";
        private String os = "default value";

        public void setBoard(String board) {
            this.board = board;
        }

        public void setDisplay(String display) {
            this.display = display;
        }

        public void setOs(String os) {
            this.os = os;
        }


        public Product build() {
            return new Product(this);
        }
    }
}
