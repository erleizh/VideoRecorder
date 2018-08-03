package com.erlei.videorecorder.recorder;

import com.erlei.videorecorder.camera.Size;

public interface OnDrawTextureListener {
    /**
     * @param size -  the size of the frame
     */
    void onCameraStarted(Size size);

    /**
     * This method is invoked when camera preview has been stopped for some reason.
     * No frames will be delivered via onCameraFrame() callback after this method is called.
     */
    void onCameraStopped();

    /**
     * This method is invoked when a new preview frame from Camera is ready.
     * <p>
     * <p>
     * This method is invoked when camera preview has started. After this method is invoked
     * the frames will startRecord to be delivered to client via the onCameraFrame() callback.
     *
     *
     * @param FBOin -  the OpenGL FrameBufferObject
     * @param texIn  -  the OpenGL texture ID that contains frame in RGBA format
     * @param texOut - the OpenGL texture ID that can be used to store modified frame image t display
     * @return `true` if `texOut` should be displayed, `false` - to show `texIn`
     */
    boolean onDrawTexture(int FBOin, int texIn, int texOut);

    /**
     * @param size surfaceSize
     */
    void onSizeChanged(Size size);
}
