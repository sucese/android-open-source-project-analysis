package com.guoxiaoxing.android.framework.demo.native_framwork.thread;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
        new ThreadClient().threadMethod();
    }

    private void deadLock1() {
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

    private void deadLock2() {
        Lock lock = new ReentrantLock();
        lock.lock();
        try {
            System.out.println("A exception happened");
            throw new Throwable("A exception happened");
        } catch (Exception e) {
            // 不应该在此处释放锁，假如发生无法捕获的异常，会导致锁无法释放，可能会导致死锁
            lock.unlock();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            // 应该在此处释放锁
//            lock.unlock();
        }
    }

    private void threadMethod(){
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    System.out.println("I am thread1");
                }
            }
        });
        thread1.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    System.out.println("I am thread2");
                }
            }
        });
        thread2.start();
    }
}
