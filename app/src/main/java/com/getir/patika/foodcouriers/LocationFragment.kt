import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.getir.patika.foodcouriers.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import java.io.IOException

class LocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap
    private lateinit var searchView: SearchView
    private lateinit var locationCard: CardView
    private lateinit var locationTextView: TextView
    private lateinit var setLocationButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geocoder = Geocoder(requireContext())

        mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        searchView = view.findViewById<SearchView>(R.id.search_label)
        locationCard = view.findViewById(R.id.location_details_card)
        locationTextView = view.findViewById(R.id.full_address_text)
        setLocationButton = view.findViewById(R.id.set_location_button)

        searchView.queryHint = "Where is your location"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchLocation(requireContext(), it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        setLocationButton.setOnClickListener {
            // Handle setting the location
        }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLocation = LatLng(location.latitude, location.longitude)
                        googleMap.addMarker(
                            MarkerOptions().position(currentLocation)
                                .title("Current Location")
                        )
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLocation,
                                17f
                            )
                        )

                        // Update location card text
                        try {
                            val addresses = geocoder.getFromLocation(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                1
                            )
                            if (addresses != null && addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val addressLine = address.getAddressLine(0)
                                if (addressLine != null) {
                                    locationTextView.text = addressLine
                                } else {
                                    locationTextView.text = "Address not available"
                                }
                            }

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle failure to obtain last known location
                }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun searchLocation(context: Context, query: String) {
        val placesClient = Places.createClient(context)

        // Specify the maximum number of results to return
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountry("your_country_code") // Specify the country for better results
            .setTypeFilter(TypeFilter.ADDRESS) // Use TypeFilter.ADDRESS for addresses
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val prediction = response.autocompletePredictions.firstOrNull()
            if (prediction != null) {
                fetchPlaceDetails(context, prediction.placeId)
            } else {
                Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Log.e("PlacesSearch", "Error searching for location: ${exception.message}")
        }
    }

    private fun fetchPlaceDetails(context: Context, placeId: String) {
        val placesClient = Places.createClient(context)
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)

        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            val latLng = place.latLng
            if (latLng != null) {
                // Handle the retrieved location (LatLng)
                // For example, you can update UI elements or perform other tasks
                // Note: Ensure you have appropriate UI elements and logic to handle the retrieved location
            }
        }.addOnFailureListener { exception ->
            Log.e("PlacesSearch", "Error fetching place details: ${exception.message}")
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}
