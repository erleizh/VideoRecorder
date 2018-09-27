package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import com.erlei.videorecorder.util.LogUtil;

public class GLTextureView extends TextureView implements IRenderView, TextureView.SurfaceTextureListener {
    private static final String TAG = "GLSurfaceViewI";
    private boolean mPreserveEGLContextOnPause;
    private Renderer mRenderer;
    private GLThread mGLThread;
    private boolean mDetached;

    public GLTextureView(Context context) {
        super(context);
        init();
    }

    public GLTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GLTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mGLThread != null) {
                // GLThread may still be running if this view was never
                // attached to a window.
                mGLThread.requestExitAndWait();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * @return 获取渲染模式
     */
    @Override
    public RenderMode getRenderMode() {
        return mGLThread.getRenderMode();
    }

    /**
     * 请求渲染
     */
    @Override
    public void requestRender() {
        mGLThread.requestRender();
    }

    /**
     * 设置渲染模式
     *
     * @param renderMode 渲染模式
     */
    @Override
    public void setRenderMode(RenderMode renderMode) {
        mGLThread.setRenderMode(renderMode);
    }

    /**
     * 设置渲染器
     *
     * @param renderer 渲染器
     */
    @Override
    public void setRenderer(Renderer renderer) {
        checkRenderThreadState();
        mRenderer = renderer;
        mGLThread = new GLThread(this);
        mGLThread.start();
    }

    private void checkRenderThreadState() {
        if (mGLThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
    }

    @Override
    public Renderer getRenderer() {
        return mRenderer;
    }

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
    @Override
    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        mPreserveEGLContextOnPause = preserveOnPause;
    }

    /**
     * @return true if the EGL context will be preserved when paused
     */
    @Override
    public boolean getPreserveEGLContextOnPause() {
        return mPreserveEGLContextOnPause;
    }

    @Override
    public Object getSurface() {
        return getSurfaceTexture();
    }


    /**
     * Pause the rendering thread, optionally tearing down the EGL context
     * depending upon the value of {@link #setPreserveEGLContextOnPause(boolean)}.
     * <p>
     * This method should be called when it is no longer desirable for the
     * GLSurfaceViewI to continue rendering, such as in response to
     * {@link android.app.Activity#onStop Activity.onStop}.
     * <p>
     * Must not be called before a renderer has been set.
     */
    public void onPause() {
        mGLThread.onPause();
    }

    /**
     * Resumes the rendering thread, re-creating the OpenGL context if necessary. It
     * is the counterpart to {@link #onPause()}.
     * <p>
     * This method should typically be called in
     * {@link android.app.Activity#onStart Activity.onStart}.
     * <p>
     * Must not be called before a renderer has been set.
     */
    public void onResume() {
        mGLThread.onResume();
    }

    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     *
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) {
        mGLThread.queueEvent(r);
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLSurfaceViewI.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogUtil.logd(TAG, "onAttachedToWindow reattach =" + mDetached);
        if (mDetached && (mRenderer != null)) {
            RenderMode renderMode = RenderMode.CONTINUOUSLY;
            if (mGLThread != null) {
                renderMode = mGLThread.getRenderMode();
            }
            mGLThread = new GLThread(this);
            if (renderMode != RenderMode.CONTINUOUSLY) {
                mGLThread.setRenderMode(renderMode);
            }
            mGLThread.start();
        }
        mDetached = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        LogUtil.logd(TAG, "onDetachedFromWindow");
        if (mGLThread != null) {
            mGLThread.requestExitAndWait();
        }
        mDetached = true;
        super.onDetachedFromWindow();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mGLThread.surfaceCreated();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        mGLThread.onWindowResize(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mGLThread.onWindowResize(w, h);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        mGLThread.surfaceDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        requestRender();
    }
}
