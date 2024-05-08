package com.by.sasa.bistrovic.ethereumandnfc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Uint;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainId;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.ContractEIP1559GasProvider;
import org.web3j.utils.Convert;
import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static android.app.PendingIntent.getActivity;

import static com.by.sasa.bistrovic.AppConstants.DEFAULT_ERC20_CONTRACT_ADDRESS;
import static com.by.sasa.bistrovic.AppConstants.FIVE_SECONDS;
import static com.by.sasa.bistrovic.AppConstants.PREFERENCE_FILENAME;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_ERC20_AMOUNT;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_ERC20_CONTRACT_ADDRESS;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_ERC20_RECIPIENT_ADDRESS;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_MAIN_NETWORK;
import static com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils.showToast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.by.sasa.bistrovic.ethereumandnfc.adapter.UnitSpinnerAdapter;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.contract.SimpleStorage;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.exeptions.InvalidEthereumAddressException;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.utils.UriUtils;
import com.by.sasa.bistrovic.ethereumandnfc.qrcode.QrCodeScanner;
import com.by.sasa.bistrovic.ethereumandnfc.utils.InputErrorUtils;
import com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils;
import com.kenai.jffi.Main;


/**
 * Activity class used for ER20 Token functionality.
 */
public class SendToSmartContractActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private String pubKeyString;
    private String privateKeyString;
    private String ethAddress;
    public static BigDecimal ethvalue;

    @BindView(R.id.recipientAddress)
    TextView recipientAddressTxt;
    @BindView(R.id.amount)
    TextView amountTxt;
    @BindView(R.id.gasPrice)
    TextView gasPriceTxt;
    @BindView(R.id.gasLimit)
    TextView gasLimitTxt;
    @BindView(R.id.contractAddress)
    TextView contractAddress;
    @BindView(R.id.currentBalance)
    TextView currentBalance;
    @BindView(R.id.textViewInfo)
    TextView infoTxt;
    @BindView(R.id.pin)
    TextView pinTxt;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.spinner)
    Spinner spinner;

    private InputErrorUtils inputErrorUtils;

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
        setContentView(R.layout.activity_send_to_smart_contract);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        spinnerAdapter.addSpinnerAdapter(this, spinner);
        inputErrorUtils = new InputErrorUtils(this, recipientAddressTxt, amountTxt, gasPriceTxt,
                gasLimitTxt, contractAddress);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        pendingIntent = getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            pubKeyString = b.getString("pubKey");
            ethAddress = b.getString("ethAddress");
        }

        SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        final String savedContractAddress = pref.getString(PREF_KEY_ERC20_CONTRACT_ADDRESS, DEFAULT_ERC20_CONTRACT_ADDRESS);
        contractAddress.setText(savedContractAddress.trim().isEmpty() ? DEFAULT_ERC20_CONTRACT_ADDRESS : savedContractAddress);
        recipientAddressTxt.setText(pref.getString(PREF_KEY_ERC20_RECIPIENT_ADDRESS,
                ""));
        if (recipientAddressTxt.getText().toString().equals("") || recipientAddressTxt.getText().toString()==null) {
            recipientAddressTxt.setText("0x210e51C4Efba450ba3ca2E4339fC92d849229E6b");
        }
        amountTxt.setText(pref.getString(PREF_KEY_ERC20_AMOUNT, "1"));

        new Thread(() -> {
            try {
                while (!activityPaused) {
                    readAndDisplayErc20Balance();
                    TimeUnit.SECONDS.sleep(FIVE_SECONDS);
                }
            } catch (InterruptedException e) {
            }
        }).start();
    }

    /**
     * this method read ERC20 balance via Api request and displays it.
     */
    public void readAndDisplayErc20Balance() {
    }

    @Override
    public void onPause() {
        super.onPause();
        activityPaused = true;
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }

        SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor
                .putString(PREF_KEY_ERC20_CONTRACT_ADDRESS, contractAddress.getText().toString())
                .putString(PREF_KEY_ERC20_RECIPIENT_ADDRESS, recipientAddressTxt.getText().toString())
                .putString(PREF_KEY_ERC20_AMOUNT, amountTxt.getText().toString())
                .apply();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (MainActivity.scanQRCodeLevel!=null) {
            if (MainActivity.scanQRCodeLevel == 2) {
                if (MainActivity.scanQRCodeEthereumAddress != null) {
                    if (isValidEthereumAddress(MainActivity.scanQRCodeEthereumAddress)) {
                        contractAddress.setText(MainActivity.scanQRCodeEthereumAddress);
                        playSystemSound();
                        MainActivity.scanQRCodeEthereumAddress="";
                    } else {
                    }
                }
            }
        }

        if (MainActivity.scanQRCodeLevel!=null) {
            if (MainActivity.scanQRCodeLevel == 3) {
                if (MainActivity.scanQRCodeEthereumAddress != null) {
                    if (isValidEthereumAddress(MainActivity.scanQRCodeEthereumAddress)) {
                        recipientAddressTxt.setText(MainActivity.scanQRCodeEthereumAddress);
                        playSystemSound();
                        MainActivity.scanQRCodeEthereumAddress="";
                    } else {
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
        if (inputErrorUtils.isNoInputError()) {
            showToast(getString(R.string.hold_card_for_while), this);
            resolveIntent(intent);
        }
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

        final String valueStr = amountTxt.getText().toString();
        ethvalue = new BigDecimal(valueStr);
        BigDecimal amountInTokenBaseUnit =  new BigDecimal("10000000000000000");

        new Thread(() -> {
            try {
                isoDep.connect();

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
                    pubKeyString = myPrivate.getEcKeyPair().getPublicKey().toString(16);
                    privateKeyString = myPrivate.getEcKeyPair().getPrivateKey().toString(16);
                }

                if (myPrivateboolean==false) {
                }

                isoDep.close();

                SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
                long chainId = ChainId.MAINNET;
                if (!pref.getBoolean(PREF_KEY_MAIN_NETWORK, true)) {
                    chainId = 11155111;
                }

                Web3j web3 =  Web3j.build(new HttpService(UiUtils.getFullNodeUrl(this))); // for ropsten test network

                System.out.println("Connecting to Ethereum ...");
                System.out.println("Successfuly connected to Ethereum");

                String pk = MainActivity.privateKeyString;
                Credentials credentials = Credentials.create(pk);

                String contractAddress2 = contractAddress.getText().toString();

                EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                TransactionManager txManager = new RawTransactionManager(web3, credentials);

                long finalChainId = chainId;
                ContractGasProvider contractGasProvider = new ContractEIP1559GasProvider() {
                    @Override
                    public boolean isEIP1559Enabled() {
                        return true;
                    }

                    @Override
                    public long getChainId() {
                        return finalChainId;
                    }

                    @Override
                    public BigInteger getMaxFeePerGas(String contractFunc) {
                        return Convert.toWei(gasLimitTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger();
                    }

                    @Override
                    public BigInteger getMaxPriorityFeePerGas(String contractFunc) {
                        return Convert.toWei(gasPriceTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger();
                    }

                    @Override
                    public BigInteger getGasPrice(String contractFunc) {
                        return GAS_PRICE.divide(new BigInteger("100"));
                    }

                    @Override
                    public BigInteger getGasPrice() {
                        return GAS_PRICE.divide(new BigInteger("100"));
                    }

                    @Override
                    public BigInteger getGasLimit(String contractFunc) {
                        return GAS_LIMIT.divide(new BigInteger("100"));
                    }

                    @Override
                    public BigInteger getGasLimit() {
                        return GAS_LIMIT.divide(new BigInteger("100"));
                    }
                };

                Function function = new Function("set", // Function name
                        Arrays.asList(new Uint(BigInteger.valueOf(20))), // Function input parameters
                        Collections.emptyList()); // Function returned parameters

                String txData = FunctionEncoder.encode(function);

                BigInteger initVal = BigInteger.valueOf(42);

                SimpleStorage contract = SimpleStorage.load(
                        contractAddress2, web3, txManager, contractGasProvider);

                contract.transfer(recipientAddressTxt.getText().toString(), Convert.toWei(valueStr.equals("") ? "0" : valueStr, Convert.Unit.ETHER).toBigInteger()).send();

                showToast(getString(R.string.send_success), this);

            } catch (Exception e) {
                showToast("ERROR : "+e.getMessage(), this);
            }
        }).start();

        finish();


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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Do something when camera permission is granted
                Intent intent = new Intent(SendToSmartContractActivity.this, ScanQRCode.class);
                startActivity(intent);
            } else {
                // Handle case when camera permission is denied
                //Toast.makeText(MainActivity.this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onScanContract(View view) {
        MainActivity.scanQRCodeLevel=2;
        if (ContextCompat.checkSelfPermission(SendToSmartContractActivity.this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SendToSmartContractActivity.this,
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Camera permission already granted, perform desired action
            // For example, start camera preview
            Intent intent = new Intent(SendToSmartContractActivity.this, ScanQRCode.class);
            startActivity(intent);
        }
        //QrCodeScanner.scanQrCode(this, 0);
    }

    public void onScanRecipient(View view) {
        MainActivity.scanQRCodeLevel=3;
        //QrCodeScanner.scanQrCode(this, 1);
        if (ContextCompat.checkSelfPermission(SendToSmartContractActivity.this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SendToSmartContractActivity.this,
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Camera permission already granted, perform desired action
            // For example, start camera preview
            Intent intent = new Intent(SendToSmartContractActivity.this, ScanQRCode.class);
            startActivity(intent);
        }
    }
}