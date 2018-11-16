package es.uma.lcc.neo.cintrano.neotrack.services.activityrecognition;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import es.uma.lcc.neo.cintrano.neotrack.services.rest.ApiRestCall;

public class ActivityRecognizedService extends IntentService {
    private static final int STATUS_STOP = 0;

    private static final String PREFERENCE_DRIVING = "preference_spot_driving";

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean driving = pref.getBoolean(PREFERENCE_DRIVING, true);

        for (DetectedActivity activity : probableActivities) {
            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.e("ActivityRecognition", "In Vehicle: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && !driving) {
                        // Save state in the preferences of the app
                        saveState(pref, true);
                        // Liberar
                        new ApiRestCall(this).execute((float) STATUS_STOP, 0.0f, 0.0f);
                    }
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    Log.e("ActivityRecognition", "On Bicycle: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.e("ActivityRecognition", "On Foot: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && driving) {
                        // Save state in the preferences of the app
                        saveState(pref, false);
                        // Aparcar
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        LocationManager locationManager =
                                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                                new MyLocationListenerGPS(this), Looper.getMainLooper());
                    }
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.e("ActivityRecognition", "Running: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.e("ActivityRecognition", "Still: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.e("ActivityRecognition", "Tilting: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.e("ActivityRecognition", "Walking: " + activity.getConfidence());

                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.e( "ActivityRecognition", "Unknown: " + activity.getConfidence());
                    break;
                }
            }
        }
    }

    private static void saveState(SharedPreferences pref, boolean state) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PREFERENCE_DRIVING, state);
        editor.apply();
    }
}