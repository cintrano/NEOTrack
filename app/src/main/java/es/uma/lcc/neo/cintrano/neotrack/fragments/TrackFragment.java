package es.uma.lcc.neo.cintrano.neotrack.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import es.uma.lcc.neo.cintrano.neotrack.R;


/**
 * Created by Christian Cintrano on 14/05/15.
 *
 */
public class TrackFragment extends Fragment {
    private TextView out;
    private View view;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.activity_tab_fragment_track, container, false);
        } catch (InflateException e) {
        /* map is already there, just return view as it is */
            Log.e("T", e.getMessage());
            e.printStackTrace();
        }
        out = (TextView) view.findViewById(R.id.track_text);
        out.setMovementMethod(new ScrollingMovementMethod());

        appendLog("Información: ");
        return view;
    }

    // Métodos para mostrar información
    public void appendLog(String text) {
        out.append(text + "\n");

        // Scrolling down
        final Layout layout = out.getLayout();
        if(layout != null){
            int scrollDelta = layout.getLineBottom(out.getLineCount() - 1)
                    - out.getScrollY() - out.getHeight();
            if(scrollDelta > 0)
                out.scrollBy(0, scrollDelta);
        }
    }
}
