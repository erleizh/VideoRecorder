package com.erlei.videorecorder.effects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.CoordinateTransform;
import com.erlei.videorecorder.gles.GLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.erlei.videorecorder.gles.GLUtil.checkGlError;
import static com.erlei.videorecorder.gles.GLUtil.checkLocation;

public abstract class CanvasOverlayEffect implements VideoEffect {
    private static final String TAG = "CanvasOverlayEffect";


    private Canvas mCanvas;
    private Bitmap mCanvasBitmap;
    private int mTexture;
    private Size mSize;
    private Render mRender;


    @Override
    public void prepare(Size size) {
        mSize = size;
        initCanvas(size);
        mTexture = GLUtil.loadTexture(mCanvasBitmap, GLUtil.NO_TEXTURE, false);

        initRender();
    }

    @Override
    public int applyEffect(int fbo, int textureIdIn) {
        clearCanvas();

        drawCanvas(mCanvas);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mCanvasBitmap, 0);

        mRender.drawTexture(mTexture, fbo);

        GLES20.glDisable(GLES20.GL_BLEND);

        return textureIdIn;
    }

    @Override
    public void destroy() {

    }

    private void initRender() {
        mRender = new Render(mSize);
    }

    protected void initCanvas(Size size) {
        if (mCanvas == null) {
            mCanvasBitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mCanvasBitmap);
        }
    }


    protected void clearCanvas() {
        mCanvasBitmap.eraseColor(Color.TRANSPARENT);
    }

    protected abstract void drawCanvas(Canvas canvas);


    private class Render {

        private static final String TAG = "Render";

        private final float[] sVertexCoords = {
                -1.0f, -1.0f,       // 0 bottom left
                1.0f, -1.0f,        // 1 bottom right
                -1.0f, 1.0f,        // 2 top left
                1.0f, 1.0f,         // 3 top right
        };
        protected float TEXTURE_ROTATED_0[] = {
                0.0f, 0.0f,     // bottom left
                1.0f, 0.0f,     // bottom right
                0.0f, 1.0f,     // top left
                1.0f, 1.0f,     // top right
        };

        private static final String sVertexShader = ""
                + "uniform mat4 uMVPMatrix;\n"
                + "uniform mat4 uTexMatrix;\n"
                + "attribute vec4 aPosition;\n"
                + "attribute vec4 aTextureCoord;\n"
                + "varying vec2 texCoord;\n"
                + "void main() {\n"
                + "    gl_Position = uMVPMatrix * aPosition;\n"
                + "    texCoord = (uTexMatrix * aTextureCoord).xy;\n"
                + "}\n";
        private static final String sFragment2DShader = ""
                + "precision mediump float;\n"
                + "uniform sampler2D sTexture;\n"
                + "varying vec2 texCoord;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(sTexture,texCoord);\n"
                + "}";

        private final int mProgram;
        private int vPos2D, vTexCoord2D, uMVPMatrix2D, uTexMatrix2D, uTexture2D;
        private FloatBuffer mTextureBuffer, mVertexBuffer;
        private float[] mMVPMatrix2D = new float[16], mTexMatrix2D = new float[16];

        public Render(Size size) {
            initFloatBuffer();
            Matrix.setIdentityM(mMVPMatrix2D, 0);
            Matrix.setIdentityM(mTexMatrix2D, 0);

            CoordinateTransform.flip(mMVPMatrix2D, false, true);

            mProgram = GLUtil.createProgram(sVertexShader, sFragment2DShader);

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
            GLES20.glEnableVertexAttribArray(vPos2D);
            checkGlError("glEnableVertexAttribArray vPos2D");
            GLES20.glEnableVertexAttribArray(vTexCoord2D);
            checkGlError("glEnableVertexAttribArray vTexCoord2D");

        }

        private void drawTexture(int tex, int fbo) {

            checkGlError("draw startRecord");
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);

            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");
            GLES20.glVertexAttribPointer(vPos2D, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
            checkGlError("glVertexAttribPointer");
            GLES20.glVertexAttribPointer(vTexCoord2D, 2, GLES20.GL_FLOAT, false, 4 * 2, mTextureBuffer);
            checkGlError("glVertexAttribPointer");
            // Copy the model / view / projection matrix over.
            GLES20.glUniformMatrix4fv(uMVPMatrix2D, 1, false, mMVPMatrix2D, 0);
            checkGlError("glUniformMatrix4fv");
            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(uTexMatrix2D, 1, false, mTexMatrix2D, 0);
            checkGlError("glUniformMatrix4fv");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
            GLES20.glUniform1i(uTexture2D, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
            GLES20.glUseProgram(0);
        }

        private void initFloatBuffer() {
            int size = 8 * Float.SIZE / Byte.SIZE;
            mVertexBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer().put(sVertexCoords);
            mTextureBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer().put(TEXTURE_ROTATED_0);
            mVertexBuffer.position(0);
            mTextureBuffer.position(0);
        }
    }


}
