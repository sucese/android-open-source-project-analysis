package com.guoxiaoxing.android.sdk.design._11_oberver_pattern;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2017/9/29 上午11:01
 */
public class Client {

    public static void main(String[] args) {

        Coder coder1 = new Coder();
        Coder coder2 = new Coder();
        Coder coder3 = new Coder();
        Coder coder4 = new Coder();
        Coder coder5 = new Coder();

        WebSite webSite = new WebSite();
        webSite.addObserver(coder1);
        webSite.addObserver(coder2);
        webSite.addObserver(coder3);
        webSite.addObserver(coder4);
        webSite.addObserver(coder5);

        webSite.postNewDocument("有新文章啦");
    }
}
