package com.example.jai.googlemapstest;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class FlightPath extends AppCompatActivity {

    private GoogleMap mMap;
    protected Marker point;
    protected Marker drop;
    final Handler mHandler = new Handler();
    int i = 1;
    Polyline line;
    boolean pause = false;
    double waypointlat = 0;
    double waypointlong = 0;
    LatLng waypoint;
    double oldwaypointlat = 0;
    double oldwaypointlong = 0;
    LatLng oldwaypoint;
    double speed = 0;
    double height = 0;
    boolean dropShow = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_path);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        setUpMapIfNeeded();
        createGraph();
    }

    private void setUpMapIfNeeded() {
        // Confirm map is not already instantiated
        if (mMap == null) {
            // Attempt to obtain map from SupportMapFragment
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //zoomCameraToLocation();
    }

    public void zoomCameraToLocation() {
        /*LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        Location myLocation = locationManager.getLastKnownLocation(provider);

        double latitude;
        double longitude;

        if (myLocation == null) {
            //locationManager.requestLocationUpdates(provider, 1000, 0, );
            latitude = 0;
            longitude = 0;
        } else {
            latitude = myLocation.getLatitude();
            longitude = myLocation.getLongitude();
        }

        LatLng currentLocation = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14));*/

        //Delete the following and uncommnet the top (just for testing):
        LatLng sydney = new LatLng(43.0096, -81.2737);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));

        /*mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14));*/
        //Zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
    }

    protected void createGraph() {

        final Bitmap wp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_plane);
        final Bitmap wpHalfSize = Bitmap.createScaledBitmap(wp, wp.getWidth() / 2, wp.getHeight() / 2, false);
        update.start();




        /*for (int i =0; i < waypoints.size(); i++) {
            double waypointlat = waypoints.get(i).latitude;
            double waypointlong = waypoints.get(i).longitude;
            LatLng waypoint = new LatLng(waypointlat, waypointlong);
            //double speed = speeds.get(i);
            double height = heights.get(i);
            if (i == droppedCount && droppedCount != 0) {
                point = mMap.addMarker(new MarkerOptions()
                        .position(waypoint)
                        .anchor(0.5f, 0.5f)
                        .title(Double.toString(height)));
            }
            point = mMap.addMarker(new MarkerOptions()
                    .position(waypoint)
                    .icon(BitmapDescriptorFactory.fromBitmap(wpHalfSize))
                    .anchor(0.5f, 0.5f)
                    .title(Double.toString(height)));
        }*/
    }

    Thread update = new Thread() {

        @Override
        public void run() {
            Intent intent = getIntent();
            final ArrayList<LatLng> waypoints = intent.getParcelableArrayListExtra("WAYPOINT_ID");
            final ArrayList<Double> heights = (ArrayList<Double>) intent.getSerializableExtra("HEIGHT_ID");
            final ArrayList<Double> speeds = (ArrayList<Double>) intent.getSerializableExtra("SPEED_ID");


            while (i < waypoints.size()-5) {

                try {
                    sleep(100);
                    oldwaypointlat = waypoints.get(i-1).latitude;
                    oldwaypointlong = waypoints.get(i-1).longitude;
                    oldwaypoint = new LatLng(oldwaypointlat, oldwaypointlong);

                    waypointlat = waypoints.get(i).latitude;
                    waypointlong = waypoints.get(i).longitude;
                    waypoint = new LatLng(waypointlat, waypointlong);

                    speed = speeds.get(i);
                    height = heights.get(i);


                }catch (InterruptedException e) {
                    e.printStackTrace();
                }


                if (!pause) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            i++;
                            updatePlane();

                        }
                    });
                }

            }
        }
    };

    public void updatePlane() {
        Intent intent = getIntent();
        final int droppedCount = intent.getIntExtra("DROPPED_COUNT", 0);
        final Bitmap wp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_plane);
        final Bitmap wpHalfSize = Bitmap.createScaledBitmap(wp, wp.getWidth() / 2, wp.getHeight() / 2, false);
        final Bitmap dp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_drop_icon);

        //if i != 0
        if (i != 2)
            point.remove();

        //Line from oldwaypoint to waypoint
        line = mMap.addPolyline(new PolylineOptions()
                .add(oldwaypoint, waypoint)
                .width(5)
                .color(Color.WHITE));

        if (i == droppedCount && droppedCount != 0) {
            drop = mMap.addMarker(new MarkerOptions()
                    .position(waypoint)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(dp))
                    .title(Double.toString(height)));
           dropShow = true;
        }

        if (dropShow)
            drop.showInfoWindow();

        point = mMap.addMarker(new MarkerOptions()
                .position(waypoint)
                .icon(BitmapDescriptorFactory.fromBitmap(wp))
                .anchor(0.5f, 0.5f)
                .flat(true)
                .rotation((float)angleFromCoordinate(oldwaypointlat,oldwaypointlong,waypointlat,waypointlong))
                .title("Height: " + Double.toString(height) + " Speed: " + Double.toString(speed)));
        point.showInfoWindow();


    }

    //ddelete this function and replace the .rotation in poin = mMap.addMarker to heading
    //(or just use this it's probably jsut as good and you don't need to send heading)


    private double angleFromCoordinate(double lat1, double long1, double lat2,
                                       double long2) {

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        //brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }

    public void pausePressed (View view) {
        pause = true;

    }


    @Override
    protected void onResume() {
        super.onResume();
    }
    //////////////////////
}
