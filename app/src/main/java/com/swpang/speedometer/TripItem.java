package com.swpang.speedometer;

import java.util.List;

public class TripItem {
    private String mname;
    private double maverageSpeed;
    private double mdistance;
    private long mtime;
    private List<Positions> mpositionsList;
    
    public TripItem(String name, double averageSpeed, double distance, long time,
                    List<Positions> positionsList) {
        mname = name;
        maverageSpeed = averageSpeed;
        mdistance = distance;
        mtime = time;
        mpositionsList = positionsList;
    }
    
    public String getName() { return mname; }
    public double getSpeed() { return maverageSpeed; }
    public double getDistance() { return mdistance; }
    public long getTime() { return mtime; }
    public List<Positions> getList() { return mpositionsList; }
}
