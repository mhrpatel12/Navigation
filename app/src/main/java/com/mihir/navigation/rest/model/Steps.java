package com.mihir.navigation.rest.model;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mihir on 04-07-2017.
 */

public class Steps {
    @SerializedName("polyline")
    private OverviewPolyLine polyline;
    @SerializedName("html_instructions")
    private String instruction;

    public String getInstruction() {
        return instruction;
    }

    public OverviewPolyLine getPolyline() {
        return polyline;
    }
}
