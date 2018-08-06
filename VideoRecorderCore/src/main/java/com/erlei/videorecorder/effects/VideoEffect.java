package com.erlei.videorecorder.effects;

import com.erlei.videorecorder.camera.Size;

public interface VideoEffect {

    void prepare(Size size);

    void applyEffect(int fbo,int textureIdIn,int textureIdOut);

    void destroy();
}
