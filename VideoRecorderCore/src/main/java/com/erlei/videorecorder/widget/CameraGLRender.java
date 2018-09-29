package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.graphics.CameraTexture;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.VertexAttributes;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.graphics.glutils.ShaderProgram;
import com.erlei.gdx.utils.Logger;

class CameraGLRender extends Gdx implements SurfaceTexture.OnFrameAvailableListener {

    private final ICameraPreview mCameraPreview;
    private Logger mLogger = new Logger("CameraGLRender");

    private SurfaceTexture mSurfaceTexture;
    private CameraTexture mCameraTexture;
    private ShaderProgram mProgram2d;
    private ShaderProgram mProgramOES;
    private float[] mTexMatrixOES = new float[16];
    private Mesh mMesh;

    CameraGLRender(Context context, ICameraPreview renderView) {
        super(context, renderView);
        mCameraPreview = renderView;
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        mLogger.debug("create(" + mCameraPreview.getSurfaceWidth() + "x" + mCameraPreview.getSurfaceHeight() + ")");
        initSurfaceTexture();
        initFrameBuffers();
        initShaderProgram();
        initMesh();
        mCameraPreview.startPreview(mSurfaceTexture);
    }

    protected void initMesh() {
        mMesh = new Mesh(true,4,6,VertexAttribute.Position(),VertexAttribute.ColorPacked(),VertexAttribute.TexCoords(0));
        mMesh.setVertices(new float[] {-0.5f, -0.5f, 0, 1, 1, 1, 1, 0, 1, 0.5f, -0.5f, 0, 1, 1, 1, 1, 1, 1, 0.5f, 0.5f, 0, 1, 1, 1,
                1, 1, 0, -0.5f, 0.5f, 0, 1, 1, 1, 1, 0, 0});
        mMesh.setIndices(new short[] {0, 1, 2, 2, 3, 0});

    }

    protected void initShaderProgram() {
        mProgram2d = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_2d.glsl"));
        mProgramOES = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_oes.glsl"));
    }

    protected void initFrameBuffers() {
        FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, getBackBufferWidth(), getBackBufferHeight(), false);


    }



    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mLogger.debug("resize(" + width + "x" + height + ")");
    }

    @Override
    public void resume() {
        super.resume();
        mCameraPreview.startPreview(mSurfaceTexture);
        mLogger.debug("resume");
    }

    @Override
    public void render(EglSurfaceBase windowSurface, Runnable swapErrorRunnable) {
        super.render(windowSurface, swapErrorRunnable);
        mLogger.debug("render");
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTexMatrixOES);

        clear();


    }

    @Override
    public void pause() {
        super.pause();
        mCameraPreview.stopPreview();
        mLogger.debug("pause");
    }

    @Override
    public void dispose() {
        super.dispose();
        mCameraPreview.stopPreview();
        mLogger.debug("dispose");
    }


    private void deleteSurfaceTexture() {
        mLogger.debug("deleteSurfaceTexture");
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
            mCameraTexture.dispose();
        }
    }

    protected void initSurfaceTexture() {
        mLogger.debug("initSurfaceTexture");
        deleteSurfaceTexture();

        mCameraTexture = new CameraTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mCameraTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        mCameraTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureObjectHandle());
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mRenderView.requestRender();
    }


}
