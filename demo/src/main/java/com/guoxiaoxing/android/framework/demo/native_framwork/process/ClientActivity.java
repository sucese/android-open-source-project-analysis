package com.guoxiaoxing.android.framework.demo.native_framwork.process;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.guoxiaoxing.android.framework.demo.R;

public class ClientActivity extends AppCompatActivity {

    private static final String TAG = "ClientActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binder);

        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                Log.d(TAG, "ClientActivity Process Pid : " + android.os.Process.myPid());
                IRemoteService iRemoteService = IRemoteService.Stub.asInterface(service);
                try {
                    Log.d(TAG, iRemoteService.getMessage());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
            }
        };
        Intent intent = new Intent(ClientActivity.this, RemoteService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
