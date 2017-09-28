package com.guoxiaoxing.android.sdk.design._02_builder_pattern.whole;

/**
 * 具体产品角色，实现具体的产品特性与行为
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:06
 */
public class RealProduct extends AbstractProduct {

    @Override
    public void setOs(String os) {
        os = "mac osx";
    }

    /**
     * 设置参数内部类，构建的时候先构建参数，完成后再创建实例并装配参数
     */
    public static class RealProductParams {
        public String board;
        public String display;
        public String os;

        public void applyParams(RealProduct product) {
            product.setBoard(board);
            product.setDisplay(display);
            product.setOs(os);
        }
    }
}
