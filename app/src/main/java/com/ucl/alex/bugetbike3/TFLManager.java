package com.ucl.alex.bugetbike3;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

public class TFLManager {
    private TFLWebManager tflWebManager;
    private DockStationParser dockStationParser = new DockStationParser();

    private static final double DOCKS_SEARCH_RADIUS = 500f;

    private MapsActivity mapsActivity;

    public TFLManager(MapsActivity activity) {
        this.mapsActivity = activity;
        tflWebManager = new TFLWebManager(this);
    }

    public void startLoadingTFL(LatLng position) {
        tflWebManager.startLoadingDocksTflInfo(position.latitude, position.longitude, DOCKS_SEARCH_RADIUS);
    }

    public void reflectResult(String result) {
        JSONObject tflJSON = null;
        try {
            tflJSON = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        DockingStation[] docksArray = null;
        try {
            docksArray = dockStationParser.parse(tflJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (TFLWebManager.findInitial) {
            case 0: mapsActivity.findInitialPoint(docksArray);
            case 1: mapsActivity.findFinalPoint(docksArray);
            case 2: mapsActivity.findHalfWayPoints(docksArray);
        }
    }
}
