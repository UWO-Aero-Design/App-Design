/**
 * Responsible for handling input from the radio's buffer and turning that
 * into telemetry data which can be polled by other components of the app.
 */

package com.example.jai.googlemapstest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.BufferedWriter;
import java.io.IOException;


public class RadioIO extends FragmentActivity {

    private FT_Device ftDev;
    private D2xxManager ftD2xx = null;


    private double[] telemetry = {0,0,0,0,0,0,0,0};

    public static final int LATITUDE = 0;
    public static final int LONGITUDE = 1;
    public static final int ALTITUDE = 2;
    public static final int ROLL = 3;
    public static final int PITCH = 4;
    public static final int YAW = 5;
    public static final int TIME = 6;
    public static final int DIST = 7;


    public double planeLong;
    public double planeLat;
    public double planeAlt;
    public double planeTime;
    public double planeHeading;
    public double planePitch;
    public double planeRoll;
    public double planeDistance;

    protected boolean telemetryOpen = false;
    protected String TAG = "TAG";

    public int port = 0;

    protected BroadcastReceiver mUsbReceiver;
    protected IntentFilter filter;

    Context global_context;

    public static final int READBUF_SIZE  = 256;
    private byte[] buffer  = new byte[READBUF_SIZE];
    protected BufferedWriter bfWriter;




    public boolean mThreadIsStopped = true;     //implemented in runnable, therefore not needed



    public final byte XON = 0x11;    /* Resume transmission */
    public final byte XOFF = 0x13;    /* Pause transmission */

    private final int BAUD = 57600;

    Handler mHandler = new Handler();

    public RadioIO(Context context) {

        this.global_context = context;



        mUsbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    // never come here(when attached, go to onNewIntent)
                    openDevice();
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    closeDevice();
                }
            }
        };

        // Initialize USB socket
        try {
            ftD2xx = D2xxManager.getInstance(global_context);
        } catch (D2xxManager.D2xxException e) {
            Log.e("FTDI_HT", "getInstance fail!!");
        }
        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);


        global_context.registerReceiver(mUsbReceiver, filter);

    }

    private Runnable mLoop = new Runnable(){
        @Override
        public void run(){
            int readSize;
            mThreadIsStopped = false;

            while (true) {
                if(mThreadIsStopped) {
                    break;
                }
                synchronized (ftDev) {

                    /*try {
                        //should probably lower this
                        Thread.sleep(500);
                    } catch (Exception e) {
                        Log.e("TEL", "Error trying to sleep");
                    }*/


                    readSize = ftDev.getQueueStatus();
                    if (readSize > 0) {

                        if (readSize > READBUF_SIZE)
                            readSize = READBUF_SIZE;

                        ftDev.read(buffer, readSize);

                        for(int i=0; i<telemetry.length; i++) {
                            telemetry[i] = (double)buffer[i*8];
                        }

                       // ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

                        //for (int i = 0; i < telemetry.length; i++)

                            //telemetry[i] = (double)buffer[i*8];

                            //getDouble seems to give you really weird numbers
                            //telemetry[i] = byteBuffer.getDouble(i * 8);
                            //telemetry[i] = byteBuffer.get(i*8);


                            setTelemetry(telemetry);

                    /*mHandler.post(new Runnable() {
                        @Override
                                public void run() {

                                    setTelemetry(telemetry);

                        }
                    });*/
                    }
                }
            }
        }
    };

    /*public void run() {
        if(!ftDev.isOpen()) {
            Log.e(TAG, "onClickWrite : Device is not open");
            return;
        }

        mThreadIsStopped = false;
        synchronized (ftDev) {
            // Check if buffer has data
            // If there's data, read into local buffer
            readSize = ftDev.getQueueStatus();
            //Log.v("Buffer Size", Double.toString(readSize));

            ftDev.read(buffer, readSize);
            byteArrayToTelemetry(buffer, telemetry);

            // Read newest data into telemetry object
        }

    }*/

   /* private void byteArrayToTelemetry(byte[] array, double telemetry[]) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        for (int i = 0; i < telemetry.length; i++)
            telemetry[i] = byteBuffer.getDouble(i * 8);
            setTelemetry(telemetry);
    }*/

    private void setTelemetry(double[] telem) {

        planeLong = telem[LONGITUDE];
        planeLat = telem[LATITUDE];
        planeAlt = telem[ALTITUDE];
        planeTime = telem[TIME];
        planeHeading = telem[YAW];
        planePitch = telem[PITCH];
        planeRoll = telem[ROLL];
        planeDistance = telem[DIST];

        //currentTime = System.currentTimeMillis() - startTime;   // time

    }


    @Override
    protected void onNewIntent(Intent intent) {
        openDevice();
    }


    void setConfig() {
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET); // reset to UART mode for 232 devices
        ftDev.setBaudRate(BAUD);
        ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1,
                D2xxManager.FT_PARITY_NONE);
        ftDev.setFlowControl(D2xxManager.FT_FLOW_RTS_CTS, XON, XOFF);
    }

    protected void openDevice() {
        if (ftDev != null) {
            if (ftDev.isOpen()) {
                if(mThreadIsStopped) {
                //updateView(true);
                setConfig();
                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                ftDev.restartInTask();
                new Thread(mLoop).start();

                }
                return;
            }
        }

        int devCount = 0;
        devCount = ftD2xx.createDeviceInfoList(global_context);

        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
        ftD2xx.getDeviceInfoList(devCount, deviceList);

        if (devCount <= 0) {
            //Error catching
            return;
        }

        if (ftDev == null) {
            ftDev = ftD2xx.openByIndex(global_context, 0);
        } else {
            synchronized (ftDev) {
                ftDev = ftD2xx.openByIndex(global_context, 0);
            }
        }

        if (ftDev.isOpen()) {
            if(mThreadIsStopped) {
                setConfig();
                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                ftDev.restartInTask();
            new Thread(mLoop).start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Log.v(TAG, "Telemetry Destroyed!");
        mThreadIsStopped = true;
        global_context.unregisterReceiver(mUsbReceiver);
    }

/*    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mUsbReceiver);
    };*/
    public void closeDevice() {
        mThreadIsStopped = true;
        telemetryOpen = false;
        //updateView(false);
        if (ftDev != null) {
            ftDev.close();
            try {
                bfWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets up the USB socket if not already opened. Checks for connectivity
     * and then opens 'port 0' since there is only one USB port on the
     * device. This step is critical for communication with the plane.
     */
    protected void setUpUsbIfNeeded() {
        // Check if already connected
        if (telemetryOpen) {
            String msg = "Port("+port+") is already opened.";
            Toast.makeText(global_context, msg, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check whether there is a device plugged in
        if (ftD2xx.createDeviceInfoList(global_context) < 1) {
            String msg = "Connect the USB radio";
            Toast.makeText(global_context, msg, Toast.LENGTH_SHORT).show();
            return;
        }

        // Open the device on port 0 (USB radio by default)
        ftDev = ftD2xx.openByIndex(global_context, 0);

        // Check for successful connection
        if (ftDev == null) {
            String msg = "Connect the USB radio";
            Toast.makeText(global_context, msg, Toast.LENGTH_SHORT).show();
            return;
        }

        setConfig();
        Toast.makeText(global_context, "Connected", Toast.LENGTH_SHORT).show();
        openDevice();
        telemetryOpen = true;
    }

 /*   public void payloadToggle(boolean dropLoad)
    {

        if(ftDev == null){
            return;
        }

        if (!ftDev.isOpen()) {
            String msg = "Device not open!";
            Toast.makeText(global_context, msg, Toast.LENGTH_SHORT).show();
            return;
        }


        synchronized (ftDev) {
            if(!ftDev.isOpen()) {
                Log.e(TAG, "onClickWrite : Device is not open");
                return;
            }

            ftDev.setLatencyTimer((byte)16);

            //payload toggle command logic
            if (dropLoad) {
                ftDev.write(drop, drop.length);
                Log.v(TAG,"DROPPED");
                payload = false;
            }
            else {
                ftDev.write(load, load.length);
                Log.v(TAG, "LOADED");
                payload = true;
            }
        }
    }

*/



}

