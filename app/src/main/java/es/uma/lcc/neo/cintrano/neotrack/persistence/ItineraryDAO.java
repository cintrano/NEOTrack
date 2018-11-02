package es.uma.lcc.neo.cintrano.neotrack.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christian Cintrano.
 */
public class ItineraryDAO {
    private SQLiteDatabase db;
    private MySQLiteOpenHelper dbHelper;
    private String[] columns = {MySQLiteOpenHelper.TableItinerary.COLUMN_ID,
            MySQLiteOpenHelper.TableItinerary.COLUMN_NAME,
            MySQLiteOpenHelper.TableItinerary.COLUMN_LATITUDE,
            MySQLiteOpenHelper.TableItinerary.COLUMN_LONGITUDE,
            MySQLiteOpenHelper.TableItinerary.COLUMN_ADDRESS};

    public ItineraryDAO(Context context) {
        dbHelper = new MySQLiteOpenHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void create(Itinerary itinerary) {
        Log.i("dbdbdbdb", "create");
        for(Object p : itinerary.getPoints()) {
            ContentValues values = new ContentValues();
            values.put(MySQLiteOpenHelper.TableItinerary.COLUMN_NAME, itinerary.getName());
            Log.i("--------", itinerary.toString());
            values.put(MySQLiteOpenHelper.TableItinerary.COLUMN_LATITUDE, ((Point) p).getLatitude());
            values.put(MySQLiteOpenHelper.TableItinerary.COLUMN_LONGITUDE, ((Point) p).getLongitude());
            values.put(MySQLiteOpenHelper.TableItinerary.COLUMN_ADDRESS, ((Point) p).getAddress());
            db.insert(MySQLiteOpenHelper.TableItinerary.TABLE_NAME, null, values);
        }
    }

    public List<Itinerary> getAll() {
        Log.i("dbdbdbdb", "getAll");
        List<Itinerary> listItinerary = new ArrayList<>();

        Cursor cursor = db.query(MySQLiteOpenHelper.TableItinerary.TABLE_NAME, columns, null, null,
                null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Log.i("CURSOR------", cursor.toString());
            Itinerary itinerary = cursorToItinerary(cursor);
            Log.i("ITINERARY------", itinerary.toString());
            int index = -1;
            for (int i = 0; i < listItinerary.size(); i++) {
                if(listItinerary.get(i).getName().equals(itinerary.getName())) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                listItinerary.add(itinerary);
            } else {
                listItinerary.get(index).getPoints().add(itinerary.getPoints().get(0));
            }
            cursor.moveToNext();
        }

        cursor.close();
        return listItinerary;
    }

    public List<Itinerary> get(String name) {
        List<Itinerary> listItinerary = new ArrayList<>();
        if(name == null || name.equals("")) {
            name = "";
        }
        String[] arg = new String[] {"%" + name + "%"};
        String where = " UPPER(" + MySQLiteOpenHelper.TableItinerary.COLUMN_NAME + ") like UPPER(?)";
        Cursor cursor = db.query(MySQLiteOpenHelper.TableItinerary.TABLE_NAME, columns,
                where,arg, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Itinerary streetTrack = cursorToItinerary(cursor);
            listItinerary.add(streetTrack);
            cursor.moveToNext();
        }

        cursor.close();
        return listItinerary;
    }

    public void delete(Itinerary itinerary) {
        String name = itinerary.getName();
        String[] arg = new String[] {"%" + name + "%"};
        String where = " UPPER(" + MySQLiteOpenHelper.TableItinerary.COLUMN_NAME + ") like UPPER(?)";
        db.delete(MySQLiteOpenHelper.TableItinerary.TABLE_NAME, where, arg);
    }

    public void deleteAll() {
        db.delete(MySQLiteOpenHelper.TableItinerary.TABLE_NAME, null, null);
    }

    private Itinerary cursorToItinerary(Cursor cursor) {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(cursor.getLong(0));
        itinerary.setName(cursor.getString(1));
        Point point = new Point();
        point.setLatitude(cursor.getDouble(2));
        point.setLongitude(cursor.getDouble(3));
        point.setAddress(cursor.getString(4));
        List points = new ArrayList();
        points.add(point);
        itinerary.setPoints(points);
        return itinerary;
    }

}
