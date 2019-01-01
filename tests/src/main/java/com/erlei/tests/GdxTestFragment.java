package com.erlei.tests;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.gdx.android.widget.GLSurfaceView;
import com.erlei.gdx.android.widget.IRenderView;

/**
 * Created by lll on 2018/9/14
 * Email : lllemail@foxmail.com
 * Describe : 测试GDX
 */
public class GdxTestFragment extends Fragment {

    private static final String TAG = "GdxTestFragment";

    public static GdxTestFragment newInstance() {
        return new GdxTestFragment();
    }

    private com.erlei.gdx.android.widget.GLSurfaceView mSurfaceView;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return mSurfaceView = new GLSurfaceView(getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);

    }

    private void initView(View view) {
        mSurfaceView.setRenderer(getRenderer());
        mSurfaceView.setRenderMode(IRenderView.RenderMode.CONTINUOUSLY);
    }

    @NonNull
    private IRenderView.Renderer getRenderer() {
        IRenderView.Renderer renderer = null;
//        renderer = new Renderer(getContext(), mSurfaceView);
//        renderer = new MeshTestRender(getContext(), mSurfaceView);
        renderer = new TextureRender(getContext(), mSurfaceView);
        return renderer;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSurfaceView.onDestroy();
    }


}
