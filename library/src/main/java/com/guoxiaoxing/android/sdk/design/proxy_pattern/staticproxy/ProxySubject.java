package com.guoxiaoxing.android.sdk.design.proxy_pattern.staticproxy;

/**
 * 代理类
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:22
 */
public class ProxySubject extends AbstractSubject {

    private RealSubject mRealSubject;

    public ProxySubject(RealSubject realSubject) {
        mRealSubject = realSubject;
    }

    @Override
    public void visit() {
        //调用真实主题里方法
        mRealSubject.visit();
    }
}
