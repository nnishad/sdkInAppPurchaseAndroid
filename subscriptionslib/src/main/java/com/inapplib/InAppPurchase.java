package com.inapplib;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import com.util.IabBroadcastReceiver;
import com.util.IabHelper;
import com.util.IabResult;
import com.util.Inventory;
import com.util.Purchase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.util.IabHelper.RESPONSE_INAPP_PURCHASE_DATA;
import static com.util.IabHelper.RESPONSE_INAPP_SIGNATURE;


public class InAppPurchase implements IabBroadcastReceiver.IabBroadcastListener {

    private static final String TAG = "InAppPurchase";
    private Context context;

    /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY */
    String base64EncodedPublicKey = "CONSTRUCT_YOUR_KEY_AND_PLACE_IT_HERE";
    //Instance of app purchase
    public static final int RC_REQUEST = 10001;
    // The helper object
    IabHelper mHelper;
    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;
    // SKU for our subscription (infinite gas)
    private String SKU_MONTHLY = "";
    private String payload = "";
    private String mInfiniteGasSku = "";
    // Will the subscription auto-renew?
    boolean mAutoRenewEnabled = true;
    // Does the user have an active subscription to the infinite gas plan?
    boolean mSubscribedToInfiniteGas = false;
    // Interface call
    private OnPaymentListener onPaymentListener;

    public InAppPurchase(Context context, String base64EncodedPublicKey, String SKU, String payload, OnPaymentListener listener) {
        this.context = context;
        this.base64EncodedPublicKey = base64EncodedPublicKey;
        this.SKU_MONTHLY = SKU;
        this.payload = payload;
        this.onPaymentListener = listener;
    }

    public void setUpInApp() {
        Log.e(TAG, "setUpInApp: ");
        Log.e(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(context, base64EncodedPublicKey);
        mHelper.enableDebugLogging(true);
        Log.e(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.e(TAG, "Setup finished.");
                Log.e(TAG, "onIabSetupFinished: " + result.isSuccess());

                if (!result.isSuccess()) {
                    Log.e(TAG, "onIabSetupFinished:=");
                    return;
                }

                if (mHelper == null) return;
                mBroadcastReceiver = new IabBroadcastReceiver(InAppPurchase.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                context.registerReceiver(mBroadcastReceiver, broadcastFilter);

                Log.e(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public void receivedBroadcast() {
        Log.e(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.e(TAG, "Query inventory finished.");
            if (mHelper == null) {
                return;
            }
            // Is it a failure?
            if (result.isFailure()) {
                return;
            }
            Log.e(TAG, "Query inventory was successful.");
            Purchase purchase = inventory.getPurchase(SKU_MONTHLY);
            if (purchase != null && purchase.isAutoRenewing()) {
                mInfiniteGasSku = SKU_MONTHLY;
                mAutoRenewEnabled = true;
            } else {
                mInfiniteGasSku = "";
                mAutoRenewEnabled = false;
            }

            // The user is subscribed if either subscription exists, even if neither is auto
            // renewing
            mSubscribedToInfiniteGas = (purchase != null && verifyDeveloperPayload(purchase));
            Log.e(TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
                    + " infinite gas subscription.");
            if (mSubscribedToInfiniteGas) {
                if (onPaymentListener!=null)
                    onPaymentListener.onAlreadyPurchase(purchase);
                Log.e(TAG, "Full gas subscription");
            }
        }
    };


    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.e(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (mHelper == null) return;
            if (result.isFailure()) {
                Log.e(TAG, "result.isFailure() " + result.isFailure());
                if (onPaymentListener != null)
                    onPaymentListener.onFailurePayment(result.getMessage()+"");
                return;
            }

            if (!verifyDeveloperPayload(purchase)) {
                Log.e(TAG, "verifyDeveloperPayload(purchase): " + verifyDeveloperPayload(purchase));
                if (onPaymentListener != null)
                    onPaymentListener.onFailurePayment("Payload string does not match.");
                return;
            }

            if (onPaymentListener != null)
                onPaymentListener.onSuccessPayment();
            Log.e(TAG, "Purchase successful.");
        }
    };

    public void InAppDetails(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
        } else {
            String jsonInAppData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
            if (jsonInAppData != null) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonInAppData);
                    InAppDataModel model = new InAppDataModel();
                    model.setOrderId(jsonObject.optString("orderId"));
                    model.setPackageName(jsonObject.optString("packageName"));
                    model.setProductId(jsonObject.optString("productId"));
                    model.setPurchaseTime(jsonObject.optLong("purchaseTime"));
                    model.setPurchaseState(jsonObject.optString("purchaseState"));
                    model.setDeveloperPayload(jsonObject.optString("developerPayload"));
                    model.setPurchaseToken(jsonObject.optString("purchaseToken"));
                    model.setAutoRenewing(jsonObject.optBoolean("autoRenewing"));
                    model.setSignature(data.getStringExtra(RESPONSE_INAPP_SIGNATURE));
                    if (onPaymentListener != null)
                        onPaymentListener.onSuccessPaymentDetails(model);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        Log.e(TAG, "payload : " + payload);
        if (payload.equals(payload))
            return true;
        else
            return false;
    }

    private void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setMessage(message);
        bld.setPositiveButton("OK", null);
        Log.e(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }


    public void subscriptionPurchase(){
        if (!mHelper.subscriptionsSupported()) {
            alert("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        if (!mSubscribedToInfiniteGas || !mAutoRenewEnabled) {
            List<String> oldSkus = null;
            if (!TextUtils.isEmpty(mInfiniteGasSku)
                    && !mInfiniteGasSku.equals(SKU_MONTHLY)) {
                // The user currently has a valid subscription, any purchase action is going to
                // replace that subscription
                oldSkus = new ArrayList<String>();
                oldSkus.add(mInfiniteGasSku);
            }

            Log.d(TAG, "Launching purchase flow for gas subscription.");
            try {
                mHelper.launchPurchaseFlow((Activity) context, SKU_MONTHLY, IabHelper.ITEM_TYPE_SUBS,
                        oldSkus, RC_REQUEST, mPurchaseFinishedListener, payload);
            } catch (IabHelper.IabAsyncInProgressException e) {
                alert("Error launching purchase flow. Another async operation in progress.");
            }
        }else{
            alert("This subscription is already purchased by user.");
        }
    }

    // unregister receiver when activity destroy
    public void unregisterReceiver(){
        // very important:
        if (mBroadcastReceiver != null) {
            context.unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

}
