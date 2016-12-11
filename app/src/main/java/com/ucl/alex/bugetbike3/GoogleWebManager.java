package com.ucl.alex.bugetbike3;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class GoogleWebManager
{
    private final static String KEY = "AIzaSyAgIhVAlnB5opD6nGa7lwdK0zc_ysvFCkE";
    private DownloadTimeDistJSONTask asyncTask;

    private MapsActivity activity;

    public GoogleWebManager(MapsActivity activity) {
        this.activity = activity;
    }

    private String composeRequest(LatLng init, LatLng fin)
    {
        return "https://maps.googleapis.com/maps/api/distancematrix/json?units=metric&origins="+
                init.latitude+","+init.longitude+"&destinations="+
                //"New+York+City,NY"+
                fin.latitude+","+fin.longitude+
                "&mode=bicycling"+"&key=" + KEY;
    }


    public void startLoadingDocksTflInfo(LatLng init, LatLng fin) {
        String composedAddress = composeRequest(init, fin);

        asyncTask = new DownloadTimeDistJSONTask();
        asyncTask.execute(composedAddress);
    }

    private void onFinishLoading(String result) {
        activity.timeDistLoaded(result);
    }

    class DownloadTimeDistJSONTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... composedAddress) {
            URL url = null;
            try
            {
                url = new URL(composedAddress[0]);
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }

            Scanner scanner = null;
            try
            {
                scanner = new Scanner(url.openStream());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            StringBuilder stringBuilder = new StringBuilder();
            while (scanner.hasNextLine())
                stringBuilder.append(scanner.nextLine());
            return stringBuilder.toString();
        }

        protected void onPostExecute(String result) {
            onFinishLoading(result);
        }
    }
}
