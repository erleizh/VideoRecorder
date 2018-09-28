package com.erlei.gdx.android.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import com.erlei.gdx.utils.Logger;


public class GLTextureView extends TextureView implements IRenderView, TextureView.SurfaceTextureListener {
    private static final String TAG = "GLSurfaceViewI";
    private boolean mPreserveEGLContextOnPause = true;
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

    @Override
    public ViewType getViewType() {
        return ViewType.TextureView;
    }

    /**
     * @return 获取渲染模式
     */
    @Override
    public RenderMode getRenderMode() {
        return mGLThread.getRenderMode();
    }

    @Override
    public int getGLESVersion() {
        return mGLThread.getGLESVersion();
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

    @Override
    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        mPreserveEGLContextOnPause = preserveOnPause;
    }

    @Override
    public boolean getPreserveEGLContextOnPause() {
        return mPreserveEGLContextOnPause;
    }

    @Override
    public Object getSurface() {
        return getSurfaceTexture();
    }


    public void onPause() {
        mGLThread.onPause();
    }

    public void onResume() {
        mGLThread.onResume();
    }

    public void queueEvent(Runnable r) {
        mGLThread.queueEvent(r);
    }

    @Override
    public int getSurfaceWidth() {
        return getWidth();
    }

    @Override
    public int getSurfaceHeight() {
        return getHeight();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Logger.info(TAG, "onAttachedToWindow reattach =" + mDetached);
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
        Logger.info(TAG, "onDetachedFromWindow");
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
