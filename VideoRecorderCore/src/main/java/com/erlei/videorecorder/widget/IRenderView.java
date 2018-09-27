package com.erlei.videorecorder.widget;


import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.gles.EglSurfaceBase;
import com.erlei.videorecorder.gles.WindowSurface;
import com.erlei.videorecorder.util.LogUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public interface IRenderView {

    enum RenderMode {
        CONTINUOUSLY, WHEN_DIRTY
    }

    /**
     * @return 获取渲染模式
     */
    RenderMode getRenderMode();


    /**
     * 请求渲染
     */
    void requestRender();


    /**
     * 设置渲染模式
     *
     * @param renderMode 渲染模式
     */
    void setRenderMode(RenderMode renderMode);


    /**
     * 设置渲染器
     *
     * @param renderer 渲染器
     */
    void setRenderer(Renderer renderer);

    void onPause();


    void onResume();


    Renderer getRenderer();


    /**
     * Control whether the EGL context is preserved when the GLSurfaceViewI is paused and
     * resumed.
     * <p>
     * If set to true, then the EGL context may be preserved when the GLSurfaceViewI is paused.
     * <p>
     * Prior to API level 11, whether the EGL context is actually preserved or not
     * depends upon whether the Android device can support an arbitrary number of
     * EGL contexts or not. Devices that can only support a limited number of EGL
     * contexts must release the EGL context in order to allow multiple applications
     * to share the GPU.
     * <p>
     * If set to false, the EGL context will be released when the GLSurfaceViewI is paused,
     * and recreated when the GLSurfaceViewI is resumed.
     * <p>
     * <p>
     * The default is false.
     *
     * @param preserveOnPause preserve the EGL context when paused
     */
    void setPreserveEGLContextOnPause(boolean preserveOnPause);

    /**
     * @return true if the EGL context will be preserved when paused
     */
    boolean getPreserveEGLContextOnPause();

    Object getSurface();

    interface Renderer {

        void onSurfaceCreated(EglCore egl, EglSurfaceBase eglSurface);

        void onSurfaceChanged(int width, int height);

        void onDrawFrame();
    }


    GLThreadManager sGLThreadManager = new GLThreadManager();

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     * <p>
     * All potentially blocking synchronization is done through the
     * sGLThreadManager object. This avoids multiple-lock ordering issues.
     */
    class GLThread extends Thread {
        private static final String TAG = "GLThread";

        private final static boolean LOG_ENABLE = LogUtil.LOG_ENABLE;

        // Once the thread is started, all accesses to the following member
        // variables are protected by the sGLThreadManager monitor
        private boolean mShouldExit;
        private boolean mExited;
        private boolean mRequestPaused;
        private boolean mPaused;
        private boolean mHasSurface;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private boolean mFinishedCreatingEglSurface;
        private boolean mShouldReleaseEglContext;
        private int mWidth;
        private int mHeight;
        private RenderMode mRenderMode;
        private boolean mRequestRender;
        private boolean mWantRenderNotification;
        private boolean mRenderComplete;
        private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
        private boolean mSizeChanged = true;
        private Runnable mFinishDrawingRunnable = null;

        /**
         * Set once at thread construction time, nulled out when the parent view is garbage
         * called. This weak reference allows the GLSurfaceViewI to be garbage collected while
         * the GLThread is still alive.
         */
        private WeakReference<IRenderView> mRenderViewWeakRef;
        private EglCore mEglCore;
        private EglSurfaceBase mWindowSurface;

        GLThread(IRenderView IRenderView) {
            super();
            mWidth = 0;
            mHeight = 0;
            mRequestRender = true;
            mRenderMode = RenderMode.CONTINUOUSLY;
            mWantRenderNotification = false;
            mRenderViewWeakRef = new WeakReference<>(IRenderView);
        }

        @Override
        public void run() {
            setName("GLThread " + getId());
            if (LOG_ENABLE) {
                LogUtil.logi(TAG, "starting tid=" + getId());
            }

            try {
                guardedRun();
            } catch (InterruptedException e) {
                // fall thru and exit normally
            } finally {
                sGLThreadManager.threadExiting(this);
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sGLThreadManager) block.
         */
        private void stopEglSurfaceLocked() {
            if (mHaveEglSurface) {
                mHaveEglSurface = false;
                mWindowSurface.release();
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sGLThreadManager) block.
         */
        private void stopEglContextLocked() {
            if (mHaveEglContext) {
                mEglCore.release();
                mHaveEglContext = false;
                sGLThreadManager.releaseEglContextLocked(this);
            }
        }

        private Object getSurface() {
            if (mRenderViewWeakRef.get() == null) {
                throw new RuntimeException("renderView can not be null");
            }
            Object surface = mRenderViewWeakRef.get().getSurface();
            if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture) && !(surface instanceof EglSurfaceBase)) {
                throw new RuntimeException("invalid surface: " + surface);
            }
            return surface;
        }

        private void guardedRun() throws InterruptedException {
            mHaveEglContext = false;
            mHaveEglSurface = false;
            mWantRenderNotification = false;

            try {
                boolean createEglContext = false;
                boolean createEglSurface = false;
                boolean lostEglContext = false;
                boolean sizeChanged = false;
                boolean wantRenderNotification = false;
                boolean doRenderNotification = false;
                boolean askedToReleaseEglContext = false;
                int w = 0;
                int h = 0;
                Runnable event = null;
                Runnable finishDrawingRunnable = null;

                while (true) {
                    synchronized (sGLThreadManager) {
                        while (true) {
                            if (mShouldExit) {
                                return;
                            }

                            if (!mEventQueue.isEmpty()) {
                                event = mEventQueue.remove(0);
                                break;
                            }

                            // Update the pause state.
                            boolean pausing = false;
                            if (mPaused != mRequestPaused) {
                                pausing = mRequestPaused;
                                mPaused = mRequestPaused;
                                sGLThreadManager.notifyAll();
                                if (LOG_ENABLE) {
                                    LogUtil.logi(TAG, "mPaused is now " + mPaused + " tid=" + getId());
                                }
                            }

                            // Do we need to give up the EGL context?
                            if (mShouldReleaseEglContext) {
                                if (LOG_ENABLE) {
                                    LogUtil.logi(TAG, "releasing EGL context because asked to tid=" + getId());
                                }
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                mShouldReleaseEglContext = false;
                                askedToReleaseEglContext = true;
                            }

                            // Have we lost the EGL context?
                            if (lostEglContext) {
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                lostEglContext = false;
                            }

                            // When pausing, release the EGL surface:
                            if (pausing && mHaveEglSurface) {
                                if (LOG_ENABLE) {
                                    LogUtil.logi(TAG, "releasing EGL surface because paused tid=" + getId());
                                }
                                stopEglSurfaceLocked();
                            }

                            // When pausing, optionally release the EGL Context:
                            if (pausing && mHaveEglContext) {
                                IRenderView view = mRenderViewWeakRef.get();
                                boolean preserveEglContextOnPause = view != null && view.getPreserveEGLContextOnPause();
                                if (!preserveEglContextOnPause) {
                                    stopEglContextLocked();
                                    if (LOG_ENABLE) {
                                        LogUtil.logi(TAG, "releasing EGL context because paused tid=" + getId());
                                    }
                                }
                            }

                            // Have we lost the SurfaceView surface?
                            if ((!mHasSurface) && (!mWaitingForSurface)) {
                                if (LOG_ENABLE) {
                                    LogUtil.logi(TAG, "noticed surfaceView surface lost tid=" + getId());
                                }
                                if (mHaveEglSurface) {
                                    stopEglSurfaceLocked();
                                }
                                mWaitingForSurface = true;
                                mSurfaceIsBad = false;
                                sGLThreadManager.notifyAll();
                            }

                            // Have we acquired the surface view surface?
                            if (mHasSurface && mWaitingForSurface) {
                                if (LOG_ENABLE) {
                                    LogUtil.logi(TAG, "noticed surfaceView surface acquired tid=" + getId());
                                }
                                mWaitingForSurface = false;
                                sGLThreadManager.notifyAll();
                            }

                            if (doRenderNotification) {
                                if (LOG_ENABLE) {
                                    LogUtil.logi(TAG, "sending render notification tid=" + getId());
                                }
                                mWantRenderNotification = false;
                                doRenderNotification = false;
                                mRenderComplete = true;
                                sGLThreadManager.notifyAll();
                            }

                            if (mFinishDrawingRunnable != null) {
                                finishDrawingRunnable = mFinishDrawingRunnable;
                                mFinishDrawingRunnable = null;
                            }

                            // Ready to draw?
                            if (readyToDraw()) {

                                // If we don't have an EGL context, try to acquire one.
                                if (!mHaveEglContext) {
                                    if (askedToReleaseEglContext) {
                                        askedToReleaseEglContext = false;
                                    } else {
                                        try {
                                            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
                                        } catch (RuntimeException t) {
                                            sGLThreadManager.releaseEglContextLocked(this);
                                            throw t;
                                        }
                                        mHaveEglContext = true;
                                        createEglContext = true;

                                        sGLThreadManager.notifyAll();
                                    }
                                }

                                if (mHaveEglContext && !mHaveEglSurface) {
                                    mHaveEglSurface = true;
                                    createEglSurface = true;
                                    sizeChanged = true;
                                }

                                if (mHaveEglSurface) {
                                    if (mSizeChanged) {
                                        sizeChanged = true;
                                        w = mWidth;
                                        h = mHeight;
                                        mWantRenderNotification = true;
                                        if (LOG_ENABLE) {
                                            LogUtil.logi(TAG,
                                                    "noticing that we want render notification tid="
                                                            + getId());
                                        }

                                        // Destroy and recreate the EGL surface.
                                        createEglSurface = true;

                                        mSizeChanged = false;
                                    }
                                    mRequestRender = false;
                                    sGLThreadManager.notifyAll();
                                    if (mWantRenderNotification) {
                                        wantRenderNotification = true;
                                    }
                                    break;
                                }
                            } else {
                                if (finishDrawingRunnable != null) {
                                    LogUtil.logw(TAG, "Warning, !readyToDraw() but waiting for " +
                                            "draw finished! Early reporting draw finished.");
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            }
                            // By design, this is the only place in a GLThread thread where we wait().
                            if (LOG_ENABLE) {
                                LogUtil.logi(TAG, "waiting tid=" + getId()
                                        + " mHaveEglContext: " + mHaveEglContext
                                        + " mHaveEglSurface: " + mHaveEglSurface
                                        + " mFinishedCreatingEglSurface: " + mFinishedCreatingEglSurface
                                        + " mPaused: " + mPaused
                                        + " mHasSurface: " + mHasSurface
                                        + " mSurfaceIsBad: " + mSurfaceIsBad
                                        + " mWaitingForSurface: " + mWaitingForSurface
                                        + " mWidth: " + mWidth
                                        + " mHeight: " + mHeight
                                        + " mRequestRender: " + mRequestRender
                                        + " mRenderMode: " + mRenderMode);
                            }
                            sGLThreadManager.wait();
                        }
                    } // end of synchronized(sGLThreadManager)

                    if (event != null) {
                        event.run();
                        event = null;
                        continue;
                    }

                    if (createEglSurface) {
                        if (LOG_ENABLE) {
                            LogUtil.logw(TAG, "egl createSurface");
                        }
                        try {
                            mWindowSurface = new WindowSurface(mEglCore, getSurface(), false);
                            mWindowSurface.makeCurrent();
                            synchronized (sGLThreadManager) {
                                mFinishedCreatingEglSurface = true;
                                sGLThreadManager.notifyAll();
                            }
                        } catch (Exception e) {
                            synchronized (sGLThreadManager) {
                                mFinishedCreatingEglSurface = true;
                                mSurfaceIsBad = true;
                                sGLThreadManager.notifyAll();
                            }
                            continue;
                        }
                        createEglSurface = false;
                    }

                    if (createEglContext) {
                        if (LOG_ENABLE) {
                            LogUtil.logw(TAG, "onSurfaceCreated");
                        }
                        IRenderView view = mRenderViewWeakRef.get();
                        if (view != null) {
                            Renderer renderer = view.getRenderer();
                            if (renderer != null) {
                                try {
                                    renderer.onSurfaceCreated(mEglCore, mWindowSurface);
                                } catch (Exception e) {
                                    LogUtil.loge(TAG, "renderer onSurfaceCreated error", e);
                                }
                            }
                        }
                        createEglContext = false;
                    }

                    if (sizeChanged) {
                        if (LOG_ENABLE) {
                            LogUtil.logw(TAG, "onSurfaceChanged(" + w + ", " + h + ")");
                        }
                        IRenderView view = mRenderViewWeakRef.get();
                        if (view != null) {
                            Renderer renderer = view.getRenderer();
                            if (renderer != null) {
                                try {
                                    renderer.onSurfaceChanged(w, h);
                                } catch (Exception e) {
                                    LogUtil.loge(TAG, "renderer onSurfaceChanged error", e);
                                }
                            }
                        }
                        sizeChanged = false;
                    }

                    if (LOG_ENABLE) {
                        LogUtil.logw(TAG, "onDrawFrame tid=" + getId());
                    }
                    {
                        IRenderView view = mRenderViewWeakRef.get();
                        if (view != null) {
                            Renderer renderer = view.getRenderer();
                            if (renderer != null) {
                                try {
                                    renderer.onDrawFrame();
                                    if (finishDrawingRunnable != null) {
                                        finishDrawingRunnable.run();
                                        finishDrawingRunnable = null;
                                    }
                                } catch (Exception e) {
                                    LogUtil.loge(TAG, "renderer onDrawFrame error", e);
                                }
                            }
                        }
                    }

                    boolean swapError = mWindowSurface.swapBuffers();
                    if (!swapError) {
                        if (LOG_ENABLE) {
                            LogUtil.logi(TAG, "egl context lost tid=" + getId());
                        }
                        lostEglContext = true;
                        synchronized (sGLThreadManager) {
                            mSurfaceIsBad = true;
                            sGLThreadManager.notifyAll();
                        }
                    }
                    if (wantRenderNotification) {
                        doRenderNotification = true;
                        wantRenderNotification = false;
                    }
                }

            } finally {
                /*
                 * clean-up everything...
                 */
                synchronized (sGLThreadManager) {
                    stopEglSurfaceLocked();
                    stopEglContextLocked();
                }
            }
        }

        public boolean ableToDraw() {
            return mHaveEglContext && mHaveEglSurface && readyToDraw();
        }

        private boolean readyToDraw() {
            return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
                    && (mWidth > 0) && (mHeight > 0)
                    && (mRequestRender || (mRenderMode == RenderMode.CONTINUOUSLY));
        }

        public void setRenderMode(RenderMode renderMode) {
            synchronized (sGLThreadManager) {
                mRenderMode = renderMode;
                sGLThreadManager.notifyAll();
            }
        }

        public RenderMode getRenderMode() {
            synchronized (sGLThreadManager) {
                return mRenderMode;
            }
        }

        public void requestRender() {
            synchronized (sGLThreadManager) {
                mRequestRender = true;
                sGLThreadManager.notifyAll();
            }
        }

        public void requestRenderAndNotify(Runnable finishDrawing) {
            synchronized (sGLThreadManager) {
                // If we are already on the GL thread, this means a client callback
                // has caused reentrancy, for example via updating the SurfaceView parameters.
                // We will return to the client rendering code, so here we don't need to
                // do anything.
                if (Thread.currentThread() == this) {
                    return;
                }

                mWantRenderNotification = true;
                mRequestRender = true;
                mRenderComplete = false;
                mFinishDrawingRunnable = finishDrawing;

                sGLThreadManager.notifyAll();
            }
        }

        public void surfaceCreated() {
            synchronized (sGLThreadManager) {
                if (LOG_ENABLE) {
                    LogUtil.logi(TAG, "surfaceCreated tid=" + getId());
                }
                mHasSurface = true;
                mFinishedCreatingEglSurface = false;
                sGLThreadManager.notifyAll();
                while (mWaitingForSurface
                        && !mFinishedCreatingEglSurface
                        && !mExited) {
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void surfaceDestroyed() {
            synchronized (sGLThreadManager) {
                if (LOG_ENABLE) {
                    LogUtil.logi(TAG, "surfaceDestroyed tid=" + getId());
                }
                mHasSurface = false;
                sGLThreadManager.notifyAll();
                while ((!mWaitingForSurface) && (!mExited)) {
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onPause() {
            synchronized (sGLThreadManager) {
                if (LOG_ENABLE) {
                    LogUtil.logi(TAG, "onPause tid=" + getId());
                }
                mRequestPaused = true;
                sGLThreadManager.notifyAll();
                while ((!mExited) && (!mPaused)) {
                    if (LOG_ENABLE) {
                        LogUtil.logi("Main thread", "onPause waiting for mPaused.");
                    }
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onResume() {
            synchronized (sGLThreadManager) {
                if (LOG_ENABLE) {
                    LogUtil.logi(TAG, "onResume tid=" + getId());
                }
                mRequestPaused = false;
                mRequestRender = true;
                mRenderComplete = false;
                sGLThreadManager.notifyAll();
                while ((!mExited) && mPaused && (!mRenderComplete)) {
                    if (LOG_ENABLE) {
                        LogUtil.logi("Main thread", "onResume waiting for !mPaused.");
                    }
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onWindowResize(int w, int h) {
            synchronized (sGLThreadManager) {
                mWidth = w;
                mHeight = h;
                mSizeChanged = true;
                mRequestRender = true;
                mRenderComplete = false;

                // If we are already on the GL thread, this means a client callback
                // has caused reentrancy, for example via updating the SurfaceView parameters.
                // We need to process the size change eventually though and update our EGLSurface.
                // So we set the parameters and return so they can be processed on our
                // next iteration.
                if (Thread.currentThread() == this) {
                    return;
                }

                sGLThreadManager.notifyAll();

                // Wait for thread to react to resize and render a frame
                while (!mExited && !mPaused && !mRenderComplete
                        && ableToDraw()) {
                    if (LOG_ENABLE) {
                        LogUtil.logi("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                    }
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            synchronized (sGLThreadManager) {
                mShouldExit = true;
                sGLThreadManager.notifyAll();
                while (!mExited) {
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestReleaseEglContextLocked() {
            mShouldReleaseEglContext = true;
            sGLThreadManager.notifyAll();
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         *
         * @param r the runnable to be run on the GL rendering thread.
         */
        public void queueEvent(Runnable r) {
            if (r == null) {
                throw new IllegalArgumentException("r must not be null");
            }
            synchronized (sGLThreadManager) {
                mEventQueue.add(r);
                sGLThreadManager.notifyAll();
            }
        }


    }

    class GLThreadManager {
        private static String TAG = "GLThreadManager";

        public synchronized void threadExiting(GLThread thread) {
            if (LogUtil.LOG_ENABLE) {
                LogUtil.logi(TAG, "exiting tid=" + thread.getId());
            }
            thread.mExited = true;
            notifyAll();
        }

        /*
         * Releases the EGL context. Requires that we are already in the
         * sGLThreadManager monitor when this is called.
         */
        public void releaseEglContextLocked(GLThread thread) {
            notifyAll();
        }
    }

}
