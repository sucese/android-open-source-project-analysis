package com.guoxiaoxing.android.framework.demo.application.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

public class CounterService extends Service implements ICounterService {

    private boolean stop = false;
    private ICounterCallback counterCallback;
    private IBinder binder = new CounterBinder();

    public CounterService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void startCounter(int count, final ICounterCallback callback) {

        counterCallback = callback;

        AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Integer... params) {
                Integer count = params[0];

                stop = false;
                while (stop) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                }

                return count;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);

                int count = values[0];
                counterCallback.count(count);
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                counterCallback.count(integer);

            }
        };

        task.execute(count);
    }

    @Override
    public void stopCounter() {
        stop = true;
    }

    private class CounterBinder extends Binder {
        public CounterService getCounterService() {
            return CounterService.this;
        }

    }
}
