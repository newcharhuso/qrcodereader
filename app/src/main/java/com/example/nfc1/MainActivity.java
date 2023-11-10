package com.example.nfc1;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigInteger;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "USB_APP";
    private static final String ACTION_USB_PERMISSION = "com.example.yourapp.USB_PERMISSION";
    private static final int TIMEOUT_IN_MILLIS = 1000; // Adjust this based on your requirements

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbDeviceConnection;
    private TextView dataTextView;
    private TextView dataView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_main);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        dataTextView = findViewById(R.id.dataTextView);
        dataView = findViewById(R.id.dataView);

        registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        Log.d(TAG, "onCreate: Initializing USB manager and UI elements");

        // Wait for the specific USB device to be connected
        waitForUsbDevice();
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.d(TAG, String.valueOf(usbDevice));
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "USB Permission granted.");
                        if (usbDevice != null) {
                            connectToUsbDevice();
                        }
                    } else {

                        Log.d(TAG, "USB Permission denied.");
                    }
                }
            }
        }
    };
    public String toHex(String arg) {
    return String.format("%040x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
}
    private void waitForUsbDevice() {
        Log.d(TAG, "waitForUsbDevice: Waiting for USB device...");

        int YOUR_VENDOR_ID = 1155;  // Replace with your actual Vendor ID
        int YOUR_PRODUCT_ID = 22315; // Replace with your actual Product ID

        while (true) {
            // Check for the presence of the USB device
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                Log.d(TAG,"Found a device - Vendor ID:" + device.getVendorId() + "Product ID:"+ device.getProductId());
                if (device.getVendorId() == YOUR_VENDOR_ID && device.getProductId() == YOUR_PRODUCT_ID) {
                    usbDevice = device;
                    Log.d(TAG, "waitForUsbDevice: Found USB device - Vendor ID: " + device.getVendorId() +
                            ", Product ID: " + device.getProductId());

                    // Request permission and proceed with connection
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                    usbManager.requestPermission(usbDevice, permissionIntent);
                    return;
                }
            }

            // Sleep for a short duration before checking again
            try {
                Thread.sleep(1000); // Adjust the sleep duration as needed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    private void connectToUsbDevice() {
        Log.d(TAG, "connectToUsbDevice: Searching for USB devices...");
        int YOUR_VENDOR_ID = 1155;  // Replace with your actual Vendor ID
        int YOUR_PRODUCT_ID = 22315; // Replace with your actual Product ID

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getVendorId() == YOUR_VENDOR_ID && device.getProductId() == YOUR_PRODUCT_ID) {
                usbDevice = device;

                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(usbDevice, permissionIntent);
                Log.d(TAG, "connectToUsbDevice: Found USB device - Vendor ID: " + device.getVendorId() +
                        ", Product ID: " + device.getProductId());
                Log.d(TAG, device.getVendorId() + " " +  device.getProductId());
                dataView.setText("connectToUsbDevice: Matching USB device found - Requesting permission.");

                if (usbManager.hasPermission(usbDevice)) {
                    usbDeviceConnection = usbManager.openDevice(usbDevice);

                    if (usbDeviceConnection != null) {
                        UsbInterface usbInterface = usbDevice.getInterface(0);
                        if (usbDeviceConnection.claimInterface(usbInterface, true)) {
                            UsbEndpoint endpoint = usbInterface.getEndpoint(0);

                            // Buffer for receiving data
                            byte[] buffer = new byte[64];

                            // Read data from the USB device
                            int bytesRead = usbDeviceConnection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT_IN_MILLIS);

                            if (bytesRead > 0) {
                                String hexData = byteArrayToHexString(Arrays.copyOfRange(buffer, 0, bytesRead));
                                Log.d(TAG, "Received data from USB device: " + hexData);
                                // Display the received data in the UI (replace with your specific UI update logic)
                                dataView.setText("Received data: " + hexData);
                            } else {
                                Log.e(TAG, "Error reading data from USB device");
                            }

                            usbDeviceConnection.releaseInterface(usbInterface);
                        } else {
                            Log.e(TAG, "Error claiming USB interface");
                        }

                        usbDeviceConnection.close();
                    } else {
                        Log.e(TAG, "Error opening USB device connection");
                    }
                } else {
                    Log.e(TAG, "Permission denied for USB device");
                }

                break;
            }
        }
    }

    private String byteArrayToHexString(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString().trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Unregistered USB receiver");
        unregisterReceiver(usbReceiver);
    }
}