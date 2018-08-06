package com.erlei.videorecorder.effects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class TextOverlayEffect extends CanvasOverlayEffect {


    private final Paint mPaint;

    public TextOverlayEffect() {
        mPaint = new Paint();
        mPaint.setColor(Color.YELLOW);
//        mPaint.setAlpha(230);
        mPaint.setTextSize(40);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }

    private int count;

    @Override
    protected void drawCanvas(Canvas canvas) {
        canvas.drawText(String.valueOf(++count), canvas.getWidth() / 2, canvas.getHeight() / 2, mPaint);


    }
}
