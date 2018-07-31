package com.erlei.videorecorder.effects;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.OnDrawTextureListener;

public class VideoEffects implements OnDrawTextureListener {
    @Override
    public void onCameraStarted(Size size) {

    }

    @Override
    public void onCameraStopped() {

    }

    @Override
    public boolean onDrawTexture(int texIn, int texOut) {
        return false;
    }

    @Override
    public void onSizeChanged(Size size) {

    }
}
