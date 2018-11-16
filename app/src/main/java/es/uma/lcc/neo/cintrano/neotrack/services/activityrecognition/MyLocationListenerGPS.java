package es.uma.lcc.neo.cintrano.neotrack.services.activityrecognition;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import es.uma.lcc.neo.cintrano.neotrack.services.rest.ApiRestCall;

/**
 * Created by Christian Cintrano on 23/11/16.
 *
 */
class MyLocationListenerGPS implements LocationListener {
    private static final int STATUS_START = 1;
    private final Context context;

    MyLocationListenerGPS(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void onLocationChanged(Location location) {
        new ApiRestCall(context).execute((float) STATUS_START,
                (float) location.getLatitude(), (float) location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}