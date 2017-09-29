package com.guoxiaoxing.android.sdk.design._11_oberver_pattern;

import java.util.Observable;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/29 上午10:59
 */
public class WebSite extends Observable {

    public void postNewDocument(String document) {
        setChanged();
        notifyObservers(document);
    }
}
