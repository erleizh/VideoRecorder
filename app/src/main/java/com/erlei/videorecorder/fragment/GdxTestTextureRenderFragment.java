package com.erlei.videorecorder.fragment;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.gdx.files.AndroidFiles;
import com.erlei.gdx.graphics.FrameBuffer;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.utils.Pixmap;
import com.erlei.videorecorder.R;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.gles.EglSurfaceBase;
import com.erlei.videorecorder.recorder.IVideoRecorder;
import com.erlei.videorecorder.util.SaveFrameTask;
import com.erlei.videorecorder.widget.IRenderView;
import com.erlei.videorecorder.util.LogUtil;
import com.erlei.videorecorder.widget.GLSurfaceView;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by lll on 2018/9/14
 * Email : lllemail@foxmail.com
 * Describe : 测试GDX
 */
public class GdxTestTextureRenderFragment extends Fragment implements IRenderView.Renderer {

    private static final String TAG = "GdxTestFragment";

    private FrameBuffer mFrameBuffer;
    private EglSurfaceBase mEglSurface;
    private ByteBuffer mByteBuffer;

    public static GdxTestTextureRenderFragment newInstance() {
        return new GdxTestTextureRenderFragment();
    }

    private GLSurfaceView mSurfaceView;
    private SpriteBatch mBatch;
    private Texture mTexture;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gdx_test_texture_render, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);

    }

    private void initView(View view) {
        mSurfaceView = view.findViewById(R.id.SurfaceView);
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(IRenderView.RenderMode.WHEN_DIRTY);
    }


    @Override
    public void onSurfaceCreated(EglCore egl, EglSurfaceBase eglSurface) {
        mEglSurface = eglSurface;
        mFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, mSurfaceView.getWidth(), mSurfaceView.getHeight(), false);
        mBatch = new SpriteBatch(new Size(mSurfaceView.getWidth(), mSurfaceView.getHeight()));
        mTexture = new Texture(AndroidFiles.getInstance().internal("filter.png"), Pixmap.Format.RGBA8888,false);
        mTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        mByteBuffer = ByteBuffer.allocateDirect(150 * 150 * 4).order(ByteOrder.nativeOrder());
    }


    @Override
    public void onSurfaceChanged(int width, int height) {

    }

    @Override
    public void onDrawFrame() {
        long millis = System.currentTimeMillis();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//        mFrameBuffer.begin();
        mBatch.begin();
        mBatch.draw(mTexture, 0, 0,150,150);
        mBatch.end();
//        mFrameBuffer.end();
        LogUtil.logd(TAG, String.valueOf(System.currentTimeMillis() - millis));

        try {
            mEglSurface.saveFrame(new File("sdcard/AIM/Pictures/filtered.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

//        millis = System.currentTimeMillis();
//        mBatch.begin();
//        mBatch.draw(mFrameBuffer.getColorBufferTexture(), 0, 0, 150, 150, 0, 0,
//                mFrameBuffer.getColorBufferTexture().getWidth(),
//                mFrameBuffer.getColorBufferTexture().getHeight(), false, true);
//        mBatch.end();
//        LogUtil.logd(TAG, String.valueOf(System.currentTimeMillis() - millis));

//        mByteBuffer.rewind();
//        GLES20.glReadPixels(0, 0, 150, 150, GLES30.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mByteBuffer);
//        new SaveFrameTask(150, 150, new IVideoRecorder.TakePictureCallback() {
//            @Override
//            public void onPictureTaken(File picture) {
//
//            }
//
//            @Override
//            public File onPictureTaken(Bitmap bitmap) {
//                return new File("sdcard/filtered.png");
//            }
//        }).execute(mByteBuffer);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFrameBuffer.dispose();
        mTexture.dispose();
        mBatch.dispose();
    }

}
