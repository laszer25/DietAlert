package com.example.sisir.dietchecker.GoogleApiConnector;

import android.content.Context;
import android.location.Location;
import android.support.v4.util.ArrayMap;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.sisir.dietchecker.R;
import com.example.sisir.dietchecker.network.VolleyRequestQueue;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by sisir on 11/1/16.
 */
public class GoogleApiConnector {
    private Context context;

    private GoogleApiConnector mInstance;

    public GoogleApiConnector(Context context) {
        this.context = context;
    }

    public GoogleApiConnector getmInstance(Context context) {
        if(mInstance == null) {
            mInstance = new GoogleApiConnector(context);
        }
        return mInstance;
    }

    public void getNearbyRestaurants(String apiKey, String tag, Location location,Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        VolleyRequestQueue requestQueue = VolleyRequestQueue.getInstance();
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                + "key=" + apiKey
                + "&location=" + String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude())
                + "&radius=5000"
                + "&rankby=distance"
                + "&types=bakery|bar|cafe|food|grocery_or_supermarket|liquor_store|meal_delivery|meal_takeaway|restaurant";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, responseListener, errorListener){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return super.getHeaders();
            }
        };
        requestQueue.addToRequestQueue(request, tag);
    }
}
