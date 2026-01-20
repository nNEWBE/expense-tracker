package com.example.trackexpense.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for fetching currency exchange rates.
 * Uses the free exchangerate-api.com API.
 */
public class CurrencyConverter {

    private static final String TAG = "CurrencyConverter";
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnConversionRateListener {
        void onSuccess(double rate);

        void onError(String error);
    }

    /**
     * Fetch the exchange rate from one currency to another.
     * 
     * @param fromCurrency Source currency code (e.g., "BDT")
     * @param toCurrency   Target currency code (e.g., "USD")
     * @param listener     Callback for the result
     */
    public static void getExchangeRate(String fromCurrency, String toCurrency, OnConversionRateListener listener) {
        executor.execute(() -> {
            try {
                // Build API URL
                String apiUrl = API_URL + fromCurrency;
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject rates = jsonResponse.getJSONObject("rates");
                    double rate = rates.getDouble(toCurrency);

                    Log.d(TAG, "Exchange rate from " + fromCurrency + " to " + toCurrency + ": " + rate);

                    mainHandler.post(() -> listener.onSuccess(rate));
                } else {
                    mainHandler.post(() -> listener.onError("API returned code: " + responseCode));
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching exchange rate", e);
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Convert an amount from one currency to another.
     * 
     * @param amount The amount to convert
     * @param rate   The exchange rate (target/source)
     * @return Converted amount
     */
    public static double convert(double amount, double rate) {
        return amount * rate;
    }
}
