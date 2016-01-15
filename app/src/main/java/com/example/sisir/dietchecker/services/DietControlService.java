package com.example.sisir.dietchecker.services;

import android.Manifest;
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
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.sisir.dietchecker.MainActivity;
import com.example.sisir.dietchecker.R;
import com.example.sisir.dietchecker.Zomato.Restaurant;
import com.example.sisir.dietchecker.Zomato.Zomato;
import com.example.sisir.dietchecker.Zomato.ZomatoRestaurant;
import com.example.sisir.dietchecker.ZomatoApiConnector.ZomatoApiConnector;
import com.example.sisir.dietchecker.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
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
    IntentFilter mBroadcastFilterGeofence;
    List<Restaurant> polygon = null;
    Location mLastLocation;
    PendingIntent pendingIntent;
    int counter = 0;
    int previousActivity;
    boolean isNetworkCall = false;
    boolean isgeofenceSetup = false;
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
                try {
                    int activity = intent.getIntExtra("DETECTED_ACTIVITY", 0);
                    int confidence = intent.getIntExtra("CONFIDENCE", 0);
                    //Toast.makeText(context, activity + " " + confidence, Toast.LENGTH_SHORT).show();
                    checkForActivityChange();

                    if (confidence > 0) {
                        //Toast.makeText(context, activity + " " + confidence, Toast.LENGTH_SHORT).show();
                        checkForActivityChange();
                        if (activity == previousActivity) {
                            counter++;
                        } else {
                            if (previousActivity != DetectedActivity.STILL && counter >= 1) {
                                //checkForActivityChange();
                            }
                            previousActivity = activity;
                            counter = 0;
                        }
                    }
                }
                catch (Exception e) {
                    Log.d(e.getClass().getName(), e.getMessage());
                }
            }
        };
        mBroadcastManager.registerReceiver(updateListReceiver, mBroadcastFilter);

        mBroadcastFilterGeofence = new IntentFilter(Utils.ACTION_GEOFENCE_STATUS);
        mBroadcastFilterGeofence.addCategory(Utils.CATEGORY_GEOFENCE_SERVICES);
        BroadcastReceiver geofenceReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int geofenceTransition = intent.getIntExtra("GEOFENCE_TRANSITION", 0);

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    if (pendingIntent != null) {
                        List<String> reqId = new ArrayList<>();
                        reqId.add("geofence");
                        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, reqId);
                        isgeofenceSetup = false;
                    }
                }
            }
        };
        mBroadcastManager.registerReceiver(geofenceReciever, mBroadcastFilterGeofence);
    }

    private void setupGoogleApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
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
            Restaurant nearRestaurant = isNearRestaurant();
            if (nearRestaurant != null) {
                sendNotification(nearRestaurant);
            }
        } else {
            makeServiceCall();
        }
    }

    private void sendNotification(Restaurant nearRestaurant) {

        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setContentTitle(nearRestaurant.getRestaurant().getName())
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
        //makeGoogleServiceCall();
        if (!isNetworkCall) {
            //Toast.makeText(getApplicationContext(),"network false", Toast.LENGTH_SHORT).show();
            makeZomatoServiceCall();
        } else {
            //Toast.makeText(getApplicationContext(),"network true", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeGoogleServiceCall() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.d("Google Api", String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                }
                likelyPlaces.release();
            }
        });
    }

    private Response.Listener<JSONObject> onGoogleRestaurantsRecieved() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Google", response.toString());
            }
        };
    }

    private void makeZomatoServiceCall() {
        //Toast.makeText(getApplicationContext(),"Service Called", Toast.LENGTH_SHORT).show();
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
        if (mLastLocation != null) {
            isNetworkCall = true;
            connector.getNearbyRestaurants(getString(R.string.zomato_api_key), "From Service", mLastLocation, onZomatoRestaurantsRecieved(), onError());
        }
    }

    private Response.ErrorListener onError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isNetworkCall = false;
                //Toast.makeText(getApplicationContext(),"Service Error", Toast.LENGTH_SHORT).show();
                if (error != null) {
                    if (error.getMessage() != null) {
                        Log.d("Google Api", error.getMessage());
                    }
                }
            }
        };
    }

    private Response.Listener<JSONObject> onZomatoRestaurantsRecieved() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isNetworkCall = false;
                //Toast.makeText(getApplicationContext(),"Service Response", Toast.LENGTH_SHORT).show();
                polygon = new ArrayList<>();
                Zomato zomato = Utils.gson.fromJson(response.toString(), Zomato.class);
                List<Restaurant> restaurants = zomato.getRestaurants();
                polygon = restaurants;
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    //LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    Restaurant restaurant = getRestaurantFromLocation(mLastLocation);
                    Restaurant nearRestaurant = isNearRestaurant();
                    if (nearRestaurant != null) {
                        sendNotification(nearRestaurant);
                    }
                    Intent intent = new Intent(getApplicationContext(), GeofenceService.class);
                    pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    PendingResult<Status> pendingResult = LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, getGeofencingRequest(mLastLocation), pendingIntent);
                    isgeofenceSetup = true;
                }
            }
        };
    }

    private List<Geofence> getGeofencingRequest(Location mLastLocation) {
        List<Geofence> geofences = new ArrayList<>();
        geofences.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("geofence")

                .setCircularRegion(
                        mLastLocation.getLatitude(),
                        mLastLocation.getLongitude(),
                        200
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        return geofences;
    }

    private Restaurant getRestaurantFromLocation(Location mLastLocation) {
        Restaurant restaurant = new Restaurant();
        ZomatoRestaurant zomatoRestaurant = new ZomatoRestaurant();
        zomatoRestaurant.setName("point");
        com.example.sisir.dietchecker.Zomato.Location location = new com.example.sisir.dietchecker.Zomato.Location();
        location.setLatitude(String.valueOf(mLastLocation.getLatitude()));
        location.setLongitude(String.valueOf(mLastLocation.getLongitude()));
        zomatoRestaurant.setLocation(location);
        restaurant.setRestaurant(zomatoRestaurant);
        return restaurant;
    }

    private Restaurant isNearRestaurant() {
        for (Restaurant restaurant : polygon) {
            LatLng latLng = getLatLngFromRestaurant(restaurant);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastLocation != null) {
                double dist = distance(latLng, new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                //Toast.makeText(this, "isNearRestaurant " + String.valueOf(dist), Toast.LENGTH_SHORT).show();
                if (dist < 20) {
                    return restaurant;
                }
            }
        }
        return null;
    }

    private double distance(LatLng l1, LatLng l2) {
        double lat1 = l1.latitude;
        double lng1 = l1.longitude;
        double lat2 = l2.latitude;
        double lng2 = l2.longitude;

        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;
        return dist;
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private boolean isWithinGeoFence() {
        //Toast.makeText(this, "isWithinGeoFence", Toast.LENGTH_SHORT).show();
        if (polygon == null) {
            //Toast.makeText(this, "Polygon null", Toast.LENGTH_SHORT).show();
            return false;
        }
        return isgeofenceSetup;
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

    private boolean isPointInPolygon(Restaurant restaurant, List<Restaurant> vertices) {
        LatLng tap = getLatLngFromRestaurant(restaurant);
        //Toast.makeText(this, "isPointInPolygon " + tap.latitude + " " + tap.longitude, Toast.LENGTH_SHORT).show();
        int intersectCount = 0;
        for (int j = 0; j < vertices.size() - 1; j++) {
            LatLng jLatLng = getLatLngFromRestaurant(vertices.get(j));
            LatLng kLatLng = getLatLngFromRestaurant(vertices.get(j + 1));
            if (rayCastIntersect(tap, jLatLng, kLatLng)) {
                intersectCount++;
            }
        }

        return ((intersectCount % 2) == 1); // odd = inside, even = outside;
    }

    private LatLng getLatLngFromRestaurant(Restaurant restaurant) {
        double restaurantLat = Double.valueOf(restaurant.getRestaurant().getLocation().getLatitude());
        double restaurantLng = Double.valueOf(restaurant.getRestaurant().getLocation().getLongitude());
        LatLng latLng = new LatLng(restaurantLat,restaurantLng);
        return latLng;
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
