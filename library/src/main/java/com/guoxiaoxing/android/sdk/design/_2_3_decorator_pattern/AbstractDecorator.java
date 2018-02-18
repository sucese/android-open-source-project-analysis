package com.guoxiaoxing.android.sdk.design._2_3_decorator_pattern;

// 抽象装饰类
public abstract class AbstractDecorator extends AbstractComponent {

    private AbstractComponent mComponent;

    public AbstractDecorator(AbstractComponent component) {
        mComponent = component;
    }

    @Override
    protected void operation() {
        mComponent.operation();
    }
}
