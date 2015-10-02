package uk.org.maps3.sleeplogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

interface BleHrmMonitorListener {
    public void onHrmDataReceived(int type, int data, String msg);
}

/**
 * A class that will connect with a BLE Heart Rate Monitor and send notifications to a BleHrmMonitorListener when heart rate data is received.
 * Created by graham on 01/10/15.
 */
public class BleHrmMonitor {
    public final static int TYPE_DATA = 1;   // A message containing HRM data
    public final static int TYPE_CONNECTION = 2;  // A message describing the connection state.
    public final static int TYPE_READY = 3;       // A message saying the device is ready (HRM service discovered).
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    private String TAG = "BleHrmMonitor";
    private String mHrmAddr = null;
    private String mHrmName = null;
    private Context mContext = null;
    private BleHrmMonitorListener mCallback = null;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mAdapter;
    private BluetoothGatt mGatt;
    private List<BluetoothGattService> mGattServices;
    private BluetoothGattCharacteristic mHrmCharacteristic;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;



    public BleHrmMonitor(Context context, String address, BleHrmMonitorListener callback) {
        Log.v(TAG,"BleHrmMonitor() Constructor");
        mContext = context;
        mHrmAddr = address;
        mCallback = callback;
        if (mBluetoothManager == null) {
            Log.v(TAG,"Context.BLUETOOTH_SERVICE = "+ Context.BLUETOOTH_SERVICE);
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
            }
        }
        mAdapter = mBluetoothManager.getAdapter();
        if (mAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        } else {
            Log.v(TAG,"Created Bluetooth Adapter ok.");
        }

        if (mHrmAddr!=null) {
            // Connect to the requested HRM.
            Log.v(TAG,"Connecting to requested device "+mHrmAddr);

        } else {
            // Connect to the first HRM we find.
            Log.v(TAG,"Looking for a HRM device...(FIXME - not implemented yet!!!!)");

        }
        start();

    }

    public void start() {
        Log.v(TAG,"start()");
        BluetoothDevice device = mAdapter.getRemoteDevice(mHrmAddr);
        mGatt = device.connectGatt(mContext, false, mGattCallback);
    }

    public void stop() {
        Log.v(TAG,"stop()");
        mGatt.disconnect();
    }

    private void listServices() {
        Log.v(TAG,"listServices()");
        for (BluetoothGattService gattService : mGattServices) {
            Log.v(TAG,"Service "+gattService.toString());
            Log.v(TAG,"Service - "+SampleGattAttributes.lookup(gattService.getUuid().toString(), "unknown"));
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.v(TAG,"  -  Characteristic "+gattCharacteristic.toString());
                Log.v(TAG,"  -  Characteristic - "+SampleGattAttributes.lookup(gattCharacteristic.getUuid().toString(),"unknown"));
            }
        }

    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.v(TAG,"onCharacteristicChanged() - characteristic = "+characteristic.toString());
            // This is special handling for the Heart Rate Measurement profile. Data
            // parsing is carried out as per profile specifications.
            Log.v(TAG, "UUID=" + characteristic.getUuid());
            if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(TAG, "Heart rate format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(TAG, "Heart rate format UINT8.");
                }
                final int heartRate = characteristic.getIntValue(format, 1);
                Log.d(TAG, String.format("Received heart rate: %d", heartRate));
                mCallback.onHrmDataReceived(TYPE_DATA, heartRate, String.format("Received heart rate: %d", heartRate));
            }

            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "onCharacteristicRead()");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "Characteristic Read OK - " + characteristic.toString());
                // This is special handling for the Heart Rate Measurement profile. Data
                // parsing is carried out as per profile specifications.
                Log.v(TAG, "UUID=" + characteristic.getUuid());
                if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        Log.d(TAG, "Heart rate format UINT16.");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        Log.d(TAG, "Heart rate format UINT8.");
                    }
                    final int heartRate = characteristic.getIntValue(format, 1);
                    Log.d(TAG, String.format("Received heart rate: %d", heartRate));
                }

                super.onCharacteristicRead(gatt, characteristic, status);
            }
        }

        /**
         * onConnectionState Change - handle connection changes
         * @param gatt
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.v(TAG,"onConnectionStateChange()");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG,"Connected!");
                mConnectionState = STATE_CONNECTED;
                mCallback.onHrmDataReceived(TYPE_CONNECTION,1,"Connected");
                // Start service discovery
                mGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v(TAG,"Disconnected");
                mConnectionState = STATE_DISCONNECTED;
                mCallback.onHrmDataReceived(TYPE_CONNECTION,0,"Disconnected");
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.v(TAG, "onservicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG,"Successful service discovery");
                mGattServices = gatt.getServices();
                // Now we have discovered all the services, list them for debugging, and ask for
                // notifications of Heart Rate Measurement changes.
                listServices();
                mHrmCharacteristic = getHrmCharacteristic();
                if (UUID_HEART_RATE_MEASUREMENT.equals(mHrmCharacteristic.getUuid())) {
                    Log.v(TAG,"Check OK - characteristic is a HRM UUID");
                } else {
                    Log.e(TAG,"****ERROR - characteristic does not have the HRM UUID!!");
                }
                mGatt.setCharacteristicNotification(mHrmCharacteristic, true);
                BluetoothGattDescriptor descriptor = mHrmCharacteristic.getDescriptor(
                        UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mGatt.writeDescriptor(descriptor);
                mCallback.onHrmDataReceived(TYPE_READY, 1, "Ready - HRM Service found.");
            } else {
                Log.e(TAG,"Service discovery failed - something will not work!");
                mGattServices = null;
            }
            super.onServicesDiscovered(gatt, status);
        }

        private BluetoothGattCharacteristic getHrmCharacteristic() {
            // Loop through the services and associated characteristics, and return the heart rate measurement
            // characteristic.
            Log.v(TAG, "getHrmCharacteristic()");
            for (BluetoothGattService gattService : mGattServices) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (SampleGattAttributes.lookup(gattCharacteristic.getUuid().toString(),"unknown").equals("Heart Rate Measurement")) {
                        Log.v(TAG,"Returning HRM Characteristic - "+gattCharacteristic.toString());
                        return gattCharacteristic;
                    }
                }
            }
            return null;
        }


    };

}
