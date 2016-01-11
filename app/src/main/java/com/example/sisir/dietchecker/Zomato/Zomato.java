package com.example.sisir.dietchecker.Zomato;

import java.util.Map;

/**
 * Created by sisir on 10/1/16.
 */
public class Zomato {
    public ZomatoLocation getLocation() {
        return location;
    }

    public void setLocation(ZomatoLocation location) {
        this.location = location;
    }

    public Map<String, Restaurant> getNearbyRestaurants() {
        return nearbyRestaurants;
    }

    public void setNearbyRestaurants(Map<String, Restaurant> nearbyRestaurants) {
        this.nearbyRestaurants = nearbyRestaurants;
    }

    ZomatoLocation location;
    Map<String,Restaurant> nearbyRestaurants;
}
