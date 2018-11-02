package es.uma.lcc.neo.cintrano.neotrack.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.google.android.gms.maps.model.Marker;

import java.util.List;

import es.uma.lcc.neo.cintrano.neotrack.R;

public class ItineraryArrayAdapter extends ArrayAdapter<Marker> {
    private final Context context;
    private final List values;

    public ItineraryArrayAdapter(Context context, int layoutId, List<Marker> values) {
        super(context, layoutId, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_marker, null);
        }

        TextView textViewIndex = (TextView) convertView.findViewById(R.id.itinerary_point_index);
        TextView textViewText = (TextView) convertView.findViewById(R.id.itinerary_point_text);
        textViewIndex.setText(String.valueOf(position));
        Marker m = (Marker) values.get(position);
        String text =
                m.getTitle();// + "\n" + m.getPosition().latitude + " " + m.getPosition().longitude;
        textViewText.setText(text);

        return convertView;
    }
}