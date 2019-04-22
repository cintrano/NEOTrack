package es.uma.lcc.neo.cintrano.neotrack.fragments;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import es.uma.lcc.neo.cintrano.neotrack.R;
import es.uma.lcc.neo.cintrano.neotrack.TrackActivity;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Sample;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Fragment by main tab: Map view
 */
public class MapTabFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback {

    private static final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros", "Reanudar"};
    private static final int ZOOM = 20;

    public enum Marker_Type {GPS, STOP, POSITION, ITINERARY}

    private Context context;
    private View view;
    private Marker currentMarker;
    private String title = null;
    public GoogleMap map; // Might be null if Google Play services APK is not available.

    private SimpleDateFormat sdf;
    private List<Marker> itineraryMarkers;

    public boolean ready = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.activity_maps, container, false);
        } catch (InflateException e) {
            /* map is already there, just return view as it is */
            Log.e("M", e.getMessage());
            e.printStackTrace();
        }

        Button bStop = view.findViewById(R.id.stop_button);
        bStop.setOnClickListener(this);
        Button bChangeMap = view.findViewById(R.id.change_map_button);
        bChangeMap.setOnClickListener(this);

        itineraryMarkers = new ArrayList<>();

        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        context = container.getContext();

        if (map == null) {
            SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            mapFrag.getMapAsync(this);
        }
        return view;
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
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        setZoom(ZOOM);
        ready = true;
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    public void displayStopChoices() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // EditText by default hidden
        final EditText editText = new EditText(context);
        editText.setEnabled(false);

        builder.setTitle("Seleccione una opción");
        builder.setSingleChoiceItems(stopChoices, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (stopChoices[item].equals("Otros")) {
                    editText.setEnabled(true);
                    editText.requestFocus();
                    InputMethodManager imm =
                            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    assert imm != null;
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    editText.setEnabled(false);
                    editText.getText().clear();
                }
                title = stopChoices[item];
                String text = "Has elegido la opcion: " + stopChoices[item];
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        builder.setView(editText);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (title != null) {
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Location loc = ((TrackActivity) context)
                            .locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    String text = null;
                    if(title.equals("Otros")) {
                        text = editText.getText().toString();
                    }

                    Sample dc = new Sample();
                    dc.setLatitude(loc.getLatitude());
                    dc.setLongitude(loc.getLongitude());
                    dc.setStopType(title);
                    dc.setComment(text);
                    dc.setDate(sdf.format(Calendar.getInstance().getTime()));

                    ((TrackActivity) context).runSaveData(dc);
                    addMarker(Marker_Type.STOP, title, loc);
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    public void setCamera(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
        map.moveCamera(cameraUpdate);
    }

    public void setZoom(float zoom) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(zoom);
        map.animateCamera(cameraUpdate);
    }

    public void changeMap() {
        if (map.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) {
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else {
            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
    }

    /**
     * Simple method to add markers to the map
     * @param title text of the marker
     * @param type If is GPS marker or Stop marker
     * @param loc location of market
     */
    public void addMarker(Marker_Type type, String title, Location loc) {
        LatLng coordinates = new LatLng(loc.getLatitude(), loc.getLongitude());
        Log.i("DB", "Adding marker to (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

        switch (type) {
            case GPS: map.addMarker(new MarkerOptions().position(coordinates)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed)));
                break;
            case STOP: map.addMarker(new MarkerOptions().position(coordinates).title(title));
                break;
            case POSITION:
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                currentMarker = map.addMarker(new MarkerOptions().position(coordinates)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));
                break;
            case ITINERARY:
                itineraryMarkers.add(map.addMarker(new MarkerOptions().position(coordinates)
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                ));
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
                String syncConnPref = pref.getString("pref_key_it_radius", "10");
                int radius = Integer.parseInt(syncConnPref);
                Circle circle = map.addCircle(new CircleOptions()
                        .center(coordinates)
                        .radius(radius)
                        .strokeColor(Color.argb(100,0,0,255))
                        .fillColor(Color.argb(50,0,0,255)));
                break;
            default: Log.e("MAP", "Marker type is not valid");
        }
    }

    public void clearItineraryMarkers() {
        for (Marker marker : itineraryMarkers) {
            marker.remove();
        }
        itineraryMarkers = new ArrayList<>();
    }

    @Override
    public void onClick(View v) {
        //do what you want to do when button is clicked
        System.out.println(v);
        switch(v.getId()){
            case R.id.stop_button:
                Log.i("Click", "stop_button");
                displayStopChoices();
                break;
            case R.id.change_map_button:
                Log.i("Click", "button_change_map");
                changeMap();
                break;
        }
    }

}
