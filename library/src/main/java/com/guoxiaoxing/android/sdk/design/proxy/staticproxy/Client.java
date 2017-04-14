package com.guoxiaoxing.android.sdk.design.proxy.staticproxy;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:25
 */
public class Client {

    public static void main(String[] args) {
        //创建真实角色对象
        RealSubject realSubject = new RealSubject();
        //创建代理角色对象
        ProxySubject proxySubject = new ProxySubject(realSubject);
        //调用代理角色方法
        proxySubject.visit();
    }
}
