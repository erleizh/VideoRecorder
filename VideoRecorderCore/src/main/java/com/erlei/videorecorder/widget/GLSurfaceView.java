package com.erlei.videorecorder.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class GLSurfaceView extends SurfaceView implements IRenderView {
    public GLSurfaceView(Context context) {
        super(context);
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GLSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @return 获取渲染模式
     */
    @Override
    public int getRenderMode() {
        return 0;
    }

    /**
     * 请求渲染
     */
    @Override
    public void requestRender() {

    }

    /**
     * 设置渲染模式
     *
     * @param renderMode 渲染模式
     */
    @Override
    public void setRenderMode(int renderMode) {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    /**
     * 设置渲染器
     *
     * @param renderer 渲染器
     */
    @Override
    public void setRenderer(Renderer renderer) {

    }

    @Override
    public Object getSurface() {
        return null;
    }
}
