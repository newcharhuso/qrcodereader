package com.example.nfc1;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UsbReceiver extends BroadcastReceiver {
    private MainActivity instance;
    private static final String TAG = "USB_APP";
    private static final int TIMEOUT_IN_MILLIS = 30000; // Adjust this based on your requirements
    private Set<String> uniqueReads = new HashSet<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.example.nfc1.USB_PERMISSION".equals(action)) {
            synchronized (this) {
                // Instead of relying on USB_PERMISSION broadcast, use vendor and product IDs

                UsbDevice usbDevice = findUsbDevice(context);
                if (usbDevice != null) {
                    initializeUniqueReads(context);
                    connectToUsbDevice(context, usbDevice);
                }
            }
        }
    }

    private UsbDevice findUsbDevice(Context context) {
        int knownVendorId = 0x0483; // Replace with your actual vendor ID
        int knownProductId = 0x0572b; // Replace with your actual product ID

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
                            if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                                // Continue with the existing logic for data transfer

                                // Buffer for receiving data
                                byte[] buffer = new byte[endpoint.getMaxPacketSize()];

                                while (true) {
                                    // Read data from the USB device
                                    int bytesRead = usbDeviceConnection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT_IN_MILLIS);

                                    if (bytesRead > 0) {
                                        // Process and log the received data if it's unique
                                        String hexData = byteArrayToHexString(Arrays.copyOfRange(buffer, 0, bytesRead));
                                        Log.d(TAG,hexData);

                                        try{

                                            if (instance != null) {
                                                instance.updateUI("Some message");
                                            } else {
                                                // Handle the case when MainActivityInstance is null
                                                // You might want to log an error or take appropriate action.
                                                Log.d(TAG,"Insatnce is null");
                                            }

                                        }
                                        catch(Exception e){
                                            e.printStackTrace();
                                        }
                                        if (uniqueReads.add(hexData)) {
                                            // Log the unique read

                                            // Append the unique read to a log file
                                            writeToLogFile(context, hexData);
                                        }
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else if (bytesRead < 0) {
                                        // Log an error if an error occurs during data transfer

                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }

                                        // Add more error handling or diagnostics as needed
                                    }
                                    // No need for an else statement, since bytesRead == 0 is a valid case and will be handled in the next iteration
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

    private String byteArrayToHexString(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString().trim();
    }



    private void writeToLogFile(Context context, String logMessage) {
        try {
            // Define the file path
            File logFile = new File(context.getExternalFilesDir(null), "log.txt");

            // Create the log file if it doesn't exist
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            // Write the log message to the file
            FileWriter writer = new FileWriter(logFile, true); // 'true' to append to the file
            writer.write(logMessage + "\n");
            writer.flush();
            writer.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void initializeUniqueReads(Context context) {
        try {
            // Read existing data from the log file and add to uniqueReads set
            File logFile = new File(context.getExternalFilesDir(null), "log.txt");
            if (logFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Assuming the log file contains hexadecimal data, add it to the set
                    uniqueReads.add(line.trim());
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}




