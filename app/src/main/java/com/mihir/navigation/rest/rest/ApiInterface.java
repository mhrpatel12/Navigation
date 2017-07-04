package com.mihir.navigation.rest.rest;

import com.mihir.navigation.rest.model.DirectionResults;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface ApiInterface {

    @GET("/maps/api/directions/json")
    Call<DirectionResults> getJson(@Query("origin") String origin, @Query("destination") String destination, @Query("mode") String transportMode, @Query("key") String apiKey, @Query("alternatives") String alternatives);
}
