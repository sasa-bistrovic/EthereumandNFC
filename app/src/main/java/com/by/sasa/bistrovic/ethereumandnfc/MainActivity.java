package com.by.sasa.bistrovic.ethereumandnfc;

import static com.by.sasa.bistrovic.AppConstants.FIVE_SECONDS;
import static com.by.sasa.bistrovic.AppConstants.PREFERENCE_FILENAME;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_MAIN_NETWORK;
import static com.by.sasa.bistrovic.AppConstants.TEN_SECONDS;
import static com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils.showToast;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;

import com.by.sasa.bistrovic.ethereumandnfc.ethereum.bean.EthBalanceBean;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.utils.EthereumUtils;
import com.by.sasa.bistrovic.ethereumandnfc.qrcode.QrCodeGenerator;
import com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils;
import com.kenai.jffi.Main;

/**
 * Main activity. Entry point of the application.
 *
 * @author Coinfinity.co, 2018
 */
public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private EthBalanceBean ethBalance;

    @BindView(R.id.ethAddress)
    TextView ethAddressView;
    @BindView(R.id.balance)
    TextView balance;
    @BindView(R.id.qrCode)
    ImageView qrCodeView;
    @BindView(R.id.holdCard)
    TextView holdCard;
    @BindView(R.id.send)
    Button sendEthBtn;
    @BindView(R.id.sendErc20)
    Button sendErc20Btn;
    @BindView(R.id.voting)
    Button votingBtn;
    @BindView(R.id.brandProtection)
    Button brandProtectionBtn;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.image_nfc_icon)
    ImageView nfcIcon;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    public static String pubKeyString;
    public static String privateKeyString;
    public static String ethAddress;

    public static Integer scanQRCodeLevel;

    public static String scanQRCodeEthereumAddress;
    private volatile boolean activityPaused = false;

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
            return;
        }

        displayOnUI(GuiState.PROGRESS_BAR);

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

                if (myPrivateboolean==false) {
                }

            } catch(Exception e) {
                showToast("Error: " + e.getMessage(), this);
                return;
            }

            isoDep.close();
        } catch (IOException e) {
            showToast(e.getMessage(), this);
            resetGuiState();
            return;
        }

        Credentials myPrivate = Credentials.create(privateKeyString);

        pubKeyString = myPrivate.getEcKeyPair().getPublicKey().toString(16);

        ethAddress = Keys.toChecksumAddress(Keys.getAddress(pubKeyString));

        ethAddressView.setText(ethAddress);
        qrCodeView.setImageBitmap(QrCodeGenerator.generateQrCode(ethAddress));
        holdCard.setText(R.string.card_found);
    }

    /**
     * this method is updating the balance and euro price on UI.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void updateBalance() throws Exception {
        ethBalance = EthereumUtils.getBalance(ethAddress, UiUtils.getFullNodeUrl(this));
    }

    /**
     * this method is updating the balance and euro price on UI.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void updateEuroPrice() throws Exception {
        if (ethBalance == null)
            return;
        if (pubKeyString != null) {
            this.runOnUiThread(() -> {
                balance.setText(ethBalance.toString());
                if (!sendEthBtn.isEnabled()) {
                    sendEthBtn.setEnabled(true);
                    sendErc20Btn.setEnabled(true);
                    votingBtn.setEnabled(true);
                    //brand protection
                    brandProtectionBtn.setEnabled(true);
                }
                displayOnUI(GuiState.BALANCE_TEXT);
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_activity_title);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // Get the overflow icon from the toolbar
        Drawable overflowIcon = toolbar.getOverflowIcon();

        // Change the color of the overflow icon to white
        int whiteColor = getResources().getColor(android.R.color.white);
        DrawableCompat.setTint(overflowIcon, whiteColor);
        toolbar.setOverflowIcon(overflowIcon);

        displayOnUI(GuiState.NFC_ICON);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            showToast(getString(R.string.no_nfc), this);
            finish();
            return;
        }

        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                openNfcSettings();
            }
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        activityPaused = false;


        new Thread(() -> {
            //Log.d(TAG, "Main activity, start reading eth balance thread...");
            try {
                while (!activityPaused && ethAddress != null) {
                    updateBalance();
                    TimeUnit.SECONDS.sleep(FIVE_SECONDS);
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showToast(e.getMessage(), this);
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        resetGuiState();
                    }
                });
            }
        }).start();

        new Thread(() -> {
            try {
                if (ethAddress != null) {
                    updateBalance();
                    while (!activityPaused) {
                        updateEuroPrice();
                        TimeUnit.SECONDS.sleep(TEN_SECONDS);
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showToast(e.getMessage(), this);
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        resetGuiState();
                    }
                });
            }
        }).start();

    }

    @Override
    protected void onPause() {
        activityPaused = true;
        nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    /**
     * Opens system settings, wireless settings.
     */
    private void openNfcSettings() {
        showToast(getString(R.string.enable_nfc), this);
        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
        startActivity(intent);
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
        activityPaused = false; // onPause() gets called when a Intent gets dispatched by Android
        setIntent(intent);
        resolveIntent(intent);
    }

    /**
     * If we have already a Public key, allow the user to reset by pressing back.
     */
    @Override
    public void onBackPressed() {
        if (pubKeyString != null) {
            resetGuiState();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * reset everything, like we never had seen a card.
     */
    private void resetGuiState() {
        displayOnUI(GuiState.NFC_ICON);
        pubKeyString = null;
        ethAddress = null;
        ethAddressView.setText("");
        qrCodeView.setImageResource(R.drawable.ic_eth_3);
        holdCard.setText(R.string.hold_card);
        sendEthBtn.setEnabled(false);
        sendErc20Btn.setEnabled(false);
        votingBtn.setEnabled(false);
        brandProtectionBtn.setEnabled(false);
    }


    private enum GuiState {NFC_ICON, PROGRESS_BAR, BALANCE_TEXT}

    /**
     * On button click SEND ETH.
     */
    public void onSend(View view) {
        Intent intent = new Intent(this, SendTransactionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("pubKey", pubKeyString);
        bundle.putString("ethAddress", ethAddress);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * On button click SEND ERC-20.
     */
    public void onSendErc20(View view) {
        Intent intent = new Intent(this, SendToSmartContractActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("pubKey", pubKeyString);
        bundle.putString("ethAddress", ethAddress);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * On button click VOTING.
     */


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

    /**
     * Display only GUI elements of 1 of 3 states.
     * NFC Icon (when waiting for NFC), spinner (when waiting for network background tasks,
     * Text (when displaying balance results)
     *
     * @param state NFC_ICON, PROGRESS_BAR, BALANCE_TEXT
     */
    public void displayOnUI(GuiState state) {
        // only display NFC Icon
        if (GuiState.NFC_ICON.equals(state)) {
            progressBar.setVisibility(View.GONE);
            balance.setVisibility(View.GONE);
            nfcIcon.setVisibility(View.VISIBLE);
        }
        // only display progress bar
        else if (GuiState.PROGRESS_BAR.equals(state)) {
            nfcIcon.setVisibility(View.GONE);
            balance.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
        // only display balance text
        else if (GuiState.BALANCE_TEXT.equals(state)) {
            nfcIcon.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            balance.setVisibility(View.VISIBLE);
        }
    }
}
