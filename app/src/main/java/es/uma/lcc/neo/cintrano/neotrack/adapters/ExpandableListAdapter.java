package es.uma.lcc.neo.cintrano.neotrack.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import es.uma.lcc.neo.cintrano.neotrack.ItineraryActivity;
import es.uma.lcc.neo.cintrano.neotrack.R;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Itinerary;
import es.uma.lcc.neo.cintrano.neotrack.persistence.Point;


public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context _context;
    private List<Itinerary> listData;

    public ExpandableListAdapter(Context context, List<Itinerary> listData) {
        this._context = context;
        this.listData = listData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return listData.get(groupPosition).getPoints().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        Point p = (Point) getChild(groupPosition, childPosition);
        final String childText = p.getAddress() + "\n" + p.getLatitude() + " " + p.getLongitude();

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = convertView.findViewById(R.id.lblListItem);

        txtListChild.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return listData.get(groupPosition).getPoints().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listData.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return listData.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        String headerTitle = ((Itinerary) getGroup(groupPosition)).getName();
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }
        ImageButton editButton = convertView.findViewById(R.id.button_edit);
        editButton.setFocusable(false);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ItineraryActivity) _context).editItinerary(groupPosition);
            }
        });

        final ImageButton removeButton = convertView.findViewById(R.id.button_remove);
        removeButton.setFocusable(false);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ItineraryActivity) _context).removeItinerary(groupPosition);
            }
        });

        TextView lblListHeader = convertView.findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
