package com.example.nfc1;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbPermissionManager {

    private static final String TAG = "UsbPermissionManager";
    private static final String ACTION_USB_PERMISSION = "com.example.nfc1.USB_PERMISSION";

    private final Context context;
    private final UsbManager usbManager;

    public UsbPermissionManager(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public void requestUsbPermission(UsbDevice usbDevice) {
        if (usbDevice != null) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            context.registerReceiver(usbReceiver, filter);
            usbManager.requestPermission(usbDevice, permissionIntent);
        } else {
            Log.e(TAG, "USB device is null");
        }
    }

    public void unregisterReceiver() {
        try {
            context.unregisterReceiver(usbReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
    }

    private final UsbReceiver usbReceiver = new UsbReceiver();

    private class UsbReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "USB Permission granted.");
                        if (usbDevice != null) {
                            // You can proceed with connecting to the USB device here if needed
                            // For example: connectToUsbDevice(usbDevice);
                        }
                    } else {
                        Log.d(TAG, "USB Permission denied.");
                    }
                    unregisterReceiver();  // Unregister the receiver after handling permission result
                }
            }
        }
    }
}
