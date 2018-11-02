package es.uma.lcc.neo.cintrano.neotrack;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.uma.lcc.neo.cintrano.neotrack.adapters.ExpandableListAdapter;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Itinerary;
import es.uma.lcc.neo.cintrano.neotrack.persistence.ItineraryDAO;

/**
 * Created by Christian Cintrano.
 */
public class ItineraryActivity extends AppCompatActivity {

    private static final String TAG = "ItineraryActivity";
    private static final String EXTRA_TAB = "newItinerary";
    private static final String FOLDER_PATH = "/neoTrack";
    private static final String FILE_NAME = "itineraries";
    private static final String FILE_EXTENSION = ".json";

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<Itinerary> itineraryList;
    private ItineraryDAO db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        itineraryList = new ArrayList<>();

        // get the list view
        expListView = findViewById(R.id.expandableListView_itineraries);
        listAdapter = new ExpandableListAdapter(this, itineraryList);
        // setting list adapter
        expListView.setAdapter(listAdapter);

        loadItinerariesFromDB();
    }

    private void loadItinerariesFromDB() {
        db = new ItineraryDAO(ItineraryActivity.this);
        db.open();
        itineraryList.addAll(db.getAll());
        Log.i("========", itineraryList.toString());
        db.close();

        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void createItinerary(View view) {
        Log.i("ItineraryActivity", "new itinerary...");
        Intent intent = new Intent(this, ItineraryMapActivity.class);
        startActivity(intent);
    }

    public void editItinerary(int index) {
        Log.i("ItineraryActivity", "editing itinerary...");
        Intent intent = new Intent(this, ItineraryMapActivity.class);
        Bundle bundle = new Bundle();
        Log.i("_____", "Cargando el itinerario para pasar...");
        Log.i("______", itineraryList.get(index).toString());
        bundle.putParcelable(EXTRA_TAB, itineraryList.get(index));
        intent.putExtra(EXTRA_TAB, bundle);
        startActivity(intent);
    }

    public void removeItinerary(int index) {
        Log.i("ItineraryActivity", "deleting...");
        alertMessage(index);
    }

    public void alertMessage(final int index) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // SKET remove in database
                        db = new ItineraryDAO(ItineraryActivity.this);
                        db.open();
                        db.delete(itineraryList.get(index));
                        db.close();
                        ///
                        itineraryList.remove(index);
                        listAdapter.notifyDataSetChanged();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // No button clicked // do nothing
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("¿Está seguro?")
                .setPositiveButton("Sí", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void sendMessageImportItinerary(View view) {
        try {
            JSONArray obj = new JSONArray(loadJSONFile());
            Log.i(TAG, obj.toString());
            // SKET SAVE IN DATA BASE
            db = new ItineraryDAO(ItineraryActivity.this);
            db.open();
            for (int i = 0; i < obj.length(); i++) {
                Itinerary itinerary = new Itinerary(obj.getJSONObject(i));
                itineraryList.add(itinerary);
                db.create(itinerary);
            }
            db.close();

            listAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFile() {
        String json = null;
        try {
            if (isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + FOLDER_PATH);
                File file = new File(dir, FILE_NAME + FILE_EXTENSION);
                FileInputStream in = new FileInputStream(file);
                int size = in.available();
                byte[] buffer = new byte[size];
                int numBytes = in.read(buffer);
                if (numBytes == -1)
                    Log.e("IO", "Stream reached");
                // Process load itinerary file
                in.close();
                json = new String(buffer, "UTF-8");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void sendMessageExportItinerary(View view) {
        List<JSONObject> itineraries = new ArrayList<>();
        for(Itinerary i : itineraryList) {
            itineraries.add(i.getJSONObject());
        }
        Log.i(TAG, "saving... " + new JSONArray(itineraries));
        saveJSON(new JSONArray(itineraries));
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void saveJSON(JSONArray json) {
        String content = json.toString();
        FileOutputStream out = null;
        try {
            if (isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + FOLDER_PATH);
                boolean ifCreate = dir.mkdirs();
                if (ifCreate)
                    Log.i("IO", "Directory create");
                File file = new File (dir, FILE_NAME + FILE_EXTENSION);
                out = new FileOutputStream(file);
                out.write(content.getBytes());
                out.close();
                Toast.makeText(this, getResources().getString(R.string.itineraries_saved), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
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

}


