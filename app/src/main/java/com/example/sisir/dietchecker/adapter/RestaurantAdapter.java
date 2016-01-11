package com.example.sisir.dietchecker.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.sisir.dietchecker.R;
import com.example.sisir.dietchecker.Zomato.Restaurant;

import java.util.List;

/**
 * Created by sisir on 10/1/16.
 */
public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantViewHolder> {

    Context context;
    List<Restaurant> restaurants;
    public RestaurantAdapter(Context context, List<Restaurant> restaurants){
        this.context = context;
        this.restaurants = restaurants;
    };

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View viewItem = LayoutInflater.from(context).inflate(R.layout.content_main, parent, false);
        return new RestaurantViewHolder(viewItem);
    }

    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        holder.restaurant.setText(restaurants.get(position).getRestaurant().getName());
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }
}
