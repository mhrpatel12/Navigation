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
    @SerializedName("start_location")
    private Location start_location;
    @SerializedName("end_location")
    private Location end_location;
    @SerializedName("polyline")
    private OverviewPolyLine polyline;

    public Location getStart_location() {
        return start_location;
    }

    public Location getEnd_location() {
        return end_location;
    }

    public OverviewPolyLine getPolyline() {
        return polyline;
    }
}
