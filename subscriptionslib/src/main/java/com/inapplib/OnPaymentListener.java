package com.inapplib;

import com.util.Purchase;

public interface OnPaymentListener {
    void onSuccessPayment();
    void onSuccessPaymentDetails(InAppDataModel model);
    void onFailurePayment(String error);
    void onAlreadyPurchase(Purchase purchase);
}