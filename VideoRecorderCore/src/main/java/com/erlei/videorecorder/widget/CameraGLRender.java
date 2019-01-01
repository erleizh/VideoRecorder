package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.graphics.CameraTexture;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.OrthographicCamera;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.graphics.glutils.ShaderProgram;
import com.erlei.gdx.math.Matrix4;
import com.erlei.gdx.utils.Logger;

import javax.microedition.khronos.opengles.GL10;

class CameraGLRender extends Gdx implements SurfaceTexture.OnFrameAvailableListener {

    private final ICameraPreview mCameraPreview;
    private Logger mLogger = new Logger("CameraGLRender");
    private CameraTexture mCameraTexture;
    private ShaderProgram mProgram2d;
    private ShaderProgram mProgramOES;
    private float[] mTexMatrixOES = new float[16];
    private Matrix4 mMatrix4 = new Matrix4();
    private Mesh mMesh;
    private OrthographicCamera mOrthographicCamera;

    CameraGLRender(Context context, ICameraPreview renderView) {
        super(context, renderView);
        mCameraPreview = renderView;
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        mLogger.debug("create(" + getWidth() + "x" + getHeight() + ")");
        initSurfaceTexture();
        initFrameBuffers();
        initShaderProgram();
        initMesh();
        mOrthographicCamera = new OrthographicCamera(getWidth(), getHeight());
        mCameraPreview.startPreview(mCameraTexture.getSurfaceTexture());
    }

    protected void initMesh() {
        float[] verts = new float[20];
        int i = 0;

        verts[i++] = -1; // x1
        verts[i++] = -1; // y1
        verts[i++] = 0;
        verts[i++] = 0f; // u1
        verts[i++] = 0f; // v1

        verts[i++] = 1f; // x2
        verts[i++] = -1; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u2
        verts[i++] = 0f; // v2

        verts[i++] = 1f; // x3
        verts[i++] = 1f; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u3
        verts[i++] = 1f; // v3

        verts[i++] = -1; // x4
        verts[i++] = 1f; // y4
        verts[i++] = 0;
        verts[i++] = 0f; // u4
        verts[i] = 1f; // v4

        mMesh = new Mesh(true, 4, 0,
                VertexAttribute.Position(), VertexAttribute.TexCoords(0));
        mMesh.setVertices(verts);

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
        mCameraPreview.startPreview(mCameraTexture.getSurfaceTexture());
        mLogger.debug("resume");
    }

    @Override
    public void render(EglSurfaceBase windowSurface, Runnable swapErrorRunnable) {
        super.render(windowSurface, swapErrorRunnable);
        mLogger.debug("render");
        mCameraTexture.getSurfaceTexture().updateTexImage();
        mCameraTexture.getSurfaceTexture().getTransformMatrix(mTexMatrixOES);
        mMatrix4.set(mTexMatrixOES);


        clear();
        mProgramOES.begin();
        updateProgram(mProgramOES);

        mProgramOES.setUniformMatrix("u_projectionViewMatrix", mOrthographicCamera.combined);
        mMesh.render(mProgramOES, GL10.GL_TRIANGLE_FAN);
        mProgramOES.end();

        renderEnd();

    }

    private void updateProgram(ShaderProgram program) {

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
        mCameraTexture.dispose();
        mLogger.debug("dispose");
    }


    protected void initSurfaceTexture() {
        mLogger.debug("initSurfaceTexture");
        mCameraTexture = new CameraTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, new CameraTexture.CameraTextureData(getWidth(), getHeight()));
        mCameraTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        mCameraTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        mCameraTexture.getSurfaceTexture().setOnFrameAvailableListener(this);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mRenderView.requestRender();
    }


}
