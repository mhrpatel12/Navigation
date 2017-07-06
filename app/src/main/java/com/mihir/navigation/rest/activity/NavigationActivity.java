package com.mihir.navigation.rest.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mihir.navigation.R;
import com.mihir.navigation.rest.adapter.RouteAdapter;
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
    private RecyclerView recyclerViewRouteInstructions;
    private LinearLayout layoutBottomSheet;
    private Bitmap bitmapCurrentPosition;
    private Bitmap bitmapDestination;
    private static final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        mContext = this;
        checkPermissions();
        bitmapCurrentPosition = getBitmapFromVectorDrawable(mContext, R.drawable.ic_current_location_marker_24dp);
        bitmapDestination = getBitmapFromVectorDrawable(mContext, R.drawable.ic_beenhere_black_24dp);
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
        autocompletePlacesStart.getView().findViewById(R.id.place_autocomplete_search_input).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        autocompletePlacesDestination.getView().findViewById(R.id.place_autocomplete_search_input).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        ((EditText) autocompletePlacesStart.getView().findViewById(R.id.place_autocomplete_search_input)).setTextColor(Color.WHITE);
        ((EditText) autocompletePlacesDestination.getView().findViewById(R.id.place_autocomplete_search_input)).setTextColor(Color.WHITE);

        layoutBottomSheet = (LinearLayout) findViewById(R.id.layout_bottom_sheet);

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
                if (verifyPlaces()) getDirections();
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
                markerDestination = mMap.addMarker(new MarkerOptions().position(latLngDestination)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmapDestination))
                        .title(place.getName() + ""));
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16);
                mMap.animateCamera(center);
                if (verifyPlaces()) getDirections();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
        autocompletePlacesStart.getView().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autocompletePlacesStart.setText("");
                markerStartPoint.remove();
                layoutBottomSheet.setVisibility(View.GONE);
                for (Polyline polyline : listPolyines) {
                    polyline.remove();
                }
            }
        });
        autocompletePlacesDestination.getView().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autocompletePlacesDestination.setText("");
                markerDestination.remove();
                layoutBottomSheet.setVisibility(View.GONE);
                for (Polyline polyline : listPolyines) {
                    polyline.remove();
                }
            }
        });

        fbCurrentLocation = (FloatingActionButton) findViewById(R.id.fbCurrentLocation);
        fbCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation != null) {
                    latLngCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 17);

                    mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation)
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmapCurrentPosition))
                            .draggable(true));
                    mMap.animateCamera(center);
                }
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

    private boolean verifyPlaces() {
        return (latLngStartPoint != null) && (latLngDestination != null);
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
        recyclerViewRouteInstructions = (RecyclerView) findViewById(R.id.recycler_view_route_instructions);
        recyclerViewRouteInstructions.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                for (int i = 0; i < listPolyines.size(); i++) {
                    Polyline polyline1 = listPolyines.get(i);
                    polyline1.setColor(Color.GRAY);
                    if (polyline.getId().equals(polyline1.getId())) {
                        layoutBottomSheet.setVisibility(View.VISIBLE);
                        recyclerViewRouteInstructions.setAdapter(new RouteAdapter(listRouteInstructions.get(i), R.layout.list_item_route, getApplicationContext()));
                    }
                }
                polyline.setColor(Color.BLUE);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            latLngCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 17);
            mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmapCurrentPosition))
                    .draggable(true));

            mMap.animateCamera(center);
        }
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

        if (listPolyines.size() <= 0) {
            if (latLngCurrentLocation != null) {
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 16);

                mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmapCurrentPosition))
                        .draggable(true));
                mMap.animateCamera(center);
            } else if (mLastLocation != null) {

                latLngCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 16);

                mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmapCurrentPosition))
                        .draggable(true));
                mMap.animateCamera(center);
            }
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
                        MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                            mGoogleApiClient);
                    if (mLastLocation != null) {
                        latLngCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 17);
                        mMap.addMarker(new MarkerOptions().position(latLngCurrentLocation)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmapCurrentPosition))
                                .draggable(true));

                        mMap.animateCamera(center);
                    }
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private ArrayList<Polyline> listPolyines = new ArrayList<>();
    private List<ArrayList<String>> listRouteInstructions = new ArrayList<>();

    public void getDirections() {
        for (Polyline line : listPolyines) {
            line.remove();
        }

        ApiInterface reqinterface = ApiClient.getClient().create(ApiInterface.class);

        Call<DirectionResults> call = reqinterface.getJson(latLngStartPoint.latitude + "," + latLngStartPoint.longitude, latLngDestination.latitude + "," + latLngDestination.longitude, "driving", getString(R.string.google_directions_key), "true");
        call.enqueue(new Callback<DirectionResults>() {
            @Override
            public void onResponse(Call<DirectionResults> call, Response<DirectionResults> response) {

                for (int k = 0; k < response.body().getRoutes().size(); k++) {
                    List path = new ArrayList<>();
                    List<List<HashMap<String, String>>> routes = new ArrayList<>();
                    List<LatLng> decodedPolyLine = null;
                    PolylineOptions lineOptions = new PolylineOptions();
                    Routes route = response.body().getRoutes().get(k);
                    ArrayList<String> listInstructions = new ArrayList<String>();

                    if (route.getLegs().size() > 0) {
                        List<Steps> steps = route.getLegs().get(0).getSteps();
                        Steps step;
                        String polyline;
                        for (int i = 0; i < steps.size(); i++) {
                            step = steps.get(i);
                            if (Build.VERSION.SDK_INT >= 24) {
                                listInstructions.add((Html.fromHtml(steps.get(i).getInstruction(), Html.FROM_HTML_MODE_LEGACY) + "").replace("\n", ""));
                            } else {
                                listInstructions.add((Html.fromHtml(steps.get(i).getInstruction()) + "").replace("\n", ""));
                            }
                            polyline = step.getPolyline().getPoints();
                            decodedPolyLine = decodePoly(polyline);
                            for (int l = 0; l < decodedPolyLine.size(); l++) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat", Double.toString(((LatLng) decodedPolyLine.get(l)).latitude));
                                hm.put("lng", Double.toString(((LatLng) decodedPolyLine.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                    for (int i = 0; i < routes.size(); i++) {
                        ArrayList points = new ArrayList();
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
                        lineOptions.color(Color.GRAY);
                        lineOptions.clickable(true);
                    }
                    // Drawing polyline in the Google Map for the k-th route
                    listPolyines.add(k, mMap.addPolyline(lineOptions));
                    listRouteInstructions.add(listInstructions);
                    //zoomRoute(mMap, decodedPolyLine);
                }
            }

            @Override
            public void onFailure(Call<DirectionResults> call, Throwable t) {
            }
        });
    }

    public void zoomRoute(GoogleMap googleMap, List<LatLng> lstLatLngRoute) {

        if (googleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);


        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int routePadding = (int) (width * 0.10); // offset from edges of the map 10% of screen

        LatLngBounds latLngBounds = boundsBuilder.build();

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(latLngBounds, width, height, routePadding);

        googleMap.animateCamera(cu);
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

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
