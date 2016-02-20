package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

/**
 * Code for this activity was based in the usage example provided in the open source library
 * https://github.com/dm77/barcodescanner (that uses ZBar open source library)
 */
public class BarcodeScannerActivity extends Activity implements ZBarScannerView.ResultHandler {

    private final String LOG_TAG = BarcodeScannerActivity.class.getSimpleName();
    private ZBarScannerView mScannerView;
    static String EXTRA_ISBN_13 = "ISBN-13";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZBarScannerView(this);
        // Set the default format for the scanner to be EAN-13
        mScannerView.setFormats(new ArrayList<>(Arrays.asList(BarcodeFormat.EAN13)));
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register this class as a handler for the scanner results
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result scanResult) {

        Log.v(LOG_TAG, scanResult.getContents());

        // Send barcode data back to the AddBook fragment
        Intent resultData = new Intent();
        resultData.putExtra(EXTRA_ISBN_13, scanResult.getContents());
        setResult(AddBook.SCAN_BOOK, resultData);
        // Close this activity
        finish();
    }
}
