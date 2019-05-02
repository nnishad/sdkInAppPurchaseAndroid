package com.myinapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.inapplib.InAppDataModel;
import com.inapplib.InAppPurchase;
import com.inapplib.OnPaymentListener;
import com.util.Purchase;


public class MainActivity extends AppCompatActivity implements OnPaymentListener {

    private static final String TAG = "MainActivity";
    private InAppPurchase inAppPurchase;
    private String base64EncodedPublicKey = "XXXX";
    private String SKU = "XXX";
    private String payLoad = "test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inAppPurchase = new InAppPurchase(MainActivity.this, base64EncodedPublicKey,
                SKU, payLoad, this);
        inAppPurchase.setUpInApp();

        findViewById(R.id.btnPurchase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inAppPurchase.subscriptionPurchase();
            }
        });
    }

    @Override
    public void onSuccessPayment() {
        Toast.makeText(this, "Success Purchase", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSuccessPaymentDetails(InAppDataModel model) {
        Log.e(TAG,"SKU : "+model.getPackageName());
        Log.e(TAG,"Order ID : "+model.getOrderId());
    }

    @Override
    public void onFailurePayment(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAlreadyPurchase(Purchase purchase) {
        Log.e(TAG,"Already purchase : "+purchase.getSku());
        findViewById(R.id.btnPurchase).setVisibility(View.GONE);
        Toast.makeText(this, "onAlreadyPurchase " +purchase.getSku(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (InAppPurchase.RC_REQUEST == requestCode)
            inAppPurchase.InAppDetails(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inAppPurchase.unregisterReceiver();
    }
}
