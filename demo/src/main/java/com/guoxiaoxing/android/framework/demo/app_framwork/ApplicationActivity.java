package com.guoxiaoxing.android.framework.demo.app_framwork;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;
import com.guoxiaoxing.android.framework.demo.app_framwork.component_framwork.activity.StartActivity;
import com.guoxiaoxing.android.framework.demo.app_practice.ui_develop.UIActivity;

public class ApplicationActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);

        findViewById(R.id.btn_application_activity).setOnClickListener(this);
        findViewById(R.id.btn_application_service).setOnClickListener(this);
        findViewById(R.id.btn_application_broadcast_receiver).setOnClickListener(this);
        findViewById(R.id.btn_application_ui).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_application_activity: {
                Intent intent = new Intent(ApplicationActivity.this, StartActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.btn_application_service: {
                startActivity(new Intent(ApplicationActivity.this, ClientActivity.class));
            }
            break;
            case R.id.btn_application_broadcast_receiver: {

            }
            break;
            case R.id.btn_application_ui: {
                startActivity(new Intent(ApplicationActivity.this, UIActivity.class));
            }
            break;
        }
    }
}
