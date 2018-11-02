package es.uma.lcc.neo.cintrano.neotrack.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 5/05/15.
 *
 * DAO for DataCapture points
 */

public class SampleDAO {

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss",Locale.US);

    private SQLiteDatabase db;
    private MySQLiteOpenHelper dbHelper;
    private String[] columns = {MySQLiteOpenHelper.TableSample.COLUMN_ID,
            MySQLiteOpenHelper.TableSample.COLUMN_SESSION, MySQLiteOpenHelper.TableSample.COLUMN_LATITUDE,
            MySQLiteOpenHelper.TableSample.COLUMN_LONGITUDE, MySQLiteOpenHelper.TableSample.COLUMN_STOP_TYPE,
            MySQLiteOpenHelper.TableSample.COLUMN_COMMENT, MySQLiteOpenHelper.TableSample.COLUMN_DATE,
            MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_ACCELERATION, MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_PRESSURE,
            MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_LIGHT, MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_TEMPERATURE,
            MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_HUMIDITY};

    public SampleDAO(Context context) {
        dbHelper = new MySQLiteOpenHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void create(Sample sample) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.TableSample.COLUMN_LATITUDE, sample.getLatitude());
        values.put(MySQLiteOpenHelper.TableSample.COLUMN_LONGITUDE, sample.getLongitude());
        if(sample.getSession() != null)
            values.put(MySQLiteOpenHelper.TableSample.COLUMN_SESSION, sample.getSession());
        if(sample.getStopType() != null)
            values.put(MySQLiteOpenHelper.TableSample.COLUMN_STOP_TYPE, sample.getStopType());
        if(sample.getComment() != null)
            values.put(MySQLiteOpenHelper.TableSample.COLUMN_COMMENT, sample.getComment());
        if(sample.getDate() != null)
            values.put(MySQLiteOpenHelper.TableSample.COLUMN_DATE, sample.getDate());
        values.put(MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_ACCELERATION, sample.getSensorAcceleration());
        values.put(MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_PRESSURE, sample.getSensorPressure());
        values.put(MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_LIGHT, sample.getSensorLight());
        values.put(MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_TEMPERATURE, sample.getSensorTemperature());
        values.put(MySQLiteOpenHelper.TableSample.COLUMN_SENSOR_HUMIDITY, sample.getSensorHumidity());
        db.insert(MySQLiteOpenHelper.TableSample.TABLE_NAME, null, values);
    }

    public List<Sample> getAll() {
        List<Sample> listSample = new ArrayList<>();

        Cursor cursor = db.query(MySQLiteOpenHelper.TableSample.TABLE_NAME, columns, null, null,
                null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Sample sample = cursorToDataCapture(cursor);
            listSample.add(sample);
            cursor.moveToNext();
        }

        cursor.close();
        return listSample;
    }


    /**
     * List of elements between two dates
     *
     * @param dateStart initial date
     * @param dateEnd finish date
     * @return list of results
     */
    public List<Sample> get(Calendar dateStart, Calendar dateEnd) {
        List<Sample> listSample = new ArrayList<>();

        String[] arg = new String[] { dateFormatter.format(dateStart.getTime()),
                dateFormatter.format(dateEnd.getTime())};
        String where = MySQLiteOpenHelper.TableSample.COLUMN_DATE + ">=? and " + MySQLiteOpenHelper.TableSample.COLUMN_DATE + "<=?";
        Cursor cursor = db.query(MySQLiteOpenHelper.TableSample.TABLE_NAME, columns, where,arg, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Sample sample = cursorToDataCapture(cursor);
            listSample.add(sample);
            cursor.moveToNext();
        }

        cursor.close();
        return listSample;
    }
    /**
     * List of elements of a specific session
     *
     * @param sessionId sessionId
     * @return list of results
     */
    public List<Sample> get(String sessionId) {
        List<Sample> listSample = new ArrayList<>();

        String[] arg = new String[] {sessionId};
        String where = MySQLiteOpenHelper.TableSample.COLUMN_SESSION + "=?";
        Cursor cursor = db.query(MySQLiteOpenHelper.TableSample.TABLE_NAME, columns, where,arg, null, null, MySQLiteOpenHelper.TableSample.COLUMN_DATE + " ASC", null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Sample sample = cursorToDataCapture(cursor);
            listSample.add(sample);
            cursor.moveToNext();
        }

        cursor.close();
        return listSample;
    }

    public void delete(Sample sample) {
        long id = sample.getId();
        db.delete(MySQLiteOpenHelper.TableSample.TABLE_NAME, MySQLiteOpenHelper.TableSample.COLUMN_ID + " = " + id, null);
    }

    /**
     * Delete all rows between two dates
     * @param dateStart initial date
     * @param dateEnd finish date
     */
    public void delete(String dateStart, String dateEnd) {
        String where =  MySQLiteOpenHelper.TableSample.COLUMN_DATE + " <= \"" + dateEnd +
                "\" and " + MySQLiteOpenHelper.TableSample.COLUMN_DATE + " >= \"" + dateStart + "\"";
        db.delete(MySQLiteOpenHelper.TableSample.TABLE_NAME,where , null);
    }

    /**
     * Delete all rows between two dates
     * @param dateStart initial date
     * @param dateEnd finish date
     */
    public void delete(Calendar dateStart, Calendar dateEnd) {
        String FORMAT_DATE = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        delete(sdf.format(dateStart.getTime()),sdf.format(dateEnd.getTime()));
    }

    public void deleteAll() {
        db.delete(MySQLiteOpenHelper.TableSample.TABLE_NAME, null, null);
    }

    private Sample cursorToDataCapture(Cursor cursor) {
        Sample sample = new Sample();
        sample.setId(cursor.getLong(0));
        sample.setSession(cursor.getString(1));
        sample.setLatitude(cursor.getDouble(2));
        sample.setLongitude(cursor.getDouble(3));
        sample.setStopType(cursor.getString(4));
        sample.setComment(cursor.getString(5));
        sample.setDate(cursor.getString(6));
        sample.setSensorAcceleration(cursor.getDouble(7));
        sample.setSensorPressure(cursor.getDouble(8));
        sample.setSensorLight(cursor.getDouble(9));
        sample.setSensorTemperature(cursor.getDouble(10));
        sample.setSensorHumidity(cursor.getDouble(11));
        return sample;
    }
}
