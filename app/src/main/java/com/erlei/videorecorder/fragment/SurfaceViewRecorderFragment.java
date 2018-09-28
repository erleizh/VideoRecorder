package com.erlei.videorecorder.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.erlei.multipartrecorder.MultiPartRecorder;
import com.erlei.multipartrecorder.widget.MultiPartRecorderView;
import com.erlei.videorecorder.BuildConfig;
import com.erlei.videorecorder.R;
import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.effects.CanvasOverlayEffect;
import com.erlei.videorecorder.effects.EffectsManager;
import com.erlei.videorecorder.recorder.CameraController;
import com.erlei.videorecorder.recorder.DefaultCameraPreview;
import com.erlei.videorecorder.recorder.ICameraPreview;
import com.erlei.videorecorder.recorder.IVideoRecorder;
import com.erlei.videorecorder.recorder.VideoRecorder;
import com.erlei.videorecorder.recorder.VideoRecorderHandler;
import com.erlei.videorecorder.util.FPSCounterFactory;
import com.erlei.videorecorder.util.LogUtil;
import com.erlei.videorecorder.util.RecordGestureDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;


/**
 * Created by lll on 2018/9/21
 * Email : lllemail@foxmail.com
 * Describe : 点击拍照，长按开始录制 ，松手停止录制，每一次长按操作会生成一个视频文件，最终点击Next时会使用 mp4Parser 合并视频文件
 * 使用SurfaceView 预览
 */
public class SurfaceViewRecorderFragment extends Fragment implements SettingsDialogFragment.CameraControllerView {

    private SurfaceView mSurfaceView;
    private MultiPartRecorder mRecorder;
    private View mBtnRecord;
    private TextView mTvFps;
    private MultiPartRecorderView mRecorderIndicator;
    private CameraController mCameraController;

    public static SurfaceViewRecorderFragment newInstance() {
        SurfaceViewRecorderFragment fragment = new SurfaceViewRecorderFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_surfaceview_recorder, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSurfaceView = view.findViewById(R.id.SurfaceView);
        mBtnRecord = view.findViewById(R.id.cbRecord);
        mTvFps = view.findViewById(R.id.tvFps);
        mRecorderIndicator = view.findViewById(R.id.recorderIndicator);
        mRecorderIndicator.setMaxDuration(60 * 3);
        mRecorderIndicator.setRecordListener(mRecorderIndicatorListener);

        mBtnRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(v, event);
            }
        });
        view.findViewById(R.id.btnNext).setOnClickListener(mNextClickListener);
        view.findViewById(R.id.btnRemove).setOnClickListener(mRemoveClickListener);
        mSurfaceView.getHolder().addCallback(mSurfaceCallback);
        ((CheckBox) view.findViewById(R.id.cbToggleFacing)).setOnCheckedChangeListener(mToggleFacingListener);
        view.findViewById(R.id.ivSettings).setOnClickListener(mOnSettingsClickListener);
        mSurfaceView.setOnTouchListener(mTouchListener);
    }

    /**
     * Surface 变化
     */
    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (mRecorder == null) initRecorder();
            startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mRecorder.onSizeChanged(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopPreview();
        }
    };


    /**
     * 视频分段录制监听
     */
    private MultiPartRecorder.VideoPartListener mVideoPartListener = new MultiPartRecorder.VideoPartListener() {
        @Override
        public void onRecordVideoPartStarted(MultiPartRecorder.Part part) {
            LogUtil.logd("onRecordVideoPartStarted \t" + part.toString());
        }

        @Override
        public void onRecordVideoPartSuccess(MultiPartRecorder.Part part) {
            LogUtil.logd("onRecordVideoPartSuccess \t" + part.toString());
        }

        @Override
        public void onRecordVideoPartFailure(MultiPartRecorder.Part part) {
            LogUtil.loge("onRecordVideoPartFailure \t" + part.file);
            mRecorderIndicator.removePart(part.file.getAbsolutePath());
            mRecorder.removePart(part.file.getAbsolutePath());
        }
    };


    /**
     * 视频段合并监听
     */
    private MultiPartRecorder.VideoMergeListener mVideoMergeListener = new MultiPartRecorder.VideoMergeListener() {
        @Override
        public void onStart() {
            LogUtil.logd("merge onStart");
        }

        @Override
        public void onSuccess(File outFile) {
            LogUtil.logd("merge Success \t" + outFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= 24) {
                intent.setDataAndType(FileProvider.getUriForFile(getContext().getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", outFile), "video/*");
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(outFile), "video/*");
            }
            startActivity(intent);
        }

        @Override
        public void onError(Exception e) {
            LogUtil.logd("merge Error \t" + e.toString());
        }

        /**
         * 合并进度
         *
         * @param value 0 - 1
         */
        @Override
        public void onProgress(float value) {
            LogUtil.logd("merge onProgress \t" + value);
        }

    };

    /**
     * 分段录制时 ， 如果某个分段太短 ， 会导致视频合并失败 ，所以需要过滤一下
     */
    private MultiPartRecorder.FileFilter mFileFilter = new MultiPartRecorder.FileFilter() {
        @Override
        public boolean filter(MultiPartRecorder.Part part) {
            return part.duration > 1500;
        }
    };

    /**
     * 使用Canvas 在录制视频的时候绘制到视频里面
     */
    private CanvasOverlayEffect mCanvasOverlayEffect = new CanvasOverlayEffect() {
        private FPSCounterFactory.FPSCounter1 mCounter;
        Paint mPaint;

        @Override
        public void prepare(Size size) {
            super.prepare(size);
            mPaint = new Paint();
            mPaint.setColor(Color.YELLOW);
            mPaint.setAlpha(230);
            mPaint.setTextSize(40);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);

            mCounter = new FPSCounterFactory.FPSCounter1();
        }

        @Override
        protected void drawCanvas(Canvas canvas) {
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", mCounter.getFPS()), canvas.getWidth() / 2, canvas.getHeight() / 2, mPaint);
        }
    };

    /**
     * 录制按钮手势处理器，点击拍照，长按录制
     */
    private final RecordGestureDetector mGestureDetector = new RecordGestureDetector(new RecordGestureDetector.SimpleOnGestureListener() {
        private static final String TAG = "TouchGestureDetector";

        @Override
        public void onLongPressDown(View view, MotionEvent e) {
            LogUtil.logd(TAG, "onLongPressDown");
            view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(200).start();
            view.setSelected(true);
            startRecord();
        }

        @Override
        public void onLongPressUp(View view, MotionEvent e) {
            view.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
            view.setSelected(false);
            stopRecord();
            LogUtil.logd(TAG, "onLongPressUp");
        }

        @Override
        public void onSingleTap(View view, MotionEvent e) {
            LogUtil.logd(TAG, "onSingleTap " + System.currentTimeMillis());
            mRecorder.takePicture(new IVideoRecorder.TakePictureCallback() {
                @Override
                public void onPictureTaken(File picture) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT >= 24) {
                        intent.setDataAndType(FileProvider.getUriForFile(getContext().getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", picture), "image/*");
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        intent.setDataAndType(Uri.fromFile(picture), "image/*");
                    }
                    startActivity(intent);
                }

                @Override
                public File onPictureTaken(Bitmap bitmap) {
                    return new File(getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES), System.currentTimeMillis() + ".png");
                }
            });
        }
    });


    /**
     * 下一步按钮，点击之后会开始合并视频片段
     */
    private View.OnClickListener mNextClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mRecorderIndicator.removeAllPart();
            // onDestroy task.cancel()
            AsyncTask<MultiPartRecorder.Part, Float, File> task = mRecorder.mergeVideoParts();

        }
    };


    /**
     * 当点击删除按钮
     * 会删除上一段视频
     */
    private View.OnClickListener mRemoveClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mRecorderIndicator.lastPartRemoved()) {
                if (!mRecorder.isRecordEnable()) {
                    mRecorderIndicator.markLastPartRemove();
                }
            } else {
                mRecorderIndicator.removeLastPart();
                mRecorder.removeLastPart();
            }

        }
    };


    /**
     * 分段录制进度条变化
     */
    private MultiPartRecorderView.RecordListener mRecorderIndicatorListener = new MultiPartRecorderView.RecordListener() {
        /**
         * 超过最小录制时间
         */
        @Override
        public void onOvertakeMinTime() {

        }

        /**
         * 超过最大录制时间
         *
         * @param parts 视频块
         */
        @Override
        public void onOvertakeMaxTime(ArrayList<MultiPartRecorderView.Part> parts) {
            disableRecordButton();
            stopRecord();
            mBtnRecord.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enableRecordButton();
                }
            }, 200);
        }

        /**
         * 视频时长改变
         *
         * @param duration 时长
         */
        @Override
        public void onDurationChange(long duration) {

        }
    };


    /**
     * 当触摸视频预览区域时
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mCameraController.setFocusAreaOnTouch(event);
            mCameraController.setMeteringAreaOnTouch(event);
            mCameraController.setZoomOnTouch(event);
            return true;
        }
    };


    /**
     * 当点击切换朝向
     */
    private CompoundButton.OnCheckedChangeListener mToggleFacingListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mCameraController.toggleFacing();
        }
    };


    /**
     * 相机设置按钮点击
     */
    private View.OnClickListener mOnSettingsClickListener = new View.OnClickListener() {

        private SettingsDialogFragment mSettingsDialogFragment;

        @Override
        public void onClick(View v) {
            if (mSettingsDialogFragment != null && mSettingsDialogFragment.isVisible()) {
                mSettingsDialogFragment.dismiss();
                mSettingsDialogFragment = null;
            }
            mSettingsDialogFragment = SettingsDialogFragment.newInstance();
            mSettingsDialogFragment.show(getChildFragmentManager(), SettingsDialogFragment.class.getName());
        }
    };


    private void stopPreview() {
        mRecorder.stopPreview();
    }

    private void startPreview() {
        if (mRecorder == null) initRecorder();
        mRecorder.startPreview();
    }

    public void disableRecordButton() {
        mBtnRecord.setSelected(false);
        mBtnRecord.clearAnimation();
        mBtnRecord.setScaleX(1);
        mBtnRecord.setScaleY(1);
        mBtnRecord.setClickable(false);
    }

    public void enableRecordButton() {
        mBtnRecord.setSelected(false);
        mBtnRecord.clearAnimation();
        mBtnRecord.setScaleX(1);
        mBtnRecord.setScaleY(1);
        mBtnRecord.setClickable(true);
    }

    private void stopRecord() {
        if (mRecorder != null && mRecorder.isRecordEnable()) {
            mRecorder.setRecordEnabled(false);
            mRecorderIndicator.stopRecord();
        }
    }

    private void startRecord() {
        if (mRecorder != null && !mRecorder.isRecordEnable()) {
            mRecorder.setRecordEnabled(true);
            mRecorderIndicator.startRecord(mRecorder.getCurrentPartFile());
        }
    }


    /**
     * 初始录像机
     */
    private void initRecorder() {
        ICameraPreview cameraPreview = new DefaultCameraPreview(mSurfaceView);
//        ICameraPreview cameraPreview = new OffscreenCameraPreview(getContext(), 1920, 1920);

        Camera.CameraBuilder cameraBuilder = new Camera.CameraBuilder(getActivity())
                .useDefaultConfig()
                .setFacing(android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setPreviewSize(new Size(2048, 1536))
                .setRecordingHint(true)
                .setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        EffectsManager effectsManager = new EffectsManager();
        effectsManager.addEffect(mCanvasOverlayEffect);

        VideoRecorder.Builder builder = new VideoRecorder.Builder(cameraPreview)
                .setCallbackHandler(new CallbackHandler())
                .setLogFPSEnable(false)
                .setCameraBuilder(cameraBuilder)
                .setDrawTextureListener(effectsManager)
                .setOutPutPath(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), File.separator + "VideoRecorder").getAbsolutePath())
                .setFrameRate(30)
                .setChannelCount(1);

        MultiPartRecorder.Builder multiBuilder = new MultiPartRecorder.Builder(builder);
        mRecorder = multiBuilder
                .addPartListener(mVideoPartListener)
                .setMergeListener(mVideoMergeListener)
                .setFileFilter(mFileFilter)
                .build();

        mCameraController = mRecorder.getCameraController();
    }

    @Override
    public CameraController getCameraController() {
        return mCameraController;
    }

    @Override
    public void setPreviewSize(Size item) {
        if (item.equals(mCameraController.getCameraSize())) return;
        //重新设置相机的预览分辨率 , 并调用onSizeChanged更新渲染的纹理坐标
        Camera.CameraBuilder cameraBuilder = mCameraController.getCameraBuilder();
        cameraBuilder.setPreviewSize(item);
        mCameraController.setCameraBuilder(cameraBuilder);
        Size surfaceSize = mCameraController.getSurfaceSize();
        mCameraController.closeCamera();
        mCameraController.openCamera(mRecorder.getPreviewTexture());
        mRecorder.onSizeChanged(surfaceSize.getWidth(), surfaceSize.getHeight());
    }

    // TODO: 2018/6/26 暂时让他泄露吧~
    @SuppressLint("HandlerLeak")
    private class CallbackHandler extends VideoRecorderHandler {

        private Toast mToast;

        @Override
        protected void handleUpdateFPS(float fps) {
            mTvFps.setText(String.format(Locale.getDefault(), "%.2f", fps));
        }

        @SuppressLint("ShowToast")
        @Override
        protected void handleVideoMuxerStopped(String output) {
            if (mToast != null) {
                mToast.setText(output);
            } else {
                mToast = Toast.makeText(getContext(), output, Toast.LENGTH_SHORT);
            }
            mToast.show();
        }
    }
}

