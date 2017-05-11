package com.guoxiaoxing.android.framework.demo.system;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;
import com.guoxiaoxing.android.framework.demo.system.aidl.AidlActivity;

public class SystemActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system);

        findViewById(R.id.btn_aidl).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_aidl:
                startActivity(new Intent(SystemActivity.this, AidlActivity.class));
                break;
        }
    }
}
