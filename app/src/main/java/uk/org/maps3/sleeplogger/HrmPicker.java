package uk.org.maps3.sleeplogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class HrmPicker extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private String TAG ="HrmPicker";
    private static final long SCAN_PERIOD = 2000;
    private Scanner scanner = null;
    private BluetoothAdapter bluetoothAdapter;
    private DeviceListAdapter deviceListAdapter;
    private Handler mHandler = null;
    private Timer mUiTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        setContentView(R.layout.activity_hrm_picker);
        ((TextView)(findViewById(R.id.statusTextView))).setText("Stopped");

        // create handler to deal with updating UI from different thread.
        mHandler = new Handler();


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported!!!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toast.makeText(this, "Looks like bluetooth works!!", Toast.LENGTH_SHORT).show();

        // Initializes a Bluetooth adapter.  For API level 18 and above (4.3), get a reference to
        // BluetoothAdapter through BluetoothManager.
        deviceListAdapter = new DeviceListAdapter();
        Log.v(TAG,"Context.BLUETOOTH_SERVICE = "+Context.BLUETOOTH_SERVICE);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth Not Supported!!!!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (scanner == null) {
            scanner = new Scanner(bluetoothAdapter, mLeScanCallback);
            scanner.startScanning();
        }

        ListView listView = (ListView)findViewById(R.id.devicesListView);
        listView.setAdapter(deviceListAdapter);
        listView.setOnItemClickListener(this);

        // Create timer to update the UI
        mUiTimer = new Timer();

        // A timer task to update the UI based on the status of hte scanner.
        TimerTask uiTimerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (scanner.isScanning()) {
                            ((TextView) (findViewById(R.id.statusTextView))).setText("Scanning....");
                            ((ProgressBar)(findViewById(R.id.progressBar))).setIndeterminate(true);
                        }
                        else {
                            ((TextView) (findViewById(R.id.statusTextView))).setText("Scanner Stopped");
                            ((ProgressBar)(findViewById(R.id.progressBar))).setIndeterminate(false);

                        }
                    }
                });
            }
        };

        mUiTimer.schedule(uiTimerTask,0,1000);

    }

    @Override
    protected void onRestart() {
        if (scanner!=null) scanner.startScanning();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        if (scanner!=null) scanner.stopScanning();
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (scanner!=null) scanner.stopScanning();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (scanner!=null) scanner.stopScanning();
        super.onDestroy();
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hrm_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = (BluetoothDevice)deviceListAdapter.getItem(position);
        String msgStr = "selected position "+position+" device=" +
                device.getName();
        Log.v(TAG,"onItemClick - "+msgStr);
        Toast.makeText(this, "Selected "+msgStr, Toast.LENGTH_SHORT).show();
        Intent output = new Intent();
        output.putExtra("hrmName", device.getName());
        output.putExtra("hrmAddr", device.getAddress());
        setResult(RESULT_OK, output);
        finish();

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "LeScanCallback - device=" + device.getName());
                            deviceListAdapter.addDevice(device);
                            deviceListAdapter.notifyDataSetChanged();

                        }
                    });
                }
            };


    /*************************************
     * Class to scan for BLE devices as a background process.
     */
    private static class Scanner extends Thread {
        private final BluetoothAdapter bluetoothAdapter;
        private final BluetoothAdapter.LeScanCallback mLeScanCallback;

        private volatile boolean isScanning = false;

        Scanner(BluetoothAdapter adapter, BluetoothAdapter.LeScanCallback callback) {
            bluetoothAdapter = adapter;
            mLeScanCallback = callback;
        }

        public boolean isScanning() {
            return isScanning;
        }

        public void startScanning() {
            synchronized (this) {
                isScanning = true;
                if (getState() == Thread.State.NEW) start();
            }
        }

        public void stopScanning() {
            synchronized (this) {
                isScanning = false;
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (this) {
                        if (!isScanning)
                            break;

                        bluetoothAdapter.startLeScan(mLeScanCallback);
                    }
                    sleep(SCAN_PERIOD);
                    synchronized (this) {
                        bluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
            } catch (InterruptedException ignore) {
            } finally {
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList <BluetoothDevice> deviceList;

        public DeviceListAdapter() {
            deviceList = new ArrayList<BluetoothDevice>();
        }

        public void addDevice(BluetoothDevice device) {
            if (!deviceList.contains(device)) deviceList.add(device);
        }

        @Override
        public int getCount() {
            return deviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            BluetoothDevice d = deviceList.get(position);
            tv.setText(d.getName() + " : " + d.getAddress());
            return tv;
        }
    }
}
