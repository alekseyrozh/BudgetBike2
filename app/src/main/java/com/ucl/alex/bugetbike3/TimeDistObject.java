package com.ucl.alex.bugetbike3;

public class TimeDistObject
{
    int time;
    String dist;

    public TimeDistObject(int time, String dist)
    {
        this.time = time;
        this.dist = dist;
    }

    @Override
    public String toString()
    {
        return "time: " + time + " dist: " + dist;
    }
}
