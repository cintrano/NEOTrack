package es.uma.lcc.neo.cintrano.neotrack;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import es.uma.lcc.neo.cintrano.neotrack.adapters.ItineraryArrayAdapter;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Itinerary;
import es.uma.lcc.neo.cintrano.neotrack.persistence.ItineraryDAO;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Point;

/**
 * Created by Christian Cintrano.
 */
public class ItineraryMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "ItineraryMapActivity";
    private static final String EXTRA_TAB = "newItinerary";
    private static final String DEFAULT_ITINERARY_NAME = "Sin nombre";
    private static final int ZOOM = 15;
    private static final String GPS_LOADING = "Iniciando conexión GPS. Por favor, espere.";
    private static final String ADDRESS_NOT_FOUND = "Dirección no encontrada";

    private List<Marker> points;
    private ProgressDialog dialogWait; // FALTA EL QUITARLO CUANDO EL MAPA TERMINE DE CARGAR
    private GoogleMap map;
    private ArrayAdapter arrayAdapter;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary_map);

        configureDialogWait();

        points = new ArrayList<>();
        //((MapFragment) getFragmentManager().findFragmentById(R.id.map_itinerary)).getMapAsync(this);
        //map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_itinerary)).getMap();
        if (map == null) {
            SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_itinerary);
            mapFrag.getMapAsync(this);
        }

        configureListView();

        Bundle newItinerary = getIntent().getParcelableExtra(EXTRA_TAB);
        if(newItinerary != null) {
            loadEditableData((Itinerary) Objects.requireNonNull(newItinerary.getParcelable(EXTRA_TAB)));
        }

        geocoder = new Geocoder(this, Locale.getDefault());
    }

    private void loadEditableData(Itinerary itinerary) {
        TextView textViewName = this.findViewById(R.id.editText_itinerary_name);
        textViewName.setText(itinerary.getName());
        Log.i("------", "itinerary.....");
        Log.i("------", itinerary.toString());
        for(Object point : itinerary.getPoints()) {
            Log.i("------", point.toString());
            Marker marker = map.addMarker(new MarkerOptions().position(
                    new LatLng(((Point) point).getLatitude(), ((Point) point).getLongitude())));
            marker.setTitle(((Point) point).getAddress());
            points.add(marker);
        }
        arrayAdapter.notifyDataSetChanged();
    }

    private void configureListView() {
        ListView listView = this.findViewById(R.id.points_list);
        arrayAdapter = new ItineraryArrayAdapter(this, R.layout.list_marker, points);
        listView.setAdapter(arrayAdapter);
        listView.setTextFilterEnabled(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {}
        });
    }

    private void configureMapActions() {
        //Behavior OnClick map - create mark with number
//        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
//            @Override
//            public void onMapClick(LatLng latLng) {
//                Marker marker = map.addMarker(new MarkerOptions().position(latLng));
//                points.add(marker);
//            }
//        });

        CameraUpdate center= CameraUpdateFactory.newLatLng(
                //new LatLng(36.7176109,-4.42346));
                new LatLng(36.720957,-4.4209296));
        CameraUpdate zoom= CameraUpdateFactory.zoomTo(ZOOM);
        map.moveCamera(center);
        map.animateCamera(zoom);

        map.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // Display Options
                Log.i("Itinerary", "Removing marker");
                points.remove(marker);
//                marker.showInfoWindow();
                marker.remove();
                arrayAdapter.notifyDataSetChanged();
                return true;
            }
        });
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //View markerView =
                //        ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                //        .inflate(R.layout.custom_marker_layout, null);
                Marker marker = map.addMarker(new MarkerOptions().position(latLng));
                marker.setTitle(getAddress(latLng.latitude, latLng.longitude));
                points.add(marker);

                arrayAdapter.notifyDataSetChanged();
            }
        });
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                Log.i("MAP", "onMapLoaded");
                dialogWait.dismiss();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        dialogWait.dismiss();
        map = googleMap;
        configureMapActions();
    }

    private void configureDialogWait() {
        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage(GPS_LOADING);
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
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

    public class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        MarkerInfoWindowAdapter() {
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return getLayoutInflater().inflate(R.layout.infowindow_layout, null);
        }
    }

    public void sendMessageSaveItinerary(View view) {
        List<Point> latLngList = new ArrayList<>();
        for(Marker m : points) {
            latLngList.add(new Point(m.getPosition().latitude, m.getPosition().longitude, m.getTitle()));
        }
        TextView textViewName = this.findViewById(R.id.editText_itinerary_name);
        String name = textViewName.getText().length() == 0 ? DEFAULT_ITINERARY_NAME
                : textViewName.getText().toString();

        Itinerary itinerary = new Itinerary(name, latLngList);
        Log.i("+++++++++", "sendMessageSaveItinerary");
        Log.i("+++++++++", itinerary.toString());

        ItineraryDAO db = new ItineraryDAO(ItineraryMapActivity.this);
        db.open();
        db.create(itinerary);
        db.close();

        Intent intent = new Intent(this, ItineraryActivity.class);
        startActivity(intent);
    }

    public String getAddress(double latitude, double longitude) {
        List<Address> addresses = null;
        String output;

        try {
            // We need only one result
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException | IllegalArgumentException ioException) {
            // Catch network or other I/O problems.
            Log.e(TAG, ioException.getMessage(), ioException);
        }

        if (addresses == null || addresses.size()  == 0) {
            Log.e(TAG, getString(R.string.error_no_address_found));
            output = ADDRESS_NOT_FOUND;
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            output = TextUtils.join(" ", addressFragments);
        }

        return output;
    }

}


