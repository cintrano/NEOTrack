package es.uma.lcc.neo.cintrano.neotrack.services.rest;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import es.uma.lcc.neo.cintrano.neotrack.persistence.Spot;

/**
 * Created by Christian Cintrano on 12/12/16.
 * Service to execute the cancellation service of the api
 */

public class CancellationService extends IntentService {

    public CancellationService() {
        super(CancellationService.class.getSimpleName());
    }

    public CancellationService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Get information from the intent bundle
        Bundle bundle = intent.getBundleExtra("data");
        Spot spot = getSpot(bundle);
        Log.i("CancellationService", spot.toString());
        // Prepare data to send
        spot.setStatus(2);
        // Call the API REST
        String data = spot.toString();
        final String CANCEL_URL = "https://ec2-54-167-56-75.compute-1.amazonaws.com:8443/iBlue-Service/spot/set";
        String s = HttpUrlConnectionJson.sendHTTPData(CANCEL_URL, "POST", data);
        if (s != null) {
            //Toast.makeText(this.getApplicationContext(), getString(R.string.toast_notify_cancellation), Toast.LENGTH_SHORT).show();
        }
    }

    private Spot getSpot(Bundle bundle) {
        Spot spot = new Spot();
        spot.setId(bundle.getInt("ID"));
        spot.setLatitude(bundle.getFloat("latitude"));
        spot.setLongitude(bundle.getFloat("longitude"));
        spot.setMac(bundle.getString("mac"));
        return spot;
    }
}
