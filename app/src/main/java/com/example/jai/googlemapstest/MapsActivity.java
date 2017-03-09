package com.example.jai.googlemapstest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class MapsActivity extends AppCompatActivity {

    private RadioIO radio1;

    private GoogleMap mMap;
    private SQLiteDatabase pointDb;

    // Target Variables
    public Marker targetMarker;
    public LatLng targetPoint = new LatLng(32.61004049938781, -97.4838476255536);
    public String targetName;

    // Variables for saving target location
    protected boolean savingMode = false;

    // Plane location variables to draw marker
    protected Marker planeMarker;
    protected LatLng planePoint;
    protected Marker wayPoint;
    protected double plane_lat = 0.0;
    protected double plane_long = 0.0;
    protected double plane_rotation = 0.0;
    protected double plane_speed = 0.0;
    protected double plane_height = 0.0;
    protected double plane_distance = 0.0;
    protected double plane_Time =  0.0;
    protected double plane_pitch = 0.0;
    protected double plane_roll = 0.0;
    protected boolean planeVisible = true;

    Handler handler = new Handler();

    //floating button
    protected FloatingActionButton fab;
    boolean payload = false;
    boolean dropped = false;

    Context global_context;
    ArrayList <LatLng> waypoints = new ArrayList<LatLng>();
    ArrayList <Double> heights = new ArrayList<Double>();
    ArrayList <Double> speeds = new ArrayList<>();

    int wayPointCount = 0;
    int droppedCount = 0;
    private double altitude_filter[];
    private double last_height;
    private long lastTime;
    private int altitude_filter_pos, altitude_filter_size;
    private double climb_rate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Set current view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        setUpFloatingButton();
        setUpInternalStorage();
        setUpMapIfNeeded();
        updateMarkers();

        altitude_filter_size = 20;
        altitude_filter = new double[altitude_filter_size];
        altitude_filter_pos = 0;
        lastTime = System.currentTimeMillis();

        global_context = this;

        if(radio1 == null){
            radio1 = new RadioIO(this);

        }
    }

    public void setUpFloatingButton() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Drop/Load Action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

              /*  if (payload) {
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_drop_icon, global_context.getTheme()));
                    payload = false;
                    telemetry.dropLoadToggle = true;
                } else {
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_refresh, global_context.getTheme()));
                    telemetry.dropLoadToggle = true;
                    dropped = true;
                    payload = true;
                }*/
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu customMenu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, customMenu);
        return true;
    }

    public void setUpInternalStorage() {
        pointDb = this.openOrCreateDatabase("PointDatabase", MODE_PRIVATE, null);
        pointDb.execSQL("CREATE TABLE IF NOT EXISTS points (id INTEGER PRIMARY KEY AUTOINCREMENT, lat REAL NOT NULL, long REAL NOT NULL, name BLOB NOT NULL);");
        pointDb.close();
    }

    @Override
    protected void onResume(){
        super.onResume();
        setUpMapIfNeeded();
        updateMarkers();
    }

    private void setUpMapIfNeeded() {
        // Confirm map is not already instantiated
        if(mMap == null) {
            // Attempt to obtain map from SupportMapFragment
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        zoomCameraToLocation();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                if (savingMode) {
                    targetPoint = point;
                    updateMarkers();
                }
            }
        });
    }

    public void zoomCameraToLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setTarget:
                if (!savingMode){
                    Toast.makeText(this, "Tap to Set Target Location", Toast.LENGTH_LONG).show();
                    savingMode = true;
                } else {
                    Toast.makeText(this, "Set Target Location Disabled", Toast.LENGTH_LONG).show();
                    savingMode = false;
                }
                break;
            case R.id.saveTarget:
                if(targetPoint != null) {
                    promptInput();
                }
                break;
            case R.id.targetAtCurrentLocation:
                Location myLocation = mMap.getMyLocation();
                targetPoint = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                updateMarkers();
                savingMode = false;
                break;
            case R.id.loadLocation:
                savingMode = false;
                Intent intent = new Intent(MapsActivity.this, LoadTargetActivity.class);
                pointDb.close();
                startActivityForResult(intent, 1);
                break;
            case R.id.connect:
                radio1.setUpUsbIfNeeded();
                if(!thread.isAlive()) {
                    thread.start();
                }
//                Thread thread2 = new Thread(radio1);
//                thread2.start();
                if(!planeVisible) {
                    planeVisible = true;
                } else {
                    planeVisible = true;
                }
                break;
            case R.id.artificialHorizon:
                /*radio1.closeDevice();
                savingMode = false;
                Intent intent2 = new Intent(MapsActivity.this, ArtificialHorizonActivity.class);
                pointDb.close();
                startActivityForResult(intent2, 2);*/
                break;
            case R.id.flightPath:
                /*radio1.closeDevice();
                savingMode =false;
                Intent intent3 = new Intent(MapsActivity.this, FlightPath.class);
                intent3.putExtra("WAYPOINT_ID", waypoints);
                intent3.putExtra("HEIGHT_ID", heights);
                intent3.putExtra("SPEED_ID", speeds);
                intent3.putExtra("DROPPED_COUNT", droppedCount);
                pointDb.close();
                startActivity(intent3);*/
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Double returnedPoint[] = (Double[]) data.getSerializableExtra("1");
                targetPoint = new LatLng(returnedPoint[0], returnedPoint[1]);
                updateMarkers();
                Toast.makeText(this, "Received Data", Toast.LENGTH_LONG).show();
            } else {
                return;
            }
        }
    }

    public void promptInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Name of Target");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                targetName = input.getText().toString();
                savingMode = false;
                savePoint();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void updateMarkers() {
        final Bitmap target = BitmapFactory.decodeResource(getResources(), R.drawable.ic_target);
        final Bitmap targetDoubleSize = Bitmap.createScaledBitmap(target, target.getWidth() * 1, target.getHeight() * 1, false);

        //remove previously placed Marker
        if (targetMarker != null) {
            targetMarker.remove();
        }

        //place marker where user just clicked
        if(targetPoint != null) {
            targetMarker = mMap.addMarker(new MarkerOptions()
                    .position(targetPoint)
                    .title("Target")
                    .icon(BitmapDescriptorFactory.fromBitmap(targetDoubleSize))
                    .anchor(0.5f, 0.5f));
        }

    }

    public void updatePlane() {
        // add plane marker to map
        if (planeMarker != null) {
            planeMarker.remove();
        }

        planeMarker = mMap.addMarker(new MarkerOptions()
                .position(planePoint)
                .title("Plane")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_plane))
                .anchor(0.5f, 0.5f)
                .rotation((float) plane_rotation));
    }

   /* public void updatePath(){
        Bitmap wp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_wp);
        Bitmap wpHalfSize = Bitmap.createScaledBitmap(wp, wp.getWidth() / 2, wp.getHeight() / 2, false);
        wayPoint = mMap.addMarker(new MarkerOptions()
                .position(planePoint)
                .icon(BitmapDescriptorFactory.fromBitmap(wpHalfSize))
                .anchor(0.5f, 0.5f)
                .title(Double.toString(radio1.planeAlt)));

        waypoints.add(planePoint);
        heights.add(radio1.planeAlt);
        //speeds.add(radio1.planeSpeed);
        wayPointCount++;
        if(dropped){
            droppedCount = wayPointCount;
        }
    };*/

    public void savePoint() {
        pointDb = this.openOrCreateDatabase("PointDatabase", MODE_PRIVATE, null);
        pointDb.execSQL("INSERT INTO points (lat, long, name) VALUES ( "
                + Double.toString(targetPoint.latitude)
                + " , " + Double.toString(targetPoint.longitude)
                + " , " + "'" + targetName + "'" + " );");
        pointDb.close();
        Toast.makeText(this, "Saving Target Location: " + Double.toString(targetPoint.latitude) + " " + Double.toString(targetPoint.longitude), Toast.LENGTH_LONG).show();
    }

    public void updateUi () {

        TextView textViewDropHeight = (TextView) findViewById(R.id.toolbar_title);
        TextView textViewCurrentHeight = (TextView) findViewById(R.id.toolbar_title1);
        TextView textViewTimer = (TextView) findViewById(R.id.timerValue);
        TextView textViewDistance = (TextView) findViewById(R.id.distanceValue);
        TextView textViewHeading = (TextView) findViewById(R.id.headingValue);
        //TextView textViewvSpeed = (TextView) findViewById(R.id.vSpeedValue);
        //TextView textViewAirSpeed = (TextView) findViewById(R.id.airspeed_title);


        textViewTimer.setText(String.format("%.2f s", plane_Time));
        textViewDistance.setText(String.format("%.2f m", plane_distance));

        //improve this: maybe have "LL" and "RR" if the value is greater/less than +-0.5
        if(plane_rotation < 0)
            textViewHeading.setText("R");
        else if(plane_rotation > 0)
            textViewHeading.setText("L");
        else
            textViewHeading.setText("0");
        textViewCurrentHeight.setText(String.format("%.2f m", plane_height));
        //textViewAirSpeed.setText(Double.toString(plane_speed) + " km/hr");
        //textViewvSpeed.setText(String.format("%.1f ft/s", climb_rate));
        //double dropDistance = (double) Math.round(dropAlgorithm.drop_dist);

        //double dropHeading = (double) Math.round(dropAlgorithm.drop_heading);

        if(dropped) {
            textViewDropHeight.setText(String.format("$%.2f ft",plane_height));

            dropped = false;
        }
    }

    Thread thread = new Thread() {
      @Override
      public void run() {
              while (true) {
                  try {
                      sleep(10);
                      plane_long = radio1.planeLong;
                      plane_rotation = radio1.planeHeading;
                      plane_height = radio1.planeAlt;
                      plane_distance = radio1.planeDistance;
                      plane_Time = radio1.planeTime;
                      plane_pitch = radio1.planePitch;
                      plane_roll = radio1.planeRoll;
                      planePoint = new LatLng(plane_lat, plane_long);

                      //Log.v("PLANE", Double.toString(plane_lat) + Double.toString(plane_long));



//                      long newTime = System.currentTimeMillis();
//                      altitude_filter[altitude_filter_pos++] = plane_height;
//                      if(altitude_filter_pos == altitude_filter_size) altitude_filter_pos = 0;
//                      plane_height = 0;
//                      for (int i = 0; i < altitude_filter_size; i++) plane_height += altitude_filter[i];
//                      plane_height /= altitude_filter_size;
//                      climb_rate = 1000.0 * (plane_height - last_height) / (newTime - lastTime);
//                      lastTime = newTime;
//                      last_height = plane_height;


                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
                  handler.post(new Runnable() {
                      @Override
                      public void run() {
                          updateUi();
                          updatePlane();
                      }
                  });
                  /*handler.postDelayed(new Runnable() {
                      @Override
                      public void run() {
                          updatePath();
                      }
                  }, 5000);*/
          }
       }
    };

///////////////////////////////////////////////////////
}
