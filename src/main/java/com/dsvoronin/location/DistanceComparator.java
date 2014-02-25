package com.dsvoronin.location;

import android.location.Location;

import java.util.Comparator;

public class DistanceComparator<T extends Locationable> implements Comparator<T> {

    private Location location;

    public DistanceComparator(Location location) {
        this.location = location;
    }

    public static double distance(Location userLocation, Locationable locationable) {
        Location fake = new Location("Locationable");
        fake.setLatitude(locationable.getLatitude());
        fake.setLongitude(locationable.getLongitude());
        return distanceBetween(userLocation, fake);
    }

    private static double distanceBetween(Location l1, Location l2) {
        double lat1 = l1.getLatitude();
        double lon1 = l1.getLongitude();
        double lat2 = l2.getLatitude();
        double lon2 = l2.getLongitude();
        double R = 6371; // km
        double dLat = (lat2 - lat1) * Math.PI / 180;
        double dLon = (lon2 - lon1) * Math.PI / 180;
        lat1 = lat1 * Math.PI / 180;
        lat2 = lat2 * Math.PI / 180;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000;
    }

    @Override
    public int compare(T one, T two) {
        if (location != null) {
            return Double.compare(distance(location, one), distance(location, two));
        } else {
            return 0;
        }
    }
}