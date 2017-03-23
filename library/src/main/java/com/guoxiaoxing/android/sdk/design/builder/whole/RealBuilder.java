package com.guoxiaoxing.android.sdk.design.builder.whole;

/**
 * 具体建造者橘色，完成具体构建行为。一般持有产品的引用，并在build()方法中返回该引用。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/3/23 上午10:11
 */
public class RealBuilder extends AbstractBuilder {

    private RealProduct.RealProductParams productParams;

    public RealBuilder() {
        productParams = new RealProduct.RealProductParams();
    }

    @Override
    public void setBoard(String board) {
        productParams.board = board;
    }

    @Override
    public void setDisplay(String display) {
        productParams.display = display;
    }

    @Override
    public void setOs(String os) {
        productParams.os = os;
    }

    public RealProduct build() {
        RealProduct product = new RealProduct();
        productParams.applyParams(product);
        return product;
    }
}
