package com.by.sasa.bistrovic.ethereumandnfc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import static android.app.PendingIntent.getActivity;

import static com.by.sasa.bistrovic.AppConstants.DEFAULT_ERC20_CONTRACT_ADDRESS;
import static com.by.sasa.bistrovic.AppConstants.FIVE_SECONDS;
import static com.by.sasa.bistrovic.AppConstants.PREFERENCE_FILENAME;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_ERC20_AMOUNT;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_ERC20_CONTRACT_ADDRESS;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_ERC20_RECIPIENT_ADDRESS;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_MAIN_NETWORK;
import static com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils.showToast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.by.sasa.bistrovic.ethereumandnfc.adapter.UnitSpinnerAdapter;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.contract.SimpleStorage;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.exeptions.InvalidEthereumAddressException;
import com.by.sasa.bistrovic.ethereumandnfc.ethereum.utils.UriUtils;
import com.by.sasa.bistrovic.ethereumandnfc.qrcode.QrCodeScanner;
import com.by.sasa.bistrovic.ethereumandnfc.utils.InputErrorUtils;
import com.by.sasa.bistrovic.ethereumandnfc.utils.UiUtils;


/**
 * Activity class used for ER20 Token functionality.
 */
public class SendToSmartContractActivity extends AppCompatActivity {

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
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        activityPaused = false;
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

                String sasakey="";

                String sasakey2="";

                boolean sasaboolean=false;

                for (int i=11; i<=26;i++) {
                    String sasapage=bytesToHex(isoDep.readPages(i)).substring(0,8);
                    for (int j=1; j<=sasapage.length();j++) {
                        if (!sasaboolean==true) {
                            if (!sasapage.substring(j - 1, j).equals("0")) {
                                sasaboolean = true;
                            }
                        }
                    }
                    sasakey=sasakey+sasapage;
                }

                for (int i=29; i<=36;i++) {
                    String sasapage=bytesToHex(isoDep.readPages(i)).substring(0,8);
                    for (int j=1; j<=sasapage.length();j++) {
                        if (!sasaboolean==true) {
                            if (!sasapage.substring(j - 1, j).equals("0")) {
                                sasaboolean = true;
                            }
                        }
                    }
                    sasakey2=sasakey2+sasapage;
                }

                ECKeyPair keyPair;

                if (sasaboolean==false) {
                } else
                {
                    Credentials sasa = Credentials.create(sasakey2);
                    pubKeyString = sasa.getEcKeyPair().getPublicKey().toString(16);
                    privateKeyString = sasa.getEcKeyPair().getPrivateKey().toString(16);
                }

                if (sasaboolean==false) {
                }

                isoDep.close();

                SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
                byte chainId = ChainId.MAINNET;
                if (!pref.getBoolean(PREF_KEY_MAIN_NETWORK, true)) {
                    chainId = 5;
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

                byte finalChainId = chainId;
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
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == 0) {
                    contractAddress.setText(UriUtils.extractEtherAddressFromUri(
                            data.getStringExtra("SCAN_RESULT")));
                } else if (requestCode == 1) {
                    recipientAddressTxt.setText(UriUtils.extractEtherAddressFromUri(
                            data.getStringExtra("SCAN_RESULT")));
                }
            } catch (
                    InvalidEthereumAddressException e) {
                showToast(getString(R.string.invalid_ethereum_address), this);
            }
        } else if (resultCode == RESULT_CANCELED) {
        }
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

    public void onScanContract(View view) {
        QrCodeScanner.scanQrCode(this, 0);
    }

    public void onScanRecipient(View view) {
        QrCodeScanner.scanQrCode(this, 1);
    }
}