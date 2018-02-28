package com.guoxiaoxing.android.framework.demo.app_framwork.java;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2018/2/26 下午4:31
 */
public class Client {

    public static void main(String[] args){
        Integer a = new Integer(1);
        Integer b = new Integer(2);

        Integer c = 1;
        Integer d = 2;

        int e = 1;

        System.out.println(a == b);//false
        System.out.println(c == d);//false
        System.out.println(a == e);//false
    }
}
