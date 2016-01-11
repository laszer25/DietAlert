package com.example.sisir.dietchecker.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.sisir.dietchecker.MainActivity;
import com.example.sisir.dietchecker.R;
import com.example.sisir.dietchecker.Zomato.Restaurant;
import com.example.sisir.dietchecker.Zomato.Zomato;
import com.example.sisir.dietchecker.ZomatoApiConnector.ZomatoApiConnector;
import com.example.sisir.dietchecker.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sisir on 10/1/16.
 */
public class DietControlService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    IntentFilter mBroadcastFilter;
    ArrayList<LatLng> polygon = null;
    Location mLastLocation;
    int counter = 0;
    int previousActivity;
    private LocalBroadcastManager mBroadcastManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupGoogleApi();
        checkForActivityChange();
        setupBroadcastListener();
        return Service.START_STICKY;
    }

    private void setupBroadcastListener() {
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        // Create a new Intent filter for the broadcast receiver
        mBroadcastFilter = new IntentFilter(Utils.ACTION_REFRESH_STATUS_LIST);
        mBroadcastFilter.addCategory(Utils.CATEGORY_LOCATION_SERVICES);
        BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int activity = intent.getIntExtra("DETECTED_ACTIVITY", 0);
                int confidence = intent.getIntExtra("CONFIDENCE", 0);
                if (confidence > 50) {
                    //Toast.makeText(context, activity + " " + confidence, Toast.LENGTH_SHORT).show();
                    if (activity == previousActivity) {
                        counter++;
                    } else {
                        if (previousActivity != DetectedActivity.STILL && activity == DetectedActivity.STILL && counter >= 10) {
                            checkForActivityChange();
                        }
                        previousActivity = activity;
                        counter = 0;
                    }
                }
            }
        };
        mBroadcastManager.registerReceiver(updateListReceiver, mBroadcastFilter);
    }

    private void setupGoogleApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void checkForActivityChange() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (isWithinGeoFence()) {
            if (isNearRestaurant()) {
                sendNotification();
            }
        } else {
            makeServiceCall();
        }
    }

    private void sendNotification() {

        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setContentTitle("You're on a diet")
                        .setContentText("You are near a restaurant, remember that you are on a diet");
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, mBuilder.build());
    }

    private void makeServiceCall() {
        ZomatoApiConnector connector = new ZomatoApiConnector(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if(mLastLocation != null) {
            connector.getNearbyRestaurants(getString(R.string.zomato_api_key), "From Service", mLastLocation, onRestaurantsRecieved(), onError());
        }
    }

    private Response.ErrorListener onError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        };
    }

    private Response.Listener<JSONObject> onRestaurantsRecieved() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Zomato zomato = Utils.gson.fromJson(response.toString(), Zomato.class);
                List<Restaurant> restaurants = new ArrayList<>(zomato.getNearbyRestaurants().values());
                for (Restaurant restaurant : restaurants) {
                    double lat = Double.valueOf(restaurant.getRestaurant().getLocation().getLatitude());
                    double lng = Double.valueOf(restaurant.getRestaurant().getLocation().getLongitude());
                    LatLng latLng = new LatLng(lat, lng);
                    polygon.add(latLng);
                }
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                if(mLastLocation != null) {
                    LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    if(isPointInPolygon(latLng, polygon)){
                        if(isNearRestaurant()) {
                            sendNotification();
                        }
                    }
                }
            }
        };
    }

    private boolean isNearRestaurant() {
        for(LatLng latLng : polygon) {
            double dist = distance(latLng, new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            if(dist < 20) {
                return true;
            }
        }
        return false;
    }

    private double distance(LatLng l1, LatLng l2) {
        double lat1 = l1.latitude;
        double lon1 = l1.longitude;
        double lat2 = l2.latitude;
        double lon2 = l2.longitude;
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private boolean isWithinGeoFence() {
        if (polygon == null) {
            return false;
        }
        if(mLastLocation != null) {
            LatLng point = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            if(isPointInPolygon(point, polygon)) {
                return true;
            }
            else {
                return false;
            }
        }
        return false;
    }

    private boolean activityStopped() {
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                1000 /* detection interval */,
                pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    private boolean isPointInPolygon(LatLng tap, ArrayList<LatLng> vertices) {
        int intersectCount = 0;
        for (int j = 0; j < vertices.size() - 1; j++) {
            if (rayCastIntersect(tap, vertices.get(j), vertices.get(j + 1))) {
                intersectCount++;
            }
        }

        return ((intersectCount % 2) == 1); // odd = inside, even = outside;
    }

    private boolean rayCastIntersect(LatLng tap, LatLng vertA, LatLng vertB) {
        double aY = vertA.latitude;
        double bY = vertB.latitude;
        double aX = vertA.longitude;
        double bX = vertB.longitude;
        double pY = tap.latitude;
        double pX = tap.longitude;
        if ((aY > pY && bY > pY) || (aY < pY && bY < pY)
                || (aX < pX && bX < pX)) {
            return false; // a and b can't both be above or below pt.y, and a or
            // b must be east of pt.x
        }
        double m = (aY - bY) / (aX - bX); // Rise over run
        double bee = (-aX) * m + aY; // y = mx + b
        double x = (pY - bee) / m; // algebra is neat!
        return x > pX;
    }
}
