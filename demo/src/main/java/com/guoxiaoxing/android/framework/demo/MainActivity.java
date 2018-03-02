package com.guoxiaoxing.android.framework.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.app_framwork.ApplicationActivity;
import com.guoxiaoxing.android.framework.demo.native_framwork.SystemActivity;
import com.guoxiaoxing.android.framework.demo.program_idea.ProgramActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try{
            // 模拟数据加载
            Thread.sleep(2000);
            // 上报启动时间，这个方法在API 19 以下不支持，会crash，所以调用
            // 的时候做一下try catch
            reportFullyDrawn();
        }catch(Exception e){
            e.printStackTrace();
        }

        findViewById(R.id.btn_application).setOnClickListener(this);
        findViewById(R.id.btn_system).setOnClickListener(this);
        findViewById(R.id.btn_program).setOnClickListener(this);

        Debug.startMethodTracing();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Debug.stopMethodTracing();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_application: {
                Intent intent = new Intent(MainActivity.this, ApplicationActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.btn_system: {
                Intent intent = new Intent(MainActivity.this, SystemActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.btn_program: {
                Intent intent = new Intent(MainActivity.this, ProgramActivity.class);
                startActivity(intent);
            }
            break;
        }
    }
}
