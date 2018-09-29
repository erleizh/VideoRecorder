package com.erlei.gdx.graphics;


import com.erlei.gdx.Gdx;

/**
 * Created by lll on 2018/9/29
 * Email : lllemail@foxmail.com
 * Describe :
 */
public class CameraTexture extends GLTexture {

    public CameraTexture(int glTarget) {
        super(glTarget, Gdx.gl.glGenTexture());
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    protected void reload() {

    }
}
