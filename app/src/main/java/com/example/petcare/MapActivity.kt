package com.example.petcare

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.maptiler.maptilersdk.MTConfig
import com.maptiler.maptilersdk.annotations.MTMarker
import com.maptiler.maptilersdk.annotations.MTTextPopup
import com.maptiler.maptilersdk.events.MTEvent
import com.maptiler.maptilersdk.map.LngLat
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapViewClassic
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.MTMapViewDelegate
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilersdk.map.types.MTData

class MapActivity : AppCompatActivity() {

    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // =========================================================
    // MAPTILER
    // =========================================================

    private lateinit var mapView: MTMapViewClassic
    private lateinit var mapController: MTMapViewController

    // =========================================================
    // UI
    // =========================================================

    private lateinit var tvLocationStatus: TextView

    // =========================================================
    // MAP STATE
    // =========================================================

    private var mapReady = false
    private var locationsLoaded = false

    // =========================================================
    // SAVED LOCATION MODEL
    // =========================================================

    private data class SavedLocation(
        val id: String,
        val name: String,
        val type: String,
        val latitude: Double,
        val longitude: Double
    )

    // Saved Firestore locations
    private val savedLocations =
        mutableListOf<SavedLocation>()

    // Marker objects so we can remove them from the map
    private val markers =
        mutableMapOf<String, MTMarker>()

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // =====================================================
        // FIREBASE
        // =====================================================

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()

        // =====================================================
        // LOGIN CHECK
        // =====================================================

        if (auth.currentUser == null) {

            val intent =
                Intent(
                    this,
                    MainActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()

            return
        }

        // =====================================================
        // MAPTILER API KEY
        // =====================================================

        if (BuildConfig.MAPTILER_API_KEY.isBlank()) {

            Toast.makeText(
                this,
                "MapTiler API key is missing",
                Toast.LENGTH_LONG
            ).show()
        }

        MTConfig.apiKey =
            BuildConfig.MAPTILER_API_KEY

        // IMPORTANT:
        // API key must be set before activity_map.xml
        // creates the MapTiler map.

        setContentView(
            R.layout.activity_map
        )

        // =====================================================
        // CONNECT XML
        // =====================================================

        mapView =
            findViewById(
                R.id.classicMapView
            )

        tvLocationStatus =
            findViewById(
                R.id.tvLocationStatus
            )

        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        // =====================================================
        // MAP CONTROLLER
        // =====================================================

        mapController =
            MTMapViewController(this)

        mapController.delegate =
            object : MTMapViewDelegate {

                override fun onMapViewInitialized() {

                    // Map object initialized
                }

                override fun onEventTriggered(
                    event: MTEvent,
                    data: MTData?
                ) {

                    when (event) {

                        // =====================================
                        // MAP READY
                        // =====================================

                        MTEvent.ON_READY -> {

                            mapReady =
                                true

                            tvLocationStatus.text =
                                "Tap the map to save a pet-care location"

                            if (!locationsLoaded) {

                                loadSavedLocations()
                            }
                        }

                        // =====================================
                        // MAP TAP
                        // =====================================

                        MTEvent.ON_TAP -> {

                            val coordinates =
                                data?.coordinate
                                    ?: return

                            // First try the MapTiler annotation ID
                            val markerId =
                                data.id

                            val locationFromId =
                                savedLocations.firstOrNull {
                                    it.id == markerId
                                }

                            if (locationFromId != null) {

                                showLocationDetailsDialog(
                                    locationFromId
                                )

                                return
                            }

                            // Fallback:
                            // detect whether user tapped close
                            // to an existing marker.
                            val nearbyLocation =
                                findNearbySavedLocation(
                                    coordinates
                                )

                            if (nearbyLocation != null) {

                                showLocationDetailsDialog(
                                    nearbyLocation
                                )

                            } else {

                                // Empty map area:
                                // add a new location.
                                showAddLocationDialog(
                                    coordinates
                                )
                            }
                        }

                        else -> {

                            // No action required
                        }
                    }
                }
            }

        // =====================================================
        // KATHMANDU MAP POSITION
        //
        // LngLat:
        // longitude first
        // latitude second
        // =====================================================

        val kathmandu =
            LngLat(
                85.3240,
                27.7172
            )

        // =====================================================
        // MAP OPTIONS
        // =====================================================

        val mapOptions =
            MTMapOptions(
                center = kathmandu,
                zoom = 12.0,
                navigationControlIsVisible = true,
                scaleControlIsVisible = true,
                attributionControlIsVisible = true,
                maptilerLogoIsVisible = true
            )

        // =====================================================
        // INITIALIZE MAP
        // =====================================================

        mapView.initialize(
            referenceStyle =
                MTMapReferenceStyle.STREETS,

            options =
                mapOptions,

            controller =
                mapController,

            styleVariant =
                null
        )

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        bottomNavigation.selectedItemId =
            R.id.nav_map

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                // HOME
                R.id.nav_home -> {

                    startActivity(
                        Intent(
                            this,
                            HomeActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                // CHECKLIST
                R.id.nav_checklist -> {

                    startActivity(
                        Intent(
                            this,
                            TasksActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                // ROUTINE
                R.id.nav_routine -> {

                    startActivity(
                        Intent(
                            this,
                            RoutineActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                // DELEGATE
                R.id.nav_delegate -> {

                    startActivity(
                        Intent(
                            this,
                            DelegateActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                // MAP
                R.id.nav_map -> {

                    true
                }

                else -> false
            }
        }
    }

    // =========================================================
    // ADD LOCATION DIALOG
    // =========================================================

    private fun showAddLocationDialog(
        coordinates: LngLat
    ) {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                val padding =
                    dpToPx(24)

                setPadding(
                    padding,
                    dpToPx(8),
                    padding,
                    0
                )
            }

        // =====================================================
        // LOCATION NAME
        // =====================================================

        val nameInput =
            EditText(this).apply {

                hint =
                    "Location name"

                inputType =
                    InputType.TYPE_CLASS_TEXT

                maxLines =
                    1
            }

        container.addView(
            nameInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // =====================================================
        // LOCATION TYPE LABEL
        // =====================================================

        val typeLabel =
            TextView(this).apply {

                text =
                    "Location type"

                textSize =
                    14f

                setPadding(
                    0,
                    dpToPx(18),
                    0,
                    dpToPx(6)
                )
            }

        container.addView(
            typeLabel
        )

        // =====================================================
        // LOCATION TYPES
        // =====================================================

        val locationTypes =
            arrayOf(
                "Vet",
                "Grooming",
                "Dog Park",
                "Pet Store"
            )

        val typeSpinner =
            Spinner(this)

        val spinnerAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                locationTypes
            )

        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        typeSpinner.adapter =
            spinnerAdapter

        container.addView(
            typeSpinner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // =====================================================
        // CREATE DIALOG
        // =====================================================

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Save Pet Care Location"
                )
                .setMessage(
                    "Enter a name and select the type of pet-care location."
                )
                .setView(
                    container
                )
                .setNegativeButton(
                    "Cancel"
                ) { dialogInterface, _ ->

                    dialogInterface.dismiss()
                }
                .setPositiveButton(
                    "Save",
                    null
                )
                .create()

        dialog.setOnShowListener {

            val saveButton =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            saveButton.setOnClickListener {

                val locationName =
                    nameInput.text
                        .toString()
                        .trim()

                val locationType =
                    typeSpinner.selectedItem
                        .toString()

                if (locationName.isEmpty()) {

                    nameInput.error =
                        "Please enter a location name"

                    nameInput.requestFocus()

                    return@setOnClickListener
                }

                dialog.dismiss()

                saveLocation(
                    name = locationName,
                    type = locationType,
                    coordinates = coordinates
                )
            }
        }

        dialog.show()
    }

    // =========================================================
    // SAVED LOCATION DETAILS / DELETE DIALOG
    // =========================================================

    private fun showLocationDetailsDialog(
        location: SavedLocation
    ) {

        val message =
            buildString {

                append(
                    "Type: ${location.type}"
                )

                append(
                    "\n\n"
                )

                append(
                    "Latitude: ${
                        String.format(
                            "%.6f",
                            location.latitude
                        )
                    }"
                )

                append(
                    "\n"
                )

                append(
                    "Longitude: ${
                        String.format(
                            "%.6f",
                            location.longitude
                        )
                    }"
                )

                append(
                    "\n\n"
                )

                append(
                    "Do you want to keep or delete this saved location?"
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                location.name
            )
            .setMessage(
                message
            )
            .setNegativeButton(
                "Close",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                showDeleteConfirmation(
                    location
                )
            }
            .show()
    }

    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteConfirmation(
        location: SavedLocation
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Delete location?"
            )
            .setMessage(
                "Are you sure you want to delete ${location.name}?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteLocation(
                    location
                )
            }
            .show()
    }

    // =========================================================
    // SAVE LOCATION TO FIRESTORE
    // =========================================================

    private fun saveLocation(
        name: String,
        type: String,
        coordinates: LngLat
    ) {

        val currentUser =
            auth.currentUser
                ?: return

        tvLocationStatus.text =
            "Saving $name..."

        val locationDocument =
            db.collection(
                "locations"
            ).document()

        val locationData =
            hashMapOf<String, Any>(

                "id" to
                        locationDocument.id,

                "userId" to
                        currentUser.uid,

                "name" to
                        name,

                "type" to
                        type,

                "latitude" to
                        coordinates.lat,

                "longitude" to
                        coordinates.lng,

                "createdAt" to
                        FieldValue.serverTimestamp()
            )

        locationDocument
            .set(
                locationData
            )
            .addOnSuccessListener {

                val location =
                    SavedLocation(
                        id =
                            locationDocument.id,

                        name =
                            name,

                        type =
                            type,

                        latitude =
                            coordinates.lat,

                        longitude =
                            coordinates.lng
                    )

                savedLocations.add(
                    location
                )

                addMarker(
                    location
                )

                updateLocationStatus()

                Toast.makeText(
                    this,
                    "$name saved successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                tvLocationStatus.text =
                    "Could not save location"

                Toast.makeText(
                    this,
                    "Error: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // LOAD SAVED LOCATIONS
    // =========================================================

    private fun loadSavedLocations() {

        if (!mapReady) {

            return
        }

        val currentUser =
            auth.currentUser
                ?: return

        locationsLoaded =
            true

        savedLocations.clear()

        tvLocationStatus.text =
            "Loading saved locations..."

        db.collection(
            "locations"
        )
            .whereEqualTo(
                "userId",
                currentUser.uid
            )
            .get()
            .addOnSuccessListener { snapshot ->

                for (document in snapshot.documents) {

                    val name =
                        document.getString(
                            "name"
                        ) ?: "Pet Care Location"

                    val type =
                        document.getString(
                            "type"
                        ) ?: "Location"

                    val latitude =
                        document.getDouble(
                            "latitude"
                        )
                            ?: continue

                    val longitude =
                        document.getDouble(
                            "longitude"
                        )
                            ?: continue

                    val id =
                        document.getString(
                            "id"
                        ) ?: document.id

                    val location =
                        SavedLocation(
                            id =
                                id,

                            name =
                                name,

                            type =
                                type,

                            latitude =
                                latitude,

                            longitude =
                                longitude
                        )

                    savedLocations.add(
                        location
                    )

                    addMarker(
                        location
                    )
                }

                updateLocationStatus()
            }
            .addOnFailureListener { exception ->

                locationsLoaded =
                    false

                tvLocationStatus.text =
                    "Could not load saved locations"

                Toast.makeText(
                    this,
                    "Error: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // ADD MARKER
    // =========================================================

    private fun addMarker(
        location: SavedLocation
    ) {

        val coordinates =
            LngLat(
                location.longitude,
                location.latitude
            )

        // Popup gives quick visual information
        // when MapTiler opens the marker popup.
        val popup =
            MTTextPopup(
                coordinates,
                "${location.name}\n${location.type}"
            )

        val marker =
            MTMarker(
                location.id,
                coordinates
            ).apply {

                color =
                    getMarkerColor(
                        location.type
                    )

                this.popup =
                    popup

                scale =
                    1.1
            }

        controllerAddMarker(
            marker
        )

        markers[location.id] =
            marker
    }

    // =========================================================
    // ADD MARKER SAFELY
    // =========================================================

    private fun controllerAddMarker(
        marker: MTMarker
    ) {

        mapController.style
            ?.addMarker(
                marker
            )
    }

    // =========================================================
    // FIND SAVED LOCATION NEAR TAP
    // =========================================================

    private fun findNearbySavedLocation(
        tappedCoordinate: LngLat
    ): SavedLocation? {

        var nearestLocation: SavedLocation? =
            null

        var nearestDistance =
            Float.MAX_VALUE

        for (location in savedLocations) {

            val result =
                FloatArray(1)

            Location.distanceBetween(
                tappedCoordinate.lat,
                tappedCoordinate.lng,
                location.latitude,
                location.longitude,
                result
            )

            val distance =
                result[0]

            if (distance < nearestDistance) {

                nearestDistance =
                    distance

                nearestLocation =
                    location
            }
        }

        // User must tap reasonably close to the marker.
        //
        // Kathmandu map starts around zoom 12,
        // so this provides an easy mobile touch target
        // without treating normal map taps as marker taps.
        return if (nearestDistance <= 300f) {

            nearestLocation

        } else {

            null
        }
    }

    // =========================================================
    // DELETE LOCATION FROM FIRESTORE
    // =========================================================

    private fun deleteLocation(
        location: SavedLocation
    ) {

        tvLocationStatus.text =
            "Deleting ${location.name}..."

        db.collection(
            "locations"
        )
            .document(
                location.id
            )
            .delete()
            .addOnSuccessListener {

                // =============================================
                // REMOVE MARKER FROM MAP
                // =============================================

                markers[location.id]
                    ?.remove(
                        mapController
                    )

                markers.remove(
                    location.id
                )

                // =============================================
                // REMOVE LOCAL LOCATION DATA
                // =============================================

                savedLocations.removeAll {
                    it.id == location.id
                }

                updateLocationStatus()

                Toast.makeText(
                    this,
                    "${location.name} deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                updateLocationStatus()

                Toast.makeText(
                    this,
                    "Unable to delete location: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // STATUS TEXT
    // =========================================================

    private fun updateLocationStatus() {

        val count =
            savedLocations.size

        tvLocationStatus.text =
            when (count) {

                0 -> {
                    "No saved locations yet • Tap map to add one"
                }

                1 -> {
                    "1 saved location • Tap map to add another"
                }

                else -> {
                    "$count saved locations • Tap map to add another"
                }
            }
    }

    // =========================================================
    // MARKER COLOR
    // =========================================================

    private fun getMarkerColor(
        type: String
    ): Int {

        return when (type.lowercase()) {

            "vet" -> {

                // Red
                Color.rgb(
                    211,
                    47,
                    47
                )
            }

            "grooming" -> {

                // Purple
                Color.rgb(
                    156,
                    39,
                    176
                )
            }

            "dog park" -> {

                // Green
                Color.rgb(
                    46,
                    125,
                    50
                )
            }

            "pet store" -> {

                // Orange
                Color.rgb(
                    245,
                    124,
                    0
                )
            }

            else -> {

                Color.rgb(
                    25,
                    118,
                    210
                )
            }
        }
    }

    // =========================================================
    // DP TO PIXELS
    // =========================================================

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }
}