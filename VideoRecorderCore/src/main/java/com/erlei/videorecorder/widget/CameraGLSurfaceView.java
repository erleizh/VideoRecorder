package com.erlei.videorecorder.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.erlei.gdx.android.widget.GLSurfaceView;

public class CameraGLSurfaceView extends GLSurfaceView {

    public CameraGLSurfaceView(Context context) {
        super(context);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        super.init();
        setRenderer(new CameraGLRender(getContext(), this));
        setRenderMode(RenderMode.WHEN_DIRTY);

    }

}
