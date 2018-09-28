package com.erlei.videorecorder.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.videorecorder.widget.CameraController;
import com.erlei.videorecorder.widget.CameraGLSurfaceView;

/**
 * Created by lll on 2018/9/28 .
 * Email : lllemail@foxmail.com
 * Describe : 自定义的CameraGLSurfaceView，CameraGLTextureView
 */
public class CameraRenderViewFragment extends Fragment {


    private CameraGLSurfaceView mCameraGLSurfaceView;
    private CameraController mCameraController;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mCameraGLSurfaceView = new CameraGLSurfaceView(getContext());
        return mCameraGLSurfaceView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mCameraController = new CameraController(mCameraGLSurfaceView);
        mCameraGLSurfaceView.setCameraController(mCameraController);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraGLSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraGLSurfaceView.onPause();
    }

    public static Fragment newInstance() {
        return new CameraRenderViewFragment();
    }
}
