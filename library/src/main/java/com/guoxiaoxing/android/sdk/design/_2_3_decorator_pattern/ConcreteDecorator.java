package com.guoxiaoxing.android.sdk.design._2_3_decorator_pattern;

// 具体装饰类
public class ConcreteDecorator extends AbstractDecorator {

    public ConcreteDecorator(AbstractComponent component) {
        super(component);
    }

    @Override
    protected void operation() {
        operationA();
        super.operation();
        operationB();
    }

    private void operationA() {

    }

    private void operationB() {

    }
}
