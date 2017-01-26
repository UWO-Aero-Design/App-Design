/**
 * Responsible for handling input from the radio's buffer and turning that
 * into telemetry data which can be polled by other components of the app.
 */

package com.example.jai.googlemapstest;

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

    public static final int READBUF_SIZE  = 256;
    private byte[] buffer  = new byte[READBUF_SIZE];
    int readSize;

    @Override
    public void run() {
        while (ftDev.isOpen()) {
            // Check if buffer has data
            // If there's data, read into local buffer
            readSize = ftDev.getQueueStatus();
            ftDev.read(buffer, readSize);
            //byte[] buffer = {'H', 'e', 'l', 'l', 'o'};
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
                new Thread(mLoop).start();
            }
        }
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
