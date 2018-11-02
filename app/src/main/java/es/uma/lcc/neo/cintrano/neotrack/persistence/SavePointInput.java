package es.uma.lcc.neo.cintrano.neotrack.persistence;

import android.location.Location;

/**
 * Created by CH on 14/02/2016.
 */
public class SavePointInput {

    private Itinerary itinerary;
    private Location location;
    private String cause;

    public SavePointInput() {
    }

    public SavePointInput(Itinerary itinerary, Location location) {
        this.itinerary = itinerary;
        this.location = location;
        this.cause = null;
    }

    public SavePointInput(Itinerary itinerary, Location location, String cause) {
        this.itinerary = itinerary;
        this.location = location;
        this.cause = cause;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

}
