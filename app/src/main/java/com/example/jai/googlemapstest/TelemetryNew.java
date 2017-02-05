package com.example.jai.googlemapstest;

import android.content.Context;

/**
 * Created by marko on 2017-01-15.
 */
public class TelemetryNew {
    public static final int LONGITUDE = 0;
    public static final int LATITUDE = 1;
    public static final int ALTITUDE = 2;
    public static final int SPEED = 3;
    public static final int HEADING = 4;
    public static final int YAW = 5;
    public static final int PITCH = 6;
    public static final int ROLL = 7;

    public double[] data;
    public boolean payload;
    public boolean dropLoadToggle;

    protected boolean telemetryOpen = false;

    Context global_context;


    public TelemetryNew(Context context) {
        data[TelemetryNew.LONGITUDE] = 0;
        data[TelemetryNew.LATITUDE] = 0;
        data[TelemetryNew.ALTITUDE] = 0;
        data[TelemetryNew.SPEED] = 0;
        data[TelemetryNew.HEADING] = 0;
        data[TelemetryNew.YAW] = 0;
        data[TelemetryNew.PITCH] = 0;
        data[TelemetryNew.ROLL] = 0;

        this.global_context = context;
        this.payload = true;
        this.dropLoadToggle = false;
    }
}
