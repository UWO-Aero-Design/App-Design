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
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RadioIO implements Runnable {
    private FT_Device ftDev;
    private D2xxManager ftD2xx = null;
    private TelemetryNew telemetry;

    protected BroadcastReceiver mUsbReceiver;
    protected IntentFilter filter;

    Context global_context;

    public static final int READBUF_SIZE  = 256;
    private byte[] buffer  = new byte[READBUF_SIZE];
    public boolean mThreadIsStopped = true;
    int readSize;


    public final byte XON = 0x11;    /* Resume transmission */
    public final byte XOFF = 0x13;    /* Pause transmission */

    private final int BAUD = 57600;




    public RadioIO (Context context) {

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
        }
        catch (D2xxManager.D2xxException e) {
            Log.e("FTDI_HT", "getInstance fail!!");
        }
        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        global_context.registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void run() {
        while (ftDev.isOpen()) {
            // Check if buffer has data
            // If there's data, read into local buffer
            readSize = ftDev.getQueueStatus();
            ftDev.read(buffer, readSize);
            byteArrayToTelemetry(buffer, telemetry);

            // Read newest data into telemetry object
        }
    }

    private void byteArrayToTelemetry(byte[] array, TelemetryNew telemetry) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        for (int i = 0; i < telemetry.data.length; i++)
            telemetry.data[i] = byteBuffer.getDouble(i*8);
    }

    public TelemetryNew getTelemetry() {
        return telemetry;
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
        if(ftDev != null) {
            if(ftDev.isOpen()) {
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

        if(devCount <= 0) {
            return;
        }

        if(ftDev == null) {
            ftDev = ftD2xx.openByIndex(global_context, 0);
        } else {
            synchronized (ftDev) {
                ftDev = ftD2xx.openByIndex(global_context, 0);
            }
        }

        if(ftDev.isOpen()) {
            if(mThreadIsStopped) {
                setConfig();
                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                ftDev.restartInTask();
                //new Thread(mLoop).start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Telemetry Destroyed!");
        mThreadIsStopped = true;
        global_context.unregisterReceiver(mUsbReceiver);
    }

    public void closeDevice() {
        mThreadIsStopped = true;
        telemetryOpen = false;
        //updateView(false);
        if(ftDev != null) {
            ftDev.close();
            try {
                bfWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
