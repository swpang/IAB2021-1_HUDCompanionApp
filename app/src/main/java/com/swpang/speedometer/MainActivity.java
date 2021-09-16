package com.swpang.speedometer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static net.daum.mf.map.api.MapView.MapType.Hybrid;
import static net.daum.mf.map.api.MapView.MapType.Satellite;
import static net.daum.mf.map.api.MapView.MapType.Standard;

public class MainActivity extends AppCompatActivity {
    
    private FloatingActionButton btnConnect;
    private FloatingActionButton btnDone;
    private FloatingActionButton btnStart;
    private FloatingActionButton btnDisconnect;
    private FloatingActionButton btnZoomIn;
    private FloatingActionButton btnZoomOut;
    private FloatingActionButton btnCurrent;
    private FloatingActionButton btnChangeMode;
    private FloatingActionButton btnSummary;
    private CardView cardViewLoadingAnimation;
    private TextView txtCoords;
    private TextView txtSpeed;
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    public Location location;
    
    private String deviceName = null;
    private String deviceAddress = null;
    public static Handler handler;
    public static BluetoothSocket bluetoothSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private BluetoothAdapter bluetoothAdapter = null;
    
    private final int REQUEST_CODE = 1;
    private final static int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;
    private final static int MESSAGE_WRITE = 3;
    private final static int MESSAGE_TOAST = 4;
    private final static String TAG = "DEBUG_TAG";
    private int polyTag = 0;
    private Context context = this;
    
    private double global_longitude;
    private double global_latitude;
    private double global_distance;
    private int global_speed;
    private long global_starttime;
    private long global_endtime;
    private long coarse_starttime;
    private double tempDistance;
    private MapView mapView;
    
    private double[] locationHistory;
    private List<Double> Track;
    private List<Positions> TrackArray;
    private Handler mLocationHandler;
    public MapPolyline mapPolyline;
    
    private int mInterval = 2000;
    final double radius = 6371e3; // meters

    Runnable mLocationChecker = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            try {
                getCurrentLocation();
                String heading;
                Positions positions = new Positions(global_latitude, global_longitude);
                if (global_latitude != 0 && global_longitude != 0) {
                    mapView.setShowCurrentLocationMarker(true);
                    mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
                    txtCoords.setText("N " + global_latitude + ", W " + global_longitude);
                    mapPolyline.addPoint(MapPoint.mapPointWithGeoCoord(global_latitude, global_longitude));
                    TrackArray.add(positions);
                    getCurrentSpeed();
                    if (locationHistory[0] != locationHistory[2] && locationHistory[1] != locationHistory[3])
                        heading = headingToString(getHeading(locationHistory[0], locationHistory[1], locationHistory[2], locationHistory[3]));
                    else
                        heading = "";
                    txtSpeed.setText(" " + global_speed + " km/h");
                    Track.add(global_distance / 1000);
                    tempDistance += global_distance / 1000.0;
                } else {
                    heading = "";
                }
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                String tempTime = simpleDateFormat.format(calendar.getTime());
                double tempDistanceRounded = (double)Math.round(tempDistance * 100d) / 100d;
                connectedThread.write(global_speed+"/"+heading+"/"+tempTime+"/"+tempDistanceRounded);
            } finally {
                mLocationHandler.postDelayed(mLocationChecker, mInterval);
            }
        }
    };
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        Boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (!isGPSEnabled && !isNetworkEnabled) {
        } else {
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        global_longitude = location.getLongitude();
                        global_latitude = location.getLatitude();
                    }
                }
            } else {
                if (isNetworkEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 0, locationListener);
                        if (locationManager != null) {
                            if (location != null) {
                                global_longitude = location.getLongitude();
                                global_latitude = location.getLatitude();
                            }
                        }
                    }
                }
            }
        }
    }
    private void getCurrentSpeed() {
        double temp_speed;
        locationHistory[0] = locationHistory[2];
        locationHistory[1] = locationHistory[3];
        locationHistory[2] = global_longitude;
        locationHistory[3] = global_latitude;
        if (locationHistory[0] != 0 && locationHistory[1] != 0) {
            global_distance = getHaversine(locationHistory[0], locationHistory[1], locationHistory[2], locationHistory[3]);
            Log.d("Distance", "Distance is : " + global_distance);
            temp_speed = global_distance / 2.0 * 3600.0 / 1000.0;
            global_speed = (int) Math.round(temp_speed);
            Log.d("Speed", "Speed is : " + String.valueOf(global_speed));
        } else {
            global_speed = 0;
        }
    }
    private void getHashKey() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");
        
        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", (Base64.encodeToString(md.digest(), Base64.DEFAULT) + " key"));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest.signature = " + signature, e);
            }
        }
    }
    private double getHaversine(double long1, double lat1, double long2, double lat2) {
        double phi1 = lat1 * Math.PI / 180.0;
        double phi2 = lat1 * Math.PI / 180.0;
        double delta_phi = (lat2 - lat1) * Math.PI / 180.0;
        double delta_lambda = (long2 - long1) * Math.PI / 180.0;
        double a = Math.sin(delta_phi / 2) * Math.sin(delta_phi / 2) +
                Math.cos(phi1) * Math.cos(phi2) * Math.sin(delta_lambda / 2) *
                        Math.sin(delta_lambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = radius * c;
        return d;
    }
    private double getDistance() {
        double temp = 0.0;
        if (!Track.isEmpty()) {
            for (int i = 0; i < Track.size(); i++) {
                double item = Track.get(i);
                temp += item;
            }
        }
        return temp;
    }
    private int getHeading(double long1, double lat1, double long2, double lat2) {
        double y = Math.sin(long2 - long1) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(long2 - long1);
        double theta = Math.atan2(y,x);
        int heading = (int)Math.round((theta * 180 / Math.PI + 360) % 360);
        return heading;
    }
    private static String headingToString(int heading) {
        String directions[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[ (int)Math.round((  ((double)heading % 360) / 45)) ];
    }
    public static String setTimeConvertDate(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String formatTime = dateFormat.format(time);
        return formatTime;
    }
    
    @RequiresApi(api = Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getHashKey();
        
        mapView = new MapView(this);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.mapView);
        mapViewContainer.addView(mapView);
        
        btnConnect = (FloatingActionButton) findViewById(R.id.btnConnect);
        btnDisconnect = (FloatingActionButton) findViewById(R.id.btnDisconnect);
        btnDone = (FloatingActionButton) findViewById(R.id.btnDone);
        btnStart = (FloatingActionButton) findViewById(R.id.btnStart);
        btnZoomIn = (FloatingActionButton) findViewById(R.id.btnZoomIn);
        btnZoomOut = (FloatingActionButton) findViewById(R.id.btnZoomOut);
        btnCurrent = (FloatingActionButton) findViewById(R.id.btnCurrent);
        btnChangeMode = (FloatingActionButton) findViewById(R.id.btnChangeMode);
        btnSummary = (FloatingActionButton) findViewById(R.id.btnSummary);
        cardViewLoadingAnimation = (CardView) findViewById(R.id.cardViewLoadingAnimation);
        txtCoords = (TextView) findViewById(R.id.txtCoords);
        txtSpeed = (TextView) findViewById(R.id.txtSpeed);
        locationHistory = new double[]{0.0, 0.0, 0.0, 0.0};
        //locationHistory : 이전 지점 longitude, latitude, 현재지점 longitude, latitude
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, REQUEST_CODE);
            }
        }
        int permissionCheck_location = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck_location != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);
        }
        int permissionCheck_bluetooth = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH);
        int permissionCheck_bluetoothAdmin = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_ADMIN);
        if(permissionCheck_bluetooth != PackageManager.PERMISSION_GRANTED
                || permissionCheck_bluetoothAdmin != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_CODE);
        }
        
        locationListener  = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                //위치 바뀔때마다 위치 정보 location 변수에 저장하기
                global_longitude = location.getLongitude();
                global_latitude = location.getLatitude();
            }
        };
        locationManager = (LocationManager) MainActivity.this.getSystemService(Context.LOCATION_SERVICE);
        mLocationHandler = new Handler();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceSelectActivity.class);
                startActivity(intent);
            }
        });
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.cancel();
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                btnConnect.setVisibility(View.VISIBLE);
                btnDisconnect.setVisibility(View.GONE);
                btnStart.setVisibility(View.GONE);
                btnDone.setVisibility(View.GONE);
            }
        });
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.zoomIn(true);
            }
        });
        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.zoomOut(true);
            }
        });
        btnCurrent.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                getCurrentLocation();
                boolean tempbool = mapView.isShowingCurrentLocationMarker();
                if (tempbool) {
                    MapView.CurrentLocationTrackingMode temp = mapView.getCurrentLocationTrackingMode();
                    switch (temp) {
                        case TrackingModeOff:
                            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
                            txtCoords.setText("N " + global_latitude + ", W " + global_longitude);
                            break;
                        case TrackingModeOnWithoutHeading:
                            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
                            txtCoords.setText("N " + global_latitude + ", W " + global_longitude);
                            break;
                        case TrackingModeOnWithHeading:
                            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
                            mapView.setShowCurrentLocationMarker(false);
                    }
                } else {
                    mapView.setShowCurrentLocationMarker(true);
                    mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
                    txtCoords.setText("N " + global_latitude + ", W " + global_longitude);
                }
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtSpeed.setText("Connecting!");
                cardViewLoadingAnimation.setVisibility(View.VISIBLE);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getCurrentLocation();
                        getCurrentLocation();
                        getCurrentLocation();
                        getCurrentLocation();
                        txtSpeed.setText("");
                        cardViewLoadingAnimation.setVisibility(View.GONE);
                        mapView.removeAllPolylines();
                        mapPolyline = new MapPolyline();
                        mapPolyline.setTag(polyTag);
                        mapPolyline.setLineColor(Color.argb(255,255,0,75));
                        Track = new ArrayList<>();
                        TrackArray = new ArrayList<>();
                        startRepeatingTask_Location(); //mLocationChecker loop 시작
                        global_starttime = SystemClock.elapsedRealtime();
                        coarse_starttime = location.getTime();
                        tempDistance = 0.0;
                        btnDone.setVisibility(View.VISIBLE);
                        btnStart.setVisibility(View.GONE);
                        polyTag++;
                    }
                }, 2000);
                
            }
        });
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRepeatingTask_Location();
                global_endtime = SystemClock.elapsedRealtime();
                mapView.setShowCurrentLocationMarker(false);
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
                if (mapPolyline != null) {
                    mapView.addPolyline(mapPolyline);
                    MapPointBounds mapPointBounds = new MapPointBounds(mapPolyline.getMapPoints());
                    int padding = 50;
                    mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));
                }
                if (!TrackArray.isEmpty()) {
                    double finalDistance = getDistance();
                    double totalTimeHr = (global_endtime - global_starttime) / 3600.0 / 1000.0;
                    long totalTimeSec = (global_endtime - global_starttime) / 1000;
                    String name = setTimeConvertDate(coarse_starttime);
                    double avgSpeed = finalDistance / totalTimeHr;
                    TripItem item = new TripItem(name, avgSpeed, finalDistance, totalTimeSec, TrackArray);
                    ((Speedometer) MainActivity.this.getApplication()).setMitemList(item);
                }
                global_starttime = 0;
                coarse_starttime = 0;
                global_endtime = 0;
                tempDistance = 0.0;
                Track.clear();
                TrackArray.clear();
                btnDone.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
            }
        });
        btnChangeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapView.MapType temp = mapView.getMapType();
                Log.d("Map Type", temp.toString());
                switch (temp) {
                    case Standard:
                        mapView.setMapType(Satellite);
                        Log.d("Change Map Type", temp.toString());
                        break;
                    case Satellite:
                        mapView.setMapType(Hybrid);
                        Log.d("Change Map Type", temp.toString());
                        break;
                    case Hybrid:
                        mapView.setMapType(Standard);
                        Log.d("Change Map Type", temp.toString());
                        break;
                }
            }
        });
        btnSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SummaryActivity.class);
                context.startActivity(intent);
            }
        });

        Intent intent = getIntent();
        deviceName = intent.getStringExtra("deviceName");
        if (deviceName != null) {
            deviceAddress = intent.getStringExtra("deviceAddress");
            Toast.makeText(this, "Connecting to " + deviceName + "...", Toast.LENGTH_SHORT).show();
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == CONNECTING_STATUS) {
                    switch (msg.arg1) {
                        case 1:
                            Toast.makeText(MainActivity.this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                            btnConnect.setVisibility(View.GONE);
                            btnDisconnect.setVisibility(View.VISIBLE);
                            btnStart.setVisibility(View.VISIBLE);
                            break;
                        case -1:
                            Toast.makeText(MainActivity.this, "Device fails to connect", Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else if (msg.what == MESSAGE_READ) {
                    String tempMessage = null;
                    if (msg != null) {
                        tempMessage = msg.obj.toString();
                    }
                }
            }
        };
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void stopRepeatingTask_Location() {
        mLocationHandler.removeCallbacks(mLocationChecker);
    }
    public void startRepeatingTask_Location() {
        mLocationHandler.postDelayed(mLocationChecker, mInterval);
    }
    
    public static class CreateConnectThread extends Thread {
        public CreateConnectThread(@NonNull BluetoothAdapter bluetoothAdapter, String address) {
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();
            try {
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed : ", e);
            }
            bluetoothSocket = tmp;
        }
        public void run() {
            BluetoothAdapter bluetoothAdapter1 = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter1.cancelDiscovery();
            try {
                bluetoothSocket.connect();
                Log.e("Status", "Device Connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException e) {
                try {
                    bluetoothSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException e1) {
                    Log.e(TAG, "Could not close the client socket", e1);
                }
                return;
            }
            
            connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.run();
        }
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
    
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] mmBuffer;
        public ConnectedThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }
        public void run() {
            mmBuffer = new byte[1024];
            int numBytes = 0;
            while (true) {
                try {
                    mmBuffer[numBytes] = (byte) inputStream.read();
                    String readMessage;
                    if (mmBuffer[numBytes] == '\n') {
                        readMessage = new String(mmBuffer, 0, numBytes);
                        Log.d("Read Arduino Message", readMessage);
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
                        numBytes = 0;
                    } else {
                        numBytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
        public void write(String input) {
            byte[] bytes = input.getBytes();
            try {
                outputStream.write(bytes);
                Message writtenMsg = handler.obtainMessage(MESSAGE_WRITE, -1, -1, mmBuffer);
                Log.d("Written Arduino Message", input);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
                Message writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }
        public void cancel() {
            try {
                connectedThread.write("disconnected");
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}