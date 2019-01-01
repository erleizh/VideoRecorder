package com.erlei.videorecorder.renders;

import android.content.Context;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Logger;

public class Renderer extends Gdx {
    private SpriteBatch mBatch;
    private FrameBuffer mFrameBuffer;
    private Texture mTexture;
    private Logger mLogger = new Logger("Renderer", Logger.DEBUG);

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
        mLogger.debug("render : " + String.valueOf(getDeltaTime()));
        clear();

        mFrameBuffer.begin();
        mBatch.begin();
        mBatch.draw(mTexture, 0, 0, getWidth(), getHeight());
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