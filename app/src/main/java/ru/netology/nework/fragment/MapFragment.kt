package ru.netology.nework.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentMapBinding

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var selectedPoint: Point? = null
    private var initialLatitude: Double? = null
    private var initialLongitude: Double? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val MOSCOW_LATITUDE = 55.7558
        private const val MOSCOW_LONGITUDE = 37.6173
        private const val DEFAULT_ZOOM = 15.0f
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            initialLatitude = it.getDouble("latitude", MOSCOW_LATITUDE)
            initialLongitude = it.getDouble("longitude", MOSCOW_LONGITUDE)
        }

        setupMap()
        setupClickListeners()
        checkLocationPermission()
    }

    private fun setupMap() {
        mapView = binding.mapView

        val initialPoint = Point(
            initialLatitude ?: MOSCOW_LATITUDE,
            initialLongitude ?: MOSCOW_LONGITUDE
        )

        mapView.map.move(
            CameraPosition(initialPoint, DEFAULT_ZOOM, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
        mapView.map.addInputListener(object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                handleMapTap(point)
            }

            override fun onMapLongTap(map: Map, point: Point) {
                handleMapTap(point)
            }
        })

        if (initialLatitude != null && initialLongitude != null) {
            selectedPoint = Point(initialLatitude!!, initialLongitude!!)
            showSelectedMarker()
        }
    }
    private fun handleMapTap(point: Point) {
        selectedPoint = point
        showSelectedMarker()
        updateSelectedLocationText()

        Snackbar.make(binding.root, "Локация выбрана", Snackbar.LENGTH_SHORT).show()
    }

    private fun showSelectedMarker() {
        selectedPoint?.let { point ->
            mapView.map.mapObjects.clear()
            val marker = mapView.map.mapObjects.addPlacemark(point)
            marker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_marker)!!)
            marker.opacity = 0.9f
        }
    }

    private fun updateSelectedLocationText() {
        selectedPoint?.let { point ->
            binding.selectedLocationText.visibility = View.VISIBLE
            binding.selectedLocationText.text =
                "📍 Выбрано: ${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)}"
            binding.confirmButton.isEnabled = true
        } ?: run {
            binding.selectedLocationText.visibility = View.GONE
            binding.confirmButton.isEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.confirmButton.setOnClickListener {
            confirmLocationSelection()
        }

        binding.myLocationButton.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    private fun confirmLocationSelection() {
        selectedPoint?.let { point ->
            val result = Bundle().apply {
                putDouble("latitude", point.latitude)
                putDouble("longitude", point.longitude)
            }

            parentFragmentManager.setFragmentResult("location_selection", result)
            findNavController().navigateUp()
        } ?: run {
            Snackbar.make(binding.root, "Сначала выберите локацию на карте", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun moveToCurrentLocation() {
        if (hasLocationPermission()) {

            val moscowPoint = Point(MOSCOW_LATITUDE, MOSCOW_LONGITUDE)
            mapView.map.move(
                CameraPosition(moscowPoint, DEFAULT_ZOOM, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
            Snackbar.make(binding.root, "Перемещено к Москве", Snackbar.LENGTH_SHORT).show()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (!hasLocationPermission()) {
            binding.locationPermissionView.visibility = View.VISIBLE
            binding.grantPermissionButton.setOnClickListener {
                requestLocationPermission()
            }
        } else {
            binding.locationPermissionView.visibility = View.GONE
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.locationPermissionView.visibility = View.GONE
                    moveToCurrentLocation()
                } else {
                    Snackbar.make(binding.root, "Разрешение на доступ к локации не предоставлено", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MapFragment"

        fun newInstance(latitude: Double? = null, longitude: Double? = null): MapFragment {
            return MapFragment().apply {
                arguments = Bundle().apply {
                    latitude?.let { putDouble("latitude", it) }
                    longitude?.let { putDouble("longitude", it) }
                }
            }
        }
    }
}