package com.guoxiaoxing.android.sdk.design._14_template_method;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/29 上午11:34
 */
public class RealComputer extends AbstractComputer {

    @Override
    public void powerOn() {
        System.out.println("开机");
    }

    @Override
    public void checkSelf() {
        System.out.println("自检");
    }

    @Override
    public void loadOS() {
        System.out.println("加载系统");
    }

    @Override
    public void login() {
        System.out.println("登录");
    }
}
