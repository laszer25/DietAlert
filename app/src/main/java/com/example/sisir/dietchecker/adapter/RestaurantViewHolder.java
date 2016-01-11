package com.example.sisir.dietchecker.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.sisir.dietchecker.R;

/**
 * Created by sisir on 10/1/16.
 */
public class RestaurantViewHolder extends RecyclerView.ViewHolder {
    protected TextView restaurant;
    public RestaurantViewHolder(View itemView) {
        super(itemView);
        restaurant = (TextView) itemView.findViewById(R.id.restaurant_name);
    }
}
