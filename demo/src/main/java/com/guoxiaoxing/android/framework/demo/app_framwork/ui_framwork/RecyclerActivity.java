package com.guoxiaoxing.android.framework.demo.app_framwork.ui_framwork;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.guoxiaoxing.android.framework.demo.R;

import java.util.ArrayList;
import java.util.List;

public class RecyclerActivity extends AppCompatActivity {


    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);

        recyclerView = findViewById(R.id.recycler_view);
        RecyclerAdapter adapter = new RecyclerAdapter();
        List<RecyclerModel> data = new ArrayList<>();
        for(int i = 0; i < 30; i++){
            data.add(new RecyclerModel());
        }
        adapter.setData(data);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}
