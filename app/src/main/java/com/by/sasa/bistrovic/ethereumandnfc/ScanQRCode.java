package com.by.sasa.bistrovic.ethereumandnfc;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class ScanQRCode extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;

    private CaptureManager captureManager;

    private static final String ETHEREUM_ADDRESS_PATTERN =
            "^(0x)?[0-9a-fA-F]{40}$";

    public static boolean isValidEthereumAddress(String address) {
        return Pattern.matches(ETHEREUM_ADDRESS_PATTERN, address);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);

        barcodeView = findViewById(R.id.barcode_scanner);

        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);

        // Customize camera settings if needed
        CameraSettings cameraSettings = new CameraSettings();
        barcodeView.getBarcodeView().setCameraSettings(cameraSettings);

        // Set decoder(s) if needed
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory());

        // Optional: Remove the hint
        barcodeView.setStatusText(""); // Set an empty string to remove the hint

        // Start the capture manager
        captureManager.decode();

        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                // Handle the result of the QR code scan
                String qrCodeValue = result.getText();

                    if (isValidEthereumAddress(qrCodeValue)) {
                        MainActivity.scanQRCodeEthereumAddress = qrCodeValue;
                        finish();
                    } else {
                    }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Optional: Handle possible result points if needed
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureManager.onSaveInstanceState(outState);
    }
}
