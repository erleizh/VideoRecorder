package com.erlei.videorecorder.effects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.util.LogUtil;

public abstract class OverlayEffect extends VideoEffects {
    private static final String TAG = "OverlayEffect";
    public static final int NO_TEXTURE = -1;

    private Bitmap mOverlay;
    private Size mSize;

    @Override
    public void onCameraStarted(Size size) {
        super.onCameraStarted(size);
        LogUtil.logd(TAG, "onCameraStarted");
        mSize = size;
    }

    @Override
    public void onSizeChanged(Size size) {
        super.onSizeChanged(size);
        LogUtil.logd(TAG, "onSizeChanged(" + size.toString() + ")");
        mSize = size;
    }

    @Override
    public void onCameraStopped() {
        super.onCameraStopped();
        LogUtil.logd(TAG, "onCameraStopped");
    }

    @Override
    public boolean onDrawTexture(int texIn, int texOut) {
        if (mOverlay == null) {
            mOverlay = Bitmap.createBitmap(mSize.getWidth(), mSize.getHeight(), Bitmap.Config.ARGB_8888);
        }
        mOverlay.eraseColor(Color.argb(0, 0, 0, 0));

        Canvas bitmapCanvas = new Canvas(mOverlay);
        drawCanvas(bitmapCanvas);

        return super.onDrawTexture(texIn, texOut);
    }

    private void attachBitmapToTexture(int textureId, Bitmap textureBitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
    }

    public static int loadTexture(final Bitmap img, final int usedTexId, final boolean recycle) {
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if (recycle) {
            img.recycle();
        }
        return textures[0];
    }

    protected abstract void drawCanvas(Canvas canvas);
}
