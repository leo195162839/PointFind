package com.example.matt.pointfind;

import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.util.Log;
import android.location.Location;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.view.View;
import android.view.View.OnClickListener;
import android.app.Dialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


import org.json.JSONObject;


import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdateFactory  ;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class MapActivity extends ActionBarActivity implements SensorEventListener {

    /* Local variables */

    SensorManager sensorManager;
    float currentDirection = 0f;

    // enable google map
    private GoogleMap googleMap;

    //degree of view
    int range = 30;

    //orientation
    float orientation;

    // Spinner in which the location types are stored
    Spinner mSprPlaceType;

    // A button to find the near by places
    Button mBtnFind=null;

    // Stores near by places
    ArrayList<Place> mPlaces = null;

    // A String array containing place types sent to Google Place service
    String[] mPlaceType=null;

    // A String array containing place types displayed to user
    String[] mPlaceTypeName=null;

    // MyLocation
    LatLng mLocation=null;

    // Links marker id and place object
    HashMap<String, Place> mHMReference = new HashMap<String, Place>();

    // Specifies the drawMarker() to draw the marker with default color
    private static final float UNDEFINED_COLOR = -1;

    // Array of place
    String[] placeValue = new String[]{};

    //Initialises the mapView
    private void createMapView(){
        //Catch the null pointer exception that
        //may be thrown when initialising the map
        try {
            if(googleMap == null){
                googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                        R.id.mapView)).getMap();


                //If the map is still null after attempted initialisation,
                //show an error to the user
                if(googleMap == null) {
                    Toast.makeText(getApplicationContext(),
                            "Error creating map",Toast.LENGTH_SHORT).show();
                } else {
                    setUpGoogleMap();
                }
            }
        } catch (NullPointerException exception){
            Log.e("pointFind", exception.toString());
        }
    }

    //Set up the initial myLocation on the map
    private void setUpGoogleMap() {
        double latitude;
        double longitude;

        //Enable MyLocation layer of Google Map
        googleMap.setMyLocationEnabled(true);

        //Set up location manager
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //setup Criteria object to retrieve provider,
        // Get the name of the provider, get location by provider
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        Location myLocation = locationManager.getLastKnownLocation(provider);

        //create LatLng
        if (myLocation != null) {
            latitude = myLocation.getLatitude();
            longitude = myLocation.getLongitude();
        } else {
            Location getLastLocation = locationManager.getLastKnownLocation (LocationManager.PASSIVE_PROVIDER);
            longitude = getLastLocation.getLongitude();
            latitude = getLastLocation.getLatitude();
        }
        mLocation = new LatLng(latitude, longitude);

        //show location by move the camera and zoom in
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(mLocation));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));

        //marker
        //googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Marker").draggable(true));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        createMapView();
        Log.d("MapActivity", "onCreate Triggered");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        // Array of place types
        mPlaceType = getResources().getStringArray(R.array.place_type);

        // Array of place type names
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        // Creating an array adapter with an array of Place types
        // to populate the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                mPlaceTypeName);

        // Getting reference to the Spinner
        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);

        // Setting adapter on Spinner to set place types
        mSprPlaceType.setAdapter(adapter);

        // Getting reference to Find Button
        mBtnFind = ( Button ) findViewById(R.id.btn_find);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if(status!=ConnectionResult.SUCCESS){ // Google Play Services are not available
            Log.d("MapActivity", "connection failed");
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        }else { // Google Play Services are available
            Log.d("MapActivity", "Connected");

            if (savedInstanceState != null) {
                Log.d("MapActivity", "Try to get place");
                // Removes all the existing links from marker id to place object
                mHMReference.clear();

                //If near by places are already saved
                if (savedInstanceState.containsKey("places")) {
                    Log.d("MapActivity", "Contains place");
                    // Retrieving the array of place objects
                    mPlaces = savedInstanceState.getParcelableArrayList("places");

                    // Traversing through each near by place object
                    for (int i = 0; i < mPlaces.size(); i++) {
                        //Log.d("MapActivity", String.valueOf(i));
                        // Getting latitude and longitude of the i-th place
                        LatLng point = new LatLng(Double.parseDouble(((Place)mPlaces.get(i)).mLat),
                                Double.parseDouble(((Place)mPlaces.get(i)).mLng));

                        // Drawing the marker corresponding to the i-th place
                        Marker m = drawMarker(point, UNDEFINED_COLOR,i);

                        // Linkng i-th place and its marker id
                        mHMReference.put(m.getId(), (Place)mPlaces.get(i));

                        //Makeing list
                        placeValue[placeValue.length] = ((Place)mPlaces.get(i)).mPlaceName;

                    }
                }

                // If a touched location is already saved
                if (savedInstanceState.containsKey("location")) {

                    // Retrieving the touched location and setting in member variable
                    mLocation = (LatLng) savedInstanceState.getParcelable("location");

                    // Drawing a marker at the touched location
                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN, 0);
                }
            } else {
                Log.d("MapActivity", "Null Instance");
            }

            // Setting click event lister for the find button
            mBtnFind.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("MapActivity", "on click triggered");
                    int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                    String type = mPlaceType[selectedPosition];

                    googleMap.clear();

                    if (mLocation == null) {
                        Toast.makeText(getBaseContext(), "Please mark a location", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN, 0);

                    StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                    sb.append("location=" + mLocation.latitude + "," + mLocation.longitude);
                    sb.append("&radius=5000");
                    sb.append("&types=" + type);
                    sb.append("&sensor=true");
                    sb.append("&key=AIzaSyCMm8gNqeucfCRgTarvFbNDSM6a4u2CzS8");

                    // Creating a new non-ui thread task to download Google place json data
                    PlacesTask placesTask = new PlacesTask();
                    Log.d("MapActivity", "Parser triggered");

                    // Invokes the "doInBackground()" method of the class PlaceTask
                    placesTask.execute(sb.toString());
                }
            });

            // Marker click listener
            googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {

                @Override
                public boolean onMarkerClick(Marker marker) {

                    // If touched at User input location
                    if(!mHMReference.containsKey(marker.getId())) {
                        return false;
                    }

                    String title = marker.getTitle();

                    for (int i = 0; i < mPlaces.size(); i++) {
                        //Log.d("MapActivity", String.valueOf(i));
                        Log.d("MapActivity", "Name is " + mPlaces.get(i).mPlaceName);
                        Log.d("MapActivity", "Title is " + marker.getTitle());
                        if (((Place)mPlaces.get(i)).mPlaceName.equals(title)) {
                            Log.d("MapActivity", "Triggered");
                            StorePlace.mPlace = (Place)mPlaces.get(i);
                        }

                    }

                    Intent intent = new Intent(MapActivity.this, MarkerActivity.class);

                    startActivity(intent);

                    return false;
                }
            });

            // Map Click listener to set new marker
            googleMap.setOnMapClickListener(new OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {

                    // Clears all the existing markers
                    googleMap.clear();

                    //draw marker
                    mLocation = point;
                    drawMarker(mLocation,BitmapDescriptorFactory.HUE_GREEN,-1);
                }
            });
        }
    }


    //callback, (Only work when rotate screen)
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        // Saving all the near by places objects
        if(mPlaces!=null) {
            outState.putParcelableArrayList("places", mPlaces);
        }

        // Saving the touched location
        if(mLocation!=null) {
            outState.putParcelable("location", mLocation);
        }

        super.onSaveInstanceState(outState);
    }

    // download json data from  url
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Error downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    //Download Google Places
    private class PlacesTask extends AsyncTask<String, Integer, String>{

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of ParserTask
            parserTask.execute(result);
        }
    }

    //Parse the Google Places in JSON format
    private class ParserTask extends AsyncTask<String, Integer, Place[]>{
        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected Place[] doInBackground(String... jsonData) {

            Place[] places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);
                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(Place[] places){

            mPlaces = new ArrayList<>();

            for(int i=0;i< places.length ;i++){
                Log.d("MapActivity", String.valueOf(i));
                Place place = places[i];

                // Getting latitude of the place
                double lat = Double.parseDouble(place.mLat);
                Log.d("MapActivity", String.valueOf(lat));
                // Getting longitude of the place
                double lng = Double.parseDouble(place.mLng);

                //get direction of point
                double diffX = lng - mLocation.longitude;
                double diffY = lat - mLocation.latitude;
                double direction = Math.atan(diffY/diffX);
                int degrees = (int) Math.toDegrees(direction);
                if ((diffX < 0 && diffY > 0) || (diffX < 0 && diffY < 0)) {
                    degrees += 180;
                }
                degrees += 270;
                degrees = degrees % 360;

                if (degrees > (orientation - range) && degrees < (orientation + range)) {

                    LatLng latLng = new LatLng(lat, lng);
                    mPlaces.add(place);
                    Marker m = drawMarker(latLng, UNDEFINED_COLOR, mPlaces.size() - 1);

                    // Adding place reference to HashMap with marker id as HashMap key
                    // to get its reference in info window click event listener
                    mHMReference.put(m.getId(), place);
                }
            }
        }
    }

    //Drawing marker
    private Marker drawMarker(LatLng latLng,float color, int index){
        // Creating a marker
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting the position for the marker
        markerOptions.position(latLng);

        // Setting title of the marker
        if (index == 0) {
            markerOptions.title("MyLocation");
        } else if (index == -1) {
            markerOptions.title("New Location");
        } else {
            markerOptions.title(((Place)mPlaces.get(index)).mPlaceName);
        }

        if(color != UNDEFINED_COLOR)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));

        // Placing a marker on the touched position
        Marker m = googleMap.addMarker(markerOptions);

        return m;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        orientation = Math.round(event.values[0])*-1 + 360;
        Log.d("ORTAG", "Current orientation" + orientation);

        //currentDirection = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor s, int x){


    }
}
