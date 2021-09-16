package com.swpang.speedometer;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class Speedometer extends Application {
    private List<TripItem> mitemList = new ArrayList<>();
    public List<TripItem> getMitemList() { return mitemList; }
    public void setMitemList(TripItem item) { this.mitemList.add(item); }
    public void removeMitemList(int index) { this.mitemList.remove(index); }
}
