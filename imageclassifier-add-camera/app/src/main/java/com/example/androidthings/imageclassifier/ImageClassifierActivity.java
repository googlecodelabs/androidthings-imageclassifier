/*
 * Copyright 2017 The Android Things Samples Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;

public class ImageClassifierActivity extends Activity {
    private static final String TAG = "ImageClassifierActivity";

    private ButtonInputDriver mButtonDriver;
    private boolean mProcessing;
    private String[] labels;
    private TensorFlowInferenceInterface inferenceInterface;
    private CameraHandler mCameraHandler;
    private ImagePreprocessor mImagePreprocessor;

    /**
     * Initialize the classifier that will be used to process images.
     */
    private void initClassifier() {
        this.inferenceInterface = new TensorFlowInferenceInterface(
                getAssets(), Helper.MODEL_FILE);
        this.labels = Helper.readLabels(this);
    }

    /**
     * Clean up the resources used by the classifier.
     */
    private void destroyClassifier() {
        inferenceInterface.close();
    }

    /**
     * Process an image and identify what is in it. When done, the method
     * {@link #onPhotoRecognitionReady(String[])} must be called with the results of
     * the image recognition process.
     *
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    private void doRecognize(Bitmap image) {
        float[] pixels = Helper.getPixels(image);

        // Feed the pixels of the image into the TensorFlow Neural Network
        inferenceInterface.feed(Helper.INPUT_NAME, pixels,
                Helper.NETWORK_STRUCTURE);

        // Run the TensorFlow Neural Network with the provided input
        inferenceInterface.run(Helper.OUTPUT_NAMES);

        // Extract the output from the neural network back into an array of confidence per category
        float[] outputs = new float[Helper.NUM_CLASSES];
        inferenceInterface.fetch(Helper.OUTPUT_NAME, outputs);

        // Get the results with the highest confidence and map them to their labels
        onPhotoRecognitionReady(Helper.getBestResults(outputs, labels));
    }

    /**
     * Initialize the camera that will be used to capture images.
     */
    private void initCamera() {
        mImagePreprocessor = new ImagePreprocessor();
        mCameraHandler = CameraHandler.getInstance();
        Handler threadLooper = new Handler(getMainLooper());
        mCameraHandler.initializeCamera(this, threadLooper,
                new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Bitmap bitmap = mImagePreprocessor.preprocessImage(imageReader.acquireNextImage());
                onPhotoReady(bitmap);
            }
        });
    }

    /**
     * Clean up resources used by the camera.
     */
    private void closeCamera() {
        mCameraHandler.shutDown();
    }

    /**
     * Load the image that will be used in the classification process.
     * When done, the method {@link #onPhotoReady(Bitmap)} must be called with the image.
     */
    private void loadPhoto() {
        mCameraHandler.takePicture();
    }



    // --------------------------------------------------------------------------------------
    // NOTE: The normal codelab flow won't require you to change anything below this line,
    // although you are encouraged to read and understand it.

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initCamera();
        initClassifier();
        initButton();
        Log.d(TAG, "READY");
    }

    /**
     * Register a GPIO button that, when clicked, will generate the {@link KeyEvent#KEYCODE_ENTER}
     * key, to be handled by {@link #onKeyUp(int, KeyEvent)} just like any regular keyboard
     * event.
     *
     * If there's no button connected to the board, the doRecognize can still be triggered by
     * sending key events using a USB keyboard or `adb shell input keyevent 66`.
     */
    private void initButton() {
        try {
            String pin = Build.DEVICE.equals("rpi3") ? RainbowHat.BUTTON_C : "GPIO_39";
            mButtonDriver = RainbowHat.createButtonInputDriver(pin, KeyEvent.KEYCODE_ENTER);
            mButtonDriver.register();
        } catch (IOException e) {
            Log.w(TAG, "Cannot find button. Ignoring push button. Use a keyboard instead.", e);
        }
    }

    private Bitmap getStaticBitmap() {
        Log.d(TAG, "Using sample photo in res/drawable/sampledog_224x224.png");
        return BitmapFactory.decodeResource(this.getResources(), R.drawable.sampledog_224x224);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mProcessing) {
                Log.e(TAG, "Still processing, please wait");
                return true;
            }
            Log.d(TAG, "Running photo recognition");
            mProcessing = true;
            loadPhoto();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void onPhotoReady(Bitmap bitmap) {
        doRecognize(bitmap);
    }

    private void onPhotoRecognitionReady(String[] results) {
        Log.d(TAG, "RESULTS: " + Helper.formatResults(results));
        mProcessing = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            destroyClassifier();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            closeCamera();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mButtonDriver != null) mButtonDriver.close();
        } catch (Throwable t) {
            // close quietly
        }
    }
}
