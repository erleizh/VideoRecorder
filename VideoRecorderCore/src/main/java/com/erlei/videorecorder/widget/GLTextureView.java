package com.erlei.videorecorder.widget;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import com.erlei.videorecorder.util.LogUtil;

public class GLTextureView extends TextureView implements IRenderView, TextureView.SurfaceTextureListener {

    private static final String TAG = "GLTextureView";

    private RenderThread mRenderThread;
    private Renderer mRenderer;
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

    /**
     * @return 获取渲染模式
     */
    @Override
    public int getRenderMode() {
        return mRenderThread.getRenderMode();
    }

    /**
     * 请求渲染
     */
    @Override
    public void requestRender() {
        mRenderThread.requestRender();
    }

    /**
     * 设置渲染模式
     *
     * @param renderMode 渲染模式
     */
    @Override
    public void setRenderMode(int renderMode) {
        mRenderThread.setRenderMode(renderMode);
    }

    @Override
    public void onPause() {
        mRenderThread.onPause();
    }

    @Override
    public void onResume() {
        mRenderThread.onResume();
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLSurfaceView.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogUtil.logd(TAG, "onAttachedToWindow reattach =" + mDetached);

        if (mDetached && (mRenderer != null)) {
            int renderMode = RENDER_MODE_CONTINUOUSLY;
            if (mRenderThread != null) {
                renderMode = mRenderThread.getRenderMode();
            }
            mRenderThread = new RenderThread(this, mRenderer);
            if (renderMode != RENDER_MODE_CONTINUOUSLY) {
                mRenderThread.setRenderMode(renderMode);
            }
            mRenderThread.start();
        }
        mDetached = false;
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLSurfaceView.
     * Must not be called before a renderer has been set.
     */
    @Override
    protected void onDetachedFromWindow() {
        LogUtil.logd(TAG, "onDetachedFromWindow");
        if (mRenderThread != null) {
            mRenderThread.shutdown();
        }
        mDetached = true;
        super.onDetachedFromWindow();
    }

    /**
     * 设置渲染器
     *
     * @param renderer 渲染器
     */
    @Override
    public void setRenderer(Renderer renderer) {
        if (renderer == null) return;
        checkRenderThreadState();
        mRenderer = renderer;
        mRenderThread = new RenderThread(this, renderer);
    }

    @Override
    public Object getSurface() {
        return null;
    }

    private void checkRenderThreadState() {
        if (mRenderThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mRenderThread.surfaceCreated();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        mRenderThread.onSizeChanged(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mRenderThread.onSizeChanged(w, h);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        mRenderThread.surfaceDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mRenderThread != null) {
                mRenderThread.shutdown();
            }
        } finally {
            super.finalize();
        }
    }
}
