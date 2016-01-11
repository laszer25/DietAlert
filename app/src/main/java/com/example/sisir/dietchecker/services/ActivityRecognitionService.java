package com.example.sisir.dietchecker.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.example.sisir.dietchecker.utils.Utils;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by sisir on 10/1/16.
 */
public class ActivityRecognitionService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *

     */
    public ActivityRecognitionService() {
        super("tag");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult result =
                    ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity
                    = result.getMostProbableActivity();


            int confidence = mostProbableActivity.getConfidence();


            int activityType = mostProbableActivity.getType();

            Intent broadcastIntent = new Intent();


            broadcastIntent.addCategory(Utils.CATEGORY_LOCATION_SERVICES);

            broadcastIntent.setAction(Utils.ACTION_REFRESH_STATUS_LIST);

            broadcastIntent.putExtra("DETECTED_ACTIVITY", activityType);

            broadcastIntent.putExtra("CONFIDENCE", confidence);

            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        }
    }
}