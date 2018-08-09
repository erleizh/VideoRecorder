package com.erlei.videorecorder.effects;

import com.erlei.videorecorder.camera.Size;

public interface VideoEffect {

    void prepare(Size size);

    int applyEffect(int fbo,int textureIdIn);

    void destroy();
}
