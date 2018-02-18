package com.guoxiaoxing.android.sdk.design._3_7_proxy_pattern;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

// 动态代理类，实现InvocationHandler接口。
public class DynamicProxy implements InvocationHandler {

    private Subject mSubject;

    public DynamicProxy(Subject subject) {
        mSubject = subject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        System.out.println("函数执行前自定义操作");
        // 调用被代理类的方法时会调用该方法
        method.invoke(mSubject, args);
        System.out.println("函数执行后自定义操作");
        return null;
    }
}
