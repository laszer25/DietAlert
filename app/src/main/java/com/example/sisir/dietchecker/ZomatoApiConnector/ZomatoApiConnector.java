package com.example.sisir.dietchecker.ZomatoApiConnector;

import android.app.DownloadManager;
import android.content.Context;
import android.location.Location;
import android.support.v4.util.ArrayMap;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.sisir.dietchecker.R;
import com.example.sisir.dietchecker.network.VolleyRequestQueue;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by sisir on 10/1/16.
 */
public class ZomatoApiConnector {

    private Context context;

    private ZomatoApiConnector mInstance;

    public ZomatoApiConnector(Context context) {
        this.context = context;
    }

    public ZomatoApiConnector getmInstance(Context context) {
        if(mInstance == null) {
            mInstance = new ZomatoApiConnector(context);
        }
        return mInstance;
    }

    public void getNearbyRestaurants(String apiKey, String tag, Location location,Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        VolleyRequestQueue requestQueue = VolleyRequestQueue.getInstance();
        String url = "https://developers.zomato.com/api/v2.1/geocode?lat="+String.valueOf(location.getLatitude())+"&lon="+String.valueOf(location.getLongitude());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, responseListener, errorListener){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new ArrayMap<String, String>();
                headers.put("user_key", context.getString(R.string.zomato_api_key));
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        requestQueue.addToRequestQueue(request, tag);
    }
}
