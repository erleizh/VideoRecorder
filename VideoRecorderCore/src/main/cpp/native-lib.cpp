#include <jni.h>
#include <GLES2/gl2.h>


#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_erlei_videorecorder_gles_GLUtil_glReadPixels(JNIEnv *env, jclass type_, jint x, jint y,
                                                    jint width, jint height, jint format, jint type,
                                                    jint offset) {
    glReadPixels(x, y, width, height, format, type, 0);
}


#ifdef __cplusplus
}
#endif
