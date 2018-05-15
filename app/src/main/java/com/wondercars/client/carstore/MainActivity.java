package com.wondercars.client.carstore;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progress;
    private AsyncHttpClient client = new AsyncHttpClient();
    private String clientToken;
    // create a mock product instance
    private Map product = new HashMap();
    private Map order = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress = new ProgressDialog(this);
        // create a mock product as a map
        product.put("id", 1);
        product.put("name", "Model X");
        product.put("description", " The Best SUV");
        product.put("price", 87.60);
        //get client token from server
        getToken();
    }

    private void getToken() {
        loading(true, "Loading", "getting client token ...");
        client.get("http://13.57.3.69/rest/paypal/client_token", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String token) {
                System.out.println("client Token:  " + token);
                clientToken = token;
                loading(false, "", "");
            }
        });
    }

    /**
     * when buy button clicked, create a order first, then invoke Express Checkout
     */
    public void onBuyButtonClicked(View view) throws Exception {
        createOrder(product);
    }

    /**
     * invoke Express Checkout immediately after new order is created
     */
    public void createOrder(final Map product) throws Exception {
        JSONObject jsonObject = new JSONObject(product);
        StringEntity entity = new StringEntity(jsonObject.toString());

        loading(true, "Please Wait", "creating order ...");
        client.post(null, "http://13.57.3.69/rest/order", entity, "application/json",
                new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        order.put("productId", product.get("id"));
                        order.put("amount", product.get("price"));
                        order.put("id", Integer.parseInt(responseString)); // this api only return created order ID
                        System.out.println("Order created, order id: " + responseString);
                        loading(false, "", "");
                        // express checkout start
                        setupBraintreeAndStartExpressCheckout();
                    }
                }
        );
    }

    /**
     * invoking Express Checkout
     */
    public void setupBraintreeAndStartExpressCheckout() {
        BraintreeFragment mBraintreeFragment = null;
        try {
            // According to https://github.com/braintree/braintree_android/issues/109
            // The SDK requires Google Play Services >= 8.4.0 and < 9.0.0.
            // add build.gradle with implementation 'com.google.android.gms:play-services-wallet:8.4.0'
            mBraintreeFragment = BraintreeFragment.newInstance(this, clientToken);
            mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
                @Override
                public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                    loading(false, "", "");
                    // Send nonce to server
                    String nonce = paymentMethodNonce.getNonce();
                    postNonceToServer(nonce);
                }
            });
        } catch (InvalidArgumentException e) {
            // There was an issue with your authorization string.
        }
        PayPalRequest request = new PayPalRequest(order.get("amount").toString())
                .currencyCode("USD")
                .intent(PayPalRequest.INTENT_AUTHORIZE);
        loading(true, "Please Wait", "Invoking Express Checkout ...");
        PayPal.requestOneTimePayment(mBraintreeFragment, request);
    }

    // Send payment method nonce to server
    private void postNonceToServer(String nonce) {
        AsyncHttpClient client = new AsyncHttpClient();
        // server accept json format , not http param format
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("paymentMethodNonce", nonce);
            jsonParams.put("amount", order.get("amount"));
            jsonParams.put("orderId", order.get("id")); // send order id also in order to update order status
        } catch (JSONException e) {
            e.printStackTrace();
        }
        StringEntity entity = null;
        try {
            entity = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        loading(true, "Please Wait", "Sending nonce and creating Transaction ...");
        client.post(null, "http://13.57.3.69/rest/paypal/checkout", entity, "application/json",
                new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        loading(false, "", "");
                        try {
                            String transactionId = response.getString("transactionId");
                            paymentComplete("payment success, transaction id: " + transactionId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {

                    }
                }
        );
    }

    // show payment success message in another activity
    public void paymentComplete(String message) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        intent.putExtra("msg", message);
        startActivity(intent);
    }

    private void loading(Boolean flag, String title, String message) {
        progress.setTitle(title);
        progress.setMessage(message);
        progress.setCancelable(true); // disable dismiss by tapping outside of the dialog
        if (flag) {
            progress.show();
        } else {
// To dismiss the dialog
            progress.dismiss();
        }
    }
}
