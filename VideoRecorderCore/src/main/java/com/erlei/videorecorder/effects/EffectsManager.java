package com.erlei.videorecorder.effects;


import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.OnDrawTextureListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lll on 2018/8/6
 * Email : lllemail@foxmail.com
 * Describe : 视频特效管理器
 */
public class EffectsManager implements OnDrawTextureListener {

    private final List<VideoEffect> mEffects = new ArrayList<>();

    public EffectsManager() {


    }


    public void addEffect(VideoEffect effect) {
        if (effect == null) return;
        if (mEffects.contains(effect)) return;
        mEffects.add(effect);
    }

    public void removeEffect(VideoEffect effect) {
        if (effect == null) return;
        if (mEffects.contains(effect)) mEffects.remove(effect);
    }

    @Override
    public void onCameraStarted(Size size) {
        for (VideoEffect videoEffect : mEffects) {
            videoEffect.prepare(size);
        }
    }

    @Override
    public void onCameraStopped() {
        for (VideoEffect videoEffect : mEffects) {
            videoEffect.destroy();
        }
    }

    @Override
    public int onDrawTexture(int FBOin, int texIn) {
        int textureId = texIn;
        for (VideoEffect videoEffect : mEffects) {
            textureId = videoEffect.applyEffect(FBOin, textureId);
        }
        return textureId;
    }

    @Override
    public void onSizeChanged(Size size) {
        for (VideoEffect videoEffect : mEffects) {
            videoEffect.prepare(size);
        }
    }
}
