package com.ucl.alex.bugetbike3;

import com.google.android.gms.maps.model.LatLng;

public class DockingStation
{
    public String name = null;
    public Double lat = null, lon = null;
    public Integer numOfAvailableBikes = null, numOfFreeSlots = null;

    public DockingStation(String name, Double lat, Double lon, Integer numOfAvailableBikes, Integer numOfFreeSlots)
    {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.numOfAvailableBikes = numOfAvailableBikes;
        this.numOfFreeSlots = numOfFreeSlots;
    }


    @Override
    public String toString()
    {
        return "Name: " + name +
                "\nLat: " + lat + " Lon: " + lon +
                "\nNumOfAvalableBikes: " + numOfAvailableBikes +
                "\nNumOfFreeSlots: " + numOfFreeSlots +"\n\n";
    }

    public LatLng getLagLng() {
        return new LatLng(lat, lon);
    }

    public String getBikesInfo() {
        return "NumOfAvalableBikes: " + numOfAvailableBikes +
                "\nNumOfFreeSlots: " + numOfFreeSlots;
    }
}
