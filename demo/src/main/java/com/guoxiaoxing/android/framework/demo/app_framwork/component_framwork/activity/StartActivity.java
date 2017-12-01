package com.guoxiaoxing.android.framework.demo.app_framwork.component_framwork.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;

public class StartActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        findViewById(R.id.activity_start_btn_in_new_process).setOnClickListener(this);
        findViewById(R.id.activity_start_btn_in_same_process).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_start_btn_in_new_process: {
                Intent intent = new Intent(StartActivity.this, SubInNewProcessActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            break;
            case R.id.activity_start_btn_in_same_process: {
                startActivity(new Intent(StartActivity.this, SubInProcessActivity.class));
            }
            break;
        }
    }
}
