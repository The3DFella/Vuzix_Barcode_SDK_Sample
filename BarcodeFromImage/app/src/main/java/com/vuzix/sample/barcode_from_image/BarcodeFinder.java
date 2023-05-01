/*
 Copyright (c) 2018, Vuzix Corporation
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.

 Neither the name of Vuzix Corporation nor the names of
 its contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.vuzix.sample.barcode_from_image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.util.Log;

import com.vuzix.sdk.barcode.BarcodeType2;
import com.vuzix.sdk.barcode.ScanResult2;
import com.vuzix.sdk.barcode.Scanner2;
import com.vuzix.sdk.barcode.Scanner2Factory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class to find barcodes in images.
 *
 * This shows a real-world conversion of image data and how to call the barcode engine
 *
 * This is called from a worker thread, not the UI thread since it might take a noticeable amount
 * of time to analyze the image
 */

class BarcodeFinder {
    private Scanner2 mScanner=null;
    private final BarcodeType2[] barcodeTypes = {
            BarcodeType2.QR_CODE,
            BarcodeType2.CODE_128
    };

    /**
     * Initialize the scan engine
     *
     * note: Failure to do this will leave the engine in a demonstration mode, and scan data will not be usable.
     */
    public BarcodeFinder(Context iContext) {

        //Call into the SDK to create a scanner instance.
        try {
            mScanner = Scanner2Factory.getScanner(iContext);
            mScanner.setFormats(barcodeTypes);
        }catch (Exception ex){
        }
    }

    boolean saveBusy;
    synchronized private void saveBitmap(final Image image, final File file) {
        if(saveBusy) {
            Log.d(MainActivity.LOG_TAG, "Not saving bitmap... prior request in-progress");
            return;
        }
        saveBusy = true;

        ByteBuffer buffer = image.getPlanes()[0].getBuffer(); // Y component is all we need
        buffer.rewind();
        final byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        buffer.rewind();

        AsyncTask.execute( () -> {
            Log.d(MainActivity.LOG_TAG, "Converting to bitmap...");
            final int width = image.getWidth();
            final int height = image.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, image.getHeight(), Bitmap.Config.ARGB_8888);
            int inputIdx = 0;
            for(int row=0; row<height; row ++){
                for(int col=0; col<width; col ++) {
                    int eachVal = (data[inputIdx] & 0xFF);
                    inputIdx++;
                    int eachColor = 0xFF000000 | (eachVal<<16) | (eachVal<<8) | eachVal;
                    bitmap.setPixel(col, row, eachColor);
                }
            }
            Log.d(MainActivity.LOG_TAG, "Writing bitmap...");
            try {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(MainActivity.LOG_TAG, "Converted to bitmap " + file);
            saveBusy = false;
        });
    }

    /**
     * Parses the image data to the barcode engine and displays the results
     */
    public String getBarcodeResults(ImageReader reader) {
        String resultString = null;

        // get the latest image and convert to a bitmap
        Image image = reader.acquireNextImage(); // Use acquireNextImage() instead of acquireLatestImage() since we created the reader with a maxImages of 1
        saveBitmap(image, new File("/sdcard/DCIM/latest.png"));

        ByteBuffer buffer = image.getPlanes()[0].getBuffer(); // Y component is all we need
        byte[] data = new byte[buffer.remaining()*3];
        buffer.get(data, 0, buffer.remaining());

        int width = image.getWidth();
        int height = image.getHeight();
        Log.d(MainActivity.LOG_TAG, "Processing image: " + width + "x" + height);

        // The format of the rect is upper left x, upper left  y, width, height
        Rect[] scanRects = {
                new Rect(0, 0, width, height), // Full image
                //new Rect(width/2, 2*height/3, width/2, height/3), // upper left - 1/2 width 1/3 height. (Note: image is upside-down when M400 is on the right eye)
                //new Rect(width/4, height/3, width/2, height/3), // center - 1/2 width, 1/3 height.
        };

        // pass data into barcode scan engine
        for (Rect eachRect : scanRects) {
            Log.d(MainActivity.LOG_TAG, "Scanning rectangle image: " + eachRect);
            ScanResult2[] results = mScanner.scan(data, width, height, eachRect);
            // Examine the results
            if (results != null && results.length > 0) {
                resultString = results[0].getText();   // Use the first one, if any are available
                break;
            }
        }
        return resultString;
    }

}
