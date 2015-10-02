package uk.org.maps3.sleeplogger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class Loggerservice extends Service {
    public Loggerservice() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
