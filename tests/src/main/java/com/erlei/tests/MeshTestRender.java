package com.erlei.tests;

import android.content.Context;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.Color;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.OrthographicCamera;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.VertexAttributes;
import com.erlei.gdx.graphics.glutils.ShaderProgram;

public class MeshTestRender extends Gdx {

    //Position attribute - (x, y)
    public static final int POSITION_COMPONENTS = 2;

    //Color attribute - (r, g, b, a)
    public static final int COLOR_COMPONENTS = 4;

    //Total number of components for all attributes
    public static final int NUM_COMPONENTS = POSITION_COMPONENTS + COLOR_COMPONENTS;

    //The "size" (total number of floats) for a single triangle
    public static final int PRIMITIVE_SIZE = 3 * NUM_COMPONENTS;

    //The maximum number of triangles our mesh will hold
    public static final int MAX_TRIS = 1;

    //The maximum number of vertices our mesh will hold
    public static final int MAX_VERTS = MAX_TRIS * 3;

    //The array which holds all the data, interleaved like so:
//    x, y, r, g, b, a
//    x, y, r, g, b, a,
//    x, y, r, g, b, a,
//    ... etc ...
    protected float[] verts = new float[MAX_VERTS * NUM_COMPONENTS];

    //The current index that we are pushing triangles into the array
    protected int idx = 0;
    private Mesh mesh;
    private OrthographicCamera mCamera;
    private ShaderProgram mShaderProgram;

    public MeshTestRender(Context context, IRenderView renderView) {
        super(context, renderView);
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        mCamera = new OrthographicCamera(getWidth(), getHeight());
        mesh = new Mesh(true, MAX_VERTS, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, POSITION_COMPONENTS, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, COLOR_COMPONENTS, "a_color"));
        mShaderProgram = new ShaderProgram(files.internal("vertex_shader.glsl"), files.internal("fragment_shader.glsl"));
    }

    @Override
    public void render(EglSurfaceBase windowSurface, Runnable swapErrorRunnable) {
        super.render(windowSurface, swapErrorRunnable);

        clear();

        drawTriangle(3, 0, 40, 40, Color.RED);
        drawTriangle(0, 100, 70, 90, Color.BLUE);

        flush();

        renderEnd();
    }


    private void drawTriangle(float x, float y, float width, float height, Color color) {
        //we don't want to hit any index out of bounds exception...
        //so we need to flush the batch if we can't store any more verts
        if (idx == verts.length)
            flush();

        //now we push the vertex data into our array
        //we are assuming (0, 0) is lower left, and Y is up

        //bottom left vertex
        verts[idx++] = x;            //Position(x, y)
        verts[idx++] = y;
        verts[idx++] = color.r;    //Color(r, g, b, a)
        verts[idx++] = color.g;
        verts[idx++] = color.b;
        verts[idx++] = color.a;

        //top left vertex
        verts[idx++] = x;            //Position(x, y)
        verts[idx++] = y + height;
        verts[idx++] = color.r;    //Color(r, g, b, a)
        verts[idx++] = color.g;
        verts[idx++] = color.b;
        verts[idx++] = color.a;

        //bottom right vertex
        verts[idx++] = x + width;     //Position(x, y)
        verts[idx++] = y;
        verts[idx++] = color.r;         //Color(r, g, b, a)
        verts[idx++] = color.g;
        verts[idx++] = color.b;
        verts[idx++] = color.a;
    }

    void flush() {
        //if we've already flushed
        if (idx == 0)
            return;

        //sends our vertex data to the mesh
        mesh.setVertices(verts);

        //no need for depth...
        Gdx.gl.glDepthMask(false);

        //enable blending, for alpha
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        //number of vertices we need to render
        int vertexCount = (idx / NUM_COMPONENTS);

        //update the camera with our Y-up coordiantes
        mCamera.setToOrtho(true, getWidth(), getHeight());

        //start the shader before setting any uniforms
        mShaderProgram.begin();

        //update the projection matrix so our triangles are rendered in 2D
        mShaderProgram.setUniformMatrix("u_projTrans", mCamera.combined);

        //render the mesh
        mesh.render(mShaderProgram, GL20.GL_TRIANGLES, 0, vertexCount);

        mShaderProgram.end();

        //re-enable depth to reset states to their default
        Gdx.gl.glDepthMask(true);

        //reset index to zero
        idx = 0;
    }
}
