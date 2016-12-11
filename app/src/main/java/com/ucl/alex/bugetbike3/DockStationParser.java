package com.ucl.alex.bugetbike3;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DockStationParser
{
    private final static int INDEX_OF_NUM_OF_EMPTY_SLOTS = 7;
    private final static int INDEX_OF_NUM_OF_AVAILABLE_BIKES = 6;

    public DockingStation[] parse(JSONObject mainJson) throws JSONException
    {
        JSONArray yourCoords = mainJson.getJSONArray("centrePoint");
        double ourLat = yourCoords.getDouble(0);
        double ourLon = yourCoords.getDouble(1);

        JSONArray dockStationsJSON = mainJson.getJSONArray("places");
        int numOfDocks = dockStationsJSON.length();

        //vars
        DockingStation[] dockStations = new DockingStation[numOfDocks];
        JSONObject dockJSON;
        String name, keyVal;
        double lat, lon;
        JSONArray additionalParams;
        JSONObject tmpObj;
        //
        int numOfEmptySlots, numOfAvailableBikes;
        for (int i = 0; i < numOfDocks; i++)
        {
            dockJSON = dockStationsJSON.getJSONObject(i);
            name = dockJSON.getString("commonName");
            lat = dockJSON.getDouble("lat");
            lon = dockJSON.getDouble("lon");

            additionalParams = dockJSON.getJSONArray("additionalProperties");
            tmpObj = additionalParams.getJSONObject(INDEX_OF_NUM_OF_EMPTY_SLOTS);
            keyVal = tmpObj.getString("key");
            if (!keyVal.equals("NbEmptyDocks"))
                throw new JSONException("additionParams[7] 'key' : " + keyVal + " instead of NbEmptyDocks");
            numOfEmptySlots = tmpObj.getInt("value");

            tmpObj = additionalParams.getJSONObject(INDEX_OF_NUM_OF_AVAILABLE_BIKES);
            keyVal = tmpObj.getString("key");
            if (!keyVal.equals("NbBikes"))
                throw new JSONException("additionParams[6] 'key' : " + keyVal + " instead of NbBikes");
            numOfAvailableBikes = tmpObj.getInt("value");

            dockStations[i] = new DockingStation(name, lat, lon, numOfAvailableBikes, numOfEmptySlots);
        }

        return dockStations;
    }
}
