package com.erlei.videorecorder.widget;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;

import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.gles.WindowSurface;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by lll on 2018/9/25 .
 * Email : lllemail@foxmail.com
 * Describe : 渲染视图接口
 */
public interface IRenderView {

    /**
     * The renderer only renders
     * when the surface is created, or when {@link #requestRender} is called.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     * @see #requestRender()
     */
    int RENDER_MODE_WHEN_DIRTY = 0;
    /**
     * The renderer is called
     * continuously to re-render the scene.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     */
    int RENDER_MODE_CONTINUOUSLY = 1;

    @IntDef({
            RENDER_MODE_CONTINUOUSLY,
            RENDER_MODE_WHEN_DIRTY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderMode {
    }

    /**
     * @return 获取渲染模式
     */
    int getRenderMode();


    /**
     * 请求渲染
     */
    void requestRender();


    /**
     * 设置渲染模式
     *
     * @param renderMode 渲染模式
     */
    void setRenderMode(@RenderMode int renderMode);


    void onPause();


    void onResume();


    /**
     * 设置渲染器
     *
     * @param renderer 渲染器
     */
    void setRenderer(Renderer renderer);


    interface Renderer {

        void onSurfaceCreated(GL10 var1, EGLConfig var2);

        void onSurfaceChanged(GL10 var1, int var2, int var3);

        void onDrawFrame(GL10 var1);
    }


    class RenderThread extends HandlerThread {

        private WeakReference<GLSurfaceView> mSurfaceViewWeakReference;
        private WeakReference<GLTextureView> mTextureViewWeakReference;
        private EglCore mEglCore;
        private WindowSurface mWindowSurface;
        private volatile RenderHandler mHandler;

        public RenderThread(GLSurfaceView surfaceView, Renderer renderer) {
            super(RenderThread.class.getSimpleName());
            start();
            mSurfaceViewWeakReference = new WeakReference<>(surfaceView);
        }

        public RenderThread(GLTextureView textureView, Renderer renderer) {
            super(RenderThread.class.getSimpleName());
            start();
            mTextureViewWeakReference = new WeakReference<>(textureView);
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

        public void requestExitAndWait() {

        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
            mWindowSurface = new WindowSurface(mEglCore, getSurface(), false);
            mWindowSurface.makeCurrent();
        }


        private Object getSurface() {
            GLTextureView textureView = mTextureViewWeakReference == null ? null : mTextureViewWeakReference.get();
            GLSurfaceView surfaceView = mSurfaceViewWeakReference == null ? null : mSurfaceViewWeakReference.get();
            if (surfaceView != null) {
                return surfaceView.getHolder().getSurface();
            } else if (textureView != null) {
                return textureView.getSurfaceTexture();
            }
            throw new IllegalStateException("textureView and surfaceView cannot both be null");
        }

        /**
         * @return 获取渲染模式
         */
        int getRenderMode() {
            return RENDER_MODE_CONTINUOUSLY;
        }


        /**
         * 设置渲染模式
         *
         * @param renderMode 渲染模式
         */
        void setRenderMode(@RenderMode int renderMode) {

        }

        public void onResume() {

        }

        public void onPause() {

        }

        public void requestRender() {

        }

        public void surfaceCreated() {

        }

        public void onSizeChanged(int width, int height) {

        }

        public void surfaceDestroyed() {

        }

    }

    class RenderHandler extends Handler {

        private final WeakReference<RenderThread> mReference;

        RenderHandler(Looper looper, RenderThread renderThread) {
            super(looper);
            mReference = new WeakReference<>(renderThread);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
