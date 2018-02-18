package com.guoxiaoxing.android.sdk.design._1_6_abstract_factory_pattern;

// 具体工厂
public class ConcreteFactory extends AbstractFactory {

    @Override
    public AbstractProductA createProductA() {
        return new ConcreteProductA1();
    }

    @Override
    public AbstractProductB createProductB() {
        return new ConcreteProductB1();
    }
}
