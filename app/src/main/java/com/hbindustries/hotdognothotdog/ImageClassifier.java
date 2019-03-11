package com.hbindustries.hotdognothotdog;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class ImageClassifier {

    private Interpreter tflite;
    private AssetFileDescriptor modelFd;

    // Specify the input size
    final private int NOMINAL_IMG_SIZE_X = 100;
    final private int NOMINAL_IMG_SIZE_Y = 100;
    final private int DIM_PIXEL_SIZE = 3;

    // Number of bytes to hold a float (32 bits / float) / (8 bits / byte) = 4 bytes / float
    private static final int BYTE_SIZE_OF_FLOAT = 4;


    public ImageClassifier(AssetFileDescriptor modelFd) {
        try {
            this.modelFd = modelFd;
            tflite = new Interpreter(loadModelFile());
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        // Load OpenCV
        if(OpenCVLoader.initDebug()) {
            // Toast.makeText(getApplicationContext(), "OpenCV module loaded successfully.", Toast.LENGTH_SHORT).show();
        }
        else {
            // Toast.makeText(getApplicationContext(), "Could not load OpenCV module.", Toast.LENGTH_SHORT).show();
        }

    }

    float doInference(Bitmap image) {

        // Scale image to nominal dimensions and convert to array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap scaled_image = Bitmap.createScaledBitmap(image,NOMINAL_IMG_SIZE_X, NOMINAL_IMG_SIZE_Y, false );
        scaled_image.compress(Bitmap.CompressFormat.PNG, 100, stream);

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect( BYTE_SIZE_OF_FLOAT * NOMINAL_IMG_SIZE_X * NOMINAL_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        inputBuffer.order(ByteOrder.nativeOrder());

        // Load bitmap data into a pixels array
        int[] pixels = new int[scaled_image.getWidth() * scaled_image.getHeight()];
        float[][][][] floatValues = new float[1][NOMINAL_IMG_SIZE_X][NOMINAL_IMG_SIZE_Y][3];
        scaled_image.getPixels(pixels, 0, scaled_image.getWidth(), 0, 0, scaled_image.getWidth(), scaled_image.getHeight());
        for (int x = 0; x < NOMINAL_IMG_SIZE_X; ++x) {
            for (int y = 0; y < NOMINAL_IMG_SIZE_Y; ++y) {
                int pixel = pixels[x * NOMINAL_IMG_SIZE_X + y ];
                floatValues[0][x][y][0] = ((pixel >> 16) & 0xFF) / 1.0f;
                floatValues[0][x][y][1] = ((pixel >> 8) & 0xFF) / 1.0f;
                floatValues[0][x][y][2] = (pixel & 0xFF) / 1.0f;
            }
        }

        float[][] resultCategory = new float[1][1];
        tflite.run(floatValues, resultCategory);
        return resultCategory[0][0];
    }

    /* Memory map the model file in Assets */
    private MappedByteBuffer loadModelFile() throws IOException {
        // Open the model file using an input stream, and memory map it to load
        FileInputStream inputStream = new FileInputStream(modelFd.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = modelFd.getStartOffset();
        long declaredLength = modelFd.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
