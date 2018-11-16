package es.uma.lcc.neo.cintrano.neotrack.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import es.uma.lcc.neo.cintrano.neotrack.persistence.Sample;
import es.uma.lcc.neo.cintrano.neotrack.persistence.SampleDAO;

public class BackupUtilities {

    private static final String BACKUP_DB_NAME = "backup";
    private static final String PREF_LAST_DATE_BACKUP = "backupLastDate";
    private static final String PREF_DELAY_TO_BACKUP = "pref_key_delay_backup_database";

    public static void makeBackup(SampleDAO db, SimpleDateFormat sdf, Context context) {
        Calendar startDate = getLastBackup(sdf, context);
        Log.i("DB","Last backup was in : " + sdf.format(startDate.getTime()));
        Calendar endDate = Calendar.getInstance();
        String fileName = BACKUP_DB_NAME + "_" + sdf.format(startDate.getTime()) +
                "_" + sdf.format(endDate.getTime());
        if(saveFileAndRemove(db, fileName,startDate ,endDate, context)) {
            Log.i("DB","Set a new date for save backup");
            setNewDelayToBackup(sdf, context);
        }
    }

    private static boolean saveFileAndRemove(SampleDAO db, String fileName, Calendar dateStart,
                                             Calendar dateEnd, Context context) {
        if(saveFile(fileName, dateStart, dateEnd, context)) {
            Log.i("MainActivity", "Delete database");
            db.delete(dateStart, dateEnd);
            return true;
        } else {
            return false;
        }
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private static boolean saveFile(String fileName, Calendar dateStart,
                                    Calendar dateEnd, Context context) {
        Toast.makeText(context, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream out = null;
        SampleDAO db = new SampleDAO(context);
        db.open();
        List<Sample> data = db.get(dateStart, dateEnd);
        Log.i("DB", "Find " + data.size() + " elements");
        String extension = ".csv";
        String folderName = "/mdaFolder";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                if(dir.mkdirs()) Log.i("DB", "Folder created");
                File file = new File (dir, fileName + extension);
                out = new FileOutputStream(file);
            } else {
                out = context.openFileOutput(fileName + extension, Context.MODE_PRIVATE);
            }
            String head = "_id,latitude,longitude,session,stopType,comment,date\n";
            out.write(head.getBytes());
            for(Sample dc : data) {
                System.out.print(dc);
                out.write((String.valueOf(dc.getId()) + ",").getBytes());
                if(dc.getSession() != null) {
                    out.write(("\"" + dc.getSession() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                out.write((String.valueOf(dc.getLatitude()) + ",").getBytes());
                out.write((String.valueOf(dc.getLongitude()) + ",").getBytes());
                if(dc.getStopType() != null) {
                    out.write(("\"" + dc.getStopType() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                if(dc.getComment() != null) {
                    out.write(("\"" + dc.getComment() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                out.write(("\"" + dc.getDate() + "\",").getBytes());
                out.write((String.valueOf(dc.getSensorAcceleration()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorPressure()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorTemperature()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorHumidity()) + "\n").getBytes());
            }
            out.flush();
            out.close();
            Log.i("DB", "File saved with name: " + fileName);
            Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                db.close();
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Calendar getLastBackup(SimpleDateFormat sdf, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        String lastDate = settings.getString(PREF_LAST_DATE_BACKUP, "");
        Calendar cMinDate = Calendar.getInstance();
        try {
            cMinDate.setTime(sdf.parse(lastDate));
        } catch (ParseException e) {

            e.printStackTrace();
        }
        return cMinDate;
    }

    public static boolean checkBackupTime(SimpleDateFormat sdf, Context context) {
        Log.i("Preferences","Checking preferences");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int delay = Integer.parseInt(settings.getString(PREF_DELAY_TO_BACKUP, "0")); // num of days

        Calendar minDateForNewBackup = getLastBackup(sdf, context);
        minDateForNewBackup.add(Calendar.DATE, delay);
        Calendar now = Calendar.getInstance();

        return now.compareTo(minDateForNewBackup) >= 0;
    }

    private static void setNewDelayToBackup(SimpleDateFormat sdf, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Calendar newDate = Calendar.getInstance();

        String newLastDate = sdf.format(newDate.getTime());

        // Update date of last backup
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_LAST_DATE_BACKUP,newLastDate);
        editor.apply();
    }
}
