package com.erlei.videorecorder.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.erlei.videorecorder.R;
import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.util.LogUtil;
import com.erlei.videorecorder.widget.CameraGLSurfaceView;

import java.io.File;

/**
 * Created by lll on 2018/1/20.
 * CameraGLSurfaceViewFragment
 */
public class CameraGLSurfaceViewFragment extends Fragment {

    private CameraGLSurfaceView mCameraGLSurfaceView;
    private Spinner mSpinner;
    private ArrayAdapter<Size> mAdapter;
    private Camera mCamera;

    public static CameraGLSurfaceViewFragment newInstance() {
        return new CameraGLSurfaceViewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_glsurfaceview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCameraGLSurfaceView = view.findViewById(R.id.CameraGLSurfaceView);
        mCameraGLSurfaceView.setCameraBuilder(getCameraBuilder());
        File file = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES), System.currentTimeMillis() + ".mp4");
        mSpinner = view.findViewById(R.id.spinner);
        view.findViewById(R.id.btnSwitch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera == null) return;
                mCamera.toggleFacing();
            }
        });
        mAdapter = new ArrayAdapter<Size>(getContext(), android.R.layout.simple_list_item_1);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Size item = mAdapter.getItem(position);
//                if (item == null) return;
//                LogUtil.logd("onItemSelected(" + position + ")");
//                Camera.CameraBuilder builder = new Camera.CameraBuilder(getContext())
//                        .useDefaultConfig()
//                        .addPreviewCallback(new Camera.CameraCallback() {
//                            @Override
//                            public void onSuccess(Camera camera) {
//                                mCamera = camera;
//                                mAdapter.clear();
//                                mAdapter.addAll(camera.getSupportedPreviewSizes());
//                            }
//
//                            @Override
//                            public void onFailure(int code, String msg) {
//
//                            }
//                        })
//                        .onSizeChanged(item);
//                mCameraGLTextureView.updateCameraParams(builder);


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private Camera.CameraBuilder getCameraBuilder() {
        return new Camera.CameraBuilder(getContext())
                .useDefaultConfig()
                .setFacing(android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setPreviewSize(new Size(2160, 1080))
                .addPreviewCallback(new Camera.CameraCallback() {
                    @Override
                    public void onSuccess(final Camera camera) {
                        mCamera = camera;
                        mAdapter.clear();
                        mAdapter.addAll(camera.getSupportedPreviewSizes());
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        LogUtil.loge("onFailure " + msg);
                    }
                });
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
