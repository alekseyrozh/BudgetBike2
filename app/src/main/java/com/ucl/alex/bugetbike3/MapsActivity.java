package com.ucl.alex.bugetbike3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, CompoundButton.OnCheckedChangeListener {

    private final static int TIME_LIMIT = 25*60;//30min

    private GoogleMap mMap;
    private ArrayList<MarkerOptions> initialBikePoints;
    private ArrayList<MarkerOptions> finalBikePoints;
    private ArrayList<MarkerOptions> halfWayPoints;

    private GoogleApiClient mGoogleApiClient;
    private GoogleTimeDistParser googleTimeDistParser = new GoogleTimeDistParser();
    private GoogleWebManager googleWebManager = new GoogleWebManager(this);

    private LocationRequest mLocationRequest;

    private TFLManager tflManager = new TFLManager(this);
    //curr location
    private MarkerOptions currentPosition;
    private MarkerOptions tmpCurrentPosition;
    private MarkerOptions finalDest;

    private MarkerOptions fakeLocation;

    private Switch aSwitch;

    boolean fakeData = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // Initializing
        initialBikePoints = new ArrayList<>();
        finalBikePoints = new ArrayList<>();
        halfWayPoints = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fakeLocation = new MarkerOptions();
        fakeLocation.position(new LatLng(51.543124, -0.149248));

        aSwitch = ((Switch) this.findViewById(R.id.fakeSwitch));
        aSwitch.setOnCheckedChangeListener(this);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        fakeData = isChecked;
        TFLWebManager.findInitial = 0;
        if (isChecked) {
            tmpCurrentPosition = currentPosition;
            currentPosition = fakeLocation;
        }
        else
        {
            currentPosition = tmpCurrentPosition;
        }

        mMap.clear();
        findStartingPoints();
    }
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        // Setting onclick event listener for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                mMap.clear();
                for (int i = 0; i < initialBikePoints.size(); i++)
                    mMap.addMarker(initialBikePoints.get(i));
                finalBikePoints.clear();
                halfWayPoints.clear();

                finalDest = new MarkerOptions();
                finalDest.position(point);
                finalDest.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                mMap.addMarker(finalDest);
                drawPaths(new LatLng[]{currentPosition.getPosition(), finalDest.getPosition()});
                findEndingPoints();

                googleWebManager.startLoadingDocksTflInfo(currentPosition.getPosition(), finalDest.getPosition());


              /*
                // Already two locations
                if (markerPoints.size() >= 1) {
                    markerPoints.clear();
                    mMap.clear();
                }

                // Adding new item to the ArrayList
                markerPoints.add(point);

                // Creating MarkerOptions
                MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(point);


                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));


                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options);

                // Checks, whether start and end locations are captured
                //if (markerPoints.size() >= 2)
                {
                    //LatLng origin = markerPoints.get(0);
                    //LatLng dest = markerPoints.get(1);

                    LatLng dest = markerPoints.get(0);

                    // Getting URL to the Google Directions API
                    String url = getUrl(currentPosition.getPosition(), dest);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();

                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);
                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition.getPosition()));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                }*/

            }
        });

    }


    public void findHalfWayPoints(DockingStation[] loadedDockingStations) {
        int closestNotFullDockInd = 0;
        for (int i = 0; i < loadedDockingStations.length; i++) {
            if (loadedDockingStations[i].numOfAvailableBikes > 0 && firstCloserToCurr(loadedDockingStations[i].getLagLng(), loadedDockingStations[closestNotFullDockInd].getLagLng()))
                closestNotFullDockInd = i;
        }

        halfWayPoints.clear();

        MarkerOptions[] options = new MarkerOptions[loadedDockingStations.length];
        for (int i = 0; i < loadedDockingStations.length; i++) {
            {
                options[i] = new MarkerOptions();
                options[i].position(loadedDockingStations[i].getLagLng());
                options[i].title(loadedDockingStations[i].name);
                //options.snippet(dock.getBikesInfo());

                if (i != closestNotFullDockInd) {
                    options[i].icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    options[i].alpha(0.3f);
                } else
                    options[closestNotFullDockInd].icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                //markerPoints.add(options[i]);
                halfWayPoints.add(options[i]);

                mMap.addMarker(options[i]);
            }
        }
    }

    public void findFinalPoint(DockingStation[] loadedDockingStations) {

        int closestNotFullDockInd = 0;
        for (int i = 0; i < loadedDockingStations.length; i++) {
            if (loadedDockingStations[i].numOfAvailableBikes > 0 && firstCloserToCurr(loadedDockingStations[i].getLagLng(), loadedDockingStations[closestNotFullDockInd].getLagLng()))
                closestNotFullDockInd = i;
        }

        finalBikePoints.clear();

        MarkerOptions[] options = new MarkerOptions[loadedDockingStations.length];
        for (int i = 0; i < loadedDockingStations.length; i++) {
            {
                options[i] = new MarkerOptions();
                options[i].position(loadedDockingStations[i].getLagLng());
                options[i].title(loadedDockingStations[i].name);
                //options.snippet(dock.getBikesInfo());

                if (i != closestNotFullDockInd) {
                    options[i].icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    options[i].alpha(0.3f);
                } else
                    options[closestNotFullDockInd].icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                //markerPoints.add(options[i]);
                finalBikePoints.add(options[i]);

                mMap.addMarker(options[i]);
            }
        }
    }


    private String[] getUrls(LatLng[] points) {

        String[] urlsOfRouts = new String[points.length - 1];

        for (int i = 0; i < urlsOfRouts.length; i++) {
            // Origin of route
            String str_origin = "origin=" + points[i].latitude + "," + points[i].longitude;

            // Destination of route
            String str_dest = "destination=" + points[i + 1].latitude + "," + points[i + 1].longitude;

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = str_origin + "&" + str_dest + "&" + sensor;

            // Output format
            String output = "json";

            // Building the url to the web service
            urlsOfRouts[i] = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        }


        return urlsOfRouts;
    }

    /*
    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }*/

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    public void timeDistLoaded(String result) {
        try {
            TimeDistObject timeDistObject = googleTimeDistParser.parse(new JSONObject(result));
            if(timeDistObject.time>=TIME_LIMIT)
            {
                loadHalfWay();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadHalfWay() {
        TFLWebManager.findInitial = 2;
        tflManager.startLoadingTFL(getHalfWayPos());
    }

    private LatLng getHalfWayPos() {
        return new LatLng((finalDest.getPosition().latitude+(fakeData?fakeLocation.getPosition().latitude:currentPosition.getPosition().latitude))/2f,
                (finalDest.getPosition().longitude+(fakeData?fakeLocation.getPosition().longitude:currentPosition.getPosition().longitude))/2f);
    }


    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                GoogleDataParser parser = new GoogleDataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute", "onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    public void findInitialPoint(DockingStation[] loadedDockingStations) {

        int closestNotFullDockInd = 0;
        for (int i = 0; i < loadedDockingStations.length; i++) {
            if (loadedDockingStations[i].numOfAvailableBikes > 0 && firstCloserToCurr(loadedDockingStations[i].getLagLng(), loadedDockingStations[closestNotFullDockInd].getLagLng()))
                closestNotFullDockInd = i;
        }

        initialBikePoints.clear();

        MarkerOptions[] options = new MarkerOptions[loadedDockingStations.length];
        for (int i = 0; i < loadedDockingStations.length; i++) {
            {
                options[i] = new MarkerOptions();
                options[i].position(loadedDockingStations[i].getLagLng());
                options[i].title(loadedDockingStations[i].name);
                //options.snippet(dock.getBikesInfo());

                if (i != closestNotFullDockInd) {
                    options[i].icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    options[i].alpha(0.3f);
                } else
                    options[closestNotFullDockInd].icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                //markerPoints.add(options[i]);
                initialBikePoints.add(options[i]);

                mMap.addMarker(options[i]);
            }
        }
    }


    private void drawPaths(LatLng[] path) {

        // Getting URL to the Google Directions API
        String[] urls = getUrls(path);
        //String url2 = getUrl(dest, three);
        //String url3 = getUrl(three, four);

        //Log.d("onMapClick", url.toString());

        for (int j = 0; j < urls.length; j++) {
            FetchUrl FetchUrl = new FetchUrl();
            // Start downloading json data from Google Directions API
            FetchUrl.execute(urls[j]);
        }
    }

    private boolean firstCloserToCurr(LatLng lagLng, LatLng lagLng1) {
        double distance0 = Math.sqrt(Math.pow(lagLng.latitude - currentPosition.getPosition().latitude, 2)
                + Math.pow(lagLng.longitude - currentPosition.getPosition().longitude, 2));
        double distance1 = Math.sqrt(Math.pow(lagLng1.latitude - currentPosition.getPosition().latitude, 2)
                + Math.pow(lagLng1.longitude - currentPosition.getPosition().longitude, 2));
        return distance0 < distance1;
    }

    @Override
    public void onLocationChanged(Location location) {
        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        tmpCurrentPosition = new MarkerOptions();
        tmpCurrentPosition.position(latLng);
        tmpCurrentPosition.title("Current Position");
        tmpCurrentPosition.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));

        if (!fakeData) {
            currentPosition = new MarkerOptions();
            currentPosition.position(latLng);
            currentPosition.title("Current Position");
            currentPosition.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            //mCurrLocationMarker = mMap.addMarker(currentPosition);

            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        }
        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        findStartingPoints();
    }

    private void findStartingPoints() {
        TFLWebManager.findInitial = 0;
        tflManager.startLoadingTFL(fakeData? fakeLocation.getPosition():currentPosition.getPosition());
    }

    private void findEndingPoints() {
        TFLWebManager.findInitial = 1;
        tflManager.startLoadingTFL(finalDest.getPosition());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }
}