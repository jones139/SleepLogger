package uk.org.maps3.sleeplogger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class LoggerService extends Service implements BleHrmMonitorListener {
    private final String TAG = "LoggerService";
    private String mHrmAddr = null;
    private String mHrmName = null;
    public boolean mConnected = false;
    public boolean mReady = false;
    public int mHR = 0;

    private BleHrmMonitor mBleHrmMonitor;

    private final int NOTIFICATION_ID = 1;

    private final IBinder mBinder = new LocalBinder();


    public LoggerService() {
        Log.v(TAG, "LoggerService() constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand()");
        updatePrefs();
        Log.v(TAG,"Connecting to Device at Address "+mHrmAddr);
        mBleHrmMonitor = new BleHrmMonitor(this.getApplicationContext(),mHrmAddr,this);


        /**
         * Show a notification while this service is running.
         */
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this);
        Notification notification = builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.star_of_life_24x24).setTicker("SleepLogger_1").setWhen(System.currentTimeMillis())
                .setAutoCancel(false).setContentTitle("SleepLogger_2")
                .setContentText("SlepLogger_3").build();


        NotificationManager nM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nM.notify(NOTIFICATION_ID, notification);


        return super.onStartCommand(intent, flags, startId);
    }

    public void stop() {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG,"onDestroy()");
        /* Cancel notification */
        Log.v(TAG,"onDestroy(): cancelling notification");
        NotificationManager nM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nM.cancel(NOTIFICATION_ID);

        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public LoggerService getService() {
            return LoggerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind()");
        updatePrefs();
        return mBinder;
    }

    public void onHrmDataReceived(int type,int data, String msg) {
        Log.v(TAG,"onBleHrmDataReceived - msg="+msg);
        if (type==BleHrmMonitor.TYPE_CONNECTION) {
            mConnected = (data!=0);
            Log.v(TAG,"mConnected = "+mConnected);
        }
        else if (type==BleHrmMonitor.TYPE_READY) {
            mReady = (data!=0);
            Log.v(TAG,"mReady = "+mReady);
        }
        else if (type==BleHrmMonitor.TYPE_DATA) {
            mHR = data;
            Log.v(TAG,"mHR = "+mHR);

        }
    }

    private void updatePrefs() {
        Log.v(TAG,"updatePrefs()");
        SharedPreferences sp = getSharedPreferences("SleepLogger",0);
        mHrmAddr = sp.getString("hrmAddr", null);
        mHrmName = sp.getString("hrmName", null);
        Log.v(TAG,"mHrmAddr = "+mHrmAddr);
        Log.v(TAG, "mHrmName = " + mHrmName);

    }

}
