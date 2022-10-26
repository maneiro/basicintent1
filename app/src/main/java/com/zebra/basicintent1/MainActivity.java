// **********************************************************************************************
// *                                                                                            *
// *    This application is intended for demonstration purposes only. It is provided as-is      *
// *    without guarantee or warranty and may be modified to suit individual needs.             *
// *                                                                                            *
// **********************************************************************************************

package com.zebra.basicintent1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class  MainActivity extends AppCompatActivity {

    // Array of strings
    List<String> items = new ArrayList<String>();
    // Location String
    String location = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(getResources().getString(R.string.activity_intent_filter_action));
        registerReceiver(myBroadcastReceiver, filter);

        // FOR DEBUGGING without the scanner
//        location = "dfdfgdf";
//        items.add("sdfsdg");
//        items.add("sdgsdfg");
//        items.add("dsfsdf");
//        items.add("dsfsdf");
//
//        final TextView location_txt = (TextView) findViewById(R.id.location_txt);
//        location_txt.setText(location);
//
//        String display_txt = "";
//
//        for (String txt : items) {
//            display_txt = display_txt + " | " + txt;
//        }
//
//        final TextView display_view = (TextView) findViewById(R.id.display_txt);
//        display_view.setText(display_txt);
        // FOR DEBUGGING without the scanner
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }

    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();

            //  This is useful for debugging to verify the format of received intents from DataWedge
            //for (String key : b.keySet())
            //{
            //    Log.v(LOG_TAG, key);
            //}

            if (action.equals(getResources().getString(R.string.activity_intent_filter_action))) {
                //  Received a barcode scan
                try {
                    displayScanResult(intent, "via Broadcast");

                } catch (Exception e) {
                    //  Catch if the UI does not exist when we receive the broadcast
                }
            }
        }
    };

    //
    // The section below assumes that a UI exists in which to place the data. A production
    // application would be driving much of the behavior following a scan.
    //
    private void displayScanResult(Intent initiatingIntent, String howDataReceived)
    {
        String decodedData = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_data_legacy));

        // Set Location first
        if (location == ""){
            location = decodedData;
            final TextView location_txt = (TextView) findViewById(R.id.location_txt);

            location_txt.setText(location);

        }
        // Then append to items
        else {
            items.add(decodedData);
            // Temp way to display text
            String display_txt = "";

            for (String txt : items) {
                display_txt = display_txt + " | " + txt;
            }

            final TextView display_view = (TextView) findViewById(R.id.display_txt);
            display_view.setText(display_txt);
        }
    }


    public void postData() {
        Thread sendPOSTRequest = new Thread(new Runnable() {
            @Override
            public void run() {
                // Create URL Object
                try {
                    URL url = new URL("https://us-central1-tnode-1a0be.cloudfunctions.net/app/allocate");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    try{
                        // Open Connection and set method/headers
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setDoOutput(true);
                        conn.setDoInput(true);
                        conn.setUseCaches(false);

                        // Convert values to JSON
                        JSONObject json = new JSONObject();
                        json.put("location", location);
                        json.put("items", new JSONArray(items));
                        Log.i("DEBUG: ", "JSON STR: " + json.toString());

                        conn.setRequestProperty("Content-length", json.toString().getBytes().length + "");

                        OutputStream os = conn.getOutputStream();
                        // Send final JSON input
                        os.write(json.toString().getBytes("UTF-8"));
                        os.close();
                        conn.connect();

                        // Response Code
                        Log.i("DEBUG", "RESPONSE CODE: " + conn.getResponseCode());
                    } finally {
                        conn.disconnect();
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        // Make thread stop main thread so data isn't cleared before its sent
        sendPOSTRequest.start();
        try {
            sendPOSTRequest.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void completeGroup(View view) {
        if(location != "" && items.size() > 1){
            postData();
            resetGroupItems(view);
        }
    }

    public void resetGroupItems(View view) {
        // Clear data and textview
        items.clear();
        location = "";
        final TextView location_txt = (TextView) findViewById(R.id.location_txt);
        location_txt.setText("Scan to Set Location...");
        final TextView display_view = (TextView) findViewById(R.id.display_txt);
        display_view.setText("");
    }

}
