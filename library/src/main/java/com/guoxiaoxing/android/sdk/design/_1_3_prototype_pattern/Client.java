package com.guoxiaoxing.android.sdk.design._1_3_prototype_pattern;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2018/2/15 下午8:53
 */
public class Client {

    public static void main(String[] args) {
        Person person1 = new Person();
        person1.age = 20;
        person1.name = "Li Lei";

        try {
            Person person2 = person1.clone();
            System.out.println(person1 == person2);
            System.out.println(person1.age == person2.age);
            System.out.println(person1.name == person2.name);

            person1.name = "Han Mei Mei";

            System.out.println(person2.name);

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }
}