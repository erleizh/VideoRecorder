/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.erlei.gdx;

import android.content.Context;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.os.Debug;

import com.erlei.gdx.android.AndroidPreferences;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.files.AndroidFiles;
import com.erlei.gdx.graphics.AndroidGL20;
import com.erlei.gdx.graphics.AndroidGL30;
import com.erlei.gdx.graphics.Cubemap;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.GL30;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.TextureArray;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.graphics.glutils.ShaderProgram;
import com.erlei.gdx.utils.FPSCounter;
import com.erlei.gdx.utils.Logger;
import com.erlei.gdx.utils.SnapshotArray;


public abstract class Gdx implements Application, IRenderView.Renderer {


    static {
        System.loadLibrary("gdx");
    }

    private static final String TAG = "Gdx";

    public static Gdx app;
    public static Files files;

    public static GL20 gl;
    public static GL30 gl30;
    public static GL20 gl20;

    protected Context mContext;
    protected final SnapshotArray<LifecycleListener> lifecycleListeners = new SnapshotArray<>(LifecycleListener.class);
    private String extensions;
    protected IRenderView mRenderView;
    protected boolean mPause;
    protected FPSCounter mFPSCounter;
    protected EglSurfaceBase mWindowSurface;
    protected Runnable mSwapErrorRunnable;

    public Gdx(Context context, IRenderView renderView) {
        this(context.getApplicationContext(), renderView, renderView.getGLESVersion() == 3 ? new AndroidGL30() : new AndroidGL20());
    }

    public Gdx(Context context, IRenderView renderView, GL20 gl) {
        AndroidGL20.init();
        mContext = context.getApplicationContext();
        mRenderView = renderView;
        files = new AndroidFiles(mContext.getAssets(), mContext.getFilesDir().getAbsolutePath());
        app = this;
        if (gl != null) setGLES(gl);
        mFPSCounter = initFPSCounter();
    }

    protected FPSCounter initFPSCounter() {
        return new FPSCounter(new FPSCounter.FPSCounter2());
    }

    public void setGLES(GL20 gles) {
        gl = gles;
        gl20 = gles;
        if (gl instanceof AndroidGL30) gl30 = (GL30) gles;
    }

    public GL20 getGL() {
        return gl;
    }

    public GL30 getGL30() {
        return gl30;
    }

    public GL20 getGL20() {
        return gl20;
    }

    @Override
    public Files getFiles() {
        return files;
    }

    @Override
    public long getJavaHeap() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap() {
        return Debug.getNativeHeapAllocatedSize();
    }

    @Override
    public Preferences getPreferences(String name) {
        return new AndroidPreferences(mContext.getSharedPreferences(name, Context.MODE_PRIVATE));
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.removeValue(listener, true);
        }
    }

    @Override
    public void postRunnable(Runnable runnable) {
        mRenderView.queueEvent(runnable);
    }

    @Override
    public boolean supportsExtension(String extension) {
        if (extensions == null) extensions = gl.glGetString(GLES10.GL_EXTENSIONS);
        return extensions.contains(extension);
    }

    public Context getContext() {
        return mContext;
    }

    public static boolean isGL30Available() {
        return gl30 != null;
    }


    protected void logManagedCachesStatus() {
        Logger.info(TAG, Mesh.getManagedStatus());
        Logger.info(TAG, Texture.getManagedStatus());
        Logger.info(TAG, Cubemap.getManagedStatus());
        Logger.info(TAG, ShaderProgram.getManagedStatus());
        Logger.info(TAG, FrameBuffer.getManagedStatus());
    }

    public int getWidth() {
        return mRenderView.getSurfaceWidth();
    }

    public int getHeight() {
        return mRenderView.getSurfaceHeight();
    }

    public int getBackBufferWidth() {
        return mRenderView.getSurfaceWidth();
    }

    public int getBackBufferHeight() {
        return mRenderView.getSurfaceHeight();
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        Mesh.invalidateAllMeshes(app);
        Texture.invalidateAllTextures(app);
        Cubemap.invalidateAllCubemaps(app);
        TextureArray.invalidateAllTextureArrays(app);
        ShaderProgram.invalidateAllShaderPrograms(app);
        FrameBuffer.invalidateAllFrameBuffers(app);
        logManagedCachesStatus();

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void render(EglSurfaceBase windowSurface, Runnable swapErrorRunnable) {
        mFPSCounter.update();
        mWindowSurface = windowSurface;
        mSwapErrorRunnable = swapErrorRunnable;


    }

    /**
     * 使用黑色清除屏幕
     */
    protected void clearColor() {
        gl.glClearColor(0, 0, 0, 0);
    }

    /**
     * 使用黑色清除屏幕
     */
    protected void clear() {
        clearColor();
        clearBuffers();
    }


    /**
     * 清除颜色缓冲区，深度缓冲区
     */
    protected void clearBuffers() {
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * 清除颜色缓冲区
     */
    protected void clearColorBuffer() {
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * 清除深度缓冲区
     */
    protected void clearDepthBuffer() {
        gl.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
    }

    public float getDeltaTime() {
        return mFPSCounter.getFPS();
    }


    protected void renderEnd() {
        boolean swapResult = mWindowSurface.swapBuffers();
        if (!swapResult) mSwapErrorRunnable.run();
    }

    @Override
    public void pause() {
        mPause = true;
    }

    public boolean isPause() {
        return mPause;
    }

    @Override
    public void resume() {
        mPause = false;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void release() {
        app = null;
        files = null;
        gl = null;
        gl20 = null;
        gl30 = null;
        mRenderView = null;
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    public void setGL30(GL30 gles30) {
        gl = gles30;
        gl20 = gles30;
        gl30 = gles30;
    }

    public void setGL20(GL20 interceptor) {
        gl20 = interceptor;
        gl = interceptor;
    }
}
