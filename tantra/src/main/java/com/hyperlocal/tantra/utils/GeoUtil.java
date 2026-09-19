package com.hyperlocal.tantra.utils;

/**
 * Haversine distance and bounding-box utilities for geo-radius queries.
 * Used as a second-pass exact filter on top of the bounding-box SQL pre-filter.
 */
public class GeoUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /** Bounding-box degree offsets for a given radius in km. */
    public static double latOffset(double radiusKm) {
        return radiusKm / 111.0;
    }

    public static double lngOffset(double radiusKm, double lat) {
        return radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
    }

    /**
     * Haversine great-circle distance in kilometres between two lat/lng points.
     * Call this for exact filtering after the bounding-box SQL pre-filter.
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.pow(Math.sin(dLng / 2), 2);
        return EARTH_RADIUS_KM * 2 * Math.asin(Math.sqrt(a));
    }

    /** Round distance to 1 decimal place for display. */
    public static double roundKm(double km) {
        return Math.round(km * 10.0) / 10.0;
    }
}
