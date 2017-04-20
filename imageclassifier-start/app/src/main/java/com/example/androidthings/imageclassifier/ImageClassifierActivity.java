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
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageClassifierActivity extends Activity {
    private static final String TAG = "ImageClassifierActivity";

    private static final String BUTTON_PIN = "BCM21";

    private TensorFlowImageClassifier mTensorFlowClassifier;

    private ButtonInputDriver mButtonDriver;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTensorFlowClassifier = new TensorFlowImageClassifier(this);
        initButton();
    }

    private void initButton() {
        try {
            mButtonDriver = new ButtonInputDriver(BUTTON_PIN, Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonDriver.register();
        } catch (IOException e) {
            Log.w(TAG, "Cannot find pin " + BUTTON_PIN + ". Ignoring push button on PIO. " +
                    "Use a keyboard instead", e);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            Log.d(TAG, "Button pressed, initiating photo recognition");
            long start = System.currentTimeMillis();
            doRecognize();
            Log.i(TAG, "Image processing and recognition took " +
                    (System.currentTimeMillis() - start) + "ms");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void doRecognize() {
        Bitmap bitmap = getBitmap();
        final List<Pair<String, Float>> results = mTensorFlowClassifier.recognizeImage(bitmap);
        Log.d(TAG, "Got the following results from Tensorflow: " + results);
    }

    private Bitmap getBitmap() {
        File imageFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "photo.jpg");
        Bitmap bitmap;
        if (imageFile.exists()) {
            Log.d(TAG, "Using image " + imageFile.getPath());
            bitmap = BitmapFactory.decodeFile(imageFile.getPath());
        } else {
            Log.d(TAG, "Using sample photo, couldn't find image " + imageFile.getPath());
            bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.sampledog_224x224);
        }
        return bitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mTensorFlowClassifier != null) mTensorFlowClassifier.close();
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
