package es.uma.lcc.neo.cintrano.neotrack;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.uma.lcc.neo.cintrano.neotrack.adapters.MyRecognitionListener;
import es.uma.lcc.neo.cintrano.neotrack.adapters.TabsAdapter;
import es.uma.lcc.neo.cintrano.neotrack.fragments.MapTabFragment;
import es.uma.lcc.neo.cintrano.neotrack.fragments.TrackFragment;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Itinerary;
import es.uma.lcc.neo.cintrano.neotrack.persistence.ItineraryDAO;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Point;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Sample;
import es.uma.lcc.neo.cintrano.neotrack.persistence.SampleDAO;
import es.uma.lcc.neo.cintrano.neotrack.persistence.SavePointInput;
import es.uma.lcc.neo.cintrano.neotrack.persistence.SavePointInput2;
import es.uma.lcc.neo.cintrano.neotrack.services.rest.ApiRestCallBash;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Maps Activity with tabs
 */
public class TrackActivity extends AppCompatActivity {

    private static final String TAG = "TrackActivity";

    private AlertDialog saveFileDialog;
    private DatePickerDialog dateInitDialog;
    private final static SimpleDateFormat DATE_FORMATTER_VIEW =
            new SimpleDateFormat("dd-MM-yyyy", new Locale("es", "ES"));
    private final static SimpleDateFormat DATE_FORMATTER_SAVE =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("es", "ES"));
    private EditText etDateStart;
    private EditText etDateEnd;
    private DatePickerDialog dateEndDialog;
    private EditText etNameSaveFile;
    private Calendar newDateStart;
    private Calendar newDateEnd;

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    private MapTabFragment mapFragment;
    private ProgressDialog dialogWait;

    // Preferences
    private SharedPreferences pref; // Settings listener
    private long intervalTimeGPS; // milliseconds
    private float minDistance; // meters
    private float speedMin; // Km/h
    private float speedMax; // Km/h
    // Tracking
    private Itinerary visitItinerary;
    private boolean runningTracking = false;
    // GPS and location
    public LocationManager locationManager;
    // Location
    private LocationListener gpsLocationListener;
    private GpsStatus.Listener mGPSStatusListener;
    // Data base and ids
    public SampleDAO dbSample;
    private String SESSION_ID;
    final static private String TAG_SESSION_ID = "SESSION_ID";
    // Speech
    public TextToSpeech speakerOut;
    private boolean speakerOutReady = false;
    public SpeechRecognizer sr;
    public boolean speeching = false;
    public boolean waitToStart = true;
    public boolean runningSpeech = false;
    // Sensors
    private SensorManager mSensorManager;
    private boolean tStop = true; // if vehicle is stopped
    private double pressure = 0.0;
    private double light = 0.0;
    private double temperature = 0.0;
    private double humidity = 0.0;
    private SensorEventListener mSensorListener;
    // Velocity
    private static final double[] ACCELERATION_OFFSET = new double[]{
            0.0215942828322981, 0.0199339222385277, 0.1215942828322981};
    private double[] acceleration;
    private long oldTime;
    private double speed;
    private double[] velocity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Configure Interface
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_map);
        mViewPager = findViewById(R.id.fragment_container);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        configureActionBar(savedInstanceState);

        newSessionId(); // route ID
        dbSample = new SampleDAO(this); // Data base connector

        // reset sensors variables
        speed = 0.0;
        pressure = 0.0;
        light = 0.0;
        temperature = 0.0;
        humidity = 0.0;
        oldTime = System.currentTimeMillis();
        acceleration = new double[]{0, 0, 0};
        velocity = new double[]{0, 0, 0};
    }

    private void newSessionId() {
        String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String timestamp = DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime());
        SESSION_ID = android_id + "::" + timestamp;
        Log.i(TAG, "new session ID: " + SESSION_ID);
    }

    @Override
    public void onResume() {
        super.onResume();
        dbSample.open();
        configurePreference();

        mGPSStatusListener = new GpsStatus.Listener() {
            public void onGpsStatusChanged(int event) {
                switch (event) {
                    case GpsStatus.GPS_EVENT_STARTED:
                        Log.i("GPS", "Searching...");
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        Log.i("GPS", "STOPPED");
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        // GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                        Log.i("GPS", "Locked position");
                        setHiddenFragment(); // visual log
                        if (mapFragment.ready)
                            dialogWait.dismiss();
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        break;
                }
            }
        };
        configureDialogWait();
        configureLocation();
        configureSpeech();
        configureSensors();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        SESSION_ID = savedInstanceState.getString(TAG_SESSION_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putString(TAG_SESSION_ID, SESSION_ID);
        if (getSupportActionBar() != null)
            outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(gpsLocationListener);
        locationManager.removeGpsStatusListener(mGPSStatusListener);
        dbSample.close();

        sr.stopListening();
        mSensorManager.unregisterListener(mSensorListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sr.stopListening();
        mSensorManager.unregisterListener(mSensorListener);
        sr.cancel();
        sr.destroy();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu items for use in the action bar
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_map, menu);
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                sendSettings();
                return true;
            case R.id.action_save_file:
                displaySaveFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void configureDialogWait() {
        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage(getResources().getString(R.string.gps_loading));
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
    }

    public void configureLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            // Register GPSStatus listener for events
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;
            locationManager.addGpsStatusListener(mGPSStatusListener);
            gpsLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //if (!tStop)
                        myLocationChanged(location, null);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Toast.makeText(TrackActivity.this,
                            getResources().getString(R.string.enabled_provider) + provider,
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProviderDisabled(String provider) {
                    Toast.makeText(TrackActivity.this,
                            getResources().getString(R.string.disabled_provider) + provider,
                            Toast.LENGTH_SHORT).show();
                }
            };
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    intervalTimeGPS, minDistance, gpsLocationListener);
        } else {
            Toast.makeText(this, getResources().getString(R.string.gps_disabled),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void configurePreference() {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        loadSettings();
        PreferenceChangeListener preferenceListener = new PreferenceChangeListener();
        pref.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void configureActionBar(Bundle savedInstanceState) {
        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

            mTabsAdapter = new TabsAdapter(this, mViewPager);
            mTabsAdapter.addTab(bar.newTab().setText(getResources().getString(R.string.map)),
                    MapTabFragment.class, null);
            mTabsAdapter.addTab(bar.newTab().setText(getResources().getString(R.string.data)),
                    TrackFragment.class, null);

            if (savedInstanceState != null) {
                bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
            }
        }
    }


    private void configureSpeech() {
        speakerOut = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    speakerOut.setLanguage(new Locale("es", "ES"));
                    speakerOutReady = true;
                }
            }
        });

        sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        MyRecognitionListener listener = new MyRecognitionListener(this);
        sr.setRecognitionListener(listener);
    }

    private void configureSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor mySensor = event.sensor;
                if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    // Previous data
                    double x = acceleration[0];
                    double y = acceleration[1];
                    double z = acceleration[2];

                    long trackTime = System.currentTimeMillis();
                    if(trackTime - oldTime > 100) {
                        long diffTime = trackTime - oldTime;
                        oldTime = trackTime;

                        // Check if have anomalous data
                        if ((event.values[0] < 100.0) && (event.values[0] > -100.0)
                                && (event.values[1] < 100.0) && (event.values[1] > -100.0)
                                && (event.values[2] < 100.0) && (event.values[2] > -100.0)) {

                            // Reduce noise
                            if (event.values[0]>0)
                                acceleration[0] = event.values[0]-ACCELERATION_OFFSET[0];
                            else
                                acceleration[0] = event.values[0]+ACCELERATION_OFFSET[0];
                            if (event.values[1]>0)
                                acceleration[1] = event.values[1]-ACCELERATION_OFFSET[1];
                            else
                                acceleration[1] = event.values[1]+ACCELERATION_OFFSET[1];
                            if (event.values[2]>0)
                                acceleration[2] = event.values[2]-ACCELERATION_OFFSET[2];
                            else
                                acceleration[2] = event.values[2]+ACCELERATION_OFFSET[2];

                            // Low-pass filter.
                            final float alpha = 0.99f;
                            acceleration[0] = alpha * acceleration[0] + (1 - alpha) * x;
                            acceleration[1] = alpha * acceleration[1] + (1 - alpha) * y;
                            acceleration[2] = alpha * acceleration[2] + (1 - alpha) * z;

                            // Integration
                            double h = ((double) diffTime) / 6.0f;
                            velocity[0] =
                                    h * (x + (4.0 * ((acceleration[0]-x)/2.0d)) + acceleration[0]);
                            velocity[1] =
                                    h * (y + (4.0 * ((acceleration[1]-y)/2.0d)) + acceleration[1]);
                            velocity[2] =
                                    h * (z + (4.0 * ((acceleration[2]-z)/2.0d)) + acceleration[2]);

                            // Module of velocity vector in km/h
                            speed = Math.sqrt((velocity[0]*velocity[0])
                                    + (velocity[1]*velocity[1])
                                    + (velocity[2]*velocity[2])) * 3.6d;
                            Log.i("Speed","speed: " + speed + " " + diffTime + " " + speedMin
                                    + " " + speedMax + " " +acceleration[0] + "-"+ acceleration[1]
                                    + "-" + acceleration[2]);

                            // Update stop condition
                            if (tStop) {
                                if (speed > speedMax) {
//                                    speakerOut.speak("Andando", TextToSpeech.QUEUE_ADD, null);
                                    tStop = false;
                                }
                            } else {
                                if (speed < speedMin) {
//                                    speakerOut.speak("no", TextToSpeech.QUEUE_ADD, null);
                                    tStop = true;
                                }
                            }

                            // Running recognition speech
                            if (tStop) {
                                if (!waitToStart) {
                                    waitToStart = true;
                                    if (!runningSpeech) {
                                        runningSpeech = true;
                                        restartSpeech();
                                    }
                                }
                            } else {
                                waitToStart = false;
                            }
                        }
                    }

                }

                if (mySensor.getType() == Sensor.TYPE_PRESSURE) {
                    pressure = event.values[0];
                }
                if (mySensor.getType() == Sensor.TYPE_LIGHT) {
                    light = event.values[0];
                }
                if (mySensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    temperature = event.values[0];
                }
                if (mySensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                    humidity = event.values[0];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void restartSpeech() {
        speakerOut.speak("Parada", TextToSpeech.QUEUE_ADD, null);
        sr.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));
    }

    public void displayItineraries(View view) {
        ItineraryDAO db = new ItineraryDAO(TrackActivity.this);
        db.open();
        ArrayList<Itinerary> itineraryList = new ArrayList<>(db.getAll());
        db.close();

        List<String> list = new ArrayList<>();
        for(Itinerary i : itineraryList) {
            list.add(i.getName());
        }

        String title = getResources().getString(R.string.itinerary_select_title);
        CharSequence[] array = list.toArray(new CharSequence[list.size()]);
        Dialog dialog = onCreateDialogSingleChoice(title, array, itineraryList);
        dialog.show();
    }

    public void controlTracking(View view) {
        Button button = (Button) view;
        runningTracking = !runningTracking;
        Drawable top;
        if(runningTracking){
            top = getResources().getDrawable(R.drawable.baseline_pause_white_48);
        } else {
            top = getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp);
        }
        button.setCompoundDrawablesWithIntrinsicBounds(null, top , null, null);
        Log.i(TAG, "capturing points: " + runningTracking);
    }

    public void stopTracking(View view) {
        // UI changes
        runningTracking = false;
        Button button = findViewById(R.id.b_start_tracking);
        Drawable top = getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp);
        button.setCompoundDrawablesWithIntrinsicBounds(null, top , null, null);

        Log.i(TAG, "Stop capturing points");
        final String old_session_id = SESSION_ID;

        List<Sample> results = dbSample.get(old_session_id);
        Log.i(TAG, "recover " + results.size() + " elements");
        if (results.size() > 0) {
            // Display summary
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tracking_summary)
                    .setMessage(printSummaryTracking(SESSION_ID));
            builder.setNeutralButton("Enviar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dlg2, int which) {
                    ApiRestCallBash task = new ApiRestCallBash(getApplicationContext());
                    List<Sample> results = dbSample.get(old_session_id);
                    Toast.makeText(getBaseContext(), "Enviando...", Toast.LENGTH_SHORT).show();
                    Sample[] data = new Sample[results.size()];
                    data = results.toArray(data);
                    task.execute(data);
                    Toast.makeText(getBaseContext(), "Datos enviados correctamente", Toast.LENGTH_SHORT).show();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else  {
            Toast.makeText(getBaseContext(), "No hay datos para enviar", Toast.LENGTH_SHORT).show();
        }
        // Reset sessionId
        newSessionId();
    }

    private String printSummaryTracking(String tag) {
        long time = 0;
        float distance = 0;
        int stops = 0;

        Log.i(TAG, "Search for sessionId: " + tag);
        List<Sample> results = dbSample.get(tag);
        Log.i(TAG, "recover " + results.size() + " elements");
        if (results.size() > 0) {
            // time
            Date dateStart = null;
            Date dateEnd = null;
            try {
                dateStart = DATE_FORMATTER_SAVE.parse(results.get(0).getDate());
                dateEnd = DATE_FORMATTER_SAVE.parse(results.get(results.size() - 1).getDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (dateStart != null && dateEnd != null) {
                time = (dateEnd.getTime() - dateStart.getTime()) / 1000;
            }
            // distance
            Location lastLoc = new Location("");
            lastLoc.setLatitude(results.get(0).getLatitude());
            lastLoc.setLongitude(results.get(0).getLongitude());
            for (Sample dc : results) {
                Location newLoc = new Location("");
                newLoc.setLatitude(dc.getLatitude());
                newLoc.setLongitude(dc.getLongitude());
                distance += lastLoc.distanceTo(newLoc);
                lastLoc = newLoc;
                // number of stops
                if (dc.getStopType() != null) {
                    stops++;
                }
            }
            // Save data of itinerary automatically
            saveTrack(results, time, distance);
        }

        float vel = time != 0 ? distance * 3.6f / time : 1;
        return "Tiempo del trayecto:\t" + time + "s.\n"
                + "Distancia recorrida:\t" + distance + "m.\n"
                + "Velocidad media:\t" + vel + "km/h\n"
                + "Número de paradas:\t" + stops + "\n";
    }

    public void sendSettings() {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void saveFile(String fileName) {
        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream out = null;
        SampleDAO db = new SampleDAO(this);
        db.open();
        List<Sample> data = db.get(newDateStart, newDateEnd);
        Log.i("DB", "Find " + data.size() + " DataCapture elements");
        String extension = ".csv";
        String folderName = "/neoTrack";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                boolean ifCreate = dir.mkdirs();
                if (ifCreate)
                    Log.i("IO", "Directory create");
                File file = new File (dir, fileName + extension);
                out = new FileOutputStream(file);
            } else {
                out = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
            }
            String head = "_id,latitude,longitude,street,stoptype,comment,date," +
                    "acceleration,pressure,light,temperature,humidity\n";
            out.write(head.getBytes());
            for(Sample dc : data) {
                out.write((String.valueOf(dc.getId()) + ",").getBytes());
                if(dc.getSession() != null) {
                    out.write(("\"" + dc.getSession() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                out.write((String.valueOf(dc.getLatitude()) + ",").getBytes());
                out.write((String.valueOf(dc.getLongitude()) + ",").getBytes());
                if(dc.getStopType() != null) {
                    out.write(("\"" + dc.getStopType() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                if(dc.getComment() != null) {
                    out.write(("\"" + dc.getComment() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                out.write(("\"" + dc.getDate() + "\",").getBytes());
                out.write((String.valueOf(dc.getSensorAcceleration()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorPressure()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorLight()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorTemperature()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorHumidity()) + "\n").getBytes());
            }
            out.flush();
            out.close();
            Log.i("DB", "File saved");
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                db.close();
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveTrack(List<Sample> results, float time, float distance) {
//        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");

        String fileName = "itinerary" + SESSION_ID;

        FileOutputStream out = null;
        String extension = ".csv";
        String folderName = "/neoTrack";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                boolean ifCreate = dir.mkdirs();
                if (ifCreate)
                    Log.i("IO", "Directory create");
                File file = new File (dir, fileName + extension);
                out = new FileOutputStream(file);
            } else {
                out = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
            }
            String head = "ID\ttime\tdistance\n";
            out.write(head.getBytes());
            out.write((SESSION_ID + "\t" + time + "\t" + distance + "\n").getBytes());
            head = "_id\tsession\tlatitude\tlongitude\tstoptype\tcomment\tdate" +
                    "\tacceleration\tpressure\tlight\ttemperature\thumidity\n";
            out.write(head.getBytes());
            for(Sample dc : results) {
                out.write((String.valueOf(dc.getId()) + "\t").getBytes());
                if(dc.getSession() != null) {
                    out.write(("\"" + dc.getSession() + "\"\t").getBytes());
                } else {
                    out.write(("null\t").getBytes());
                }
                out.write((String.valueOf(dc.getLatitude()) + "\t").getBytes());
                out.write((String.valueOf(dc.getLongitude()) + "\t").getBytes());
                if(dc.getStopType() != null) {
                    out.write(("\"" + dc.getStopType() + "\"\t").getBytes());
                } else {
                    out.write(("null\t").getBytes());
                }
                if(dc.getComment() != null) {
                    out.write(("\"" + dc.getComment() + "\"\t").getBytes());
                } else {
                    out.write(("null\t").getBytes());
                }
                out.write(("\"" + dc.getDate() + "\"\t").getBytes());
                out.write((String.valueOf(dc.getSensorAcceleration()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorPressure()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorLight()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorTemperature()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorHumidity()) + "\n").getBytes());
            }
            out.flush();
            out.close();
            Log.i("DB", "File saved");
            Toast.makeText(this, "Copia de seguridad almacenada", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void displaySaveFile() {
        Calendar newCalendar = Calendar.getInstance();

        newDateStart = Calendar.getInstance();
        newDateStart.set(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH),0,0,0);
        newDateEnd = Calendar.getInstance();
        newDateEnd.set(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH),23,59,59);
        newDateEnd.set(Calendar.HOUR,23);
        dateInitDialog = new DatePickerDialog(this, new OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Log.i("Dialog", "Change date picker");
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    etDateStart.setText(DATE_FORMATTER_VIEW.format(newDate.getTime()));
                }
            },
                newCalendar.get(Calendar.YEAR),
                newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dateInitDialog.setButton(DatePickerDialog.BUTTON_POSITIVE,
                getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg2, int which) {
                        dlg2.cancel();
                        saveFileDialog.show();
                    }
        });
        dateInitDialog.getDatePicker().init(newCalendar.get(Calendar.YEAR),
                newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH),
                new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker datePicker, int year, int monthOfYear,
                                              int dayOfMonth) {
                        Log.i("Dialog", "Change date picker");
                        newDateStart = Calendar.getInstance();
                        newDateStart.set(year, monthOfYear, dayOfMonth,0,0,0);
                        etDateStart.setText(DATE_FORMATTER_VIEW.format(newDateStart.getTime()));
                    }
                }
        );

        dateEndDialog = new DatePickerDialog(this,new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.i("Dialog", "Change datepicker");
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                etDateEnd.setText(DATE_FORMATTER_VIEW.format(newDate.getTime()));
            }
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH));
        dateEndDialog.setButton(DatePickerDialog.BUTTON_POSITIVE,
                getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg2, int which) {
                dlg2.cancel();
                saveFileDialog.show();
            }
        });
        dateEndDialog.getDatePicker().init(newCalendar.get(Calendar.YEAR),
                newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH),
                new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int day) {
                Log.i("Dialog", "Change datepicker");
                newDateEnd = Calendar.getInstance();
                newDateEnd.set(year, month, day,23,59,59);
                newDateEnd.set(Calendar.HOUR,23);
                etDateEnd.setText(DATE_FORMATTER_VIEW.format(newDateEnd.getTime()));
            }
        });

        AlertDialog.Builder saveFileDialogBuilder = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setMessage(getResources().getString(R.string.save_file))
                .setPositiveButton(getResources().getString(R.string.button_ok),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        dateInitDialog.show();
                        saveFile(etNameSaveFile.getText().toString());
                    }
                })
                .setNegativeButton(getResources().getString(R.string.button_cancel),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setView(R.layout.dialog_save_file);
        saveFileDialog = saveFileDialogBuilder.create();

        saveFileDialog.show();

        etNameSaveFile = saveFileDialog.findViewById(R.id.d_save_file_name);
        etDateStart = saveFileDialog.findViewById(R.id.d_save_file_date_start);
        etDateEnd = saveFileDialog.findViewById(R.id.d_save_file_date_end);
        Button bStart = saveFileDialog.findViewById(R.id.db_save_file_date_start);
        if (bStart != null) {
            bStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dateInitDialog.show();
                }
            });
        }
        Button bEnd = saveFileDialog.findViewById(R.id.db_save_file_date_end);
        if (bEnd != null) {
            bEnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dateEndDialog.show();
                }
            });
        }
        etDateStart.setText(DATE_FORMATTER_VIEW.format(newCalendar.getTime()));
        etDateEnd.setText(DATE_FORMATTER_VIEW.format(newCalendar.getTime()));
        String name = getResources().getString(R.string.info_track) + "_" + etDateStart.getText()
                + "_" + etDateEnd.getText();
        etNameSaveFile.setText(name);

    }

    private void loadSettings() {
        Log.i("MapActivity","Loading settings...");
        String syncConnPref = pref.getString("pref_key_interval_time", "0");
        intervalTimeGPS = Integer.parseInt(syncConnPref) * 1000;

        syncConnPref = pref.getString("pref_key_min_distance", "0");
        minDistance = Integer.parseInt(syncConnPref);

        syncConnPref = pref.getString("pref_key_min_speed", "10");
        speedMin = Integer.parseInt(syncConnPref);
        syncConnPref = pref.getString("pref_key_max_speed", "15");
        speedMax = Integer.parseInt(syncConnPref);
    }

    public void myLocationChanged(Location location, String cause) {
        if (mapFragment.ready) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            setHiddenFragment(); // visual log
            mapFragment.setCamera(latLng);
            // Print marker car position
            mapFragment.addMarker(MapTabFragment.Marker_Type.POSITION, null, location);

            if (runningTracking) {
                // Print marker track point
                mapFragment.addMarker(MapTabFragment.Marker_Type.GPS, null, location);
                new SavePointTask().execute(new SavePointInput(visitItinerary, location, cause));
            }
        }
    }

    /**
     * Method to save data from external fragments
     * @param sample data
     */
    public void runSaveData(Sample sample) {
        sample.setSensorAcceleration(speed);
        sample.setSensorPressure(pressure);
        sample.setSensorLight(light);
        sample.setSensorTemperature(temperature);
        sample.setSensorHumidity(humidity);
        new SavePointTask2().execute(new SavePointInput2(visitItinerary, sample));
    }

    public void setHiddenFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        Log.i("Activity","Num of fragments: " + fragments.size());
        for(Fragment fragment : fragments){
            if(fragment != null) {
                if (fragment instanceof MapTabFragment) {//!fragment.isVisible())
                    Log.i("Activity","Adding map Fragment");
                    mapFragment = (MapTabFragment) fragment;
                }
//                else if (fragment instanceof TrackFragment)//!fragment.isVisible())
//                    trackFragment = fragment;
            }
        }
    }

    /**
     * Handle preferences changes
     */
    private class PreferenceChangeListener implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Log.i("Settings", "Changed settings");
            loadSettings();
        }
    }

    private String getStopType(ArrayList<String> result) {
        final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros", "Reanudar"};
        final String[] stopChoicesPattern = {"asco", "bra", "ente", "tro", "anudar"};

        for(String str : result) {
         System.out.println(str);
            str = Normalizer.normalize(str, Normalizer.Form.NFD);
            // remove accents
            str = str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            for(int i = 0; i<stopChoicesPattern.length;i++) {
                if(str.toLowerCase().contains(stopChoicesPattern[i])) {
                    return stopChoices[i];
                }
            }
        }
        return null;
    }

    /*
     * ITIENRARIES
     */

    public Dialog onCreateDialogSingleChoice(String title, CharSequence[] array, final List data) {
        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the dialog title
        builder.setTitle(title)
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(array, 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(getResources().getString(R.string.button_ok),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int selectedPosition = ((AlertDialog) dialog).getListView()
                                .getCheckedItemPosition();
                        displayItinerarySelected((Itinerary) data.get(selectedPosition));
                    }
                })
                .setNegativeButton(getResources().getString(R.string.button_cancel),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    private void displayItinerarySelected(Itinerary itinerary) {
        mapFragment.clearItineraryMarkers();
        if(speakerOutReady)
            speakerOut.speak(getResources().getString(R.string.speak_out_itinerary_added) +
                    itinerary.getPoints().size() + " puntos", TextToSpeech.QUEUE_ADD, null);

        for(Object point : itinerary.getPoints()) {
            Location location = new Location("Test");
            location.setLatitude(((Point) point).getLatitude());
            location.setLongitude(((Point) point).getLongitude());
            mapFragment.addMarker(MapTabFragment.Marker_Type.ITINERARY,
                    ((Point) point).getAddress(), location);
        }
        visitItinerary = itinerary;
    }


    /*
     * Background task to save tracking data
     */
    public class SavePointTask extends AsyncTask<SavePointInput, Void, Boolean> {
        private float MIN_DISTANCE = 0.00015f; // 15 meters

        @Override
        protected Boolean doInBackground(SavePointInput... params) {
            // Save point
            savePoint(params[0].getLocation(), params[0].getCause());
            // Check if a itinerary is loaded
            if ((params[0].getItinerary() != null) &&
                    (params[0].getItinerary().getPoints().size() > 0)) {
                double distance = distance(params[0].getLocation(),
                        (Point) params[0].getItinerary().getPoints().get(0));
                if (distance < MIN_DISTANCE) {
                    params[0].getItinerary().getPoints().remove(0);
                    return true;
                }
            }
            return false;
        }

        private void savePoint(Location location, String cause) {
            Sample sample = new Sample();
            sample.setLatitude(location.getLatitude());
            sample.setLongitude(location.getLongitude());
            sample.setStopType(cause);
            sample.setDate(DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime()));
            sample.setSession(SESSION_ID);
            sample.setSensorAcceleration(speed);
            sample.setSensorPressure(pressure);
            sample.setSensorLight(light);
            sample.setSensorTemperature(temperature);
            sample.setSensorHumidity(humidity);
            dbSample.create(sample);
        }

        protected void onPostExecute(Boolean wasItineraryPoint) {
            if (wasItineraryPoint && speakerOutReady) {
                speakerOut.speak(getResources().getString(R.string.speak_out_visit_itinerary_point),
                        TextToSpeech.QUEUE_ADD, null);
            }
        }

    }

    public class SavePointTask2 extends AsyncTask<SavePointInput2, Void, Boolean> {
        private float MIN_DISTANCE = 0.00015f; // 15 meters

        @Override
        protected Boolean doInBackground(SavePointInput2... params) {
            // Save point
            savePoint(params[0].getLocation());
            // Check itinerary
            if (params[0].getItinerary() != null) {
                Location loc = new Location("");
                loc.setLatitude(params[0].getLocation().getLatitude());
                loc.setLongitude(params[0].getLocation().getLongitude());
                double distance = distance(loc,(Point) params[0].getItinerary().getPoints().get(0));
                if (distance < MIN_DISTANCE) {
                    params[0].getItinerary().getPoints().remove(0);
                    return true;
                }
            }
            return false;
        }

        private void savePoint(Sample location) {
            location.setSession(SESSION_ID);
            dbSample.create(location);
        }

        protected void onPostExecute(Boolean wasItineraryPoint) {
            // Notification speech out
            speakerOut.speak(getResources().getString(R.string.speak_out_stop_added),
                    TextToSpeech.QUEUE_ADD, null);
        }

    }

    // Euclidian distance
    private static double distance(Location location, Point point) {
        return Math.sqrt(Math.pow(location.getLatitude() - point.getLatitude(), 2) +
                Math.pow(location.getLongitude() - point.getLongitude(), 2));
    }
}
