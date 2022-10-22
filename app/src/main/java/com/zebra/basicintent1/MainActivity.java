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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class  MainActivity extends AppCompatActivity {

    // Hash Map to associate location with items
    HashMap<String, List<String>> locationMap = new HashMap<>();
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
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }

    //
    // After registering the broadcast receiver, the next step (below) is to define it.
    // Here it's done in the MainActivity.java, but also can be handled by a separate class.
    // The logic of extracting the scanned data and displaying it on the screen
    // is executed in its own method (later in the code). Note the use of the
    // extra keys defined in the strings.xml file.
    //
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
        // Create URL Object
        URL url = null;
        try {
            url = new URL("http://localhost:3000/allocate");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // Open Connection and set method/headers
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Send Request
        try ( OutputStream os = conn.getOutputStream()) {
            // Convert hash map to JSON
            JSONObject json = new JSONObject(locationMap);
            // Convert JSON to byte
            byte[] input = json.toString().getBytes("utf-8");
            // Send final JSON input
            os.write(input, 0, input.length);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }
    }

    public void completeGroup(View view) {
        locationMap.put(location, items);
        postData();
        locationMap.clear();

        resetGroupItems(view);
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
