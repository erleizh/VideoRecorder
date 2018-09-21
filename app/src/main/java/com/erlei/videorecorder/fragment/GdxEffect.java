package com.erlei.videorecorder.fragment;

import com.erlei.gdx.files.AndroidFiles;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.Batch;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.effects.VideoEffect;

class GdxEffect implements VideoEffect {

    private Batch mBatch;
    private Texture mTexture;
    private Size mSize;

    @Override
    public void prepare(Size size) {
        mSize = size;
        mBatch = new SpriteBatch(size);
        mTexture = new Texture(AndroidFiles.getInstance().absolute("/sdcard/test.png"));
    }

    @Override
    public int applyEffect(int fbo, int textureIdIn) {
        mBatch.begin();
        mBatch.draw(mTexture, 0 , 0,mSize.getWidth(),mSize.getHeight());
        mBatch.end();
        return textureIdIn;
    }

    @Override
    public void destroy() {
        mBatch.dispose();
        mTexture.dispose();
    }
}
