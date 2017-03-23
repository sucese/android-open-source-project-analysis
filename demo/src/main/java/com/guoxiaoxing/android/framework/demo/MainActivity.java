package com.guoxiaoxing.android.framework.demo;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_chapter1).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_chapter1:
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("标题")
                        .setMessage("消息")
                        .create();
                dialog.show();
                break;
        }
    }
}
