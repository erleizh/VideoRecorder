package com.erlei.gdx.graphics;


import android.graphics.SurfaceTexture;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.utils.GdxRuntimeException;

/**
 * Created by lll on 2018/9/29
 * Email : lllemail@foxmail.com
 * Describe :
 */
public class CameraTexture extends Texture {

    private final SurfaceTexture mSurfaceTexture;

    public CameraTexture(int glTarget, CameraTextureData data) {
        super(glTarget, Gdx.gl.glGenTexture(), data);
        mSurfaceTexture = new SurfaceTexture(getTextureObjectHandle());
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public int getWidth() {
        return data.getWidth();

    }

    @Override
    public int getHeight() {
        return data.getHeight();

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


    @Override
    public void dispose() {
        super.dispose();
        mSurfaceTexture.release();
    }

    public static class CameraTextureData implements TextureData {

        private int width;
        private int height;

        public CameraTextureData(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public TextureDataType getType() {
            return TextureDataType.Custom;
        }

        @Override
        public boolean isPrepared() {
            return true;
        }

        @Override
        public void prepare() {

        }

        @Override
        public Pixmap consumePixmap() {
            throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
        }

        @Override
        public boolean disposePixmap() {
            throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
        }

        @Override
        public void consumeCustomData(int target) {
            if (!Gdx.app.supportsExtension("OES_texture_float"))
                throw new GdxRuntimeException("Extension OES_texture_float not supported!");
        }


        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public Pixmap.Format getFormat() {
            return Pixmap.Format.RGBA8888; // it's not true, but FloatTextureData.getFormat() isn't used anywhere
        }

        @Override
        public boolean useMipMaps() {
            return false;
        }

        @Override
        public boolean isManaged() {
            return true;
        }
    }
}
