package com.mihir.navigation.rest.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mihir.navigation.R;
import com.mihir.navigation.rest.lib.BottomSheetBehaviorGoogleMapsLike;
import com.mihir.navigation.rest.lib.MergedAppBarLayoutBehavior;
import com.mihir.navigation.rest.model.DirectionResults;
import com.mihir.navigation.rest.model.Routes;
import com.mihir.navigation.rest.model.Steps;
import com.mihir.navigation.rest.rest.ApiClient;
import com.mihir.navigation.rest.rest.ApiInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private final String TAG = getClass() + "";
    private GoogleMap mMap;
    private Context mContext;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LatLng latLngCurrentLocation, latLngStartPoint, latLngDestination;
    private Marker markerStartPoint;
    private Marker markerDestination;

    private PlaceAutocompleteFragment autocompletePlacesStart;
    private PlaceAutocompleteFragment autocompletePlacesDestination;

    private FloatingActionButton fbCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        mContext = this;
        checkPermissions();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initializeBottomSheet();

        autocompletePlacesStart = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.placeStart);
        autocompletePlacesDestination = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.placeDestination);
        autocompletePlacesStart.setHint(getString(R.string.hint_start));
        autocompletePlacesDestination.setHint(getString(R.string.hint_destination));

        autocompletePlacesStart.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName());

                latLngStartPoint = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                if (markerStartPoint != null && markerStartPoint.isVisible()) {
                    markerStartPoint.remove();
                }
                markerStartPoint = mMap.addMarker(new MarkerOptions().position(latLngStartPoint).title(place.getName() + ""));
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16);
                mMap.animateCamera(center);
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        autocompletePlacesDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName());

                latLngDestination = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                if (markerDestination != null && markerDestination.isVisible()) {
                    markerDestination.remove();
                }
                markerDestination = mMap.addMarker(new MarkerOptions().position(latLngDestination).title(place.getName() + ""));
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16);
                mMap.animateCamera(center);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        fbCurrentLocation = (FloatingActionButton) findViewById(R.id.fbCurrentLocation);
        fbCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDirections();
/*                latLngCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 17);

                mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation).draggable(true));
                mMap.animateCamera(center);*/
            }
        });

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    private void initializeBottomSheet() {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_directions);
        View bottomSheet = coordinatorLayout.findViewById(R.id.bottom_sheet);
        final BottomSheetBehaviorGoogleMapsLike behavior = BottomSheetBehaviorGoogleMapsLike.from(bottomSheet);
        behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
        AppBarLayout mergedAppBarLayout = (AppBarLayout) findViewById(R.id.merged_appbarlayout);
        MergedAppBarLayoutBehavior mergedAppBarLayoutBehavior = MergedAppBarLayoutBehavior.from(mergedAppBarLayout);
        mergedAppBarLayoutBehavior.setToolbarTitle("Title Dummy");
        mergedAppBarLayoutBehavior.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_EXPANDED);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            latLngCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 17);
            mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation).draggable(true));
            mMap.animateCamera(center);
        }

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                polyline.setColor(Color.BLUE);
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (latLngCurrentLocation != null) {
            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 16);

            mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation).draggable(true));
            mMap.animateCamera(center);
        } else if (mLastLocation != null) {

            latLngCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 17);

            mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation).draggable(true));
            mMap.animateCamera(center);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void checkPermissions() {
        if ((ContextCompat.checkSelfPermission(mContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
                ||
                (ContextCompat.checkSelfPermission(mContext,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)) {

            // Should we show an explanation?
            if ((ActivityCompat.shouldShowRequestPermissionRationale(NavigationActivity.this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION))
                    &&
                    (ActivityCompat.shouldShowRequestPermissionRationale(NavigationActivity.this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION))) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(NavigationActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        2);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    public void getDirections() {

        ApiInterface reqinterface = ApiClient.getClient().create(ApiInterface.class);

        Call<DirectionResults> call = reqinterface.getJson(latLngStartPoint.latitude + "," + latLngStartPoint.longitude, latLngDestination.latitude + "," + latLngDestination.longitude, "driving", getString(R.string.google_directions_key), "true");
        call.enqueue(new Callback<DirectionResults>() {
            @Override
            public void onResponse(Call<DirectionResults> call, Response<DirectionResults> response) {
                Log.i("zacharia", "inside on success" + response.message());
                ArrayList<LatLng> routelist = new ArrayList<LatLng>();
                List path = new ArrayList<HashMap<String, String>>();
                List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
                List<LatLng> decodelist;
                for (int k = 0; k < response.body().getRoutes().size(); k++) {
                    Routes routeA = response.body().getRoutes().get(k);

                    Log.i("zacharia", "Legs length : " + routeA.getLegs().size());
                    if (routeA.getLegs().size() > 0) {

                        List<Steps> steps = routeA.getLegs().get(0).getSteps();
                        Log.i("zacharia", "Steps size :" + steps.size());
                        Steps step;
                        Location location;
                        String polyline;
                        for (int i = 0; i < steps.size(); i++) {
                            step = steps.get(i);
                            location = step.getStart_location();
                            routelist.add(new LatLng(location.getLatitude(), location.getLongitude()));
                            Log.i("Instruction=====>", steps.get(i).getInstruction());
                            polyline = step.getPolyline().getPoints();
                            decodelist = decodePoly(polyline);
                            /** Traversing all points */
                            for (int l = 0; l < decodelist.size(); l++) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat", Double.toString(((LatLng) decodelist.get(l)).latitude));
                                hm.put("lng", Double.toString(((LatLng) decodelist.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }

                    ArrayList points = null;
                    PolylineOptions lineOptions = null;
                    MarkerOptions markerOptions = new MarkerOptions();

                    for (int i = 0; i < routes.size(); i++) {
                        points = new ArrayList();
                        lineOptions = new PolylineOptions();

                        List<HashMap<String, String>> paths = routes.get(i);

                        for (int j = 0; j < path.size(); j++) {
                            HashMap<String, String> point = paths.get(j);

                            double lat = Double.parseDouble(point.get("lat"));
                            double lng = Double.parseDouble(point.get("lng"));
                            LatLng position = new LatLng(lat, lng);

                            points.add(position);
                        }

                        lineOptions.addAll(points);
                        lineOptions.width(15);
                        lineOptions.color(Color.RED);
                        lineOptions.clickable(true);
                    }

                    // Drawing polyline in the Google Map for the i-th route
                    mMap.addPolyline(lineOptions);
                }
            }

            @Override
            public void onFailure(Call<DirectionResults> call, Throwable t) {
                Log.i("zacharia", "inside on failure");
            }
        });
    }

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
