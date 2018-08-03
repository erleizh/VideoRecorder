package com.erlei.videorecorder.recorder;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.support.annotation.Nullable;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.CoordinateTransform;
import com.erlei.videorecorder.gles.DefaultCoordinateTransform;
import com.erlei.videorecorder.util.LogUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.erlei.videorecorder.gles.GLUtil.checkGlError;
import static com.erlei.videorecorder.gles.GLUtil.checkLocation;

/**
 * Created by lll on 2018/5/23
 * Email : lllemail@foxmail.com
 * Describe : 将相机预览帧数据渲染到 GLCameraView 上
 * <p>
 * 创建了三个纹理
 * <p>
 * 1 . mTexCamera[0] oes 纹理 , 相机帧数据会输出到绑定这个纹理的 SurfaceTexture
 * 2 . mTexFBO[0]   2d 纹理,
 * 3 . mTexDraw[0]  2d 纹理,
 * <p>
 * 创建了一个帧缓冲 mFBO[0] , CameraGlView 有 getCameraTextureCallback 会使用这个fbo
 * 先将相机纹理数据绘制到这个帧缓冲区里面 ,然后调用 drawTexture
 * 会传递两个纹理id , 回调方法需要将修改后的纹理数据从 mTexFBO[0] 转到 mTexDraw[0]
 * 根据回调方法的返回值决定将mTexFBO[0] 或 mTexDraw[0] 纹理的数据绘制到屏幕
 * <p>
 */
class CameraGLRenderer {
    private static final String TAG = "CameraGLRenderer";

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

    private static final String sFragmentOESShader = ""
            + "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "varying vec2 texCoord;\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(sTexture,texCoord);\n"
            + "}";

    private static final String sFragment2DShader = ""
            + "precision mediump float;\n"
            + "uniform sampler2D sTexture;\n"
            + "varying vec2 texCoord;\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(sTexture,texCoord);\n"
            + "}";
    private CoordinateTransform mTextureTransform;
    private FloatBuffer mVertexBuffer, mTexOESBuffer, mTex2DBuffer;
    private int mProgramOES, mProgram2D;
    private int aPosOES, aTexCoordOES, vPos2D, vTexCoord2D, uMVPMatrixOES, uTexMatrixOES, uMVPMatrix2D, uTexMatrix2D, uTextureOES, uTexture2D;
    private int mFBOWidth, mFBOHeight;
    private int[] mTexCamera = {0}, mTexFBO = {0}, mTexDraw = {0}, mFBO = {0};
    private SurfaceTexture mTexture;
    private float[] mMVPMatrixOES, mTexMatrixOES, mMVPMatrix2D = new float[16], mTexMatrix2D = new float[16];
    private Size mSurfaceSize;
    private CameraController mCameraController;
    private OnDrawTextureListener mDrawTextureListener;

    public CameraGLRenderer(CameraController cameraController) {
        this(cameraController, null);
    }

    public CameraGLRenderer(CameraController cameraController, @Nullable CoordinateTransform transform) {
        mCameraController = cameraController;
        mTextureTransform = transform;
        initFloatBuffer();
        generateProgram();
        initSurfaceTexture();
    }

    public void updateCoordinateTransform() {
        LogUtil.logd("updateCoordinateTransform");
        if (mTextureTransform == null) {
            mTextureTransform = new DefaultCoordinateTransform(mCameraController);
        }
        float[] oesTextureCoordinate = mTextureTransform.getOESTextureCoordinate();
        if (oesTextureCoordinate.length != 8)
            throw new IllegalArgumentException("float[] length must be 8");
        mTexOESBuffer.put(oesTextureCoordinate).position(0);

        float[] vertexCoordinate = mTextureTransform.getVertexCoordinate();
        if (vertexCoordinate.length != 8)
            throw new IllegalArgumentException("float[] length must be 8");
        mVertexBuffer.put(vertexCoordinate).position(0);

        float[] textureCoordinate = mTextureTransform.get2DTextureCoordinate();
        if (textureCoordinate.length != 8)
            throw new IllegalArgumentException("float[] length must be 8");
        mTex2DBuffer.put(textureCoordinate).position(0);

        float[] mvpMatrixOES = mTextureTransform.getMVPMatrixOES();
        if (mvpMatrixOES.length != 16)
            throw new IllegalArgumentException("matrix length must be 16");
        mMVPMatrixOES = mvpMatrixOES;

        float[] mvpMatrix2D = mTextureTransform.getMVPMatrix2D();
        if (mvpMatrix2D.length != 16)
            throw new IllegalArgumentException("matrix length must be 16");
        mMVPMatrix2D = mvpMatrix2D;

        float[] textureMatrix2D = mTextureTransform.getTextureMatrix2D();
        if (textureMatrix2D.length != 16)
            throw new IllegalArgumentException("matrix length must be 16");
        mTexMatrix2D = textureMatrix2D;

        float[] textureMatrixOES = mTextureTransform.getTextureMatrixOES();
        if (textureMatrixOES.length != 16)
            throw new IllegalArgumentException("matrix length must be 16");
        mTexMatrixOES = textureMatrixOES;


    }

    protected void setPreviewSize(Size size) {
        LogUtil.logd("onSizeChanged(" + size.getWidth() + "x" + size.getHeight() + ")");
        updateCoordinateTransform();
        mSurfaceSize = size;
        initFBO(size.getWidth(), size.getHeight());
    }

    public SurfaceTexture getTexture() {
        return mTexture;
    }


    private long mLastDrawTime;

    public void onDrawFrame() {
        if (mTexture == null) return;
        synchronized (this) {
            mTexture.updateTexImage();
            mTexture.getTransformMatrix(mTexMatrixOES);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if (mDrawTextureListener != null) {
                // texCamera(OES) -> texFBO
                if (LogUtil.LOG_ENABLE) mLastDrawTime = System.nanoTime();
                drawTexture(mTexCamera[0], true, mFBO[0]);
                if (LogUtil.LOG_ENABLE)
                    LogUtil.logi(TAG, "drawTexture -> texFBO = \t\t\t\t" + ((System.nanoTime() - mLastDrawTime) / 1000) + "μs");
                // call user code (texFBO -> texDraw)
                if (LogUtil.LOG_ENABLE) mLastDrawTime = System.nanoTime();
                boolean drawTexture = mDrawTextureListener.onDrawTexture(mFBO[0],mTexFBO[0], mTexDraw[0]);
                if (LogUtil.LOG_ENABLE)
                    LogUtil.logi(TAG, "onDrawTexture = " + drawTexture + " = \t\t\t\t" + ((System.nanoTime() - mLastDrawTime) / 1000) + "μs");
                if (drawTexture) {
                    mLastDrawTime = System.nanoTime();
                    // texDraw -> screen
                    drawTexture(mTexDraw[0], false, 0);
                    if (LogUtil.LOG_ENABLE)
                        LogUtil.logi(TAG, "drawTexture -> screen = \t\t\t\t" + ((System.nanoTime() - mLastDrawTime) / 1000) + "μs");
                } else {
                    mLastDrawTime = System.nanoTime();
                    // texFBO -> screen
                    drawTexture(mTexFBO[0], false, 0);
                    if (LogUtil.LOG_ENABLE)
                        LogUtil.logi(TAG, "drawTexture -> screen = \t\t\t\t" + ((System.nanoTime() - mLastDrawTime) / 1000) + "μs");
                }
            } else {
                // texCamera(OES) -> screen
                drawTexture(mTexCamera[0], true, 0);
            }
            //Log.i(LOGTAG, "onDrawFrame end");
        }
    }

    private void drawTexture(int tex, boolean isOES, int fbo) {

        checkGlError("draw startRecord");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);

        if (fbo == 0)
            GLES20.glViewport(0, 0, mSurfaceSize.getWidth(), mSurfaceSize.getHeight());
        else
            GLES20.glViewport(0, 0, mFBOWidth, mFBOHeight);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (isOES) {
            GLES20.glUseProgram(mProgramOES);
            checkGlError("glUseProgram");
            GLES20.glVertexAttribPointer(aPosOES, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
            checkGlError("glVertexAttribPointer");
            GLES20.glVertexAttribPointer(aTexCoordOES, 2, GLES20.GL_FLOAT, false, 4 * 2, mTexOESBuffer);
            checkGlError("glVertexAttribPointer");
            // Copy the model / view / projection matrix over.
            GLES20.glUniformMatrix4fv(uMVPMatrixOES, 1, false, mMVPMatrixOES, 0);
            checkGlError("glUniformMatrix4fv");
            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(uTexMatrixOES, 1, false, mTexMatrixOES, 0);
            checkGlError("glUniformMatrix4fv");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex);
            GLES20.glUniform1i(uTextureOES, 0);

        } else {
            GLES20.glUseProgram(mProgram2D);
            checkGlError("glUseProgram");
            GLES20.glVertexAttribPointer(vPos2D, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
            checkGlError("glVertexAttribPointer");
            GLES20.glVertexAttribPointer(vTexCoord2D, 2, GLES20.GL_FLOAT, false, 4 * 2, mTex2DBuffer);
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
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        checkGlError("glDrawArrays");
        GLES20.glUseProgram(0);
    }


    protected void destroy() {
        synchronized (this) {
            LogUtil.logd("stopPreview");
            deleteSurfaceTexture();
            deleteFBO();
        }
    }

    private void deleteFBO() {
        LogUtil.logd("deleteFBO(" + mFBOWidth + "x" + mFBOHeight + ")");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteFramebuffers(1, mFBO, 0);

        deleteTex(mTexFBO);
        deleteTex(mTexDraw);
        mFBOWidth = mFBOHeight = 0;
    }

    private void initFBO(int width, int height) {
        LogUtil.logd("initFBO(" + width + "x" + height + ")");

        deleteFBO();
//        mTexFBO[0], mTexDraw[0]
        GLES20.glGenTextures(1, mTexDraw, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexDraw[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glGenTextures(1, mTexFBO, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexFBO[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //int hFBO;
        GLES20.glGenFramebuffers(1, mFBO, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBO[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexFBO[0], 0);
        LogUtil.logd("initFBO error status: " + GLES20.glGetError());

        int FBOstatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (FBOstatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            LogUtil.loge("initFBO failed, status: " + FBOstatus);
        } else {
            LogUtil.logd("initFBO success, status: " + FBOstatus);
        }


        mFBOWidth = width;
        mFBOHeight = height;
    }

    protected SurfaceTexture initSurfaceTexture() {
        LogUtil.logd("initSurfaceTexture");
        deleteSurfaceTexture();
        initTexOES(mTexCamera);
        mTexture = new SurfaceTexture(mTexCamera[0]);
        return mTexture;
    }

    private void initTexOES(int[] tex) {
        LogUtil.logd("initTexOES");
        if (tex.length == 1) {
            GLES20.glGenTextures(1, tex, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        }
    }

    private void deleteSurfaceTexture() {
        LogUtil.logd("deleteSurfaceTexture");
        if (mTexture != null) {
            mTexture.release();
            mTexture = null;
            deleteTex(mTexCamera);
        }
    }

    private void deleteTex(int[] tex) {
        LogUtil.logd("deleteTex");
        if (tex.length == 1) {
            GLES20.glDeleteTextures(1, tex, 0);
        }
    }

    private void generateProgram() {
        String strGLVersion = GLES20.glGetString(GLES20.GL_VERSION);
        if (strGLVersion != null)
            LogUtil.logd("OpenGL ES version: " + strGLVersion);

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        mProgramOES = loadShader(sVertexShader, sFragmentOESShader);

        aPosOES = GLES20.glGetAttribLocation(mProgramOES, "aPosition");
        checkLocation(aPosOES, "aPosition");

        aTexCoordOES = GLES20.glGetAttribLocation(mProgramOES, "aTextureCoord");
        checkLocation(aTexCoordOES, "aTextureCoord");

        uMVPMatrixOES = GLES20.glGetUniformLocation(mProgramOES, "uMVPMatrix");
        checkLocation(uMVPMatrixOES, "uMVPMatrix");
        uTexMatrixOES = GLES20.glGetUniformLocation(mProgramOES, "uTexMatrix");
        checkLocation(uTexMatrixOES, "uTexMatrix");

        uTextureOES = GLES20.glGetUniformLocation(mProgramOES, "sTexture");
        checkLocation(uTextureOES, "sTexture");

        GLES20.glEnableVertexAttribArray(aPosOES);
        checkGlError("glEnableVertexAttribArray aPosOES");
        GLES20.glEnableVertexAttribArray(aTexCoordOES);
        checkGlError("glEnableVertexAttribArray aTexCoordOES");


        mProgram2D = loadShader(sVertexShader, sFragment2DShader);
        vPos2D = GLES20.glGetAttribLocation(mProgram2D, "aPosition");
        checkLocation(vPos2D, "aPosition");
        vTexCoord2D = GLES20.glGetAttribLocation(mProgram2D, "aTextureCoord");
        checkLocation(vTexCoord2D, "aTextureCoord");

        uMVPMatrix2D = GLES20.glGetUniformLocation(mProgram2D, "uMVPMatrix");
        checkLocation(uMVPMatrix2D, "uMVPMatrix2D");

        uTexMatrix2D = GLES20.glGetUniformLocation(mProgram2D, "uTexMatrix");
        checkLocation(uTexMatrix2D, "uTexMatrix2D");

        uTexture2D = GLES20.glGetUniformLocation(mProgram2D, "sTexture");
        checkLocation(uTexture2D, "sTexture");
        GLES20.glEnableVertexAttribArray(vPos2D);
        checkGlError("glEnableVertexAttribArray vPos2D");
        GLES20.glEnableVertexAttribArray(vTexCoord2D);
        checkGlError("glEnableVertexAttribArray vTexCoord2D");
    }

    private void initFloatBuffer() {
        int size = 8 * Float.SIZE / Byte.SIZE;
        mVertexBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexOESBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTex2DBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    private int loadShader(String vss, String fss) {
        LogUtil.logd("loadShader");
        int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vshader, vss);
        GLES20.glCompileShader(vshader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            LogUtil.loge("Could not compile vertex shader: " + GLES20.glGetShaderInfoLog(vshader));
            GLES20.glDeleteShader(vshader);
            return 0;
        }

        int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fshader, fss);
        GLES20.glCompileShader(fshader);
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            LogUtil.loge("Could not compile fragment shader:" + GLES20.glGetShaderInfoLog(fshader));
            GLES20.glDeleteShader(vshader);
            GLES20.glDeleteShader(fshader);
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vshader);
        GLES20.glAttachShader(program, fshader);
        GLES20.glLinkProgram(program);
        GLES20.glDeleteShader(vshader);
        GLES20.glDeleteShader(fshader);
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            LogUtil.loge("Could not link shader program: " + GLES20.glGetProgramInfoLog(program));
            return 0;
        }
        GLES20.glValidateProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, status, 0);
        if (status[0] == 0) {
            LogUtil.loge("Shader program validation error: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }
        LogUtil.logd("Shader program is built OK");
        return program;
    }

    public void setOnDrawTextureListener(OnDrawTextureListener drawTextureListener) {
        mDrawTextureListener = drawTextureListener;
    }
}