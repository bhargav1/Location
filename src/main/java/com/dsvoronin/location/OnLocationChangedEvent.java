package com.dsvoronin.location;

import android.location.Location;

/**
 * EventBus event
 */
public class OnLocationChangedEvent {

    public Location location;

    public OnLocationChangedEvent(Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "OnLocationChangedEvent{" +
                "location=" + location +
                '}';
    }
}
