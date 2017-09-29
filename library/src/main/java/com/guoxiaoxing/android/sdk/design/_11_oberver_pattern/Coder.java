package com.guoxiaoxing.android.sdk.design._11_oberver_pattern;

import java.util.Observable;
import java.util.Observer;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/29 上午10:59
 */
public class Coder implements Observer {

    @Override
    public void update(Observable o, Object arg) {
        System.out.println("我收到网站的更新啦");
    }
}
