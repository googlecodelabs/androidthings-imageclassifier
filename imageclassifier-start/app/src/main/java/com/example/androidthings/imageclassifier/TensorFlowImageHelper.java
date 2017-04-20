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

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Helper functions for the TensorFlow image classifier.
 */
public class TensorFlowImageHelper {

    /* package */ static final int IMAGE_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;

    public static String[] readLabels(AssetManager assetManager, String filename) {
        ArrayList<String> result = new ArrayList<>();
        try (InputStream is = assetManager.open(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
            return result.toArray(new String[result.size()]);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read labels from " + filename);
        }
    }

    public static float[] getPixels(Bitmap bitmap) {
        if (bitmap.getWidth() != IMAGE_SIZE || bitmap.getHeight() != IMAGE_SIZE) {
            // rescale the bitmap if needed
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_SIZE, IMAGE_SIZE);
        }
        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        float[] floatValues = new float[IMAGE_SIZE * IMAGE_SIZE * 3];
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }
        return floatValues;
    }

    public static List<Pair<String, Float>> getBestResults(
            float[] confidenceLevels, String[] labels, float threshold, int maxResults) {
        // Find the best classifications.
        PriorityQueue<Pair<String, Float>> pq = new PriorityQueue<>(3,
                new Comparator<Pair<String, Float>>() {
                    @Override
                    public int compare(Pair<String, Float> lhs, Pair<String, Float> rhs) {
                        // Intentionally reversed to put high confidence at the head of the queue.
                        return Float.compare(rhs.second, lhs.second);
                    }
                });
        for (int i = 0; i < confidenceLevels.length; ++i) {
            if (confidenceLevels[i] > threshold) {
                pq.add(Pair.create(labels[i], confidenceLevels[i]));
            }
        }

        ArrayList<Pair<String, Float>> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), maxResults);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }
}
