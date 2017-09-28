package com.guoxiaoxing.android.sdk.design._03_prototype_pattern;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/1 下午3:39
 */
public class Client {

    public static void main(String[] args) {

        ConcretePrototype source = new ConcretePrototype();
        source.setText("I am source");

        ConcretePrototype clone = source.clone();
        clone.setText("I am clone");

        System.out.println("source：" + source.getText());
        System.out.println("clone：" + clone.getText());
    }
}
