package com.mihir.navigation.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Mihir on 04-07-2017.
 */

public class DirectionResults {
    @SerializedName("routes")
    private List<Routes> routes;

    public List<Routes> getRoutes() {
        return routes;
    }
}
