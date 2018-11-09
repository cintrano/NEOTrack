package es.uma.lcc.neo.cintrano.neotrack;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Locale;

import es.uma.lcc.neo.cintrano.neotrack.persistence.SampleDAO;

import static es.uma.lcc.neo.cintrano.neotrack.BackupUtilities.checkBackupTime;
import static es.uma.lcc.neo.cintrano.neotrack.BackupUtilities.makeBackup;


public class MainActivity extends AppCompatActivity  {

    private static final String FORMAT_DATE = "yyyy-MM-dd HH:mm:ss";

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
        // Data base
        db = new SampleDAO(this);
        db.open();

        backup(); // Check and/or make a backup of the data in the database

        mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    mTts.setLanguage(new Locale("es", "ES"));
                }
            }
        });
    }

    private void backup() {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        if(checkBackupTime(sdf, this)) {
            Log.i("DB","Now is a valid date for save backup");
            makeBackup(db, sdf, this);
        } else {
            Log.i("DB", "Now not need save backup");
        }
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

}