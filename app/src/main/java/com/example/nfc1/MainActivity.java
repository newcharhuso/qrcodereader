// MainActivity.java
package com.example.nfc1;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USB_APP";
    private static final String ACTION_USB_PERMISSION = "com.example.nfc1.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private TextView dataTextView;

    private final UsbReceiver usbReceiver = new UsbReceiver();
    ;
    // instantiate the receiver

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_main);


        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        dataTextView = findViewById(R.id.dataTextView);


        Log.d(TAG, "onCreate: Initializing USB manager and UI elements");

        // Register USB receiver
        Log.d(TAG, "Registering USB receiver...");
        registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        Log.d(TAG, "USB receiver registered");

        // Wait for the specific USB device to be connected
        waitForUsbDevice();


    }


    private void waitForUsbDevice() {
        Log.d(TAG, "waitForUsbDevice: Waiting for USB device...");

        int YOUR_VENDOR_ID = 1155;  // Replace with your actual Vendor ID
        int YOUR_PRODUCT_ID = 22315; // Replace with your actual Product ID

        while (true) {
            // Check for the presence of the USB device
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                Log.d(TAG, "Found a device - Vendor ID:" + device.getVendorId() + " Product ID:" + device.getProductId());
                if (device.getVendorId() == YOUR_VENDOR_ID && device.getProductId() == YOUR_PRODUCT_ID) {
                    usbDevice = device;
                    Log.d(TAG, "waitForUsbDevice: Found USB device - Vendor ID: " + device.getVendorId() +
                            ", Product ID: " + device.getProductId());

                    // Request permission and proceed with connection
                    Log.d(TAG, "Requesting USB permission...");
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                    usbManager.requestPermission(usbDevice, permissionIntent);
                    Log.d(TAG, "USB permission requested");
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

    public void updateUI(final String s)
    {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView  textView=(TextView) findViewById(R.id.dataTextView);
                textView.setText(s);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Unregistered USB receiver");
        unregisterReceiver(usbReceiver);
    }

}