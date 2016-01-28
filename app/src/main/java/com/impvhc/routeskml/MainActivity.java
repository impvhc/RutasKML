package com.impvhc.routeskml;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;

import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks
    ,GoogleApiClient.OnConnectionFailedListener
    ,LocationListener
    ,OnMapReadyCallback
{

    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(15000)         // 15 segundos
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private GoogleApiClient mGoogleApiClient;
    Location location;

    SupportMapFragment mapFragment;
    GoogleMap googleMap;

    // GeoJSON file to download
    private final String mGeoJsonUrl
            = "http://localhost:3000/rutas";

    private GeoJsonLayer mLayer;
    ListView list_rutas;

    int[] array;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        array=getResources().getIntArray(R.array.rainbow);
        list_rutas=(ListView) findViewById(R.id.list_rutas);

        Log.i("color", String.valueOf(array[1]));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();


        mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFragment==null) {
            mapFragment = SupportMapFragment.newInstance();
            mapFragment.getMapAsync(this);
            mapFragment.setRetainInstance(true);
            getSupportFragmentManager().beginTransaction().add(R.id.map,mapFragment).commit();
        }

    }
    ArrayList<GeoJsonLayer> myLayer;
    private class ConexionServer extends AsyncTask<Void,Void,JSONArray>{

        @Override
        protected JSONArray doInBackground(Void... params) {
            StringBuilder result = new StringBuilder();

            Log.i("URLOG",api);
            try {
                //URL url=new URL("http://192.168.0.3:3000/rutasdestinos/28.649520307898822/-106.06736373156309/28.636074113389544/-106.0766777023673");
                URL url=new URL(api);
                HttpURLConnection httpURLConnection=(HttpURLConnection) url.openConnection();
                InputStream in =new BufferedInputStream(httpURLConnection.getInputStream());
                BufferedReader reader=new BufferedReader(new InputStreamReader(in));
                String line;
                while((line=reader.readLine())!=null){
                    result.append(line);
                }
                Log.i("a",result.toString());


                JSONArray jsonArray=new JSONArray(result.toString());



                return jsonArray;

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

            } finally {

            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            int color=0;
            ArrayList<GeoJsonLayer> layerRoutes=new ArrayList<>();
            List<String> rutasS =new ArrayList<>();
            for (int x=0;x<result.length();x++){
                try {
                    color=x%array.length;
                    rutasS.add(result.getJSONObject(x).getJSONObject("properties").getString("name"));
                    mLayer=new GeoJsonLayer(googleMap, result.getJSONObject(x));
                    GeoJsonLineStringStyle lineStringStyle = mLayer.getDefaultLineStringStyle();
                    lineStringStyle.setColor(array[color]);

                    layerRoutes.add(mLayer);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            crearLista(rutasS, layerRoutes);



        }

    }

    public void crearLista(List rut, final ArrayList<GeoJsonLayer> layers){
        ArrayAdapter adapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,rut);
        list_rutas.setAdapter(adapter);

        list_rutas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (int x = 0; x < layers.size(); x++) {
                    if (layers.get(x).isLayerOnMap()) {
                        layers.get(x).removeLayerFromMap();
                    }
                }
                layers.get(position).addLayerToMap();
            }
        });
        myLayer=layers;

    }

    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();

    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                REQUEST,
                this);  // LocationListener
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location loc) {
        location=loc;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
    int clicksmap=0;
    Marker origen;
    Marker destino;
    Circle origen_c;
    Circle destino_c;
    String api;
    @Override
    public void onMapReady(GoogleMap Map) {

        Map.setMyLocationEnabled(true);
        Map.getUiSettings().setZoomControlsEnabled(true);
        googleMap=Map;
        if(location!=null){
        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            CameraPosition cameraPosition = new CameraPosition.Builder().
                    target(this.googleMap.getCameraPosition().target).
                    tilt(0).
                    zoom(12).
                    bearing(0).
                    build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }else{
            LatLng latLng=new LatLng(28.66,-106.07);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            CameraPosition cameraPosition = new CameraPosition.Builder().
                    target(this.googleMap.getCameraPosition().target).
                    tilt(0).
                    zoom(12).
                    bearing(0).
                    build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                String title=marker.getTitle();
                if(title.equals("Origen")){
                    origen_c.setVisible(false);
                }else if(title.equals("Destino")){
                    destino_c.setVisible(false);
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                String title=marker.getTitle();
                if(title.equals("Origen")){
                    origen_c.setCenter(marker.getPosition());
                    origen_c.setVisible(true);
                }
                else if(title.equals("Destino")){
                    destino_c.setCenter(marker.getPosition());
                    destino_c.setVisible(true);
                }
                for (int x = 0; x < myLayer.size(); x++) {
                    if (myLayer.get(x).isLayerOnMap()) {
                        myLayer.get(x).removeLayerFromMap();
                    }
                }
                api = "http://192.168.0.104:3000/rutasdestinos/"
                        + origen.getPosition().latitude + "/" + origen.getPosition().longitude
                        + "/"
                        + destino.getPosition().latitude + "/" + destino.getPosition().longitude;
                //list_rutas.removeAllViews();

                new ConexionServer().execute();

            }
        });

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (clicksmap == 2) {
                    googleMap.clear();
                    clicksmap = 0;
                    list_rutas.setAdapter(null);
                }
                if (clicksmap == 0) {
                    origen = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(latLng)
                                    .draggable(true)
                                    .title("Origen")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    );
                    origen_c = googleMap.addCircle(new CircleOptions()
                                    .center(latLng)
                                    .radius(250)
                                    .strokeColor(Color.RED)
                                    .fillColor(Color.argb(20, 50, 0, 255))
                    );
                } else if (clicksmap == 1) {
                    destino = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(latLng)
                                    .draggable(true)
                                    .title("Destino")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                    );
                    destino_c = googleMap.addCircle(new CircleOptions()
                                    .center(latLng)
                                    .radius(250)
                                    .strokeColor(Color.RED)
                                    .fillColor(Color.argb(20, 255, 0, 55))
                    );
                    api = "http://192.168.0.104:3000/rutasdestinos/"
                            + origen.getPosition().latitude + "/" + origen.getPosition().longitude
                            + "/"
                            + destino.getPosition().latitude + "/" + destino.getPosition().longitude;
                    //list_rutas.removeAllViews();
                    new ConexionServer().execute();
                }
                clicksmap++;
            }
        });
    }

}
