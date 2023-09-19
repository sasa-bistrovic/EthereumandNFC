package com.by.sasa.bistrovic.ethereumandnfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;

import java.io.IOException;

import static android.app.PendingIntent.getActivity;
import static com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils.showToast;

import androidx.appcompat.app.AppCompatActivity;

import com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils;

import org.web3j.crypto.Credentials;

/**
 * Activity class used for generating from seed functionality.
 */
public class ImportPrivateKeyToNFCCardActivity extends AppCompatActivity {

    @BindView(R.id.seed)
    TextView seed;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private String pubKeyString;
    private String privateKeyString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_private_key_to_nfccard);
        ButterKnife.bind(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);
    }

    /**
     * Called by Android systems whenever a new Intent is received. NFC tags are also
     * delivered via an Intent.
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        resolveIntent(intent);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void resolveIntent(Intent intent) {
        // Only handle NFC intents
        if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) == null) {
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        UiUtils.logTagInfo(tag);
        MifareUltralight isoDep = MifareUltralight.get(tag);
        if (isoDep == null) {
            showToast(getString(R.string.wrong_card), this);
            return;
        }

        try {
            isoDep.connect();

            if (seed.getText().toString().length()==64) {

                Credentials credentials = Credentials.create(seed.getText().toString());

                pubKeyString = credentials.getEcKeyPair().getPublicKey().toString(16);

                privateKeyString = credentials.getEcKeyPair().getPrivateKey().toString(16);

                String myPrivatekey = pubKeyString;

                Integer myPrivateindex = ((int) pubKeyString.length() / 8) + 1;

                for (int i = 0; i <= myPrivateindex * 8; i++) {
                    if (myPrivatekey.length() < i) {
                        myPrivatekey = myPrivatekey + "0";
                    }
                }

                //showToast(" " + myPrivateindex + " " + myPrivatekey.length(), this);

                for (int i = 1; i <= myPrivatekey.length(); i++) {
                    if ((i % 8) == 0) {
                        if (i == myPrivatekey.length()) {
                            isoDep.writePage(((int) i / 8) + 10, hexStringToByteArray(myPrivatekey.substring(i - 8, myPrivatekey.length())));
                            break;
                        } else {
                            isoDep.writePage(((int) i / 8) + 10, hexStringToByteArray(myPrivatekey.substring(i - 8, i)));
                        }
                    }
                }

                myPrivatekey = privateKeyString;

                myPrivateindex = ((int) privateKeyString.length() / 8) + 1;

                for (int i = 0; i <= myPrivateindex * 8; i++) {
                    if (myPrivatekey.length() < i) {
                        myPrivatekey = myPrivatekey + "0";
                    }
                }

                //showToast(" " + myPrivateindex + " " + myPrivatekey.length(), this);

                for (int i = 1; i <= myPrivatekey.length(); i++) {
                    if ((i % 8) == 0) {
                        if (i == myPrivatekey.length()) {
                            isoDep.writePage(((int) i / 8) + 28, hexStringToByteArray(myPrivatekey.substring(i - 8, myPrivatekey.length())));
                            break;
                        } else {
                            isoDep.writePage(((int) i / 8) + 28, hexStringToByteArray(myPrivatekey.substring(i - 8, i)));
                        }
                    }
                }
            }
            isoDep.close();
        } catch (IOException e) {
            showToast(e.getMessage(), this);
            //Log.e(TAG, "Exception while generating key from seed", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }
}
