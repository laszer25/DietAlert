package com.example.sisir.dietchecker.utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by sisir on 10/1/16.
 */
public class Utils {

    public static final String CATEGORY_LOCATION_SERVICES = "CATEGORY_LOCATION_SERVICES";
    public static final String ACTION_REFRESH_STATUS_LIST = "ACTION_REFRESH_STATUS_LIST";
    public static final String ACTION_GEOFENCE_STATUS = "ACTION_GEOFENCE_STATUS";
    public static Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
}
