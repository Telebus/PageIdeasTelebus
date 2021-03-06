package com.rp.pageideastelebus;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.ApiOptions.NoOptions;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, View.OnClickListener {

    private GoogleMap mMap;

    EditText editTextOrigem;
    EditText editTextDestino;
    Button btnOrigem;
    Button btnDestino;

    LocationManager locationManager;
    LocationListener locationListener;
    Marker positionMarker;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                updateLocationMarker(lastKnownLocation);

                LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));

            }  else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }

        } else {

            startActivity(new Intent(getApplicationContext(), MainActivity.class));

        }
    }

    public void updateLocationMarker(Location location){

        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

        if (positionMarker != null) {
            animateMarker(positionMarker, userLocation, false);
        } else {

            positionMarker = mMap.addMarker(new MarkerOptions().position(userLocation).icon(
                    BitmapDescriptorFactory.fromBitmap(BitmapFactory
                            .decodeResource(getResources(),
                                    R.drawable.placeholder))));
        }

    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        editTextOrigem = findViewById(R.id.editTextOrigem);
        editTextDestino = findViewById(R.id.editTextDestino);
        btnOrigem = findViewById(R.id.btnOrigem);
        btnDestino = findViewById(R.id.btnDestino);

        btnOrigem.setOnClickListener(this);
        btnDestino.setOnClickListener(this);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if(location != null){

                    updateLocationMarker(location);
                    Log.i("Location: ", location.toString());

                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

    }

    @Override
    public void onClick(View v) {

        if (v == btnOrigem){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                String address = "";

                try {

                    List<Address> listAddresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                    if (listAddresses != null && listAddresses.size() > 0){

                        if (listAddresses.get(0).getThoroughfare() != null) {

                            if (listAddresses.get(0).getSubThoroughfare() != null){

                                address += listAddresses.get(0).getSubThoroughfare() + " ";

                            }

                            address += listAddresses.get(0).getThoroughfare();

                        }

                    }

                } catch (IOException e) {

                    e.printStackTrace();

                }

                if (address == ""){

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyy-MM-dd");
                    address = sdf.format(new Date());

                }

                Log.i("Origem: ", address);
                editTextOrigem.setText(address);

            }

        }

        if (v == btnDestino) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                String address = "";

                try {

                    List<Address> listAddresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                    if (listAddresses != null && listAddresses.size() > 0) {

                        if (listAddresses.get(0).getThoroughfare() != null) {

                            if (listAddresses.get(0).getSubThoroughfare() != null) {

                                address += listAddresses.get(0).getSubThoroughfare() + " ";

                            }

                            address += listAddresses.get(0).getThoroughfare();

                        }

                    }

                } catch (IOException e) {

                    e.printStackTrace();

                }

                if (address == "") {

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyy-MM-dd");
                    address = sdf.format(new Date());

                }

                Log.i("Destino: ", address);
                editTextDestino.setText(address);

            }
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLongClickListener(this);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if(location != null) {
                    updateLocationMarker(location);
                    Log.i("Location: ", location.toString());
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
            };

        if (Build.VERSION.SDK_INT < 23){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locationListener);

            }

        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {

                    updateLocationMarker(lastKnownLocation);

                    LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));

                }


            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }
        }

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        String address = "";

        try {

            List<Address> listAddresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (listAddresses != null && listAddresses.size() > 0){

                if (listAddresses.get(0).getThoroughfare() != null) {

                    if (listAddresses.get(0).getSubThoroughfare() != null){

                        address += listAddresses.get(0).getSubThoroughfare() + " ";

                    }

                    address += listAddresses.get(0).getThoroughfare();

                }

            }

        } catch (IOException e) {

            e.printStackTrace();

        }

        if (address == ""){

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyy-MM-dd");
            address = sdf.format(new Date());

        }

        mMap.addMarker(new MarkerOptions().position(latLng).title(address));

    }

}
