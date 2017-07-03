package com.mihir.navigation.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Mihir on 04-07-2017.
 */

public class Legs {
    @SerializedName("steps")
    private List<Steps> steps;

    public List<Steps> getSteps() {
        return steps;
    }
}
