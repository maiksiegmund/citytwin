package de.citytwin.model;

import java.util.Set;

/**
 * this class represent location
 *
 * @author Maik, SRP GmbH, Berlin
 */
public class Location {

    /** index in dump */
    public static final int INDEX_NAME = 1;
    public static final int INDEX_SYNONYMS = 3;
    public static final int INDEX_LATITUDE = 4;
    public static final int INDEX_LONGITUDE = 5;
    public static final int INDEX_FEATURECODE = 7;

    private String name;
    private String featureCode;
    private double latitude;
    private double longitude;
    private Set<String> synonyms;

    private static final double EARTH_RADIUS = 6371.0; // km value;

    public Location(String name, String featureCode, double latitude, double longitude, Set<String> synonyms) {
        this.name = name;
        this.featureCode = featureCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.synonyms = synonyms;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + featureCode.hashCode() + synonyms.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Location) {
            Location temp = (Location)object;
            return this.name.equals(temp.name);
        }
        return super.equals(object);
    }

    public double distanceTo(Location location) {
        double startLatitude = Math.toRadians(this.latitude);
        double startLongitude = Math.toRadians(this.longitude);
        double endLatitude = Math.toRadians(location.latitude);
        double endLongitude = Math.toRadians(location.longitude);
        if (startLatitude == endLatitude && startLongitude == endLongitude) {
            return 0d;
        }
        startLatitude = Math.toRadians(startLatitude);
        startLongitude = Math.toRadians(startLongitude);
        endLatitude = Math.toRadians(endLatitude);
        endLongitude = Math.toRadians(endLongitude);

        double distance = Math.pow(Math.sin((endLatitude - startLatitude) / 2.0), 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                        * Math.pow(Math.sin((endLongitude - startLongitude) / 2.0), 2);
        return 2.0 * EARTH_RADIUS * Math.asin(Math.sqrt(distance));
    }

    @Override
    public String toString() {
        return name + " (" + latitude + ", " + longitude + ") " + featureCode;
    }

    public Set<String> getSynonyms() {
        return this.synonyms;
    }

    public void setSynonyms(Set<String> synonyms) {
        this.synonyms = synonyms;
    }

}
