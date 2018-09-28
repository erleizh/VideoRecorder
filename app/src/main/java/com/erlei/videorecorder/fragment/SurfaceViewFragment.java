package com.erlei.videorecorder.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.videorecorder.R;
import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.util.LogUtil;

/**
 * Created by lll on 2018/1/20.
 * 简单的打开一个相机预览
 */
public class SurfaceViewFragment extends Fragment implements SurfaceHolder.Callback, Camera.CameraCallback {

    private SurfaceView mSurfaceView;
    private Camera mCamera;

    public static SurfaceViewFragment newInstance() {
        return new SurfaceViewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return mSurfaceView = new SurfaceView(getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = new Camera.CameraBuilder(getContext())
                .useDefaultConfig()
                .setPreviewSize(new Size(1920, 960))
                .setSurfaceHolder(holder)
                .addPreviewCallback(this)
                .build()
                .open();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) mCamera.close();
    }

    /**
     * 打开相机预览成功 ， 可以在这个回调里根据camera.getPreviewSize() 重新调整SurfaceView的大小比例 ， 避免预览变形
     */
    @Override
    public void onSuccess(Camera camera) {
        adjustSurfaceViewSize(camera.getPreviewSize());
    }

    @Override
    public void onFailure(int code, String msg) {
        LogUtil.loge(msg);
    }

    /**
     * 调整SurfaceView的比例 , 以避免预览变形
     *
     * @param previewSize 预览大小
     */
    private void adjustSurfaceViewSize(Size previewSize) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = (int) (width * ((previewSize.getWidth() * 1.0f) / previewSize.getHeight()));
        ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) mSurfaceView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mSurfaceView.setLayoutParams(lp);
    }


}
