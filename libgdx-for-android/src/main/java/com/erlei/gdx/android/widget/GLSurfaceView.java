package com.erlei.gdx.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.erlei.gdx.utils.Logger;


public class GLSurfaceView extends SurfaceView implements IRenderView, SurfaceHolder.Callback2 {
    private static final String TAG = "GLSurfaceViewI";
    private boolean mPreserveEGLContextOnPause = true;
    private Renderer mRenderer;
    private GLThread mGLThread;
    private boolean mDetached;

    public GLSurfaceView(Context context) {
        super(context);
        init();
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GLSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // setFormat is done by SurfaceView in SDK 2.3 and newer. Uncomment
        // this statement if back-porting to 2.2 or older:
        // holder.setFormat(PixelFormat.RGB_565);
        //
        // setType is not needed for SDK 2.0 or newer. Uncomment this
        // statement if back-porting this code to older SDKs.
        // holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
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
        return ViewType.SurfaceView;
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

    /**
     * @return true if the EGL context will be preserved when paused
     */
    @Override
    public boolean getPreserveEGLContextOnPause() {
        return mPreserveEGLContextOnPause;
    }

    @Override
    public Object getSurface() {
        return getHolder().getSurface();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mGLThread.surfaceCreated();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return
        mGLThread.surfaceDestroyed();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mGLThread.onWindowResize(w, h);
    }

    @Override
    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable finishDrawing) {
        if (mGLThread != null) {
            mGLThread.requestRenderAndNotify(finishDrawing);
        }
    }

    @Deprecated
    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        // Since we are part of the framework we know only surfaceRedrawNeededAsync
        // will be called.
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
        return getHolder().getSurfaceFrame().width();
    }

    @Override
    public int getSurfaceHeight() {
        return getHolder().getSurfaceFrame().height();
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLSurfaceViewI.
     */
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


}
