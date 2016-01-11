package com.example.sisir.dietchecker.Zomato;

/**
 * Created by sisir on 10/1/16.
 */
public class ZomatoRestaurant {
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    Location location;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String name;
}
