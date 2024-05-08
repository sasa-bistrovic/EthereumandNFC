package com.by.sasa.bistrovic;

/**
 * Class used for common app constants.
 */
public class AppConstants {

    /**
     * Log tag, used in logcat messages from this app.
     */
    private static final String HTTPS = "https://";
    private static final String BASEURL = ".infura.io/v3/20414d983c004018a3a19d3e5bd1121a";
    public static final String MAINNET_URI = HTTPS + "mainnet" + BASEURL;
    public static final String ROPSTEN_URI = HTTPS + "sepolia" + BASEURL;
    public static final int TEN_SECONDS = 10;
    public static final int FIVE_SECONDS = 5;

    public static final String PREFERENCE_FILENAME = "myPrefs";

    public static final String PREF_KEY_MAIN_NETWORK = "mainNetwork";
    public static final String PREF_KEY_PIN = "pin";

    public static final String DEFAULT_GASPRICE_IN_GIGAWEI = "1.5";
    public static final String DEFAULT_GASLIMIT = "1.51";

    // send eth
    public static final String PREF_KEY_RECIPIENT_ADDRESS = "recipientAddressTxt";
    public static final String PREF_KEY_GASPRICE_WEI = "gasPriceInWei";
    public static final String PREF_KEY_GASLIMIT_SEND_ETH = "gasLimitSendEth";

    public static final String DEFAULT_ERC20_CONTRACT_ADDRESS = "0x21a13018F78267469692205160B28e0A6814bE6b";
    public static final String PREF_KEY_ERC20_CONTRACT_ADDRESS = "ContractAddress";
    public static final String PREF_KEY_ERC20_RECIPIENT_ADDRESS = "RecipientAddress";
    public static final String PREF_KEY_ERC20_AMOUNT = "TokenAmount";
}

