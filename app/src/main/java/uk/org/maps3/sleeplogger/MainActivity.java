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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SleepLoggerListener {
    private String TAG = "MainActivity";
    private LoggerService mLoggerService = null;
    //private ServiceConnection mConnection = null; /* defined below because declaring it here didn't work.. */
    private boolean mBound = false;
    private Timer mUiTimer = null;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        /**
         * Called when we have connected to a running service.
         *
         * @param className
         * @param service
         */
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

        /**
         * Called when we are disconnected from a running service.
         * @param arg0
         */
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
        findViewById(R.id.startStopButton).setOnClickListener(this);

        SharedPreferences sp = getSharedPreferences("SleepLogger", 0);
        String hrmAddr = sp.getString("hrmAddr", null);
        String hrmName = sp.getString("hrmName", null);
        if (hrmAddr == null) {
            Toast.makeText(this, "No HRM Device Selected - Please select one.", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, HrmPicker.class);
            startActivityForResult(i, 1);
            ((TextView) findViewById(R.id.hrmTextView)).setText("No Device Selected");
        } else {
            ((TextView) findViewById(R.id.hrmTextView)).setText(hrmName + " : " + hrmAddr);
        }
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart()");
        // We do not automatically start the service here, because the service is started manually from
        // the UI - see the onClick and onMenuOptionSelected functions.  But if it is running, we bind to it.
        if (isServiceRunning()) {
            Intent intent = new Intent(this, LoggerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        SharedPreferences sp = getSharedPreferences("SleepLogger", 0);
        int uiUpdatePeriod = sp.getInt("uiUpdatePeriod", 1000);

        // start timer to refresh user interface periodically.
        if (mUiTimer == null) {
            Log.v(TAG, "onstart(): starting mUiTimer with period " + uiUpdatePeriod + " ms");
            mUiTimer = new Timer();
            mUiTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateUi();
                }
            }, 0, uiUpdatePeriod);
            Log.v(TAG, "onStart(): started mUiTimer");
        } else {
            Log.v(TAG, "onStart(): mUiTimer already running");
        }
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
        // Note that we do not stop the backgorund service because we want it to continue to run after this activity exits.

        // Stop the User Interface timer
        if (mUiTimer != null) {
            Log.v(TAG, "onStop(): cancelling UI timer");
            mUiTimer.cancel();
            mUiTimer.purge();
            mUiTimer = null;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Set the correct text for the start/stop service menu item.  And change image button icon to be consistent.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem startStopMenuItem = menu.findItem(R.id.action_start_stop);
        ImageButton startStopButton = (ImageButton) findViewById(R.id.startStopButton);
        Log.v(TAG, "onPrepareOptionsMenu() - startStopMenuItem = " + startStopMenuItem);
        if (isServiceRunning()) {
            startStopMenuItem.setTitle("Stop Logging");
            startStopMenuItem.setIcon(R.drawable.ic_pause_circle_outline_white_24dp);
            startStopButton.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
        } else {
            startStopMenuItem.setTitle("Start Logging");
            startStopMenuItem.setIcon(R.drawable.ic_play_circle_outline_white_24dp);
            startStopButton.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Callback for menu item selections.
     *
     * @param item the MenuItem that was selected.
     * @return true if the event ws handled ok.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent i;

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Log.v(TAG, "action_settings");
                i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_selectHrm:
                Log.v(TAG, "action_selectHrm");
                i = new Intent(this, HrmPicker.class);
                startActivityForResult(i, 1);
                return true;
            case R.id.action_start_stop:
                Log.v(TAG, "action_start_stop");
                toggleService();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback function for buttons in the UI.
     *
     * @param v - the view (button etc) that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v == (findViewById(R.id.startStopButton))) {
            Log.v(TAG, "onClick - startStopButton Pressed");
            toggleService();
        }
    }

    /**
     * onActivityResult is called when an activity started with StartActivityForResult completes its work.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // requestCode 1 is the selected BLE Heart Rate Monitor.
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                String hrmName = b.getString("hrmName");
                String hrmAddr = b.getString("hrmAddr");
                Toast.makeText(this, "Received Data - " + hrmName, Toast.LENGTH_SHORT).show();
                SharedPreferences sp = getSharedPreferences("SleepLogger", 0);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("hrmAddr", hrmAddr);
                editor.putString("hrmName", hrmName);
                editor.commit();
                ((TextView) findViewById(R.id.hrmTextView)).setText(hrmName + " : " + hrmAddr);
            } else {
                Toast.makeText(this, "Result not RESULT_OK - failed", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * update user interface - called periodically by TimerTask started in onStart().
     */
    private void updateUi() {
        Log.v(TAG, "updateUi()");
    }

    /**
     * Starts or stops the LoggerService background service, depending on whether it is running or not.
     * Updates the UI to show the correct (start or stop) menu and button options.
     */
    private void toggleService() {
        Log.v(TAG, "toggleService()");
        if (mBound) {
            Log.v(TAG, "LoggerService running - stopping it...");
            stopLoggerService();
            invalidateOptionsMenu();  // forces a call to onPrepareOptionsMenu() to change menu options text.
        } else {
            Log.v(TAG, "LoggerService not running - starting it...");
            startLoggerService();
            invalidateOptionsMenu();  // forces a call to onPrepareOptionsMenu() to change menu options text.
        }
    }

    /**
     * start the LoggerService background service.
     */
    private void startLoggerService() {
        Log.v(TAG, "startLoggerService()");
        Intent intent = new Intent(this, LoggerService.class);
        if (!isServiceRunning()) {
            Log.v(TAG, "loggerservice not running - starting it");
            startService(intent);
        } else {
            Log.v(TAG, "LoggerService already running, just binding to it");
        }
        if (!mBound)
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Stop the LoggerService background service
     */
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
                        ((TextView) findViewById(R.id.hrValueTextView)).setText("Not Connected");
                    }
                }
            }
        });
    }

    /**
     * Check if the LoggerService background service is running or not
     *
     * @return true if service running, otherwise false.
     */
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
