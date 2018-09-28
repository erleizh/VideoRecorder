package com.erlei.videorecorder.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Logger;
import com.erlei.videorecorder.R;

/**
 * Created by lll on 2018/9/14
 * Email : lllemail@foxmail.com
 * Describe : 测试GDX
 */
public class GdxTestFragment extends Fragment {

    private static final String TAG = "GdxTestFragment";

    private FrameBuffer mFrameBuffer;

    public static GdxTestFragment newInstance() {
        return new GdxTestFragment();
    }

    private com.erlei.gdx.android.widget.GLSurfaceView mSurfaceView;
    private SpriteBatch mBatch;
    private Texture mTexture;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gdx_test, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);

    }

    private void initView(View view) {
        mSurfaceView = view.findViewById(R.id.SurfaceView);
        mSurfaceView.setRenderer(new Renderer(getContext(), mSurfaceView));
        mSurfaceView.setRenderMode(IRenderView.RenderMode.WHEN_DIRTY);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSurfaceView.onDestroy();
    }

    private class Renderer extends Gdx {

        Logger mLogger = new Logger("Renderer",Logger.DEBUG);

        public Renderer(Context context, IRenderView renderView) {
            super(context, renderView);
        }

        @Override
        public void create(EglCore egl, EglSurfaceBase eglSurface) {
            super.create(egl, eglSurface);
            mLogger.debug("create");
            mFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, getWidth(), getHeight(), false);
            mBatch = new SpriteBatch();
            mTexture = new Texture(files.internal("593522e9ea624.png"));
        }

        @Override
        public void render(EglSurfaceBase windowSurface, Runnable swapErrorRunnable) {
            super.render(windowSurface, swapErrorRunnable);
            mLogger.debug("render");
            clear();

            mFrameBuffer.begin();
            mBatch.begin();
            mBatch.draw(mTexture, 0, 0, mSurfaceView.getWidth(), mSurfaceView.getHeight());
            mBatch.end();
            mFrameBuffer.end();

            mBatch.begin();
            mBatch.draw(mFrameBuffer.getColorBufferTexture(), 0, 0, getWidth(), getHeight(), 0, 0,
                    mFrameBuffer.getColorBufferTexture().getWidth(),
                    mFrameBuffer.getColorBufferTexture().getHeight(), false, true);
            mBatch.end();

            renderEnd();
        }

        @Override
        public void resume() {
            super.resume();
            mLogger.debug("resume");
        }

        @Override
        public void resize(int width, int height) {
            super.resize(width, height);
            mLogger.debug("resize :" + width + "x" + height);
        }

        @Override
        public void pause() {
            super.pause();
            mLogger.debug("pause");
        }

        @Override
        public void dispose() {
            mLogger.debug("dispose");
            mFrameBuffer.dispose();
            mTexture.dispose();
            mBatch.dispose();
        }
    }
}
