package com.erlei.videorecorder.effects;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.Drawable2d;
import com.erlei.videorecorder.gles.GLUtil;
import com.erlei.videorecorder.gles.ScaledDrawable2d;
import com.erlei.videorecorder.gles.Sprite2d;
import com.erlei.videorecorder.gles.Texture2dProgram;

public class OverlayEffect implements VideoEffect {

    private Texture2dProgram mProgram;
    private Sprite2d mSprite2d;
    private float[] mTexMatrix = new float[16];

    @Override
    public void prepare(Size size) {

        mSprite2d = new Sprite2d(new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE));

        mProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
        mSprite2d.setTexture(GLUtil.loadTexture(loadBitmap(), -1, true));
        mProgram.setTexSize(100, 200);

        mSprite2d.setColor(0.9f, 0.1f, 0.1f);
        mSprite2d.setPosition(size.getWidth() / 2.0f, size.getHeight()/ 2.0f);
        Matrix.setIdentityM(mTexMatrix, 0);
        Matrix.orthoM(mTexMatrix, 0, 0, size.getWidth(), 0, size.getHeight(), -1, 1);
    }

    private Bitmap loadBitmap() {
        return BitmapFactory.decodeFile("sdcard/DCIM/test.jpg");
    }

    @Override
    public int applyEffect(int fbo, int textureIdIn) {
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//        GLES20.glDisable(GLES20.GL_CULL_FACE);
//        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//
//        mSprite2d.draw(mProgram, mTexMatrix);
//        GLES20.glDisable(GLES20.GL_BLEND);
        return textureIdIn;
    }

    @Override
    public void destroy() {

    }
}
