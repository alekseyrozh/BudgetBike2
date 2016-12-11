package com.ucl.alex.bugetbike3;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GoogleTimeDistParser
{

    public TimeDistObject parse(JSONObject mainJSON) throws JSONException
    {
        JSONObject rows0 = mainJSON.getJSONArray("rows").getJSONObject(0);
        JSONObject element = rows0.getJSONArray("elements").getJSONObject(0);

        JSONObject duration = element.getJSONObject("duration");
        JSONObject distance = element.getJSONObject("distance");

        int durationVal = duration.getInt("value");
        String distanceVal = distance.getString("text");

        return new TimeDistObject(durationVal,distanceVal);
    }
}
