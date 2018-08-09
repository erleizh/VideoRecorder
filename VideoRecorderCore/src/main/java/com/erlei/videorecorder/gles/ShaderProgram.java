package com.erlei.videorecorder.gles;

import android.graphics.PointF;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

/**
 * Created by lll on 2018/8/7 .
 * Email : lllemail@foxmail.com
 * Describe : 渲染程序
 */
public class ShaderProgram {

    protected int mProgram;

    public ShaderProgram() {
        this("");
    }

    public ShaderProgram(String vertex) {
        this(vertex, "fragment");
    }

    public ShaderProgram(String vertex, String fragment) {
        mProgram = GLUtil.createProgram(vertex, fragment);
    }

    public void use() {
        use(true);
    }

    public void use(boolean use) {
        GLES20.glUseProgram(use ? mProgram : 0);
    }


    public void setInteger(final int location, final int intValue) {
        GLES20.glUniform1i(location, intValue);
    }

    public void setFloat(final int location, final float floatValue) {
        GLES20.glUniform1f(location, floatValue);
    }

    public void setFloatVec2(final int location, final float[] arrayValue) {
        GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    public void setFloatVec3(final int location, final float[] arrayValue) {
        GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    public void setFloatVec4(final int location, final float[] arrayValue) {
        GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    public void setFloatArray(final int location, final float[] arrayValue) {
        GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
    }

    public void setPoint(final int location, final PointF point) {
        float[] vec2 = new float[2];
        vec2[0] = point.x;
        vec2[1] = point.y;
        GLES20.glUniform2fv(location, 1, vec2, 0);
    }

    public void setUniformMatrix3fv(final int location, final float[] matrix) {
        GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
    }

    public void setUniformMatrix4fv(final int location, final float[] matrix) {
        GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
    }

    public void setInteger(String name, final int intValue) {
        int uniformLocation = getUniformLocation(name);
        setInteger(uniformLocation, intValue);
    }

    public void setFloat(final String name, final float floatValue) {
        int uniformLocation = getUniformLocation(name);
        setFloat(uniformLocation, floatValue);
    }

    public void setFloatVec2(final String name, final float[] arrayValue) {
        int uniformLocation = getUniformLocation(name);
        setFloatVec2(uniformLocation, arrayValue);
    }

    public void setFloatVec3(final String name, final float[] arrayValue) {
        int uniformLocation = getUniformLocation(name);
        setFloatVec3(uniformLocation, arrayValue);
    }

    public void setFloatVec4(final String name, final float[] arrayValue) {
        int uniformLocation = getUniformLocation(name);
        setFloatVec4(uniformLocation, arrayValue);
    }

    public void setFloatArray(final String name, final float[] arrayValue) {
        int uniformLocation = getUniformLocation(name);
        setFloatArray(uniformLocation, arrayValue);
    }

    public void setPoint(final String name, final PointF point) {
        int uniformLocation = getUniformLocation(name);
        setPoint(uniformLocation, point);
    }

    public void setUniformMatrix3fv(final String name, final float[] matrix) {
        int uniformLocation = getUniformLocation(name);
        setUniformMatrix3fv(uniformLocation, matrix);
    }

    public void setUniformMatrix4fv(final String name, final float[] matrix) {
        int uniformLocation = getUniformLocation(name);
        setUniformMatrix4fv(uniformLocation, matrix);
    }

    public int getUniformLocation(int program, String name) {
        int location = GLES20.glGetUniformLocation(program, name);
        GLUtil.checkLocation(location, "setInteger " + name);
        return location;
    }

    public int getUniformLocation(String name) {
        return getUniformLocation(mProgram, name);
    }
}
