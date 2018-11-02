package es.uma.lcc.neo.cintrano.neotrack.persistence;

/**
 * Created by CH on 14/02/2016.
 */
public class SavePointInput2 {

    private Itinerary itinerary;
    private Sample location;

    public SavePointInput2() {
    }

    public SavePointInput2(Itinerary itinerary, Sample location) {
        this.itinerary = itinerary;
        this.location = location;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
    }

    public Sample getLocation() {
        return location;
    }

    public void setLocation(Sample location) {
        this.location = location;
    }
}
