package es.uma.lcc.neo.cintrano.neotrack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import es.uma.lcc.neo.cintrano.neotrack.persistence.Sample;
import es.uma.lcc.neo.cintrano.neotrack.persistence.SampleDAO;


public class MainActivity extends AppCompatActivity {

    private static final String BACKUP_DB_NAME = "backup";
    private static final String PREF_LAST_DATE_BACKUP = "backupLastDate";
    private static final String PREF_DELAY_TO_BACKUP = "pref_key_delay_backup_database";
    private static final String FORMAT_DATE = "yyyy-MM-dd HH:mm:ss";

    private SimpleDateFormat sdf;

    private SampleDAO db;

    private TextToSpeech mTts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setLogo(R.mipmap.ic_neotrack);
        }

        db = new SampleDAO(this);
        db.open();

        sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);


        if(checkBackupTime()) {
            Log.i("DB","Now is a valid date for save backup");
            Calendar startDate = getLastBackup();
            Log.i("DB","Last backup was in : " + sdf.format(startDate.getTime()));
            Calendar endDate = Calendar.getInstance();
            String fileName = BACKUP_DB_NAME + "_" + sdf.format(startDate.getTime()) +
                    "_" + sdf.format(endDate.getTime());
            if(saveFileAndRemove(fileName,startDate ,endDate)) {
                Log.i("DB","Set a new date for save backup");
                setNewDelayToBackup();
            }
        } else {
            Log.i("DB", "Now not need save backup");
        }

        mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    mTts.setLanguage(new Locale("es", "ES"));
//                    mTts.speak( getResources().getString(R.string.speak_out_welcome), TextToSpeech.QUEUE_ADD, null);
                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        db.open();
       }

    @Override
    public void onPause() {
        super.onPause();
        db.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                sendMessagePreferences();
                break;
        }
        return true;
    }

    /** Called when the user clicks the Send button */
    public void sendMessageItinerary(View view) {
        // Do something in response to button
        // Activity is a subclass from context
        Intent intent = new Intent(this, ItineraryActivity.class);
        // Init the activity
        startActivity(intent);
    }
    /** Called when the user clicks the Send button */
    public void sendMessageMapTab(View view) {
        // Do something in response to button
        // Activity is a subclass from context
        Intent intent = new Intent(this, TrackActivity.class);
        // Init the activity
        startActivity(intent);
    }
    /** Called when the user clicks the Send button */
    public void sendMessagePreferences() {
        Intent intent = new Intent(this,SettingsActivity.class);
        startActivity(intent);
    }
    public void sendMessagePreferences(View view) {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    public boolean saveFileAndRemove(String fileName, Calendar dateStart, Calendar dateEnd) {
        if(saveFile(fileName, dateStart, dateEnd)) {
            Log.i("MainActivity", "Delete database");
            db.delete(dateStart, dateEnd);
            return true;
        } else {
            return false;
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean saveFile(String fileName, Calendar dateStart, Calendar dateEnd) {
        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream out = null;
        SampleDAO db = new SampleDAO(this);
        db.open();
        List<Sample> data = db.get(dateStart, dateEnd);
        Log.i("DB", "Find " + data.size() + " elements");
        String extension = ".csv";
        String folderName = "/mdaFolder";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                if(dir.mkdirs()) Log.i("DB", "Folder created");
                File file = new File (dir, fileName + extension);
                out = new FileOutputStream(file);
            } else {
                out = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
            }
            String head = "_id,latitude,longitude,session,stopType,comment,date\n";
            out.write(head.getBytes());
            for(Sample dc : data) {
                System.out.print(dc);
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
                out.write((String.valueOf(dc.getSensorTemperature()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorHumidity()) + "\n").getBytes());
            }
            out.flush();
            out.close();
            Log.i("DB", "File saved with name: " + fileName);
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

    private Calendar getLastBackup() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        String lastDate = settings.getString(PREF_LAST_DATE_BACKUP, "");
        Calendar cMinDate = Calendar.getInstance();
        try {
            cMinDate.setTime(sdf.parse(lastDate));
        } catch (ParseException e) {

            e.printStackTrace();
        }
        return cMinDate;
    }

    private boolean checkBackupTime() {
        Log.i("Preferences","Checking preferences");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int delay = Integer.parseInt(settings.getString(PREF_DELAY_TO_BACKUP, "0")); // num of days

        Calendar minDateForNewBackup = getLastBackup();
        minDateForNewBackup.add(Calendar.DATE, delay);
        Calendar now = Calendar.getInstance();

        return now.compareTo(minDateForNewBackup) >= 0;
    }

    private void setNewDelayToBackup() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Calendar newDate = Calendar.getInstance();

        String newLastDate = sdf.format(newDate.getTime());

        // Update date of last backup
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_LAST_DATE_BACKUP,newLastDate);
        editor.apply();
    }
}