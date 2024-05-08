package com.by.sasa.bistrovic.ethereumandnfc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import butterknife.BindView;
import butterknife.ButterKnife;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainId;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import static org.web3j.tx.Contract.GAS_LIMIT;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static android.app.PendingIntent.getActivity;

import static com.by.sasa.bistrovic.AppConstants.DEFAULT_GASLIMIT;
import static com.by.sasa.bistrovic.AppConstants.DEFAULT_GASPRICE_IN_GIGAWEI;
import static com.by.sasa.bistrovic.AppConstants.PREFERENCE_FILENAME;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_GASLIMIT_SEND_ETH;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_GASPRICE_WEI;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_MAIN_NETWORK;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_PIN;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_RECIPIENT_ADDRESS;
import static com.by.sasa.bistrovic.AppConstants.TEN_SECONDS;
import static com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils.showToast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.by.sasa.bistrovic.ethereumandnfc.adapter.UnitSpinnerAdapter;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.bean.EthBalanceBean;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.exeptions.InvalidEthereumAddressException;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.utils.EthereumUtils;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.utils.UriUtils;
import com.by.sasa.bistrovic.ethereumandnfc.qrcode.QrCodeScanner;
import com.by.sasa.bistrovic.ethereumandnfc.utils.InputErrorUtils;
import com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils;
import com.kenai.jffi.Main;

/**
 * Activity class used for Ethereum functionality.
 */
public class SendTransactionActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @BindView(R.id.recipientAddress)
    TextView recipientAddressTxt;
    @BindView(R.id.amount)
    TextView amountTxt;
    @BindView(R.id.gasPrice)
    TextView gasPriceTxt;
    @BindView(R.id.gasLimit)
    TextView gasLimitTxt;
    @BindView(R.id.priceInEuro)
    TextView priceInEuroTxt;
    @BindView(R.id.pin)
    TextView pinTxt;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.toggleButton)
    ToggleButton toggleButton;

    public static String QREthereumBarcode;

    private InputErrorUtils inputErrorUtils;

    private String pubKeyString;
    private String recipientPrivateKeyString;

    private String ethAddress;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private volatile boolean activityPaused = false;

    private UnitSpinnerAdapter spinnerAdapter = new UnitSpinnerAdapter();

    private static final String ETHEREUM_ADDRESS_PATTERN =
            "^(0x)?[0-9a-fA-F]{40}$";

    public static boolean isValidEthereumAddress(String address) {
        // Check if the given string matches the Ethereum address pattern
        return Pattern.matches(ETHEREUM_ADDRESS_PATTERN, address);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_transaction);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        inputErrorUtils = new InputErrorUtils(this, recipientAddressTxt, amountTxt, gasPriceTxt, gasLimitTxt);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        pendingIntent = getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);

        SharedPreferences mPrefs = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        String savedRecipientAddressTxt = mPrefs.getString(PREF_KEY_RECIPIENT_ADDRESS, "");
        if (savedRecipientAddressTxt.equals("") || savedRecipientAddressTxt==null) {
            recipientAddressTxt.setText("0x21a13018F78267469692205160B28e0A6814bE6b");
        } else {
            recipientAddressTxt.setText(savedRecipientAddressTxt);
        }
        String savedGasPriceWei = mPrefs.getString(PREF_KEY_GASPRICE_WEI, DEFAULT_GASPRICE_IN_GIGAWEI);
        gasPriceTxt.setText(savedGasPriceWei);
        String savedGasLimit = mPrefs.getString(PREF_KEY_GASLIMIT_SEND_ETH, DEFAULT_GASLIMIT);
        gasLimitTxt.setText(savedGasLimit);
        String savedPin = mPrefs.getString(PREF_KEY_PIN, "");
        pinTxt.setText(savedPin);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            pubKeyString = bundle.getString("pubKey");
            ethAddress = bundle.getString("ethAddress");
        }

        new Thread(() -> {
            try {
                while (!activityPaused) {
                    updateReadingEuroPrice();
                    TimeUnit.SECONDS.sleep(TEN_SECONDS);
                }
            } catch (Exception e) {
            }
        }).start();
    }

    /**
     * This method reads the euro price and updates UI accordingly.
     *
     * @throws Exception
     */
    public void updateReadingEuroPrice() throws Exception {
    }

    @Override
    public void onPause() {
        super.onPause();
        activityPaused = true;
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }

        SharedPreferences mPrefs = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putString(PREF_KEY_RECIPIENT_ADDRESS, recipientAddressTxt.getText().toString())
                .putString(PREF_KEY_GASPRICE_WEI, gasPriceTxt.getText().toString())
                .putString(PREF_KEY_GASLIMIT_SEND_ETH, gasLimitTxt.getText().toString())
                .putString(PREF_KEY_PIN, pinTxt.getText().toString())
                .apply();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (MainActivity.scanQRCodeLevel!=null) {
            if (MainActivity.scanQRCodeLevel == 1) {
                if (MainActivity.scanQRCodeEthereumAddress != null) {
                    if (isValidEthereumAddress(MainActivity.scanQRCodeEthereumAddress)) {
                        recipientAddressTxt.setText(MainActivity.scanQRCodeEthereumAddress);
                        playSystemSound();
                        MainActivity.scanQRCodeEthereumAddress = "";
                    }
                }
            }
        }

        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        activityPaused = false;
    }

    private void playSystemSound() {
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        toneGen.startTone(ToneGenerator.TONE_SUP_RINGTONE, 250);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        UiUtils.logTagInfo(tag);
        MifareUltralight isoDep = MifareUltralight.get(tag);
        if (isoDep == null) {
            showToast(getString(R.string.wrong_card), this);
            return;
        }

        if (toggleButton.isChecked()) {
            try {

                isoDep.connect();
                String readRecipientAddress="";

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
                    Credentials myPrivate = Credentials.create(myPrivatekey2);
                    readRecipientAddress = myPrivate.getEcKeyPair().getPublicKey().toString(16);
                    recipientPrivateKeyString = myPrivate.getEcKeyPair().getPrivateKey().toString(16);
                }
                if (myPrivateboolean==false) {
                }

                final String newAddress = Keys.toChecksumAddress(Keys.getAddress(readRecipientAddress));
                showToast(String.format(getString(R.string.change_recipient_address), newAddress), this);
                recipientAddressTxt.setText(newAddress);
                toggleButton.toggle();

                isoDep.close();
            } catch (IOException e) {
                showToast(e.getMessage(), this);
            }
        } else {
            if (inputErrorUtils.isNoInputError()) {
                showToast(getString(R.string.hold_card_for_while), this);

                new Thread(() -> sendTransactionAndShowFeedback(isoDep)).start();
                finish();
            }
        }
    }

    private Credentials getCredentialsFromPrivateKey() {
        return Credentials.create(MainActivity.privateKeyString);
    }

    private void sendTransactionAndShowFeedback(MifareUltralight isoDep) {
        final String valueStr = amountTxt.getText().toString();
        BigDecimal value = new BigDecimal(valueStr);
        BigInteger gasPrice = BigInteger.valueOf(20000000000L);
        BigInteger gasLimit = BigInteger.valueOf(6721975L);

        SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        byte chainId = ChainId.MAINNET;
        if (!pref.getBoolean(PREF_KEY_MAIN_NETWORK, true)) {
            chainId = (byte) 11155111;
        }

        try {
            Web3j web3 = Web3j.build(new HttpService(UiUtils.getFullNodeUrl(this)));

            RemoteCall<TransactionReceipt> rc = Transfer.sendFundsEIP1559(
                    web3, getCredentialsFromPrivateKey(),
                    recipientAddressTxt.getText().toString(),
                    value,
                    Convert.Unit.ETHER,
                    GAS_LIMIT.divide(new BigInteger("100")),
                    Convert.toWei(gasPriceTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger(),
                    Convert.toWei(gasLimitTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger()
            );

            TransactionReceipt receipt = rc.send();

            showToast(getString(R.string.send_success), this);

        } catch (IOException e) {
            showToast(e.getMessage(), this);
        } catch (Exception e) {
            showToast(e.getMessage(), this);
        } finally {
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Do something when camera permission is granted
                Intent intent = new Intent(SendTransactionActivity.this, ScanQRCode.class);
                startActivity(intent);
            } else {
                // Handle case when camera permission is denied
                //Toast.makeText(MainActivity.this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void scanQrCode(View view) {
        MainActivity.scanQRCodeLevel=1;
        if (ContextCompat.checkSelfPermission(SendTransactionActivity.this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SendTransactionActivity.this,
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Camera permission already granted, perform desired action
            // For example, start camera preview
            Intent intent = new Intent(SendTransactionActivity.this, ScanQRCode.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UiUtils.handleOptionItemSelected(this, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public void onSendAll(View view) {
        new Thread(() -> {
            try {
                EthBalanceBean balance = EthereumUtils.getBalance(ethAddress, UiUtils.getFullNodeUrl(this));
                final BigDecimal ethBalanceInWei = Convert.toWei(balance.getEther(), Convert.Unit.ETHER);
                final BigDecimal gasLimit = new BigDecimal(
                        gasLimitTxt.getText().toString().equals("") ? "0" : gasLimitTxt.getText().toString());

            } catch (Exception e) {
            }
        }).start();
    }
}