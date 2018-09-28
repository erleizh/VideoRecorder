package com.erlei.gdx.android.widget;


import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.WindowSurface;
import com.erlei.gdx.utils.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public interface IRenderView {

    void onDestroy();

    enum ViewType {
        TextureView, SurfaceView
    }

    enum RenderMode {
        CONTINUOUSLY, WHEN_DIRTY
    }

    /**
     * Get the view type
     *
     * @return the view type
     * @see ViewType#TextureView
     * @see ViewType#SurfaceView
     */
    ViewType getViewType();

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     *
     * @return the current rendering mode.
     * @see RenderMode#WHEN_DIRTY
     * @see RenderMode#CONTINUOUSLY
     */
    RenderMode getRenderMode();

    /**
     * Must not be called before a Renderer#create(EglCore, EglSurfaceBase) has been set.
     *
     * @return GLES version code
     */
    int getGLESVersion();

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to
     * {@link RenderMode#WHEN_DIRTY}, so that frames are only rendered on demand.
     * May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    void requestRender();


    /**
     * Set the rendering mode. When renderMode is
     * RenderMode#CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RenderMode#WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when {@link #requestRender} is called. Defaults to RenderMode#CONTINUOUSLY.
     * <p>
     * Using RenderMode#WHEN_DIRTY can improve battery life and overall system performance
     * by allowing the GPU and CPU to idle when the view does not need to be updated.
     * <p>
     * This method can only be called after {@link #setRenderer(IRenderView.Renderer)}
     *
     * @param renderMode one of the RenderMode enum
     * @see RenderMode#CONTINUOUSLY
     * @see RenderMode#WHEN_DIRTY
     */
    void setRenderMode(RenderMode renderMode);


    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of
     * a GLSurfaceView.
     * <p>The following IRenderView methods can only be called <em>before</em>
     * setRenderer is called:
     * <p>
     * The following GLSurfaceView methods can only be called <em>after</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(RenderMode)}}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    void setRenderer(Renderer renderer);

    /**
     * Pause the rendering thread, optionally tearing down the EGL context
     * depending upon the value of {@link #setPreserveEGLContextOnPause(boolean)}.
     * <p>
     * This method should be called when it is no longer desirable for the
     * IRenderView to continue rendering, such as in response to
     * {@link android.app.Activity#onStop Activity.onStop}.
     * <p>
     * Must not be called before a renderer has been set.
     */
    void onPause();

    /**
     * Resumes the rendering thread, re-creating the OpenGL context if necessary. It
     * is the counterpart to {@link #onPause()}.
     * <p>
     * This method should typically be called in
     * {@link android.app.Activity#onStart Activity.onStart}.
     * <p>
     * Must not be called before a renderer has been set.
     */
    void onResume();


    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     *
     * @param r the runnable to be run on the GL rendering thread.
     */
    void queueEvent(Runnable r);


    int getSurfaceWidth();

    int getSurfaceHeight();


    Renderer getRenderer();


    /**
     * Control whether the EGL context is preserved when the IRenderView is paused and
     * resumed.
     * <p>
     * If set to true, then the EGL context may be preserved when the IRenderView is paused.
     * <p>
     * Prior to API level 11, whether the EGL context is actually preserved or not
     * depends upon whether the Android device can support an arbitrary number of
     * EGL contexts or not. Devices that can only support a limited number of EGL
     * contexts must release the EGL context in order to allow multiple applications
     * to share the GPU.
     * <p>
     * If set to false, the EGL context will be released when the IRenderView is paused,
     * and recreated when the IRenderView is resumed.
     * <p>
     * <p>
     * The default is true.
     *
     * @param preserveOnPause preserve the EGL context when paused
     */
    void setPreserveEGLContextOnPause(boolean preserveOnPause);

    /**
     * @return true if the EGL context will be preserved when paused
     */
    boolean getPreserveEGLContextOnPause();

    Object getSurface();

    Context getContext();

    interface Renderer {

        /**
         * surface create
         */
        void create(EglCore egl, EglSurfaceBase eglSurface);

        /**
         * surface size changed
         */
        void resize(int width, int height);

        /**
         * example:
         * <pre class="prettyprint">
         * public void render(EglSurfaceBase windowSurface, Runnable swapBufferErrorRunnable) {
         * //do something .....
         * boolean swapResult = windowSurface.swapBuffers();
         * if (!swapResult) swapBufferErrorRunnable.run();
         * }
         * </pre>
         *
         * @param windowSurface     Surface
         * @param swapErrorRunnable swap error Runnable
         */
        void render(EglSurfaceBase windowSurface, Runnable swapErrorRunnable);

        /**
         * activity onPause
         */
        void pause();

        /**
         * activity onResume
         */
        void resume();

        /**
         * EglContext destroy
         */
        void dispose();


        /**
         * When Activity destroy needs to be called
         */
        void release();
    }

    class RendererAdapter implements Renderer {


        @Override
        public void create(EglCore egl, EglSurfaceBase eglSurface) {

        }

        @Override
        public void resize(int width, int height) {

        }

        @Override
        public void render(EglSurfaceBase windowSurface, Runnable swapBufferErrorRunnable) {

        }

        @Override
        public void pause() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void dispose() {

        }

        @Override
        public void release() {

        }
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

        public static int getVersionFromActivityManager(Context context) {
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo configInfo = activityManager.getDeviceConfigurationInfo();
            if (configInfo.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
                return ((configInfo.reqGlEsVersion & 0xffff0000) >> 16);
            } else {
                return (((1 << 16) & 0xffff0000) >> 16); // Lack of property means OpenGL ES version 1
            }
        }

        private static final String TAG = "GLThread";

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
        private boolean mLostEglContext;
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
        private Runnable mSwapBufferErrorRunnable = new Runnable() {
            @Override
            public void run() {
                Logger.error(TAG, "egl context lost tid=" + getId());
                mLostEglContext = true;
                synchronized (sGLThreadManager) {
                    mSurfaceIsBad = true;
                    sGLThreadManager.notifyAll();
                }
            }
        };


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
            Logger.info(TAG, "starting tid=" + getId());

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
                IRenderView view = mRenderViewWeakRef.get();
                if (view != null) {
                    Renderer renderer = view.getRenderer();
                    if (renderer != null) {
                        try {
                            renderer.dispose();
                        } catch (Exception e) {
                            Logger.error(TAG, "renderer dispose error", e);
                        }
                    }
                }
                mEglCore.release();
                mHaveEglContext = false;
                sGLThreadManager.releaseEglContextLocked(this);
            }
        }

        private Object getSurface() {
            IRenderView renderView = mRenderViewWeakRef.get();
            if (renderView == null) {
                throw new RuntimeException("renderView can not be null");
            }
            Object surface = renderView.getSurface();
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
                                IRenderView view = mRenderViewWeakRef.get();
                                if (view != null) {
                                    if (mRequestPaused) {
                                        view.getRenderer().pause();
                                    } else {
                                        view.getRenderer().resume();
                                    }
                                }

                                pausing = mRequestPaused;
                                mPaused = mRequestPaused;
                                sGLThreadManager.notifyAll();
                                Logger.info(TAG, "mPaused is now " + mPaused + " tid=" + getId());
                            }

                            // Do we need to give up the EGL context?
                            if (mShouldReleaseEglContext) {
                                Logger.info(TAG, "releasing EGL context because asked to tid=" + getId());
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                mShouldReleaseEglContext = false;
                                askedToReleaseEglContext = true;
                            }

                            // Have we lost the EGL context?
                            if (mLostEglContext) {
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                mLostEglContext = false;
                            }

                            // When pausing, release the EGL surface:
                            if (pausing && mHaveEglSurface) {
                                Logger.info(TAG, "releasing EGL surface because paused tid=" + getId());
                                stopEglSurfaceLocked();
                            }

                            // When pausing, optionally release the EGL Context:
                            if (pausing && mHaveEglContext) {
                                IRenderView view = mRenderViewWeakRef.get();
                                boolean preserveEglContextOnPause = view != null && view.getPreserveEGLContextOnPause();
                                if (!preserveEglContextOnPause) {
                                    stopEglContextLocked();
                                    Logger.info(TAG, "releasing EGL context because paused tid=" + getId());
                                }
                            }

                            // Have we lost the SurfaceView surface?
                            if ((!mHasSurface) && (!mWaitingForSurface)) {
                                Logger.info(TAG, "noticed surfaceView surface lost tid=" + getId());
                                if (mHaveEglSurface) {
                                    stopEglSurfaceLocked();
                                }
                                mWaitingForSurface = true;
                                mSurfaceIsBad = false;
                                sGLThreadManager.notifyAll();
                            }

                            // Have we acquired the surface view surface?
                            if (mHasSurface && mWaitingForSurface) {
                                Logger.info(TAG, "noticed surfaceView surface acquired tid=" + getId());
                                mWaitingForSurface = false;
                                sGLThreadManager.notifyAll();
                            }

                            if (doRenderNotification) {
                                Logger.info(TAG, "sending render notification tid=" + getId());
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
                                        Logger.info(TAG,
                                                "noticing that we want render notification tid="
                                                        + getId());

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
                                    Logger.debug(TAG, "Warning, !readyToDraw() but waiting for " +
                                            "draw finished! Early reporting draw finished.");
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            }
                            // By design, this is the only place in a GLThread thread where we wait().
                            Logger.info(TAG, "waiting tid=" + getId()
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
                            sGLThreadManager.wait();
                        }
                    } // end of synchronized(sGLThreadManager)

                    if (event != null) {
                        event.run();
                        event = null;
                        continue;
                    }

                    if (createEglSurface) {
                        Logger.debug(TAG, "egl createSurface");
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
                        Logger.debug(TAG, "onSurfaceCreated");
                        IRenderView view = mRenderViewWeakRef.get();
                        if (view != null) {
                            Renderer renderer = view.getRenderer();
                            if (renderer != null) {
                                try {
                                    renderer.create(mEglCore, mWindowSurface);
                                } catch (Exception e) {
                                    Logger.error(TAG, "renderer onSurfaceCreated error", e);
                                }
                            }
                        }
                        createEglContext = false;
                    }

                    if (sizeChanged) {
                        Logger.debug(TAG, "onSurfaceChanged(" + w + ", " + h + ")");
                        IRenderView view = mRenderViewWeakRef.get();
                        if (view != null) {
                            Renderer renderer = view.getRenderer();
                            if (renderer != null) {
                                try {
                                    renderer.resize(w, h);
                                } catch (Exception e) {
                                    Logger.error(TAG, "renderer onSurfaceChanged error", e);
                                }
                            }
                        }
                        sizeChanged = false;
                    }

                    Logger.debug(TAG, "onDrawFrame tid=" + getId());
                    {
                        IRenderView view = mRenderViewWeakRef.get();
                        if (view != null) {
                            Renderer renderer = view.getRenderer();
                            if (renderer != null) {
                                try {
                                    renderer.render(mWindowSurface, mSwapBufferErrorRunnable);
                                    if (finishDrawingRunnable != null) {
                                        finishDrawingRunnable.run();
                                        finishDrawingRunnable = null;
                                    }
                                } catch (Exception e) {
                                    Logger.error(TAG, "renderer onDrawFrame error", e);
                                }
                            }
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
                Logger.info(TAG, "surfaceCreated tid=" + getId());
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
                Logger.info(TAG, "surfaceDestroyed tid=" + getId());
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
                Logger.info(TAG, "onPause tid=" + getId());
                mRequestPaused = true;
                sGLThreadManager.notifyAll();
                while ((!mExited) && (!mPaused)) {
                    Logger.info("Main thread", "onPause waiting for mPaused.");
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
                Logger.info(TAG, "onResume tid=" + getId());
                mRequestPaused = false;
                mRequestRender = true;
                mRenderComplete = false;
                sGLThreadManager.notifyAll();
                while ((!mExited) && mPaused && (!mRenderComplete)) {
                    Logger.info("Main thread", "onResume waiting for !mPaused.");
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
                    Logger.info("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
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


        public int getGLESVersion() {
            return mEglCore.getGLVersion();
        }
    }

    class GLThreadManager {
        private static String TAG = "GLThreadManager";

        public synchronized void threadExiting(GLThread thread) {
            Logger.info(TAG, "exiting tid=" + thread.getId());
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
