package com.swpang.speedometer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import java.util.List;

public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.MyViewHolder> {
    
    private List<TripItem> TripList;
    private Context context;
    private MapView mapViewSum;
    private Speedometer myApp;
    
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtCardName;
        TextView txtDetails;
        TextView txtDistance;
        TextView txtAvSpeed;
        TextView txtTime;
        
        public MyViewHolder(@NonNull View v) {
            super(v);
            txtCardName = v.findViewById(R.id.txtCardName);
            txtDetails = v.findViewById(R.id.txtDetailsSum);
            txtDistance = v.findViewById(R.id.txtDistanceSum);
            txtAvSpeed = v.findViewById(R.id.txtAvSpeedSum);
            txtTime = v.findViewById(R.id.txtTimeSum);
        }
    }
    
    public SummaryAdapter(Context context, List<TripItem> TripList) {
        this.context = context;
        this.TripList = TripList;
    }
    
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_layout, parent, false);
        return new MyViewHolder(v);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MyViewHolder itemHolder = (MyViewHolder) holder;
        TripItem tempItem = TripList.get(position);
        String name = tempItem.getName();
        itemHolder.txtCardName.setText(name + " 의 경로");
        double distance = tempItem.getDistance();
        itemHolder.txtDistance.setText((int)distance + " km");
        double avgSpeed = tempItem.getSpeed();
        itemHolder.txtAvSpeed.setText(avgSpeed + " km/h");
        long time = tempItem.getTime();
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if ((double)time / 3600.0 / 24.0 >= 1.0) {
            days = (int) ((double)time / 3600.0 / 24.0);
            long temp = time - (days * 3600 * 24);
            if ((double)temp / 3600.0 >= 1.0) {
                hours = (int) ((double)temp / 3600.0);
                long temp1 = temp - hours * 3600;
                if ((double)temp1 / 60.0 >= 0) {
                    minutes = (int) ((double)temp1 / 60.0);
                }
            }
        }
        String tempTime = "";
        if (days != 0) {
            tempTime += String.valueOf(days);
            tempTime += " Days ";
        }
        if (hours != 0) {
            tempTime += String.valueOf(hours);
            tempTime += " Hours ";
        }
        if (minutes != 0) {
            tempTime += String.valueOf(minutes);
            tempTime += " Minutes ";
        }
        if (days == 0 && hours == 0 && minutes == 0)
            tempTime = "0 Minutes";
        itemHolder.txtTime.setText(tempTime);
    }
    
    @Override
    public int getItemCount() {
        return TripList.size();
    }
}
