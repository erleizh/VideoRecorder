package com.erlei.videorecorder.effects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class TextOverlayEffect extends OverlayEffect {


    private final Paint mPaint;

    public TextOverlayEffect() {
        mPaint = new Paint();
        mPaint.setColor(Color.YELLOW);
        mPaint.setAlpha(230);
        mPaint.setTextSize(40);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void drawCanvas(Canvas canvas) {

        canvas.drawText("ahahaha", canvas.getWidth() / 2, canvas.getHeight() / 2, mPaint);


    }
}
