package com.guoxiaoxing.android.framework.demo.app_framwork.component_framwork.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.guoxiaoxing.android.framework.demo.R;

public class DemoFragmentActivity extends AppCompatActivity implements DemoFragmentA.OnFragmentInteractionListener,
        DemoFragmentB.OnFragmentInteractionListener, DemoFragmentC.OnFragmentInteractionListener, View.OnClickListener {

    DemoFragmentA demoFragmentA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_fragment);

        findViewById(R.id.btn_add_fragment).setOnClickListener(this);
        findViewById(R.id.btn_remove_fragment).setOnClickListener(this);
        findViewById(R.id.btn_replace_fragment).setOnClickListener(this);
        findViewById(R.id.btn_show_fragment).setOnClickListener(this);
        findViewById(R.id.btn_hide_fragment).setOnClickListener(this);
        findViewById(R.id.btn_detach_fragment).setOnClickListener(this);
        findViewById(R.id.btn_attach_fragment).setOnClickListener(this);
        findViewById(R.id.btn_pop_back_fragment).setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        add();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add_fragment:
                add();
                break;
            case R.id.btn_remove_fragment:
                remove();
                break;
            case R.id.btn_replace_fragment:
                replace();
                break;
            case R.id.btn_show_fragment:
                show();
                break;
            case R.id.btn_hide_fragment:
                hide();
                break;
            case R.id.btn_detach_fragment:
                detach();
                break;
            case R.id.btn_attach_fragment:
                attach();
            case R.id.btn_pop_back_fragment:
                popBack();
                break;
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private void add() {
        demoFragmentA = DemoFragmentA.newInstance("param1", "param2");
        Bundle bundle = new Bundle();
        demoFragmentA.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, demoFragmentA)
                .addToBackStack("FragmentA")
                .commit();
    }

    private void remove() {
        getSupportFragmentManager().beginTransaction()
                .remove(demoFragmentA)
                .commit();
    }

    private void replace() {
        DemoFragmentB demoFragmentB = DemoFragmentB.newInstance("param1", "param2");
        Bundle bundle = new Bundle();
        demoFragmentB.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, demoFragmentB)
                .addToBackStack("FragmentA")
                .commit();
    }

    private void show() {
        getSupportFragmentManager().beginTransaction()
                .show(demoFragmentA)
                .commit();
    }

    private void hide() {
        getSupportFragmentManager().beginTransaction()
                .hide(demoFragmentA)
                .commit();
    }

    private void detach() {
        getSupportFragmentManager().beginTransaction()
                .detach(demoFragmentA)
                .commit();
    }

    private void attach() {
        getSupportFragmentManager().beginTransaction()
                .attach(demoFragmentA)
                .commit();
    }



    private void popBack() {
        getSupportFragmentManager().popBackStack();
    }
}
