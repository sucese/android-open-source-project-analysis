package com.guoxiaoxing.android.sdk.design.bridge_pattern;

/**
 * 抽象类，该类保持了一个对实现部分对象的引用
 *
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/5/31 上午9:59
 */
public abstract class Abstraction {

    private Implementor mImplementor;

    public Abstraction(Implementor implementor){
        mImplementor = implementor;
    }

    public void operation(){
        mImplementor.operationImpl();
    }


}
