package com.swpang.speedometer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SummaryActivity extends AppCompatActivity {
    
    private double distance;
    private long time;
    private double avgSpeed;
    private long starttime;
    private List<Positions> positionsList;
    
    private FloatingActionButton btnBack;
    private TextView txtDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
    
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        btnBack = (FloatingActionButton) findViewById(R.id.btnBack);
        txtDialog = (TextView) findViewById(R.id.txtDialog);
        
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        if (((Speedometer) this.getApplication()).getMitemList().isEmpty()) {
            txtDialog.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            Log.d("RECYCLERVIEW", "empty LIST");
        } else {
            txtDialog.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(new SummaryAdapter(SummaryActivity.this, ((Speedometer) SummaryActivity.this.getApplication()).getMitemList()));
            Log.d("RECYCLERVIEW", "Adapter SET!");
        }
        
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SummaryActivity.this, MainActivity.class);
                SummaryActivity.this.startActivity(intent);
            }
        });
    }
}