package com.guoxiaoxing.android.framework.demo.app_framwork.component_framwork.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.guoxiaoxing.android.framework.demo.R;

public class DemoFragmentB extends Fragment {

    private static final String TAG = "Fragment";

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public DemoFragmentB() {
        Log.d(TAG, "DemoFragmentB: DemoFragmentB()");
    }

    public static DemoFragmentB newInstance(String param1, String param2) {
        DemoFragmentB fragment = new DemoFragmentB();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "DemoFragmentB: onAttach()");
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "DemoFragmentB: onActivityCreated()");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DemoFragmentB: onCreate()");
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "DemoFragmentB: onCreateView() ");
        return inflater.inflate(R.layout.fragment_demo_b, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "DemoFragmentB: onViewCreated()");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "DemoFragmentB: onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "DemoFragmentB: onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "DemoFragmentB: onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "DemoFragmentB: onStop()");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "DemoFragmentB: onSaveInstanceState()");
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.d(TAG, "DemoFragmentB: onViewStateRestored()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "DemoFragmentB: onDestroyView()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DemoFragmentB: onDestroy()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "DemoFragmentB: onDetach()");
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "DemoFragmentB: onActivityResult()");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "DemoFragmentB: onConfigurationChanged()");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d(TAG, "DemoFragmentB: onHiddenChanged()");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(TAG, "DemoFragmentB: setUserVisibleHint()");
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        super.onAttachFragment(childFragment);
        Log.d(TAG, "DemoFragmentB: onAttachFragment()");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "DemoFragmentB: onLowMemory()");
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
