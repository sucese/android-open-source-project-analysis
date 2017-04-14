package com.guoxiaoxing.android.sdk.design.proxy.dynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/14 下午5:50
 */
public class DynamicProxySubject implements InvocationHandler {

    private Object mRealSubject;

    public DynamicProxySubject(Object realSubject) {
        mRealSubject = realSubject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(mRealSubject, args);
        return null;
    }
}
