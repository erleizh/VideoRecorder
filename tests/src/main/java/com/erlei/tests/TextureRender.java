package com.erlei.tests;

import android.content.Context;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.glutils.ShaderProgram;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by lll on 2018/10/3 .
 * Email : lllemail@foxmail.com
 * Describe : 使用mesh 渲染一张图片
 */
public class TextureRender extends Gdx {

    private static final String vertex_shader = "" +
            "attribute vec4 a_position;\n" +
            "attribute vec4 a_texCoord0;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main()\n" +
            "{\n" +
            "   v_texCoords = a_texCoord0.xy;\n" +
            "   gl_Position = a_position;\n" +
            "}\n";

    private static final String fragment_shader = "" +
            "precision mediump float;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "void main()\n" +
            "{\n" +
            "  gl_FragColor = texture2D(u_texture, v_texCoords);\n" +
            "}\n";
    private Texture mTexture;
    private ShaderProgram mShaderProgram;
    private Mesh mMesh;

    TextureRender(Context context, IRenderView renderView) {
        super(context, renderView);
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);

        mTexture = new Texture(files.internal("593522e9ea624.png"));
        mShaderProgram = new ShaderProgram(vertex_shader, fragment_shader);
        initMesh();

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


    @Override
    public void render(EglSurfaceBase windowSurface, Runnable swapErrorRunnable) {
        super.render(windowSurface, swapErrorRunnable);
        clear();


        mTexture.bind();
        mShaderProgram.begin();
        mMesh.render(mShaderProgram, GL10.GL_TRIANGLE_FAN);
        mShaderProgram.end();

        renderEnd();
    }

    @Override
    public void dispose() {
        super.dispose();
        mTexture.dispose();
        mShaderProgram.dispose();
        mMesh.dispose();
    }
}
