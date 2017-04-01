package com.guoxiaoxing.android.sdk.design.prototype;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloneable为Prototype橘色，表示具备clone能力。
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/4/1 下午3:30
 */
public class ConcretePrototype implements Cloneable {

    private String text;
    private ArrayList<String> images;

    public ConcretePrototype() {

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }

    @Override
    protected ConcretePrototype clone() {

        try {
            ConcretePrototype concretePrototype = (ConcretePrototype) super.clone();
            //java 中对于基本型变量采用的是值传递，而对于对象传递时采用的引用传递也就是地址传递
            concretePrototype.text = this.text;
            //ArrayList实现了clone()方法
            concretePrototype.images = (ArrayList<String>) this.images.clone();
            return concretePrototype;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
