package com.android_tensorflow.drawing_recognizer.drawingrecognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {

    private static final float TOUCH_TOLERANCE = 4;

    private Context mContext;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;
    private float mOldXPos;
    private float mOldYPos;

    public DrawingView(Context context) {
        super(context);
        initialize(context);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    private void initialize(Context context) {
        mContext = context;
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mPaint = new Paint();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFF000000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xPos = event.getX();
        float yPos = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                resetDrawPath(xPos, yPos);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                drawPath(xPos, yPos);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                resetTouch();
                invalidate();
                break;
            }
        }
        return true;
    }

    private void resetDrawPath(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mOldXPos = x;
        mOldYPos = y;
    }

    private void drawPath(float x, float y) {
        float dx = Math.abs(x - mOldXPos);
        float dy = Math.abs(y - mOldYPos);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mOldXPos, mOldYPos, (x + mOldXPos)/2, (y + mOldYPos)/2);
            mOldXPos = x;
            mOldYPos = y;
        }
    }

    private void resetTouch() {
        mPath.lineTo(mOldXPos, mOldYPos);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }

    public void resetCanvas() {
        mCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }
}
