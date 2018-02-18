package com.guoxiaoxing.android.sdk.design._1_3_prototype_pattern;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2018/2/15 下午9:00
 */
public class Person implements Cloneable{

    public int age;
    public String name;

    @Override
    public Person clone() throws CloneNotSupportedException {
        return (Person) super.clone();
    }
}
