package es.uma.lcc.neo.cintrano.neotrack.persistence;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christian Cintrano.
 */
public class Itinerary implements Parcelable {

    private long id;
    private String name;
    private List<Point> points;

    public Itinerary() {
    }

    public Itinerary(String name, List<Point> points) {
        this.name = name;
        this.points = points;
    }
    public Itinerary(Parcel in) {
        this.name = in.readString();
        this.points = new ArrayList<>();
        in.readList(points,Point.class.getClassLoader());
    }
    public Itinerary(JSONObject obj) {
        try {
            this.name = obj.getString("name");
            this.points = new ArrayList<>();
            JSONArray array = obj.getJSONArray("points");
            for (int i = 0; i < array.length(); i++) {
                this.points.add(new Point(array.getJSONObject(0)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return "Itinerary{" +
                "name='" + name + '\'' +
                ", points=" + points +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeList(points);
    }

    public static final Creator CREATOR = new Creator() {
        public Itinerary createFromParcel(Parcel in) {
            return new Itinerary(in);
        }

        public Itinerary[] newArray(int size) {
            return new Itinerary[size];
        }
    };

    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            List<JSONObject> pointList = new ArrayList<>();
            for (Object p : points) {
                JSONObject jsonPoint = new JSONObject();
                jsonPoint.put("latitude", ((Point) p).getLatitude());
                jsonPoint.put("longitude", ((Point) p).getLongitude());
                jsonPoint.put("address", ((Point) p).getAddress());
                pointList.add(jsonPoint);
            }
            jsonObject.put("points", new JSONArray(pointList));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
