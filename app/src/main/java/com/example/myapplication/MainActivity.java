package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import android.content.ClipboardManager;
import android.content.ClipData;


public class MainActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationManager locationManager;
    private Context mContext;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // UI elements for latitude and longitude
    private TextView tvLatitude, tvLongitude,message;
    private Button buttonCopy;

    // Location request for real-time updates
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        // Initialize UI elements
        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        message = findViewById(R.id.message);
        buttonCopy = findViewById(R.id.copy);

        // Set onClickListener for the copy button
        buttonCopy.setOnClickListener(v -> copyText());

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // Initialize location request for GPS updates
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2000);  // Request location every 2 seconds
        locationRequest.setFastestInterval(1000);  // Get location at least every 1 second

        // Initialize location callback for real-time updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    // Get latitude and longitude from real-time updates
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Update the UI with the location
                    tvLatitude.setText("Latitude: " + latitude);
                    tvLongitude.setText("Longitude: " + longitude);
                    message.setText("https://maps.google.com/?q=" + latitude + "," + longitude);

                    // You can add your emergency message function here using the location coordinates
                    // sendEmergencyMessage(latitude, longitude);
                }
            }
        };

        // Check and request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission is granted, get location immediately
            getLastKnownLocation();
            startLocationUpdates();
        }

        isLocationEnabled();
    }

    // Handle location permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getLastKnownLocation();
                startLocationUpdates();
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Location permission is required to use this feature.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Method to get the last known location for immediate use
    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<android.location.Location>() {
                        @Override
                        public void onSuccess(android.location.Location location) {
                            if (location != null) {
                                // Get latitude and longitude
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // Update the UI with the location
                                tvLatitude.setText("Latitude: " + latitude);
                                tvLongitude.setText("Longitude: " + longitude);
                                message.setText("https://maps.google.com/?q=" + latitude + "," + longitude);
                            } else {
                                // If no last known location is available, show a message
                                Toast.makeText(mContext, "Unable to find location. Try again later.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    // Start real-time location updates
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    // Stop location updates when not needed
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLocationEnabled();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void isLocationEnabled() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
            alertDialog.setTitle("Enable Location");
            alertDialog.setMessage("Turn On Location in the settings menu.");
            alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        }
    }


    // Function to copy text to clipboard
    private void copyText() {
        // Get text from EditText
        String textToCopy = message.getText().toString();

        // Copy text to clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clip);

        // Show a confirmation toast
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }


    // Optional: Define a method for sending emergency messages when sound detection occurs
    // private void sendEmergencyMessage(double latitude, double longitude) {
    //     // Code to send the message with location
    // }
}
