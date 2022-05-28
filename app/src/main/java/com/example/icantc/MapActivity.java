package com.example.icantc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


public class MapActivity extends AppCompatActivity {

    private static final int PERMISSIONS_FINE_LOCATION = 42;
    private static final int UPDATE_INTERVAL = 30;
    private static final int FAST_UPDATE_INTERVAL = 5;

    TextView longitudeData, latitudeData, altitudeData;
    Button alarmBtn;

    FusedLocationProviderClient fusedLocationProviderClient;

    LocationRequest locationRequest;
    LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        longitudeData = findViewById(R.id.longitudeData);
        latitudeData = findViewById(R.id.latitudeData);
        altitudeData = findViewById(R.id.altitudeData);
        alarmBtn = findViewById(R.id.alarmButton);

        locationRequest = LocationRequest.create()
                .setInterval(1000 * UPDATE_INTERVAL)
                .setFastestInterval(1000 * FAST_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                updateUI(location);
            }
        };

        alarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                MediaPlayer mp = MediaPlayer.create(getApplicationContext(), alarmSound);
                mp.start();
            }
        });

        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateGPS();
            }
            else {
                Toast.makeText(this, "REQUIRES PERMISSIONS", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void updateGPS() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    updateUI(location);
                }
            });
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateUI(Location location) {
        longitudeData.setText(String.valueOf(location.getLongitude()));
        latitudeData.setText(String.valueOf(location.getLatitude()));
        if (location.hasAltitude()) {
            longitudeData.setText(String.valueOf(location.getLongitude()));
        }
        else {
            longitudeData.setText("NA");
        }
    }
}