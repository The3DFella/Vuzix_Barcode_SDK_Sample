/*
Copyright (c) 2017, Vuzix Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

*  Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

*  Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

*  Neither the name of Vuzix Corporation nor the names of
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

package com.vuzix.sample.barcodefromintent;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vuzix.sdk.barcode.BarcodeType2;
import com.vuzix.sdk.barcode.ScanResult2;
import com.vuzix.sdk.barcode.ScannerIntent;


/**
 * This sample demonstrates how to use an intent to access the built-in barcode scanner to scan a
 * barcode and display the encoded text to the user
 *
 * Using intents is by far the simplest way to add barcode scanning to your application. Since the
 * barcode application is a separate activity it handles everything including permissions.
 * This leaves very little to be done by this application.
 */
public class MainActivity extends Activity {
    private static final int REQUEST_CODE_SCAN = 90001;  // Must be unique within this Activity
    private final static String TAG = "barcodeSample";
    private boolean cameraToggle = false;

    private TextView mTextEntryField;

    // Limiting the barcode formats to those you expect to encounter improves the speed of scanning
    // and increases the likelihood of properly detecting a barcode.
    private final String[] requestedBarcodeTypes = {
            BarcodeType2.QR_CODE.name(),
            BarcodeType2.UPC_A.name(),
            BarcodeType2.CODE_128.name()
    };

    /**
     * Sets up the User Interface
     *
     * @param savedInstanceState - unused and passed unchanged to the superclass
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextEntryField = (TextView) findViewById(R.id.scannedTextResult);

        Button buttonScan = (Button) findViewById(R.id.btn_scan_barcode);
        buttonScan.requestFocusFromTouch();
        buttonScan.setOnClickListener(view -> OnScanClick());
    }

    /**
     * Handler for the button press. Activates the scan.
     */
    private void OnScanClick() {
        Intent scannerIntent = new Intent(ScannerIntent.ACTION);
        scannerIntent.putExtra(ScannerIntent.EXTRA_BARCODE2_TYPES, requestedBarcodeTypes);
        if (cameraToggle){
            scannerIntent.putExtra(ScannerIntent.EXTRA_CAMERA_ID, 0);
        }
        else {
            scannerIntent.putExtra(ScannerIntent.EXTRA_CAMERA_ID, 1);
        }
        cameraToggle = !cameraToggle;
        try {
            // The Vuzix smart glasses have a built-in Barcode Scanner app that is registered for this intent.
            startActivityForResult(scannerIntent, REQUEST_CODE_SCAN);
        } catch (ActivityNotFoundException activityNotFound) {
            Toast.makeText(this, R.string.only_on_mseries, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * The  M-Series Barcode Scanner App will scan a barcode and return
     *
     * @param requestCode int identifier you provided in startActivityForResult
     * @param resultCode int result of the scan operation
     * @param data Intent containing a ScanResult whenever the resultCode indicates RESULT_OK
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                ScanResult2 scanResult = data.getParcelableExtra(ScannerIntent.RESULT_EXTRA_SCAN_RESULT2);
                if (scanResult != null) {
                    Log.d(TAG, "Got result: " + scanResult.getText());
                    mTextEntryField.setText(scanResult.getText());
                } else {
                    Log.d(TAG, "No data");
                    mTextEntryField.setText(R.string.no_data);
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
