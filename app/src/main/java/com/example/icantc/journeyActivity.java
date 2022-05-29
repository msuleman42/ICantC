package com.example.icantc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class journeyActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {

    TextView destinationData;
    Button confDestBtn;

    private static final String TAG = journeyActivity.class.getSimpleName();
    private GoogleMap map;
    private Marker marker;
    private CameraPosition cameraPosition;

    // The entry point to the Places API.
    private PlacesClient placesClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final float DEFAULT_ZOOM = 16.0f;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private static final int PERMISSIONS_FINE_LOCATION = 42;
    private static final int UPDATE_INTERVAL = 30;
    private static final int FAST_UPDATE_INTERVAL = 5;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;
    private Location origin;
    private Location destination;
    private String currentLatitude;
    private String currentLongitude;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    Polyline currentPolyline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_journey);
        destinationData = findViewById(R.id.endJourneyChild);
        confDestBtn = findViewById(R.id.end);

        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        placesClient = Places.createClient(this);

        origin = new Location(KEY_LOCATION);
        destination = new Location(KEY_LOCATION);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationRequest = LocationRequest.create()
                .setInterval(1000 * UPDATE_INTERVAL)
                .setFastestInterval(1000 * FAST_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateLatLng(locationResult);
            }
        };
        updateGPS();

        confDestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (marker != null) {
                    marker.remove();
                }
                RequestQueue queue = Volley.newRequestQueue(journeyActivity.this);

                String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + currentLatitude +
                        "," + currentLongitude + "&destination=" + destinationData.getText().toString() +
                        "&key=" + BuildConfig.MAPS_API_KEY.toString();
                //Toast.makeText(journeyActivity.this, lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude(), Toast.LENGTH_SHORT).show();

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                extractDestinationLatLong(response);
                                //Toast.makeText(journeyActivity.this, destination.getLatitude() + ", " + destination.getLongitude(), Toast.LENGTH_SHORT).show();
                                LatLng originLatLng = new LatLng(Double.parseDouble(currentLatitude), Double.parseDouble(currentLongitude));
                                LatLng destLatLng = new LatLng(destination.getLatitude(), destination.getLongitude());

                                marker = map.addMarker(new MarkerOptions()
                                        .position(destLatLng)
                                        .draggable(true)
                                        .title("Destination"));
                                map.moveCamera(CameraUpdateFactory.newLatLng(destLatLng));


                                // Draw the lines from current location to destination
                                new GetData(journeyActivity.this).execute(getUrl(originLatLng, destLatLng));
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(journeyActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                    }
                });

                queue.add(request);

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        this.map = gMap;

        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.activity_journey,
                        (FrameLayout) findViewById(R.id.map), false);

                return infoWindow;
            }
        });

        // Location permission for map
        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();

    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                Toast.makeText(journeyActivity.this, lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }

    private void extractDestinationLatLong(JSONObject response) {
        try {
            JSONArray routesInfo = response.getJSONArray("routes");
            JSONObject obj1 = routesInfo.getJSONObject(0);
            JSONArray legsInfo = obj1.getJSONArray("legs");
            JSONObject mainInfo = legsInfo.getJSONObject(0);
            JSONObject startLocationInfo = mainInfo.getJSONObject("start_location");
            double latOri = startLocationInfo.getDouble("lat");
            double lngOri = startLocationInfo.getDouble("lng");
            origin.setLatitude(latOri);
            origin.setLongitude(lngOri);
            JSONObject endLocationInfo = mainInfo.getJSONObject("end_location");
            double latDes = endLocationInfo.getDouble("lat");
            double lngDes = endLocationInfo.getDouble("lng");
            destination.setLatitude(latDes);
            destination.setLongitude(lngDes);
        } catch (JSONException e) {
            origin = lastKnownLocation;
            destination = lastKnownLocation;
        }
    }

    private String getUrl(LatLng origin, LatLng dest) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + BuildConfig.MAPS_API_KEY.toString();
        return url;
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = map.addPolyline((PolylineOptions) values[0]);
    }

    @SuppressLint("MissingPermission")
    private void updateGPS() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(journeyActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    updateLatLng(locationResult);
                }
            }, Looper.getMainLooper());
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    public void updateLatLng(LocationResult locationResult) {

        /*
        if (currentLongitude != null && currentLatitude != null) {
            boolean latCheck = locationResult.getLastLocation().getLatitude() == Double.parseDouble(currentLatitude);
            boolean lngCheck = locationResult.getLastLocation().getLatitude() == Double.parseDouble(currentLongitude);
            if (latCheck && lngCheck) {
                Toast.makeText(journeyActivity.this, "SOUND ALARM", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(journeyActivity.this, "NULL", Toast.LENGTH_SHORT).show();
        }

         */

        currentLatitude = String.valueOf(locationResult.getLastLocation().getLatitude());
        currentLongitude = String.valueOf(locationResult.getLastLocation().getLongitude());
        Toast.makeText(journeyActivity.this, currentLatitude + ", " + currentLongitude, Toast.LENGTH_SHORT).show();
    }

}