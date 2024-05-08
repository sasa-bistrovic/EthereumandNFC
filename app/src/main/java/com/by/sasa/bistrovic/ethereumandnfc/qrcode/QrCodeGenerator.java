package com.by.sasa.bistrovic.ethereumandnfc.qrcode;

import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Class used for generating QR codes.
 */
public class QrCodeGenerator {


    /**
     * Generate QR Code from given String.
     *
     * @param text text to encode
     * @return 200x200 px qrcode as bitmap or null if QR code cannot be generated.
     */
    public static Bitmap generateQrCode(String text) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
        }
        return null;
    }
}
