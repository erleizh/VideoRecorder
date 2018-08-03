package com.erlei.videorecorder.effects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.GLUtil;
import com.erlei.videorecorder.util.LogUtil;

import static com.erlei.videorecorder.gles.GLUtil.checkLocation;

public abstract class OverlayEffect extends VideoEffects {
    private static final String TAG = "OverlayEffect";
    public static final int NO_TEXTURE = -1;

    private Canvas mCanvas;
    private Bitmap mCanvasBitmap;
    private int mTexture;
    private Size mSize;

    @Override
    public void onCameraStarted(Size size) {
        super.onCameraStarted(size);
        LogUtil.logd(TAG, "onCameraStarted");
        mSize = size;
        initCanvas(size);
        mTexture = loadTexture(mCanvasBitmap, NO_TEXTURE, false);


        initOverlayRender();

    }

    private void initOverlayRender() {
        OverlayRender overlayRender = new OverlayRender();
    }

    protected void initCanvas(Size size) {
        if (mCanvas == null) {
            mCanvasBitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mCanvasBitmap);
        }
    }

    @Override
    public void onSizeChanged(Size size) {
        super.onSizeChanged(size);
        LogUtil.logd(TAG, "onSizeChanged(" + size.toString() + ")");
    }

    @Override
    public void onCameraStopped() {
        super.onCameraStopped();
        LogUtil.logd(TAG, "onCameraStopped");
    }

    @Override
    public boolean onDrawTexture(int FBOin, int texIn, int texOut) {

        clearCanvas();

        drawCanvas(mCanvas);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);


//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mCanvasBitmap, 0);


        GLES20.glDisable(GLES20.GL_BLEND);

        return super.onDrawTexture(FBOin, texIn, texOut);
    }

    protected void clearCanvas() {
        mCanvasBitmap.eraseColor(Color.TRANSPARENT);
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


    protected class OverlayRender {
        private static final String VERTEX_SHADER = ""
                + "uniform mat4 uMVPMatrix;\n"
                + "uniform mat4 uTexMatrix;\n"
                + "attribute vec4 aPosition;\n"
                + "attribute vec4 aTextureCoord;\n"
                + "varying vec2 texCoord;\n"
                + "void main() {\n"
                + "    gl_Position = uMVPMatrix * aPosition;\n"
                + "    texCoord = (uTexMatrix * aTextureCoord).xy;\n"
                + "}\n";
        private static final String FRAGMENT_SHADER = ""
                + "precision mediump float;\n"
                + "uniform sampler2D sFGTexture;\n"
                + "uniform sampler2D sBGTexture;\n"
                + "varying vec2 texCoord;\n"
                + "void main() {\n"
                + "  vec4 bg_color = texture2D(sBGTexture, texCoord);\n"
                + "  vec4 fg_color = texture2D(sFGTexture, texCoord);\n"
                + "  float colorR = (1.0 - fg_color.a) * bg_color.r + fg_color.a * fg_color.r;\n"
                + "  float colorG = (1.0 - fg_color.a) * bg_color.g + fg_color.a * fg_color.g;\n"
                + "  float colorB = (1.0 - fg_color.a) * bg_color.b + fg_color.a * fg_color.b;\n"
                + "  gl_FragColor = vec4(colorR, colorG, colorB, bg_color.a);\n"
                + "}";
        private final int mProgram;
        private final int vPos2D, vTexCoord2D, uMVPMatrix2D, uTexMatrix2D, uTexture2D;

        public OverlayRender() {

            mProgram = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

            vPos2D = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkLocation(vPos2D, "aPosition");
            vTexCoord2D = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkLocation(vTexCoord2D, "aTextureCoord");

            uMVPMatrix2D = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkLocation(uMVPMatrix2D, "uMVPMatrix2D");

            uTexMatrix2D = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");
            checkLocation(uTexMatrix2D, "uTexMatrix2D");

            uTexture2D = GLES20.glGetUniformLocation(mProgram, "sTexture");
            checkLocation(uTexture2D, "sTexture");
        }

    }
}
