package com.example.tigerspikeapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tigerspikeapp.R
import com.example.tigerspikeapp.adapter.SearchAdapter
import com.example.tigerspikeapp.databinding.ActivityMainBinding
import com.example.tigerspikeapp.db.UserInfo
import com.example.tigerspikeapp.service.FirebaseInstance
import com.example.tigerspikeapp.utils.AddResult
import com.example.tigerspikeapp.utils.GetResult
import com.example.tigerspikeapp.utils.SearchResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import java.io.IOException
import java.util.Locale


class MainActivity : FragmentActivity(), OnMapReadyCallback, FirebaseInstance.IAddNote,
    FirebaseInstance.IGetAllNotes, FirebaseInstance.IGetSingleNote, FirebaseInstance.IDeleteNote,
    FirebaseInstance.ISearch {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
        val LOCATION_PERMISSION_REQUEST_CODE = 101
        private val TAG = "MainActivity"
        private var DEFAULT_ZOOM = 15f
        private val API_KEY = "AIzaSyAX6TpBR5jnQjPixGKa2hiGNAvpLNQ1_zo" // DEBUG Google map api key
//        private val API_KEY = "AIzaSyBqXPLusJQYlSI9GNWBYrc5Fx9U4Y3wKAs" // RELEASE Google map api key
        var mLocationPermissionsGranted = false
    }

    private var startMarker: Marker? = null
    private var outputAddressLine = ""
    private lateinit var lastKnownLocation: Location
    private lateinit var currentLatlng: LatLng
    private var newMarker: Marker? = null
    private var detailUsername = ""
    private var detailContent = ""
    private var deleteMarker: Marker? = null
    private var searchList = ArrayList<SearchAdapter.SearchModel>()
    private lateinit var searchAdapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), API_KEY)

        binding.btnLogout.setOnClickListener {
            UserInfo.clearUserInfo(this@MainActivity)
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.imvClose.setOnClickListener {
            binding.edtContent.setText("")
            binding.lnInput.visibility = View.GONE
            binding.tvAddNoteSuccess.visibility = View.GONE
        }

        binding.fabAddNote.setOnClickListener {
            binding.lnInput.visibility = View.VISIBLE

        }

        binding.imvCloseDetail.setOnClickListener {
            binding.lnDetail.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener {
            val userInfo = UserInfo.getUserInfo(this@MainActivity)
            if (userInfo != null) {
                FirebaseInstance.getInstance().setIAddNote(this@MainActivity)
                if (binding.edtContent.text.toString().trim().isEmpty()) {
                    Toast.makeText(this@MainActivity, "Please type your content!", Toast.LENGTH_SHORT).show()
                } else {
                    FirebaseInstance.getInstance().addNote(
                        userInfo,
                        binding.edtContent.text.toString(),
                        currentLatlng,
                        outputAddressLine
                    )
                }
            }
        }

        binding.fabCurrent.setOnClickListener {
            moveCamera(currentLatlng, DEFAULT_ZOOM)
        }

        getAllNotes()

        buildSearchAdapter()

        binding.imvSearch.setOnClickListener {
            binding.lnDetail.visibility = View.GONE
            binding.lnInput.visibility = View.GONE
            val searchText = binding.edtSearch.text.toString().trim()
            if (searchText.isEmpty()) {
                Toast.makeText(this@MainActivity,
                    "Please type search content!", Toast.LENGTH_SHORT).show()
            } else {
                binding.pbLoadingSearch.visibility = View.VISIBLE
                FirebaseInstance.getInstance().setISearch(this@MainActivity)
                FirebaseInstance.getInstance().search(searchText)
            }
        }

        binding.imvCloseSearch.setOnClickListener {
            binding.lnSearch.visibility = View.GONE
        }
    }

    private fun buildSearchAdapter() {
        searchAdapter = SearchAdapter(this@MainActivity, searchList)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rcvSearch.setHasFixedSize(true)
        binding.rcvSearch.setLayoutManager(manager)
        binding.rcvSearch.setAdapter(searchAdapter)
        val dividerItemDecoration = DividerItemDecoration(binding.rcvSearch.context,
            LinearLayoutManager.VERTICAL)
        binding.rcvSearch.addItemDecoration(dividerItemDecoration)
    }

    /**
     * Get all notes for current user and show it on Google map
     * */
    private fun getAllNotes() {
        FirebaseInstance.getInstance().setIGetAllNotes(this@MainActivity)
        FirebaseInstance.getInstance().getAllNotes()
    }

    /** Get current position by GPS.
     Require "FINE_LOCATION" Permission and Login Google Account for your phone
    */
    private fun getDeviceLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            if (mLocationPermissionsGranted) {
                val locationResult: Task<Location> = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result as Location
                        currentLatlng = LatLng(
                            lastKnownLocation.latitude,
                            lastKnownLocation.longitude
                        )
                        val addresses: List<Address>
                        var addressLine = ""
                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                        try {
                            addresses = geocoder.getFromLocation(
                                lastKnownLocation.latitude,
                                lastKnownLocation.longitude, 1
                            )!!
                            addressLine = addresses[0].getAddressLine(0)
                            outputAddressLine = addressLine
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }
                        addCurrentPositionMarkerInfo(
                            currentLatlng,
                            addressLine
                        )
                        DEFAULT_ZOOM = 20f
                        moveCamera(currentLatlng, DEFAULT_ZOOM)
                    } else {
                        Log.d(TAG, "getDeviceLocation: Current location is null!")
                        Toast.makeText(
                            this@MainActivity,
                            "Unable to get current location !",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceLocation: Security Exception: " + e.message)
        }
    }

    /**
     * Add current marker point when map is ready for current user
     * */
    private fun addCurrentPositionMarkerInfo(
        point: LatLng,
        addressLine: String?
    ) {
        startMarker?.remove()

        /*val markerOptions = MarkerOptions()
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))*/

        if (addressLine != null) {
            if (addressLine.isNotEmpty()) {
                val userInfo = UserInfo.getUserInfo(this@MainActivity)
                startMarker = mMap.addMarker(
                    MarkerOptions().position(point).title((addressLine)).snippet(
                        "User: ${userInfo?.username}"
                    )
                        .anchor(0f, 0f).draggable(true)
                )!!
            } else {
                outputAddressLine = ""
            }
        }

        startMarker?.showInfoWindow()
    }

    private val locationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Realtime update marker point position when user
     * move to another address
     * */
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 200
            fastestInterval = 200
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            location?.let { updateMarkerLocation(it) }
        }
    }

    private fun updateMarkerLocation(location: Location) {
        val newLatLng = LatLng(location.latitude, location.longitude)
        startMarker?.position = newLatLng
        DEFAULT_ZOOM = 20f
        val newAddressLine = getAddressFromLatLng(this@MainActivity, newLatLng)
        if (outputAddressLine != newAddressLine) {
            if (newAddressLine != null) {
                outputAddressLine = newAddressLine
                currentLatlng = newLatLng
                moveCamera(newLatLng, DEFAULT_ZOOM)
                startMarker?.title = getAddressFromLatLng(this@MainActivity, newLatLng)
                startMarker?.snippet = "Content"
            }
        }

    }

    /**
     * Get address from lat lng position
     * */
    private fun getAddressFromLatLng(context: Context, latLng: LatLng): String? {
        val geocoder = Geocoder(context)
        val addresses: List<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        return if (!addresses.isNullOrEmpty()) {
            addresses[0].getAddressLine(0)
        } else {
            null
        }
    }

    /**
     * Move google map camera when have a change
     * */
    private fun moveCamera(latLng: LatLng, zoom: Float) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    /**
     * Perform tasks when Google map is already
     * */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        getDeviceLocation()
        startLocationUpdates()
        mMap.setOnCameraChangeListener { position ->
            if (position.zoom != DEFAULT_ZOOM) {
                DEFAULT_ZOOM = position.zoom
            }
        }

        // When click a note marker, get info for it and show detail
        mMap.setOnInfoWindowClickListener { marker ->
            if (marker != startMarker) {
                deleteMarker = marker
                binding.lnInput.visibility = View.GONE
                val latLng = marker.position
                detailUsername = marker.title.toString()
                detailContent = marker.snippet.toString()
                FirebaseInstance.getInstance().setIGetSingleNote(this@MainActivity)
                FirebaseInstance.getInstance().getSingleNoteInfo(detailUsername, latLng)
            }
        }

    }

    /**
     * Add a new note response when user create a note success
     * */
    override fun addResult(addResult: AddResult, latLng: LatLng,
                           content: String, address: String) {
        if (addResult == AddResult.SUCCESS) {
            val userInfo = UserInfo.getUserInfo(this@MainActivity)
            newMarker = mMap.addMarker(
                MarkerOptions().position(latLng).title(userInfo?.username).snippet(
                    content
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_yellow))
                    .anchor(0f, 0f).draggable(false)
            )!!
            newMarker?.showInfoWindow()
            binding.tvAddNoteSuccess.visibility = View.VISIBLE
            Toast.makeText(this@MainActivity, "Add success!", Toast.LENGTH_SHORT).show()

        }
    }

    /**
     * Errors when add note failed
     * */
    override fun error(ex: Exception) {
        Toast.makeText(this@MainActivity, "Error when add note: ${ex.message.toString()}",
            Toast.LENGTH_SHORT).show()
    }

    /**
     * Response when Get all note of all users
     * Create marker point for current user and other users
     */
    override fun sendNoteInfo(address: String, content: String,
                              latLng: LatLng, username: String, getResult: GetResult) {
        if (getResult == GetResult.SUCCESS) {
            val userInfo = UserInfo.getUserInfo(this@MainActivity)
            if (userInfo?.username == username) {
                // Create a note marker with current user
                val newMarker = mMap.addMarker(
                    MarkerOptions().position(latLng).title(userInfo.username).snippet(
                        content
                    ).icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_yellow))
                        .anchor(0f, 0f).draggable(false)
                )!!
                newMarker.showInfoWindow()
            } else {
                // Create a note marker for other users
                val newMarker = mMap.addMarker(
                    MarkerOptions().position(latLng).title(username).snippet(
                        content
                    ).icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_blue))
                        .anchor(0f, 0f).draggable(false)
                )!!
                newMarker.showInfoWindow()
            }
        }
    }

    /**
     * Response when click note detail on Snippet of Marker point note
     * */
    override fun sendSingleInfo(
        address: String,
        getResult: GetResult,
        generatedKey: String
    ) {
        binding.lnDetail.visibility = View.VISIBLE
        binding.tvUsername.setText("Note of: $detailUsername")
        binding.tvContent.setText("Content: $detailContent")
        binding.tvSavedAt.setText("Position: $address")

        val userInfo = UserInfo.getUserInfo(this@MainActivity)

        if (userInfo?.username == detailUsername) {
            binding.imvDeleteNote.visibility = View.VISIBLE
            binding.imvDeleteNote.setOnClickListener {
                FirebaseInstance.getInstance().setIDeleteNote(this@MainActivity)
                FirebaseInstance.getInstance().deleteNote(detailUsername, generatedKey)
            }
        } else {
            binding.imvDeleteNote.visibility = View.GONE
        }

    }

    /**
     * Response when delete a note success
     * */
    override fun deleted(result: Boolean) {
        if (result) {
            binding.lnDetail.visibility = View.GONE
            deleteMarker?.remove()
            Toast.makeText(this@MainActivity,
                "This note has been deleted!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Response when Search by username or note content
     * */
    override fun result(searchResult: SearchResult,
                        searchList: java.util.ArrayList<SearchAdapter.SearchModel>) {
        binding.pbLoadingSearch.visibility = View.GONE
        binding.lnSearch.visibility = View.VISIBLE
        if (searchResult == SearchResult.SUCCESS) {
            if (searchList.isNotEmpty()) {
                binding.rcvSearch.visibility = View.VISIBLE
                binding.tvSearchEmpty.visibility = View.GONE
                searchAdapter.reloadList(searchList)
                searchAdapter.setOnItemClickListener(object : SearchAdapter.ClickListener{
                    override fun onItemClick(position: Int, v: View) {
                        val latLng = searchList[position].latLng
                        if (latLng != null) {
                            moveCamera(latLng, DEFAULT_ZOOM)
                        }
                    }

                })
            }
        }
        if (searchResult == SearchResult.EMPTY) {
            binding.rcvSearch.visibility = View.GONE
            binding.tvSearchEmpty.visibility = View.VISIBLE
        }
    }
}