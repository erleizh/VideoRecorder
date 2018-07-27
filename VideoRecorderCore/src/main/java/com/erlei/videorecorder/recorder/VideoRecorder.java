package com.erlei.videorecorder.recorder;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Environment;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.encoder1.MediaAudioEncoder;
import com.erlei.videorecorder.encoder1.MediaMuxerWrapper;
import com.erlei.videorecorder.encoder1.MediaVideoEncoder;
import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.gles.WindowSurface;
import com.erlei.videorecorder.util.LogUtil;
import com.erlei.videorecorder.util.SaveFrameTask;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoRecorder implements RenderThread.RenderCallBack, IVideoRecorder {

    private static final String TAG = LogUtil.TAG;
    private final Object mSync = new Object();
    private final Config mConfig;
    private File mOutputFile;
    private ExecutorService mThreadExecutor;
    private RenderThread mRenderThread;
    private volatile boolean mRecordEnabled, mMuxerRunning, mRequestStart, mRequestStop, mPreviewState;
    private volatile WindowSurface mInputWindowSurface;
    private volatile MediaVideoEncoder mVideoEncoder;
    private volatile MediaMuxerWrapper mMuxer;
    private ByteBuffer mByteBuffer;
    private Size mSize;
    private volatile boolean mTakePicture;
    private TakePictureCallback mPictureCallback;

    private VideoRecorder(Config p) {
        mConfig = p;
        mOutputFile = getOutPut();
    }

    public Config getConfig() {
        return mConfig;
    }

    @Override
    public synchronized void startPreview() {
        if (mPreviewState) return;
        mThreadExecutor = Executors.newSingleThreadExecutor();
        mRenderThread = new RenderThread(mConfig);
        mRenderThread.setCallBack(this);
        mRenderThread.start();
        mPreviewState = true;
    }

    @Override
    public synchronized void startRecord() {
        setRecordEnabled(true);
    }

    @Override
    public synchronized void stopRecord() {
        setRecordEnabled(false);
    }

    @Override
    public CameraController getCameraController() {
        return mConfig.cameraController;
    }

    @Override
    public boolean isRecordEnable() {
        return mRecordEnabled;
    }

    /**
     * @return 混合器是否正在运行
     */
    @Override
    public boolean isMuxerRunning() {
        return mMuxerRunning;
    }

    @Override
    public void onSizeChanged(int width, int height) {
        mRenderThread.getHandler().onSizeChanged(width, height);

    }

    public SurfaceTexture getPreviewTexture() {
        return mRenderThread.getSurfaceTexture();
    }

    /**
     * 拍照
     */
    @Override
    public synchronized void takePicture(TakePictureCallback callback) {
        mTakePicture = true;
        mSize = mConfig.getCameraPreview().getSurfaceSize();
        mByteBuffer = ByteBuffer.allocateDirect(mSize.getHeight() * mSize.getWidth() * 4).order(ByteOrder.nativeOrder());
        mPictureCallback = callback;
    }

    public synchronized void setRecordEnabled(boolean enable) {
        if (isRecordEnable() == enable) {
            LogUtil.loge(TAG, "setRecordEnabled:mRecordEnabled == enable");
            return;
        }
        if (!mPreviewState) {
            LogUtil.loge(TAG, "setRecordEnabled:mPreviewState == true");
            return;
        }
        if (enable) {
            startEncoder();
        } else {
            stopEncoder();
        }
    }

    private synchronized void startEncoder() {
        mRecordEnabled = true;
        mRequestStart = true;
        LogUtil.loge(TAG, "startEncoder:begin");
        mOutputFile = getOutPut();
        mThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mPreviewState) return;
                LogUtil.loge(TAG, "startEncoder:begin");
                synchronized (mSync) {
                    try {
                        mMuxer = new MediaMuxerWrapper(mOutputFile.getAbsolutePath(), mConfig.viewHandler);
                        mVideoEncoder = new MediaVideoEncoder(mMuxer, mConfig);
                        new MediaAudioEncoder(mMuxer, mConfig);
                        mMuxer.prepare();
                        mMuxer.startRecording();

                        mInputWindowSurface = new WindowSurface(mRenderThread.getEglCore(), mVideoEncoder.getSurface(), true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtil.loge(TAG, "startEncoder:" + e);
                    }
                    mMuxerRunning = true;
                    mRequestStart = false;
                }
                if (mConfig.viewHandler != null) {
                    mConfig.viewHandler.onCaptureStarted(mOutputFile.getAbsolutePath());
                }
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NonNull
    private File getOutPut() {
        if (mConfig.mOutputFile != null) {
            return mConfig.mOutputFile;
        } else {
            File path = new File(mConfig.outputPath);
            if (!path.exists()) {
                path.mkdirs();
            }
            if (path.isFile()) path = path.getParentFile();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
            return new File(path, format.format(new Date()) + ".mp4");
        }
    }

    private synchronized void stopEncoder() {
        mRecordEnabled = false;
        mRequestStop = true;
        LogUtil.loge(TAG, "stopEncoder:begin");
        mThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mPreviewState && !mMuxerRunning) return;
                synchronized (mSync) {
                    LogUtil.loge(TAG, "stopEncoder:begin");
                    mMuxerRunning = false;
                    try {
                        if (mMuxer != null) {
                            mMuxer.stopRecording();
                            mMuxer = null;
                        }
                        if (mInputWindowSurface != null) {
                            mInputWindowSurface.release();
                            mInputWindowSurface = null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtil.loge(TAG, "stopEncoder:" + e);
                    }
                    mRequestStop = false;
                }
                if (mConfig.viewHandler != null) {
                    mConfig.viewHandler.onCaptureStopped(mOutputFile.getAbsolutePath());
                }
            }
        });
    }


    @Override
    public synchronized void stopPreview() {
        if (!mPreviewState) return;
        mPreviewState = false;
        mThreadExecutor.shutdownNow();
        mThreadExecutor = null;
        mMuxerRunning = false;
        mRecordEnabled = false;
        mRenderThread.getHandler().destroy();

        mConfig.cameraController.closeCamera();
    }

    @Override
    public void release() {

    }

    @Override
    public File getOutputFile() {
        return mOutputFile;
    }

    @Override
    public void onPrepared(EglCore eglCore) {
        mConfig.cameraController.openCamera(getPreviewTexture());
    }

    @Override
    public synchronized void onDrawFrame(CameraGLRenderer renderer) {
        //使用mSync同步锁将导致录制开始的时候卡顿一下
//        && !mRequestStart && !mRequestStop
        if (mInputWindowSurface != null && mVideoEncoder != null && mRecordEnabled && mMuxerRunning && mPreviewState) {
            mInputWindowSurface.makeCurrent();
            mVideoEncoder.frameAvailableSoon();
            renderer.onDrawFrame();
            mInputWindowSurface.swapBuffers();
        }

        if (mByteBuffer != null && mTakePicture) {
            mByteBuffer.rewind();
            GLES20.glReadPixels(0, 0, mSize.getWidth(), mSize.getHeight(), GLES30.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mByteBuffer);
            new SaveFrameTask(mSize.getWidth(), mSize.getHeight(), mPictureCallback).execute(mByteBuffer);
            mTakePicture = false;
            mPictureCallback = null;
            mByteBuffer = null;
        }
    }

    @Override
    public void onStopped() {

    }

    public static class Builder {

        private final Config mP;

        public Builder(ICameraPreview cameraPreview) {
            mP = new Config(cameraPreview.getContext(), cameraPreview);
        }

        public Config getConfig() {
            return mP;
        }

        public Builder setCameraController(CameraController controller) {
            mP.cameraController = controller;
            return this;
        }

        /**
         * 设置期望的帧率
         * 默认为25
         */
        public Builder setFrameRate(int frameRate) {
            mP.frameRate = frameRate;
            return this;
        }

        /**
         * 设置声道数
         */
        public Builder setChannelCount(@IntRange(from = 1, to = 2) int channelCount) {
            mP.audioChannelCount = channelCount;
            return this;
        }

        /**
         * 设置音频采样率
         * 默认为 44100
         */
        public Builder setAudioSampleRate(int sampleRate) {
            mP.audioSampleRate = sampleRate;
            return this;
        }

        /**
         * @param bitRate 设置视频比特率
         *                默认为 width * height *  3 * 4
         */
        public Builder setVideoBitRate(int bitRate) {
            mP.videoBitRate = bitRate;
            return this;
        }

        /**
         * @param bitRate 设置音频比特率
         *                默认为 64000
         */
        public Builder setAudioBitRate(int bitRate) {
            mP.audioBitRate = bitRate;
            return this;
        }

        /**
         * 设置关键帧间隔
         */
        public Builder setIFrameInterval(int interval) {
            mP.iFrameInterval = interval;
            return this;
        }

        /**
         * @param file 设置输出文件 , 无论一个 VideoRecorder实例开启几次录制 , 之后最后一次的录制文件会保存
         */
        public Builder setOutPutFile(File file) {
            mP.mOutputFile = file;
            return this;
        }

        /**
         * @param outputPath 输出文件夹 , 只有沒 setOutPutFile ,这个属性才会起作用, 每一次startRecord都会生成一个新的文件
         */
        public Builder setOutPutPath(String outputPath) {
            mP.outputPath = outputPath;
            return this;
        }

        /**
         * @param enable 是否启用FPS日志输出
         */
        public Builder setLogFPSEnable(boolean enable) {
            mP.logFPS = enable;
            return this;
        }

        public VideoRecorder build() {
            if (mP.context == null)
                throw new IllegalArgumentException("context cannot be null");

            if (mP.cameraController == null) {
                if (mP.cameraPreview != null) {
                    mP.cameraController = new DefaultCameraController(mP.cameraPreview);
                }
            }

            if (mP.cameraController == null) {
                throw new IllegalArgumentException("TextureView or SurfaceView cannot be null");
            } else {
                mP.cameraController.setCameraBuilder(mP.cameraBuilder);
            }
            if (mP.mOutputFile == null && mP.outputPath == null) {
                File filesDir = mP.context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                if (filesDir == null) filesDir = mP.context.getFilesDir();
                mP.outputPath = filesDir.getPath();
            }
            return new VideoRecorder(mP.clone());
        }

        public Builder setCallbackHandler(VideoRecorderHandler viewHandler) {
            mP.viewHandler = viewHandler;
            return this;
        }


        public Builder setCameraBuilder(Camera.CameraBuilder cameraBuilder) {
            mP.cameraBuilder = cameraBuilder;
            return this;
        }
    }

    public static class Config implements Cloneable {
        ICameraPreview cameraPreview;
        CameraController cameraController;
        Context context;
        VideoRecorderHandler viewHandler;
        boolean logFPS;
        File mOutputFile;
        int audioBitRate = 64000;
        int iFrameInterval = 5;
        int frameRate = 25;
        int audioSampleRate = 44100;
        int audioChannelCount = 1;
        int videoBitRate;
        String outputPath;
        Camera.CameraBuilder cameraBuilder;

        Config(Context context, ICameraPreview cameraPreview) {
            this.context = context;
            this.cameraPreview = cameraPreview;
        }

        public CameraController getCameraController() {
            return cameraController;
        }

        public Context getContext() {
            return context;
        }

        public VideoRecorderHandler getViewHandler() {
            return viewHandler;
        }

        public boolean isLogFPS() {
            return logFPS;
        }

        public File getOutputFile() {
            return mOutputFile;
        }

        public int getAudioBitRate() {
            return audioBitRate;
        }

        public int getIFrameInterval() {
            return iFrameInterval;
        }

        public int getFrameRate() {
            return frameRate;
        }

        public int getAudioSampleRate() {
            return audioSampleRate;
        }

        public int getAudioChannelCount() {
            return audioChannelCount;
        }

        public int getVideoBitRate() {
            return videoBitRate;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public ICameraPreview getCameraPreview() {
            return cameraPreview;
        }

        public void setCameraPreview(ICameraPreview cameraPreview) {
            this.cameraPreview = cameraPreview;
        }

        public void setCameraController(CameraController cameraController) {
            this.cameraController = cameraController;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public void setViewHandler(VideoRecorderHandler viewHandler) {
            this.viewHandler = viewHandler;
        }

        public void setLogFPS(boolean logFPS) {
            this.logFPS = logFPS;
        }

        public void setOutputFile(File outputFile) {
            mOutputFile = outputFile;
        }

        public void setAudioBitRate(int audioBitRate) {
            this.audioBitRate = audioBitRate;
        }

        public void setIFrameInterval(int iFrameInterval) {
            this.iFrameInterval = iFrameInterval;
        }

        public void setFrameRate(int frameRate) {
            this.frameRate = frameRate;
        }

        public void setAudioSampleRate(int audioSampleRate) {
            this.audioSampleRate = audioSampleRate;
        }

        public void setAudioChannelCount(int audioChannelCount) {
            this.audioChannelCount = audioChannelCount;
        }

        public void setVideoBitRate(int videoBitRate) {
            this.videoBitRate = videoBitRate;
        }

        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }

        public Camera.CameraBuilder getCameraBuilder() {
            return cameraBuilder;
        }

        public void setCameraBuilder(Camera.CameraBuilder cameraBuilder) {
            this.cameraBuilder = cameraBuilder;
        }

        @Override
        public Config clone() {
            try {
                return (Config) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


}
