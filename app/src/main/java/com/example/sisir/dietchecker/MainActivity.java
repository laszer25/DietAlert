package com.example.sisir.dietchecker;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.sisir.dietchecker.Zomato.Restaurant;
import com.example.sisir.dietchecker.Zomato.Zomato;
import com.example.sisir.dietchecker.ZomatoApiConnector.ZomatoApiConnector;
import com.example.sisir.dietchecker.adapter.RestaurantAdapter;
import com.example.sisir.dietchecker.services.DietControlService;
import com.example.sisir.dietchecker.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.argb(255,0,0,0));
        setSupportActionBar(toolbar);
        setupGoogleApi();
        setupServices();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void setupServices() {
        Intent intent = new Intent(this, DietControlService.class);
        startService(intent);
    }

    private void setupGoogleApi() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void setMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.toolbar_map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                CircleOptions circleOptions = new CircleOptions()
                        .center(latLng)
                        .fillColor(Color.argb(140, 0, 0, 0))
                        .radius(200);
                googleMap.addCircle(circleOptions);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
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
            setMap();
            ZomatoApiConnector zomatoApiConnector = new ZomatoApiConnector(this);
            zomatoApiConnector.getNearbyRestaurants(getString(R.string.zomato_api_key), "Zomato api call", mLastLocation, onResponseRecieved(), onError());
        }
    }

    private Response.ErrorListener onError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Snackbar.make(getCurrentFocus(), "Error in getting api response", Snackbar.LENGTH_LONG).show();
            }
        };
    }

    private Response.Listener<JSONObject> onResponseRecieved() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Zomato zomato = Utils.gson.fromJson(response.toString(), Zomato.class);
                if(zomato != null) {
                    RecyclerView restaurantList = (RecyclerView) findViewById(R.id.restaurant_list);
                    restaurantList.setHasFixedSize(true);
                    LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
                    llm.setOrientation(LinearLayoutManager.VERTICAL);
                    restaurantList.setLayoutManager(llm);
                    List<Restaurant> restaurants = zomato.getRestaurants();
                    RestaurantAdapter adapter = new RestaurantAdapter(getApplicationContext(), restaurants);
                    restaurantList.setAdapter(adapter);
                    restaurantList.addOnScrollListener(restaurantListScrollListener(restaurantList, restaurants));
                    setPointsOnMap(restaurants);
                }
            }
        };
    }

    private void setPointsOnMap(final List<Restaurant> restaurants) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.toolbar_map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                PolygonOptions options = new PolygonOptions()
                        .strokeColor(Color.argb(100,255,0,0))
                        .fillColor(Color.argb(100,0,255,0));
                for(Restaurant restaurant : restaurants) {
                    LatLng latLng = new LatLng(Double.valueOf(restaurant.getRestaurant().getLocation().getLatitude()),Double.valueOf(restaurant.getRestaurant().getLocation().getLongitude()));
                    options.add(latLng);
                }
                Polygon polygon = googleMap.addPolygon(options);
            }
        });
    }

    private RecyclerView.OnScrollListener restaurantListScrollListener(final RecyclerView restaurantList, List<Restaurant> restaurants) {
        return new RecyclerView.OnScrollListener() {
            int y = 0;
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                y = y + dy;
                LinearLayoutManager llm = (LinearLayoutManager) recyclerView.getLayoutManager();
                int firstPos = llm.findFirstVisibleItemPosition();
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if(y == 0) {

                    ObjectAnimator xAnim = ObjectAnimator.ofFloat(fab, View.SCALE_X, 0f, 1f);
                    ObjectAnimator yAnim = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 0f, 1f);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(xAnim, yAnim);
                    set.setDuration(300);
                    set.setInterpolator(new AccelerateInterpolator());
                    set.start();
                    fab.setEnabled(true);
                }
                else if(fab.isEnabled()){

                    ObjectAnimator xAnim = ObjectAnimator.ofFloat(fab, View.SCALE_X, 1f, 0f);
                    ObjectAnimator yAnim = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 1f, 0f);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(xAnim, yAnim);
                    set.setDuration(150);
                    set.setInterpolator(new AccelerateInterpolator());
                    set.start();
                    fab.setEnabled(false);
                }
            }
        };
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
