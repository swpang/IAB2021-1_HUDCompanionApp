package com.swpang.speedometer;

import java.io.Serializable;

public class Positions implements Serializable {
    private double mLongitude;
    private double mLatitude;
    
    public Positions(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }
    
    public double getLatitude() { return mLatitude; }
    public double getLongitude() { return mLongitude; }
}
