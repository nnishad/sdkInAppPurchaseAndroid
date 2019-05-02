## Libaray In-app purchase Android

### How to get base64EncodedPublicKey?
Goto playstore : https://play.google.com/apps/publish
then select application  
- Goto Development tools / Services & APIs


![alt text](https://github.com/ankitgondaliya/sdkInAppPurchaseAndroid/blob/master/img/key.png)

### How to generate SKU?
![alt text](https://github.com/ankitgondaliya/sdkInAppPurchaseAndroid/blob/master/img/sku.png)

## First implementation in main project in build.gradle (Add as module)
```
dependencies {
    implementation project(':subscriptionslib')
}
```

## Put below code in activity
```
private String base64EncodedPublicKey = "XXXXXXXXXXXx";
private String SKU = "XXXXX";
private String payLoad = "XXXX";
```

### - Create object
``` 
InAppPurchase inAppPurchase = new InAppPurchase(MainActivity.this, base64EncodedPublicKey, SKU, payLoad, this);
inAppPurchase.setUpInApp();
```

### - Call this line when press button for purchase
```
inAppPurchase.subscriptionPurchase();
```

### - After payment success call this method
```
@Override
public void onSuccessPayment() {
    Toast.makeText(this, "Success Purchase", Toast.LENGTH_SHORT).show();
}
```

### - After subscription success then get details about payment
``` 
@Override
public void onSuccessPaymentDetails(InAppDataModel model) {
    Log.e(TAG,"SKU : "+model.getPackageName());
    Log.e(TAG,"Order ID : "+model.getOrderId());
}
```

### - Getting error here when failure payment or user can cancel dialog
``` 
@Override
public void onFailurePayment(String error) {
    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
}
```

### - Already subscribed user when open application 
``` 
@Override
public void onAlreadyPurchase(Purchase purchase) {
    Log.e(TAG,"Already purchase : "+purchase.getSku());
    Toast.makeText(this, "onAlreadyPurchase " +purchase.getSku(), Toast.LENGTH_SHORT).show();
}
```

### - Purchase details get in onActivityResult
``` 
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//get here purchase details
    if (InAppPurchase.RC_REQUEST == requestCode)
        inAppPurchase.InAppDetails(requestCode, resultCode, data);
    super.onActivityResult(requestCode, resultCode, data);
}
```


### - Unregister receiver
``` 
@Override
protected void onDestroy() {
       super.onDestroy();

	// unregister receiver while destroy activity
      inAppPurchase.unregisterReceiver();
}
```

