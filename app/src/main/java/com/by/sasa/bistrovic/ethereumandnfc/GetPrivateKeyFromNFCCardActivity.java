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

import androidx.appcompat.app.AppCompatActivity;

import org.web3j.crypto.ECKeyPair;

/**
 * Activity class used for setting PIN functionality.
 */
public class GetPrivateKeyFromNFCCardActivity extends AppCompatActivity {

    @BindView(R.id.pin)
    TextView pin;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private String pubKeyString;
    private String privateKeyString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_private_key_from_nfccard);
        ButterKnife.bind(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
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
        if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) == null) {
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareUltralight isoDep = MifareUltralight.get(tag);
        if (isoDep == null) {
            return;
        }

        try {
            isoDep.connect();

            try {
                String password = "secr3t";

                String myPrivatekey="";

                String myPrivatekey2="";

                boolean myPrivateboolean=false;

                for (int i=11; i<=26;i++) {
                    String myPrivatepage=bytesToHex(isoDep.readPages(i)).substring(0,8);
                    for (int j=1; j<=myPrivatepage.length();j++) {
                        if (!myPrivateboolean==true) {
                            if (!myPrivatepage.substring(j - 1, j).equals("0")) {
                                myPrivateboolean = true;
                            }
                        }
                    }
                    myPrivatekey=myPrivatekey+myPrivatepage;
                }

                for (int i=29; i<=36;i++) {
                    String myPrivatepage=bytesToHex(isoDep.readPages(i)).substring(0,8);
                    for (int j=1; j<=myPrivatepage.length();j++) {
                        if (!myPrivateboolean==true) {
                            if (!myPrivatepage.substring(j - 1, j).equals("0")) {
                                myPrivateboolean = true;
                            }
                        }
                    }
                    myPrivatekey2=myPrivatekey2+myPrivatepage;
                }

                ECKeyPair keyPair;

                if (myPrivateboolean==false) {
                } else
                {
                    pubKeyString = myPrivatekey;
                    privateKeyString = myPrivatekey2;
                }

                pin.setText(privateKeyString);

                if (myPrivateboolean==false) {
                }

            } catch(Exception e) {
            }

            isoDep.close();
        } catch (IOException | IllegalArgumentException e) {
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