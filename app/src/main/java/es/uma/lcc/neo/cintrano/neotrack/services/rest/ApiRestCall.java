package es.uma.lcc.neo.cintrano.neotrack.services.rest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import es.uma.lcc.neo.cintrano.neotrack.MainActivity;
import es.uma.lcc.neo.cintrano.neotrack.R;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Spot;


/**
 * Created by Christian Cintrano on 23/11/16.
 *
 */
public class ApiRestCall extends AsyncTask<Float, Void, String> {
    private String BASIC_URL;
    //private final static String BASIC_URL =
    //        "https://ec2-54-167-56-75.compute-1.amazonaws.com:8443/iBlue-Service/spot/set";
    private Spot spot;
    private Context context;
    // Preferences
    private SharedPreferences pref; // Settings listener
    private static final String PREFERENCE_ID = "preference_spot_id";
    private static final String PREFERENCE_LATITUDE = "preference_spot_lat";
    private static final String PREFERENCE_LONGITUDE = "preference_spot_lon";

    public ApiRestCall(Context context) {
        super();
        this.context = context;
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        BASIC_URL = context.getResources().getString(R.string.server_url);
    }

    @Override
    protected String doInBackground(Float... params) {
        // Set the status for the previous slot created
        spot.setStatus(params[0].intValue());

        if (params[1] != 0.0f && params[2] != 0.0f) {
            spot.setLatitude(Double.valueOf(params[1]));
            spot.setLongitude(Double.valueOf(params[2]));

            spot.setUnit("u");
            spot.setData(0);
            spot.setType("normal");
            spot.setDevice(spot.getMac());

            SharedPreferences.Editor editor = pref.edit();
            editor.putFloat(PREFERENCE_LATITUDE, (float) spot.getLatitude());
            editor.putFloat(PREFERENCE_LONGITUDE, (float) spot.getLongitude());
            editor.apply();
        } else {
            spot.setLatitude((double) pref.getFloat(PREFERENCE_LATITUDE, 0.0f));
            spot.setLongitude((double) pref.getFloat(PREFERENCE_LONGITUDE, 0.0f));
        }

        return requestWebService(BASIC_URL, "POST", spot);
    }

    @Override
    protected void onPreExecute() {
        spot = new Spot();
        spot.setMac(getMacAddress());
        Log.i("ID", getSavedID() + "");
        spot.setId(getSavedID());
    }

    @Override
    protected void onProgressUpdate(Void... values) {}

    private String requestWebService(String serviceUrl, String method, Spot spot) {
        switch (method) {
            case "GET":
                break;
            case "POST":
                String s = HttpUrlConnectionJson.sendHTTPData(serviceUrl, "POST", spot.toString());
                if (s != null && s.equals("OK")) {

                    // Send notification to the user
                    Intent resultIntent = new Intent(context, CancellationService.class);
                    //// Anadir los extras al bundle y anadir el bundle
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", spot.getId());
                    bundle.putDouble("lat", spot.getLatitude());
                    bundle.putDouble("lon", spot.getLongitude());
                    bundle.putString("unit", "u");
                    bundle.putFloat("data", 0);
                    bundle.putString("type", "normal");
                    bundle.putString("device", spot.getMac());

                    resultIntent.putExtra("data", bundle);
                    PendingIntent resultPendingIntent = PendingIntent.getService(
                            context.getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);

                    Intent activityIntent = new Intent(context, MainActivity.class);
                    PendingIntent activityPendingIntent = PendingIntent.getActivity(
                            context.getApplicationContext(), 1, activityIntent, PendingIntent.FLAG_ONE_SHOT);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                    if (spot.getStatus() == 0) {
                        builder.setContentText(context.getResources().getString(R.string.notification_start));
                    } else {
                        builder.setContentText(context.getResources().getString(R.string.notification_stop));
                    }
                    builder.setSmallIcon(R.mipmap.ic_launcher);
                    builder.setContentTitle(context.getString(R.string.app_name));

//                    builder.addAction(R.drawable.ic_close_light, context.getString(R.string.notification_ask), resultPendingIntent);
                    //builder.addAction(new NotificationCompat.Action(R.drawable.ic_clear_light, context.getString(R.string.notification_ask), resultPendingIntent));

                    builder.setAutoCancel(true);

                    builder.setContentIntent(activityPendingIntent);

                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    Notification notification = builder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL;
                    notificationManager.notify(0, notification);
                    //NotificationManagerCompat.from(context).notify(0, builder.build());
                }
                return s;
        }
        return null;
    }

    private int getSavedID() {
        return pref.getInt(PREFERENCE_ID, -1);
    }

    private String getMacAddress() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        return wInfo.getMacAddress();
    }
}