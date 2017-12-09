package com.vikination.adbroutingexample

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.model.Direction
import com.akexorcist.googledirection.util.DirectionConverter
import com.google.android.gms.location.places.ui.PlaceAutocomplete

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    var isFromPlacePicker :Boolean = false

    var markerOptionFrom :MarkerOptions? = null
    var markerOptionTo :MarkerOptions? = null
    var polyline : Polyline? = null

    companion object {
        const val PLACE_PICKER_REQ = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // search from location place
        fromText.setOnClickListener {
            isFromPlacePicker = true
            loadPlaceAutoCompleteIntent()
        }

        // search to location place
        toText.setOnClickListener {
            isFromPlacePicker = false
            loadPlaceAutoCompleteIntent()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val bandung = LatLng(-6.903429,107.5030708)
        mMap.addMarker(MarkerOptions().position(bandung).title("Marker in Bandung"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bandung, 12F))
    }

    fun drawRoute(){
        GoogleDirection.withServerKey(resources.getString(R.string.google_maps_key))
                .from(markerOptionFrom?.position)
                .to(markerOptionTo?.position)
                .execute(object :DirectionCallback{
                    override fun onDirectionSuccess(direction: Direction?, rawBody: String?) {
                        if (direction!!.isOK){
                            val leg = direction?.routeList!![0].legList[0]
                            val polylineOption =
                                    DirectionConverter.createPolyline(
                                            this@MapsActivity, leg.directionPoint, 5,Color.RED)
                            mMap.addPolyline(polylineOption)

                            distanceText.visibility = View.VISIBLE
                            distanceText.text = String.format("distance = %s , duration = %s"
                                    , leg.distance.text, leg.duration.text)
                        }

                    }

                    override fun onDirectionFailure(t: Throwable?) {
                        Toast.makeText(this@MapsActivity, t?.message,Toast.LENGTH_SHORT).show()
                    }

                })
    }

    fun loadBound(){
        mMap.clear()
        mMap.addMarker(markerOptionFrom)
        mMap.addMarker(markerOptionTo)
        val latlongBounds = LatLngBounds.Builder().include(markerOptionFrom?.position).include(markerOptionTo?.position)
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latlongBounds.build(), 60))
    }

    fun loadPlaceAutoCompleteIntent(){
        val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                .build(this)
        startActivityForResult(intent, PLACE_PICKER_REQ)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when(requestCode){

            // process result placepicker
            PLACE_PICKER_REQ -> {
                when(resultCode){
                    Activity.RESULT_OK -> {
                        val place = PlaceAutocomplete.getPlace(this@MapsActivity, data)
                        if (isFromPlacePicker) {
                            fromText?.text = place.name
                            markerOptionFrom = MarkerOptions().title(place.name.toString()).position(place.latLng)
                        } else {
                            toText?.text = place.name
                            markerOptionTo = MarkerOptions().title(place.name.toString()).position(place.latLng)
                        }
                        if (markerOptionFrom != null && markerOptionTo != null) {
                            loadBound()
                            drawRoute()
                        }
//                        Log.i("MAP", String.format("lat long picker (%s,%s)",place.latLng.latitude, place.latLng.longitude))
                    }
                    PlaceAutocomplete.RESULT_ERROR -> {
                        val status = PlaceAutocomplete.getStatus(this@MapsActivity, data)
                        Toast.makeText(this@MapsActivity, status.statusMessage, Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
