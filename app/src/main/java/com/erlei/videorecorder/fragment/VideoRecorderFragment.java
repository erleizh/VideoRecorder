package com.erlei.videorecorder.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.erlei.videorecorder.R;
import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.recorder.DefaultCameraPreview;
import com.erlei.videorecorder.recorder.VideoRecorder;
import com.erlei.videorecorder.recorder.VideoRecorderHandler;
import com.erlei.videorecorder.util.LogUtil;

import java.io.File;
import java.util.Locale;

public class VideoRecorderFragment extends Fragment implements SurfaceHolder.Callback {

    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private VideoRecorder mRecorder;
    private CheckBox mBtnRecord;
    private TextView mTvFps;


    public static VideoRecorderFragment newInstance() {
        return new VideoRecorderFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_surface, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSurfaceView = view.findViewById(R.id.SurfaceView);
        mBtnRecord = view.findViewById(R.id.cbRecord);
        mTvFps = view.findViewById(R.id.tvFps);
        mBtnRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mRecorder != null) {
                    if (mRecorder.isRecordEnable()) {
                        mRecorder.stopRecord();
                    } else {
                        mRecorder.startRecord();
                    }
                }
            }
        });
        mSurfaceView.getHolder().addCallback(this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtil.logd("surfaceCreated " + System.currentTimeMillis());
        mRecorder = new VideoRecorder.Builder(new DefaultCameraPreview(mSurfaceView))
                .setCallbackHandler(new CallbackHandler())
                .setLogFPSEnable(false)
                .setFrameRate(30)
                .setOutPutFile(new File(getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES),"1.mp4"))
                .setChannelCount(2)
                .build();
        mRecorder.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mRecorder.onSizeChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mRecorder.stopPreview();
    }

    // TODO: 2018/6/26 暂时让他泄露吧~
    @SuppressLint("HandlerLeak")
    private class CallbackHandler extends VideoRecorderHandler {
        @Override
        protected void handleUpdateFPS(float fps) {
            mTvFps.setText(String.format(Locale.getDefault(), "%.2f", fps));
        }

        @Override
        protected void handleVideoMuxerStopped(@Nullable String output) {
            if (output != null) {
                Toast.makeText(getContext(), output, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "录制失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

