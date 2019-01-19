package com.android_tensorflow.drawing_recognizer.drawingrecognizer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.tensorflow.lite.Interpreter;

public class MainActivity extends AppCompatActivity {

    private static final String MODEL_FILE = "fashion_mnist.tflite";
    private static final String LABEL_FILE = "fashion_mnist_label.txt";
    private static final int SIZE = 28;
    private static final int PIXEL_SIZE = 1;
    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final float CONFIDENCE_THRESHOLD = 0.1f;

    ArrayList<String> mLabels;
    DrawingView mDrawingView;
    FloatingActionButton mBtnReset;
    FloatingActionButton mBtnPredict;
    Interpreter mInterpreter;
    private ByteBuffer mImgData;
    private final int[] mImagePixels = new int[SIZE * SIZE];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        preparesClassifier();
    }

    private void initView() {
        mDrawingView = findViewById(R.id.view_drawing);
        mBtnPredict = findViewById(R.id.btn_predict);
        mBtnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPredict();
            }
        });
        mBtnReset = findViewById(R.id.btn_reset);
        mBtnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingView.resetCanvas();
            }
        });
    }

    private void doPredict() {
        Bitmap bitmap = mDrawingView.getBitmap();
        Bitmap invertedBitmap = Utils.invert(bitmap);
        Log.i("TAG", "==== Size= width="+bitmap.getWidth()+" height="+bitmap.getHeight());
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(invertedBitmap, SIZE, SIZE, false);
//        ByteBuffer byteBuffer = Utils.convertBitmapToByteBuffer(scaledBitmap, BATCH_SIZE, SIZE, PIXEL_SIZE);
        convertBitmapToByteBuffer(scaledBitmap);
        float[][] result = new float[1][mLabels.size()];
//        mInterpreter.run(byteBuffer, result);
        long startTime = SystemClock.uptimeMillis();
        mInterpreter.run(mImgData, result);
        long endTime = SystemClock.uptimeMillis();
        long timeCost = endTime - startTime;
        Log.v("TAG", "run(): result = " + Arrays.toString(result[0])
                + ", timeCost = " + timeCost);
        final ArrayList<ClassificationResult> results = sortResult(result);
        new ResultDialogFragment(results).show(getSupportFragmentManager(), "RESULT");
    }

    private void preparesClassifier() {
        try {
            mInterpreter = new Interpreter(loadModelFile(this));
            mLabels = loadLabels();
        } catch (IOException ex) {
            ex.getLocalizedMessage();
        }
    }

//    private File loadFile(String file) {
//        File modelFile = new File(getCacheDir()+"/"+file);
//        try {
//            InputStream inputStream = getAssets().open(file);
//            int size = inputStream.available();
//            byte[] buffer = new byte[size];
//            inputStream.read(buffer);
//            inputStream.close();
//            FileOutputStream outputStream = new FileOutputStream(modelFile);
//            outputStream.write(buffer);
//            outputStream.close();
//        } catch (IOException ex) {
//            ex.getStackTrace();
//        }
//        return modelFile;
//    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (mImgData == null) {
            mImgData = ByteBuffer.allocateDirect(
                    4 * BATCH_SIZE * SIZE* SIZE * PIXEL_SIZE);
            mImgData.order(ByteOrder.nativeOrder());
//            return;
        }
        mImgData.rewind();

        bitmap.getPixels(mImagePixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < SIZE; ++i) {
            for (int j = 0; j < SIZE; ++j) {
                final int val = mImagePixels[pixel++];
                mImgData.putFloat(convertToGreyScale(val));
            }
        }
    }

    private float convertToGreyScale(int color) {
        return (((color >> 16) & 0xFF) + ((color >> 8) & 0xFF) + (color & 0xFF)) / 3.0f / 255.0f;
    }


    private ArrayList<String> loadLabels() {
        ArrayList<String> labelList = new ArrayList<>();
        String line;
        try {
            InputStreamReader inputStream = new InputStreamReader(getAssets().open(LABEL_FILE));
            BufferedReader reader = new BufferedReader(inputStream);
            while ((line = reader.readLine()) != null) {
                labelList.add(line);
            }
            reader.close();
        } catch (IOException ex) {
            ex.getStackTrace();
        }
        return labelList;
    }

    private ArrayList<ClassificationResult> sortResult(float[][] results) {
        ArrayList<ClassificationResult> result = new ArrayList<>();
        for (int i = 0; i < mLabels.size(); i++) {
            float confidenceRate = results[0][i];
            Log.i("TAG", "===== RESULT="+confidenceRate);
            if (confidenceRate > CONFIDENCE_THRESHOLD) {
                result.add(new ClassificationResult(i, mLabels.get(i), confidenceRate));
            }
        }
        Collections.sort(result);
        return result;
    }

    class ClassificationResult implements Comparable<ClassificationResult>{
        int id;
        String label;
        float confidenceRate;

        ClassificationResult(int id, String label, float confidenceRate) {
            this.id = id;
            this.label = label;
            this.confidenceRate = confidenceRate;
        }

        @Override
        public int compareTo(ClassificationResult input) {
            int compared = Math.round(input.confidenceRate);
            return compared - Math.round(confidenceRate);
        }
    }

    public static class ResultDialogFragment extends DialogFragment {
        private ArrayList<ClassificationResult> mResult;
        @SuppressLint("ValidFragment")
        ResultDialogFragment(ArrayList<ClassificationResult> result) {
            mResult = result;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            StringBuilder resultMessage = new StringBuilder();
            if (mResult.size() > 0) {
                for (int i = 0; i < mResult.size(); i++) {
                    if (i > MAX_RESULTS) break;
                    resultMessage.append(mResult.get(i).label + " " + mResult.get(i).confidenceRate);
                    resultMessage.append(System.getProperty("line.separator"));
                }
            } else {
                resultMessage.append("Failed to get result");
            }
            builder.setMessage(resultMessage);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            return builder.create();
        }
    }
}
