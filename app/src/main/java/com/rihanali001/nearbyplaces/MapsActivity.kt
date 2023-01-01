package com.rihanali001.nearbyplaces

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.rihanali001.nearbyplaces.databinding.ActivityMapsBinding
import com.rihanali001.nearbyplaces.model.MyPlaces
import com.rihanali001.nearbyplaces.remote.IGoogleAPIService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()
    private lateinit var mLastLocation: Location
    private var mMarker: Marker? = null
    //location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var mServices: IGoogleAPIService
    internal lateinit var currentPlace: MyPlaces

    companion object {
        private const val MY_PERMISSION_CODE: Int = 2000
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Init service
        mServices = Common.googleApiService

        //request runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (checkLocationPermission()) {
                buildLocationRequest()
                buildLocationCallBack()
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates( locationRequest, locationCallback, Looper.myLooper() )
            } else {
                buildLocationRequest()
                buildLocationCallBack()
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates( locationRequest, locationCallback, Looper.myLooper() )
            }

        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_business -> nearByPlace("business")
                R.id.action_store -> nearByPlace("stores")
                R.id.action_gas -> nearByPlace("gas_station")
                R.id.action_restaurant -> nearByPlace("restaurant")
            }
            true
        }
    }


    private fun nearByPlace(typePlace: String) {
        //clear all marker from map
        mMap.clear()
        //build url based on location
        val url = getUrl(latitude, longitude, typePlace)
        mServices.getNearbyPlaces(url).enqueue(object : Callback<MyPlaces> {
            override fun onResponse(call: Call<MyPlaces>, response: Response<MyPlaces>) {
                currentPlace = response.body()!!
                Log.d("nearByPlaces", "onResponse: "+ response.body()!!.results!!.size)
                if (response.isSuccessful) {
                    for (i in 0 until response.body()!!.results!!.size) {
                        val markerOptions = MarkerOptions()
                        val googlePlaces = response.body()!!.results!![i]
                        val lat = googlePlaces.geometry!!.location!!.lat
                        val lng = googlePlaces.geometry!!.location!!.lng
                        val placeName = googlePlaces.name
                        val latLng = LatLng(lat, lng)
                        markerOptions.position(latLng)
                        markerOptions.title(placeName)
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        markerOptions.snippet(latLng.toString())// to get the lat/lng of place
                        mMap.addMarker(markerOptions) //add marker to map
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng)) //move camera
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(11f))
                    }
                }
            }
            override fun onFailure(call: Call<MyPlaces>, t: Throwable) {
                Toast.makeText(baseContext, "" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getUrl(latitude: Double, longitude: Double, typePlace: String): String {
        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?keyword=cruise&location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=10000") //10km
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&key=${BuildConfig.MAPS_API_KEY}")
        Log.d("url_debug", googlePlaceUrl.toString())
        return googlePlaceUrl.toString()
    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                mLastLocation = p0.locations[p0.locations.size - 1] // Get last location
                if (mMarker != null) {
                    mMarker!!.remove()
                }
                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude

                val latLng = LatLng(latitude, longitude)
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                mMarker = mMap.addMarker(markerOptions)

                //Move Camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11f))
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), MY_PERMISSION_CODE
                )
            else
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), MY_PERMISSION_CODE
                )
            return false
        } else
            return true
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                        if (checkLocationPermission()) {
                            buildLocationRequest()
                            buildLocationCallBack()
                            fusedLocationProviderClient =
                                LocationServices.getFusedLocationProviderClient(this)
                            fusedLocationProviderClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                            )
                            mMap.isMyLocationEnabled = true
                        }
                } else
                    Toast.makeText(this, "location Service Not Enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
            }
        } else
            mMap.isMyLocationEnabled = true
        //zoom control
        mMap.uiSettings.isZoomControlsEnabled = true
    }
}