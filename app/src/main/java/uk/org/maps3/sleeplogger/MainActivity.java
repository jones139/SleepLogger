package uk.org.maps3.sleeplogger;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SleepLoggerListener {
    private String TAG = "MainActivity";
    private ImageButton mStartStopButton = null;
    private LoggerService mLoggerService = null;
    private boolean mBound = false;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.v(TAG, "onServiceConnected()");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LoggerService.LocalBinder binder = (LoggerService.LocalBinder) service;
            mLoggerService = binder.getService();
            mLoggerService.setCallback(MainActivity.this);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisconnected()");
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartStopButton = (ImageButton) findViewById(R.id.startStopButton);
        mStartStopButton.setOnClickListener(this);

        SharedPreferences sp = getSharedPreferences("SleepLogger",0);
        String hrmAddr = sp.getString("hrmAddr", null);
        String hrmName = sp.getString("hrmName", null);
        if (hrmAddr == null) {
            Toast.makeText(this, "No HRM Device Selected - Please select one.", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this,HrmPicker.class);
            startActivityForResult(i, 1);
            ((TextView)findViewById(R.id.hrmTextView)).setText("No Device Selected");
        } else {
            ((TextView)findViewById(R.id.hrmTextView)).setText(hrmName+" : "+hrmAddr);
        }
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop()");
        super.onStop();
        // Unbind from the service
        if (mBound) {
            Log.v(TAG, "unbinding fom service...");
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
    }

    private void startLoggerService() {
        Log.v(TAG, "startLoggerService()");
        Intent intent = new Intent(this, LoggerService.class);
        if (!isServiceRunning()) {
            Log.v(TAG, "loggerservice not running - starting it");
            startService(intent);
        } else {
            Log.v(TAG, "LoggerService already running, just binding to it");
        }
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopLoggerService() {
        Log.v(TAG, "stopLoggerService()");
        // Unbind from the service
        if (mBound) {
            Log.v(TAG, "unbinding fom service...");
            unbindService(mConnection);
            mBound = false;
        } else {
            Log.v(TAG, "service not bound - ignoring");
        }
        Intent intent = new Intent(this, LoggerService.class);
        if (!isServiceRunning()) {
            Log.v(TAG, "loggerservice not running - ignoring");
        } else {
            Log.v(TAG, "LoggerService running, stopping it");
            stopService(intent);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // requestCode 1 is the selected BLE Heart Rate Monitor.
        if (requestCode == 1) {
            if (resultCode==RESULT_OK) {
                Bundle b = data.getExtras();
                String hrmName = b.getString("hrmName");
                String hrmAddr = b.getString("hrmAddr");
                Toast.makeText(this, "Received Data - " + hrmName, Toast.LENGTH_SHORT).show();
                SharedPreferences sp = getSharedPreferences("SleepLogger", 0);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("hrmAddr", hrmAddr);
                editor.putString("hrmName", hrmName);
                editor.commit();
                ((TextView)findViewById(R.id.hrmTextView)).setText(hrmName + " : " + hrmAddr);
            } else {
                Toast.makeText(this, "Result not RESULT_OK - failed", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Log.v(TAG, "action_settings");
                return true;
            case R.id.action_selectHrm:
                Log.v(TAG, "action_selectHrm");
                Intent i = new Intent(this,HrmPicker.class);
                startActivityForResult(i, 1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == (mStartStopButton)) {
            Log.v(TAG, "onClick - mStartStopButton Pressed");
            toggleService();
        }
    }

    @Override
    public void onSleepLoggerStatusChanged(final int type, final int data, final String msg) {
        Log.v(TAG, "onSleepLoggerStatusChanged() - data=" + data + " - msg=" + msg);
        // This function may be called from a worker thread, so make sure we do the UI updates on the UI thread.
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                if (type == LoggerService.TYPE_DATA) {
                    ((TextView) findViewById(R.id.hrValueTextView)).setText(data + " bpm");
                }
                if (type == LoggerService.TYPE_CONNECTION) {
                    if (data == 0) {
                        ((TextView) findViewById(R.id.hrValueTextView)).setText("--- bpm");
                    }
                }
            }
        });
    }

    private void toggleService() {
        Log.v(TAG, "toggleService()");
        if (mBound) {
            Log.v(TAG, "LoggerService running - stopping it...");
            stopLoggerService();
            mStartStopButton.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
        } else {
            Log.v(TAG, "LoggerService not running - starting it...");
            startLoggerService();
            mStartStopButton.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("uk.org.maps3.sleeplogger.LoggerService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
