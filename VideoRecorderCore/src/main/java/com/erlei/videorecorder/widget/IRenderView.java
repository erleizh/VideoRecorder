package com.erlei.videorecorder.widget;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.view.Surface;

import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.gles.EglSurfaceBase;
import com.erlei.videorecorder.gles.WindowSurface;
import com.erlei.videorecorder.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by lll on 2018/9/25 .
 * Email : lllemail@foxmail.com
 * Describe : 渲染视图接口
 */
public interface IRenderView {

    String TAG = "IRenderView";

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

    Object getSurface();


    interface Renderer {

        void onSurfaceCreated(EglCore egl, WindowSurface windowSurface);

        void onSurfaceChanged(int width, int height);

        void onDrawFrame(GL10 var1);
    }


    class RenderThread extends HandlerThread {
        // Used to wait for the thread to start.
        private final Object mStartLock = new Object();
        private boolean mReady = false;
        private final WeakReference<Renderer> mRenderWeakReference;
        private WeakReference<IRenderView> mRenderViewWeakReference;
        private EglCore mEglCore;
        private WindowSurface mWindowSurface;
        private RenderHandler mHandler;
        private int mRenderMode;

        public RenderThread(IRenderView renderView, Renderer renderer) {
            super(RenderThread.class.getSimpleName());
            start();
            waitUntilReady();

            mRenderViewWeakReference = new WeakReference<>(renderView);
            mRenderWeakReference = new WeakReference<>(renderer);
        }


        @Override
        public void run() {
            super.run();
            release();
        }

        private void release(){
            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }
            mEglCore.makeNothingCurrent();
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mHandler = new RenderHandler(getLooper(), this);
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();    // signal waitUntilReady()
            }
        }

        public RenderHandler getHandler() {
            return mHandler;
        }


        private Object getSurface() {
            if (mRenderViewWeakReference.get() == null) {
                throw new RuntimeException("renderView can not be null");
            }
            Object surface = mRenderViewWeakReference.get().getSurface();
            if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture) && !(surface instanceof EglSurfaceBase)) {
                throw new RuntimeException("invalid surface: " + surface);
            }
            return surface;
        }

        /**
         * @return 获取渲染模式
         */
        public int getRenderMode() {
            return mRenderMode;
        }


        public void shutdown() {
            getHandler().sendEmptyMessage(RenderHandler.SHUTDOWN);
            try {
                join();
            } catch (InterruptedException ie) {
                throw new RuntimeException("join was interrupted", ie);
            }
        }

        /**
         * Waits until the render thread is ready to receive messages.
         * <p>
         * Call from the UI thread.
         */
        private void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * 设置渲染模式
         *
         * @param renderMode 渲染模式
         */
        public void setRenderMode(@RenderMode int renderMode) {
            getHandler().sendMessage(getHandler().obtainMessage(RenderHandler.SET_RENDER_MODE, renderMode));
        }

        public void onResume() {
            getHandler().sendEmptyMessage(RenderHandler.RESUME);
        }

        public void onPause() {
            getHandler().sendEmptyMessage(RenderHandler.PAUSE);
        }

        public void requestRender() {
            getHandler().sendEmptyMessage(RenderHandler.REQUEST_RENDER);
        }

        public void surfaceCreated() {
            getHandler().sendEmptyMessage(RenderHandler.SURFACE_CREATED);
        }

        public void onSizeChanged(int width, int height) {
            getHandler().sendMessage(getHandler().obtainMessage(RenderHandler.SIZE_CHANGED, width, height));
        }

        public void surfaceDestroyed() {
            getHandler().sendEmptyMessage(RenderHandler.SURFACE_DESTROYED);
        }

        private void handleSurfaceCreated() {
            mWindowSurface = new WindowSurface(mEglCore, getSurface(), false);
            mWindowSurface.makeCurrent();
            Renderer renderer = mRenderWeakReference.get();
            if (renderer != null) {
                renderer.onSurfaceCreated(mEglCore, mWindowSurface);
            }
        }

        private void handleRequestRender() {
        }

        private void handleOnPause() {
        }

        private void handleOnResume() {
        }

        private void handleSetRenderMode(int renderMode) {
            mRenderMode = renderMode;
        }

        private void handleSizeChanged(int width, int height) {
            Renderer renderer = mRenderWeakReference.get();
            if (renderer != null) {
                renderer.onSurfaceChanged(width, height);
            }
        }

        private void handleShutdown() {
            synchronized (mStartLock) {
                mReady = false;
            }
            quit();
            LogUtil.loge(TAG, "shutdown");
        }

        private void handleSurfaceDestroyed() {

        }
    }


    class RenderHandler extends Handler {

        public static final int SURFACE_DESTROYED = 1;
        public static final int SIZE_CHANGED = 2;
        public static final int SURFACE_CREATED = 3;
        public static final int REQUEST_RENDER = 4;
        public static final int PAUSE = 5;
        public static final int RESUME = 6;
        public static final int SHUTDOWN = 7;
        public static final int SET_RENDER_MODE = 8;

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
            switch (msg.what) {
                case SURFACE_DESTROYED:
                    renderThread.handleSurfaceDestroyed();
                    break;
                case SIZE_CHANGED:
                    renderThread.handleSizeChanged(msg.arg1, msg.arg2);
                    break;
                case SURFACE_CREATED:
                    renderThread.handleSurfaceCreated();
                    break;
                case REQUEST_RENDER:
                    renderThread.handleSurfaceDestroyed();
                    break;
                case PAUSE:
                    renderThread.handleOnPause();
                    break;
                case RESUME:
                    renderThread.handleOnResume();
                    break;
                case SHUTDOWN:
                    renderThread.handleShutdown();
                    break;
                case SET_RENDER_MODE:
                    renderThread.handleSetRenderMode(msg.arg1);
                    break;
            }
        }
    }
}
