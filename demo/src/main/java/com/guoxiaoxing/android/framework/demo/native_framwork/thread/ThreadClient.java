package com.guoxiaoxing.android.framework.demo.native_framwork.thread;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com
 *
 * @author guoxiaoxing
 * @since 2018/3/4 上午11:39
 */
public class ThreadClient {

    private Object lock1 = new Object();
    private Object lock2 = new Object();

    public static void main(String[] args) {
        new ThreadClient().deadLock();
    }

    private void deadLock() {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock1) {
                    System.out.println("get lock1");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("waiting lock2");
                    synchronized (lock2) {
                        System.out.println("get lock2");
                    }
                }

            }
        });
        thread1.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock2) {
                    System.out.println("get lock2");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("waiting lock1");
                    synchronized (lock1) {
                        System.out.println("get lock1");
                    }
                }
            }
        });
        thread2.start();
    }
}
