package com.mihir.navigation.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Mihir on 04-07-2017.
 */

public class Routes {
    @SerializedName("legs")
    private List<Legs> legs;

    public List<Legs> getLegs() {
        return legs;
    }
}
