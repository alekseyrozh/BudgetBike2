package com.ucl.alex.bugetbike3;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class TFLWebManager {
    private final static String APP_ID = "463138b2";
    private final static String APP_KEY = "4fd07c165a8f661269f3c49fdfc47939";

    private DownloadTFLJSONTask asyncTask;
    private TFLManager tflManager;

    //init = 0, final = 1, half = 2
    public static int findInitial = 0;

    public TFLWebManager(TFLManager tflManager) {
        this.tflManager = tflManager;
    }

    private String composeAddress(double lat, double lon, double radius) {
        return "https://api.tfl.gov.uk/BikePoint?lat=" + lat + "&lon=" + lon + "&radius="
                + radius + "&app_id=" + APP_ID + "&app_key=" + APP_KEY;
    }

    public void startLoadingDocksTflInfo(double lat, double lon, double radius) {
        String composedAddress = composeAddress(lat, lon, radius);

        asyncTask = new DownloadTFLJSONTask();
        asyncTask.execute(composedAddress);
    }

    private void onFinishLoading(String result) {
        tflManager.reflectResult(result);
    }

    class DownloadTFLJSONTask extends AsyncTask<String, Void, String> {
        private Exception exception;

        protected String doInBackground(String... composedAddress) {
            URL url = null;
            try {
                url = new URL(composedAddress[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            Scanner scanner = null;
            try {
                scanner = new Scanner(url.openStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            StringBuilder stringBuilder = new StringBuilder();
            Log.d("BAGBAD",""+findInitial);
            while (scanner.hasNextLine())
                stringBuilder.append(scanner.nextLine());
            return stringBuilder.toString();
        }

        protected void onPostExecute(String result) {
            onFinishLoading(result);
        }
    }
}
