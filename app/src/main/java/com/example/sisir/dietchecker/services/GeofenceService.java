package com.example.sisir.dietchecker.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.sisir.dietchecker.R;
import com.example.sisir.dietchecker.utils.Utils;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by sisir on 12/1/16.
 */
public class GeofenceService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *

     */
    public GeofenceService() {
        super("geofence");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.d("Error Code", String.valueOf(geofencingEvent.getErrorCode()));
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List triggeringGeofences = geofencingEvent.getTriggeringGeofences();


            // Send notification and log the transition details.
            Log.i("Success", String.valueOf(geofenceTransition));
            Intent broadcastIntent = new Intent();


            broadcastIntent.addCategory(Utils.CATEGORY_GEOFENCE_SERVICES);

            broadcastIntent.setAction(Utils.ACTION_GEOFENCE_STATUS);

            broadcastIntent.putExtra("GEOFENCE_TRANSITION", geofenceTransition);

            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        } else {
            // Log the error.
            Log.d("Error", String.valueOf(geofenceTransition));
        }
    }
}
