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

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Pair;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.List;

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class TensorFlowImageClassifier {

    private String[] labels;

    private TensorFlowInferenceInterface inferenceInterface;

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param context The context from which to get the asset manager to be used to load assets.
     */
    public TensorFlowImageClassifier(Context context) {
        AssetManager assetManager = context.getAssets();
        this.inferenceInterface = new TensorFlowInferenceInterface(assetManager,
                "file:///android_asset/tensorflow_inception_graph.pb");
        this.labels = TensorFlowImageHelper.readLabels(
                assetManager, "imagenet_comp_graph_label_strings.txt");
    }

    public List<Pair<String, Float>> recognizeImage(final Bitmap bitmap) {

        float[] pixels = TensorFlowImageHelper.getPixels(bitmap);

        // Copy the input data into TensorFlow.
        inferenceInterface.feed(
                "input:0", pixels,
                1, TensorFlowImageHelper.IMAGE_SIZE, TensorFlowImageHelper.IMAGE_SIZE, 3);

        // Run the inference call.
        inferenceInterface.run(new String[]{"output:0"});

        // Copy the output Tensor back into the output array.
        float[] outputs = new float[labels.length + 7];
        inferenceInterface.fetch("output:0", outputs);

        return TensorFlowImageHelper.getBestResults(outputs, labels, 0.1f, 3);
    }

    public void close() {
        inferenceInterface.close();
    }
}
