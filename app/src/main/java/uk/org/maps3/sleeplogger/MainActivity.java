package uk.org.maps3.sleeplogger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        Log.v(TAG,"end of onCreate()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG,"onResume");
        Intent intent = new Intent(this, LoggerService.class);
        startService(intent);
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
}
