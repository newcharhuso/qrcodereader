// MainActivity.java
package com.example.nfc1

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private val usbReceiver = UsbReceiver()

    // instantiate the receiver
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(USB_SERVICE) as UsbManager
        Log.d(TAG, "onCreate: Initializing USB manager and UI elements")

        // Register USB receiver
        Log.d(TAG, "Registering USB receiver...")
        registerReceiver(usbReceiver, IntentFilter(ACTION_USB_PERMISSION), RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "USB receiver registered")

        // Wait for the specific USB device to be connected
        waitForUsbDevice()
    }

    private fun waitForUsbDevice() {
        Log.d(TAG, "waitForUsbDevice: Waiting for USB device...")
        val YOUR_VENDOR_ID = 7851 // Replace with your actual Vendor ID
        val YOUR_PRODUCT_ID = 6672 // Replace with your actual Product ID
        while (true) {
            // Check for the presence of the USB device
            for (device in usbManager!!.deviceList.values) {
                Log.d(
                    TAG,
                    "Found a device - Vendor ID:" + device.vendorId + " Product ID:" + device.productId
                )
                if (device.vendorId == YOUR_VENDOR_ID && device.productId == YOUR_PRODUCT_ID) {
                    usbDevice = device
                    Log.d(
                        TAG, "waitForUsbDevice: Found USB device - Vendor ID: " + device.vendorId +
                                ", Product ID: " + device.productId
                    )

                    // Request permission and proceed with connection
                    Log.d(TAG, "Requesting USB permission...")
                    val permissionIntent = PendingIntent.getBroadcast(
                        this, 0, Intent(
                            ACTION_USB_PERMISSION
                        ), PendingIntent.FLAG_IMMUTABLE
                    )
                    usbManager!!.requestPermission(usbDevice, permissionIntent)
                    Log.d(TAG, "USB permission requested")
                    return
                }
            }

            // Sleep for a short duration before checking again
            try {
                Thread.sleep(1000) // Adjust the sleep duration as needed
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Unregistered USB receiver")
        unregisterReceiver(usbReceiver)
    }

    companion object {
        private const val TAG = "USB_APP"
        private const val ACTION_USB_PERMISSION = "com.example.nfc1.USB_PERMISSION"
    }
}