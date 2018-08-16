package com.erlei.videorecorder.recorder;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.gles.EglSurfaceBase;
import com.erlei.videorecorder.gles.WindowSurface;
import com.erlei.videorecorder.util.FPSCounterFactory;
import com.erlei.videorecorder.util.LogUtil;

import java.lang.ref.WeakReference;

import static com.erlei.videorecorder.gles.GLUtil.checkGlError;

public class RenderThread extends HandlerThread implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = LogUtil.TAG;

    private VideoRecorder.Config mConfig;
    private EglCore mEglCore;
    private boolean mIsStarted;
    private EglSurfaceBase mWindowSurface;
    private CameraGLRenderer mRenderer;
    private volatile RenderHandler mHandler;
    private FPSCounterFactory.FPSCounter mFPSCounter;

    private RenderCallBack mCallBack;

    public RenderThread(VideoRecorder.Config config) {
        super(RenderThread.class.getName());
        mConfig = config;
    }

    public void setCallBack(RenderCallBack callBack) {
        mCallBack = callBack;
    }

    @Override
    public void run() {
        super.run();
        mConfig.cameraController.closeCamera();
        if (mConfig.mDrawTextureListener != null) {
            mConfig.mDrawTextureListener.onCameraStopped();
        }
        if (mCallBack != null) mCallBack.onStopped();
        mRenderer.destroy();
        releaseGl();
        mEglCore.release();
        LogUtil.logd(TAG, "looper quit");
    }

    private void releaseGl() {
        checkGlError("releaseGl startRecord");
        if (mWindowSurface != null) {
            mWindowSurface.release();
            mWindowSurface = null;
        }
        mEglCore.makeNothingCurrent();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRenderer == null ? null : mRenderer.getTexture();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mFPSCounter = FPSCounterFactory.getDefaultFPSCounter();
        LogUtil.logd(TAG, "looper onLooperPrepared");

        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        Object surface = mConfig.getCameraPreview().getSurface(mEglCore);
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture) && !(surface instanceof EglSurfaceBase)) {
            throw new RuntimeException("invalid surface: " + surface);
        }
        if (surface instanceof EglSurfaceBase) {
            mWindowSurface = (EglSurfaceBase) surface;
        } else {
            mWindowSurface = new WindowSurface(mEglCore, surface, false);
        }
        mWindowSurface.makeCurrent();

        mRenderer = new CameraGLRenderer(mConfig.cameraController);
        if (mConfig.mDrawTextureListener != null)
            mRenderer.setOnDrawTextureListener(mConfig.mDrawTextureListener);
        mRenderer.getTexture().setOnFrameAvailableListener(this);

        mIsStarted = mConfig.cameraController.openCamera(mRenderer.getTexture());
        if (mIsStarted) {
            Size surfaceSize = mConfig.cameraController.getSurfaceSize();
            mRenderer.setPreviewSize(surfaceSize);
            if (mConfig.mDrawTextureListener != null) {
                mConfig.mDrawTextureListener.onCameraStarted(surfaceSize);
            }
        }
        if (mCallBack != null) mCallBack.onPrepared(mEglCore);
    }


    private void onDestroy() {
        LogUtil.logd(TAG, "onDestroy");
        quit();
    }

    private void onDrawFrame(long timestamp) {
        boolean swapBuffers;
        if (mCallBack != null) {
            swapBuffers = mCallBack.onDrawFrame(mRenderer, mWindowSurface);
        } else {
            mWindowSurface.makeCurrent();
            mRenderer.onDrawFrame();
            swapBuffers = mWindowSurface.swapBuffers();
        }
        if (!swapBuffers) {
            //如果活动停止而没有等待我们停止，就会发生这种情况。
            LogUtil.loge(TAG, "swapBuffers failed, killing renderer thread");
            onDestroy();
        }


        if (mConfig.viewHandler != null || mConfig.logFPS) {
            float fps = mFPSCounter.getFPS();
            if (mConfig.logFPS) LogUtil.logd(TAG, "FPS = " + fps);
            if (mConfig.viewHandler != null) mConfig.viewHandler.updateFPS(fps);
        }
    }

    private void onSizeChanged(Size size) {
        LogUtil.logd(TAG, "onSizeChanged" + size);
        mRenderer.setPreviewSize(size);
        if (mConfig.mDrawTextureListener != null) {
            mConfig.mDrawTextureListener.onSizeChanged(size);
        }
    }

    /**
     * This method returns the RenderHandler associated with this thread. If this thread not been started
     * or for any reason isAlive() returns false, this method will return null. If this thread
     * has been started, this method will block until the looper has been initialized.
     *
     * @return The RenderHandler.
     */
    public synchronized RenderHandler getHandler() {
        if (!isAlive()) return null;
        if (mHandler == null) {
            mHandler = new RenderHandler(getLooper(), this);
        }
        return mHandler;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        getHandler().onDrawFrame(surfaceTexture.getTimestamp());
    }

    public EglCore getEglCore() {
        return mEglCore;
    }


    public static class RenderHandler extends Handler {

        private static final int MSG_SET_PREVIEW_SIZE = 0;
        private static final int MSG_DRAW_FRAME = 1;
        private static final int MSG_DESTROY = 3;

        private final WeakReference<RenderThread> mReference;

        RenderHandler(Looper looper, RenderThread renderThread) {
            super(looper);
            mReference = new WeakReference<>(renderThread);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            RenderThread renderThread = mReference.get();
            if (renderThread == null) {
                LogUtil.loge(TAG, "handleMessage: weak ref is null");
                return;
            }
            int what = msg.what;
            switch (what) {
                case MSG_SET_PREVIEW_SIZE:
                    renderThread.onSizeChanged((Size) msg.obj);
                    break;
                case MSG_DRAW_FRAME:
                    renderThread.onDrawFrame((long) msg.obj);
                    break;
                case MSG_DESTROY:
                    renderThread.onDestroy();
                    break;
            }

        }

        void onSizeChanged(int width, int height) {
            sendMessage(obtainMessage(MSG_SET_PREVIEW_SIZE, new Size(width, height)));
        }

        public void onDrawFrame(long timestamp) {
            sendMessage(obtainMessage(MSG_DRAW_FRAME, timestamp));
        }

        public void destroy() {
            sendMessage(obtainMessage(MSG_DESTROY));
        }
    }


    public interface RenderCallBack {

        /**
         * 渲染线程已经准备好
         */
        void onPrepared(EglCore eglCore);

        /**
         * 渲染一帧
         *
         * @return swapBuffers
         */
        boolean onDrawFrame(CameraGLRenderer renderer, EglSurfaceBase windowSurface);

        /**
         * 渲染线程停止
         */
        void onStopped();

    }

}