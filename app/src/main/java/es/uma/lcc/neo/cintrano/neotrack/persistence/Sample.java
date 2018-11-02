package es.uma.lcc.neo.cintrano.neotrack.persistence;

/**
 * Created by Christian Cintrano on 5/05/15.
 *
 * DataCapture Entity Class
 */
public class Sample {
    private long id;
    private String session;
    private double latitude;
    private double longitude;
    private String stopType;
    private String comment;
    private String date;
    private double sensorAcceleration;
    private double sensorPressure;
    private double sensorLight;
    private double sensorTemperature;
    private double sensorHumidity;

    public Sample() {
        super();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String address) {
        this.session = address;
    }

    public String getStopType() {
        return stopType;
    }

    public void setStopType(String stopType) {
        this.stopType = stopType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getSensorAcceleration() {
        return sensorAcceleration;
    }

    public void setSensorAcceleration(double sensorAcceleration) {
        this.sensorAcceleration = sensorAcceleration;
    }

    public double getSensorPressure() {
        return sensorPressure;
    }

    public void setSensorPressure(double sensorPressure) {
        this.sensorPressure = sensorPressure;
    }

    public double getSensorLight() {
        return sensorLight;
    }

    public void setSensorLight(double sensorLight) {
        this.sensorLight = sensorLight;
    }


    public double getSensorTemperature() {
        return sensorTemperature;
    }

    public void setSensorTemperature(double sensorTemperature) {
        this.sensorTemperature = sensorTemperature;
    }

    public double getSensorHumidity() {
        return sensorHumidity;
    }

    public void setSensorHumidity(double sensorHumidity) {
        this.sensorHumidity = sensorHumidity;
    }
}
