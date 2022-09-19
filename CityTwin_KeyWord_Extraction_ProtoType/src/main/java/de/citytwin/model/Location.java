package de.citytwin.model;

import de.citytwin.catalog.HasName;

import java.util.Set;

import javax.annotation.Nonnull;

/**
 * this class represent location
 *
 * @author Maik, FH Erfurt
 */
public class Location implements HasName {

    /** index in dump */
    public static final int INDEX_ID = 0;
    public static final int INDEX_NAME = 1;
    public static final int INDEX_SYNONYMS = 3;
    public static final int INDEX_LATITUDE = 4;
    public static final int INDEX_LONGITUDE = 5;
    public static final int INDEX_FEATURECODE = 7;

    private Long id;
    private String name;
    private String featureCode;
    private double latitude;
    private double longitude;
    private Set<String> synonyms;

    @Override
    public String getName() {
        return name;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    private static final double EARTH_RADIUS = 6371.0; // km value;

    public Location(String name, String featureCode, double latitude, double longitude, Set<String> synonyms) {
        this.id = 0L;
        this.name = name;
        this.featureCode = featureCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.synonyms = synonyms;
    }

    public Location(Long id, String name, String featureCode, double latitude, double longitude, Set<String> synonyms) {
        this.id = id;
        this.name = name;
        this.featureCode = featureCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.synonyms = synonyms;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureCode == null) ? 0 : featureCode.hashCode());
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((synonyms == null) ? 0 : synonyms.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Location other = (Location)obj;
        if (featureCode == null) {
            if (other.featureCode != null)
                return false;
        } else if (!featureCode.equals(other.featureCode))
            return false;
        if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
            return false;
        if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (synonyms == null) {
            if (other.synonyms != null)
                return false;
        } else if (!synonyms.equals(other.synonyms))
            return false;
        return true;
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

    public void setSynonyms(@Nonnull Set<String> synonyms) {
        this.synonyms = synonyms;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

}
