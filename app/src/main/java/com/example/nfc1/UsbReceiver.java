package com.example.nfc1;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

public class UsbReceiver extends BroadcastReceiver {
    private static final String TAG = "USB_APP";
    private static final int TIMEOUT_IN_MILLIS = 30000; // Adjust this based on your requirements

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.example.nfc1.USB_PERMISSION".equals(action)) {
            synchronized (this) {
                // Instead of relying on USB_PERMISSION broadcast, use vendor and product IDs
                UsbDevice usbDevice = findUsbDevice(context);
                if (usbDevice != null) {
                    connectToUsbDevice(context, usbDevice);
                }
            }
        }
    }

    private UsbDevice findUsbDevice(Context context) {
        int knownVendorId = 7851; // Replace with your actual vendor ID
        int knownProductId = 6672; // Replace with your actual product ID

        // Access the UsbManager
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        // Get a list of connected USB devices
        for (UsbDevice device : usbManager.getDeviceList().values()) {
        // Check if the device matches the known vendor and product IDs
        if (device.getVendorId() == knownVendorId && device.getProductId() == knownProductId) {
            return device;
        }
    }
        return null;
    }

    private void connectToUsbDevice(Context context, UsbDevice usbDevice) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);

        try {
            Log.d(TAG, "Attempting to open USB device connection...");
            if (usbDeviceConnection != null) {
                Log.d(TAG, "USB device connection opened successfully.");
            } else {
                Log.e(TAG, "usbDeviceConnection is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening USB device connection: " + e.getMessage());
            e.printStackTrace();
        }

        if (usbDeviceConnection != null) {
            int interfaceCount = usbDevice.getInterfaceCount();
            for (int interfaceIndex = 0; interfaceIndex < interfaceCount; interfaceIndex++) {
                UsbInterface usbInterface = usbDevice.getInterface(interfaceIndex);

                // Check if this is the desired interface
                if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_HID) {
                    if (usbDeviceConnection.claimInterface(usbInterface, true)) {
                        int endpointCount = usbInterface.getEndpointCount();
                        for (int endpointIndex = 0; endpointIndex < endpointCount; endpointIndex++) {
                            UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);

                            // Check if this is an Interrupt endpoint
                            Log.d(TAG, "Checking for endpoint");
                            if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                // Continue with the existing logic for data transfer

                                // Buffer for receiving data
                                byte[] buffer = new byte[endpoint.getMaxPacketSize()];
                                int continuity = 1;
                                StringBuilder pack = new StringBuilder("");
                                while (true) {
                                    // Read data from the USB device
                                    pack = new StringBuilder();
                                    while (continuity == 1) {
                                        int bytesRead = usbDeviceConnection.bulkTransfer(
                                                endpoint,
                                        buffer,
                                        buffer.length,
                                        TIMEOUT_IN_MILLIS
                                        );
                                        if (bytesRead > 0) {
                                            Log.d(TAG, Arrays.toString(buffer));
                                            for (int c = 2; c <= 56; c++) {
                                                pack.append(Integer.toHexString(Byte.toUnsignedInt(buffer[c])));
                                            }
                                            continuity = buffer[63] & 1;
                                            Log.d(TAG, "Last bit of buffer[63]: " + (buffer[63] & 1));
                                            for (int d = 2; d <= 56; d++) {
                                                pack.append(Integer.toHexString(Byte.toUnsignedInt(buffer[d])));
                                            }
                                            Log.d(TAG, "pack = " + pack);
                                        } else if (bytesRead < 0) {
                                            // Log an error if an error occurs during data transfer
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    }
                                    byte[] byteData = hexStringToByteArray(pack.toString());
                                    String asciiData = new String(byteData, StandardCharsets.UTF_8);
                                    pack.setLength(0);
                                    ContentValues values = new ContentValues();
                                    String lastAsciiData = asciiData.substring(55, 142);
                                    values.put("asciiData", lastAsciiData);
                                    Log.d(TAG, "Received data : " + lastAsciiData);
                                    context.getContentResolver().insert(
                                        Uri.parse("content://com.example.nfc1.provider/asciiData"),
                                        values
                                    );
                                    continuity = 1;
                                }
                            }
                        }
                        usbDeviceConnection.releaseInterface(usbInterface);
                    } else {
                        Log.e(TAG, "Error claiming USB interface");
                    }
                }
            }
            usbDeviceConnection.close();
        } else {
            Log.e(TAG, "Error opening USB device connection");
        }
    }

    private byte[] hexStringToByteArray(String hex) {
        byte[] result = new byte[hex.length() / 2];

        for (int i = 0; i < hex.length(); i += 2) {
        int byteValue = Integer.parseInt(hex.substring(i, i + 2), 16);
        result[i / 2] = (byte) byteValue;
    }

        return result;
    }
}
