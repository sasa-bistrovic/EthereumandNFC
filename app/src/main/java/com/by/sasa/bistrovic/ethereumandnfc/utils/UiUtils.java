package com.by.sasa.bistrovic.ethereumandnfc.utils;

import static com.by.sasa.bistrovic.AppConstants.MAINNET_URI;
import static com.by.sasa.bistrovic.AppConstants.PREFERENCE_FILENAME;
import static com.by.sasa.bistrovic.AppConstants.PREF_KEY_MAIN_NETWORK;
import static com.by.sasa.bistrovic.AppConstants.ROPSTEN_URI;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.by.sasa.bistrovic.ethereumandnfc.GetPrivateKeyFromNFCCardActivity;
import com.by.sasa.bistrovic.ethereumandnfc.ImportPrivateKeyToNFCCardActivity;
import com.by.sasa.bistrovic.ethereumandnfc.MainActivity;
import com.by.sasa.bistrovic.ethereumandnfc.R;
import com.by.sasa.bistrovic.ethereumandnfc.SendToSmartContractActivity;
import com.by.sasa.bistrovic.ethereumandnfc.SendTransactionActivity;

/**
 * Utility for Activities.
 */
public class UiUtils {

    /**
     * This method will start a blinking animation for given TextView.
     *
     * @param view TextView to start animation on
     */
    public static void startBlinkingAnimation(TextView view, int blinkingDuration, int totalDuration) {
        if (view != null) {
            final ObjectAnimator animator = ObjectAnimator.ofInt(view, "textColor", Color.BLACK, Color.TRANSPARENT);
            animator.setDuration(blinkingDuration);
            animator.setEvaluator(new ArgbEvaluator());
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.start();

            Handler handler = new Handler();
            handler.postDelayed(animator::cancel, totalDuration);
        }
    }

    /**
     * Handle option menu click.
     *
     * @param activity activity
     * @param item     selected menuitem
     * @return true if handled in here, false otherwise
     */
    public static boolean handleOptionItemSelected(Activity activity, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_balance:
                showToast(activity.getString(R.string.refreshing), activity);
                new Thread(() -> {
                    try {
                        refreshBalance(activity);
                    } catch (Exception e) {
                        showToast(activity.getString(R.string.could_not_refresh), activity);
                    }
                }).start();
                return true;
            case R.id.switch_network:
                SharedPreferences prefs = activity.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
                boolean isMainNetwork = prefs.getBoolean(PREF_KEY_MAIN_NETWORK, true);

                int menuItemTextId;
                String strNetwork;
                if (isMainNetwork) {
                    menuItemTextId = R.string.switch_to_ropsten;
                    strNetwork = "sepolia test network";
                } else {
                    menuItemTextId = R.string.switch_to_mainnet;
                    strNetwork = "main network";
                }
                String finalStrNetwork = strNetwork;
                new AlertDialog.Builder(activity)
                        .setTitle(menuItemTextId)
                        .setMessage(String.format(activity.getString(R.string.ask_switch_network), strNetwork))
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            SharedPreferences.Editor mEditor = prefs.edit();
                            mEditor.putBoolean(PREF_KEY_MAIN_NETWORK, !isMainNetwork).apply();
                            showToast(String.format(activity.getString(R.string.switched_to), finalStrNetwork), activity);
                            activity.recreate();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            case R.id.get_private_key_from_nfc_card:
                Intent getPrivateKeyFromNfcCard = new Intent(activity, GetPrivateKeyFromNFCCardActivity.class);
                activity.startActivity(getPrivateKeyFromNfcCard);
                return true;
            case R.id.import_private_key_to_nfc_card:
                Intent importPrivateKeyToNFCCard = new Intent(activity, ImportPrivateKeyToNFCCardActivity.class);
                activity.startActivity(importPrivateKeyToNFCCard);
                return true;
            default:
                return false;

        }
    }

    public static void refreshBalance(Activity act) throws Exception {
        if (act instanceof MainActivity) {
            ((MainActivity) act).updateBalance();
            ((MainActivity) act).updateEuroPrice();
        } else if (act instanceof SendTransactionActivity) {
        } else if (act instanceof SendToSmartContractActivity) {
            ((SendToSmartContractActivity) act).readAndDisplayErc20Balance();
        }
    }

    /**
     * Method used to log NFC tag info.
     *
     * @param tagFromIntent actual tag to use
     */
    public static void logTagInfo(Tag tagFromIntent) {
    }

    /**
     * Method to get url of the network to connect (Mainnet or Testnet).
     *
     * @param activity read shared prefs from
     * @return mainnet or testnet url
     */
    public static String getFullNodeUrl(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY_MAIN_NETWORK, true)) {
            return MAINNET_URI;
        }

        return ROPSTEN_URI;
    }

    /**
     * Method used to show toast message on UI thread.
     *
     * @param text     message to show
     * @param activity needed for the context
     */
    public static void showToast(String text, Activity activity) {
        activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), text, Toast.LENGTH_SHORT).show());
    }
}
