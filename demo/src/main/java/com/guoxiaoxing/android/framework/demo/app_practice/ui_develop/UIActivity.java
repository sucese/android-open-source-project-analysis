package com.guoxiaoxing.android.framework.demo.app_practice.ui_develop;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;

public class UIActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui);

        findViewById(R.id.btn_ui_draw).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ui_draw:
                startActivity(new Intent(v.getContext(), DrawActivity.class));
                break;
        }
    }
}
