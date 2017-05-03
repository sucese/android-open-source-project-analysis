package com.guoxiaoxing.android.framework.demo.application.service;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.guoxiaoxing.android.framework.demo.R;

public class ServiceActivity extends AppCompatActivity implements ICounterCallback{

    private TextView mTvCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        Intent intent = new Intent(ServiceActivity.this, CounterService.class);


        mTvCounter = (TextView) findViewById(R.id.tv_counter);

        findViewById(R.id.btn_start_counter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        findViewById(R.id.btn_stop_counter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    @Override
    public void count(int count) {

    }
}
