package com.guoxiaoxing.android.sdk.design.proxy_pattern.staticproxy;

/**
 * 真实主题角色，实现了抽象主题。
 *
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:18
 */
public class RealSubject extends AbstractSubject {

    @Override
    public void visit() {
        System.out.println("真实主题");
    }
}
