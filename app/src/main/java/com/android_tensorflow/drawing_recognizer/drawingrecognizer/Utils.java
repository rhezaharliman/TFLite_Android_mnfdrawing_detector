package com.android_tensorflow.drawing_recognizer.drawingrecognizer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

    public static ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap, int batchSize, int inputSize, int pixelSize) {
        // 32-bit float value requires 4 bytes
        // https://github.com/tensorflow/tensorflow/blob/25b4086bb5ba1788ceb6032eda58348f6e20a71d/tensorflow/contrib/lite/java/demo/app/src/main/java/com/example/android/tflitecamerademo/ImageClassifierFloatInception.java#L74
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * batchSize * inputSize * inputSize * pixelSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.put((byte) ((val >> 16) & 0xFF));
                byteBuffer.put((byte) ((val >> 8) & 0xFF));
                byteBuffer.put((byte) (val & 0xFF));
            }
        }
        return byteBuffer;
    }

    private static final ColorMatrix INVERT = new ColorMatrix(
            new float[] {
                    -1,  0,  0,  0, 255,
                    0, -1,  0,  0, 255,
                    0,  0, -1,  0, 255,
                    0,  0,  0,  1,   0
            });

    private static final ColorFilter COLOR_FILTER = new ColorMatrixColorFilter(INVERT);


    public static Bitmap invert(Bitmap image) {
        Bitmap inverted = Bitmap.createBitmap(image.getWidth(), image.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(inverted);
        Paint paint = new Paint();
        paint.setColorFilter(COLOR_FILTER);
        canvas.drawBitmap(image, 0, 0, paint);
        return inverted;
    }
}
