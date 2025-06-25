package com.example.sentryone.Fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.sentryone.AppSettings
import com.example.sentryone.AppSettingsManager
import com.example.sentryone.ContactsViewModelFactory
import com.example.sentryone.R
import com.example.sentryone.databinding.FragmentHomeBinding
import com.example.sentryone.viewModels.ContactsViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.Manifest
import android.annotation.SuppressLint
import android.location.Location


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactsViewModel by activityViewModels {
        ContactsViewModelFactory(requireActivity().application)
    }
    private lateinit var appSettingsManager: AppSettingsManager
    private var currentAppSettings: AppSettings? = null // Holds the latest settings from DataStore

    // For Shake Detection
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // Shake detection threshold and debounce
    private val SHAKE_THRESHOLD_GRAVITY = 2.7f
    private val SHAKE_SLOP_TIME_MS = 500
    private var mShakeTimestamp: Long = 0

    // For Flashlight
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private val FLASHLIGHT_BLINK_INTERVAL = 400L // milliseconds
    private val FLASHLIGHT_BLINK_COUNT = 6 // Number of blinks

    // For Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // LocationRequest for SOS (high accuracy, single shot concept with timeout)
    private lateinit var sosLocationRequest: LocationRequest
    // LocationRequest for UI display (lower frequency, continuous updates)
    private lateinit var displayLocationRequest: LocationRequest

    private var sosLocationCallback: LocationCallback? = null // Receives location updates for SOS
    private var displayLocationCallback: LocationCallback? = null // Receives location updates for UI display
    private var sendingLocationSosJob: Job? = null // Manages the location fetching coroutine and its timeout for SOS

    // Unified permission launcher for all necessary permissions
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsPermissionGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        val allRequiredLocationGranted = fineLocationGranted && coarseLocationGranted

        val settings = currentAppSettings
        if (settings == null) {
            Log.e("HomeFragment", "AppSettings not loaded after permission request. Cannot proceed.")
            Snackbar.make(requireView(), "Error: Settings not loaded.", Snackbar.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        // Handle SMS sending based on permission and settings
        if (smsPermissionGranted) {
            Log.d("HomeFragment", "SMS permission granted.")
            // If location access is enabled and granted, fetch location first
            if (settings.locationAccess && allRequiredLocationGranted) {
                checkLocationSettingsAndFetchSosLocation(settings)
            } else {
                // Otherwise, send basic SOS message (with or without location if denied/not enabled)
                val message = settings.emergencyMessage.ifEmpty { "Emergency! I need help!" }
                sendSosMessageInternal(message, settings.silentlySend)
            }
        } else {
            Snackbar.make(requireView(), "SMS permission denied. Cannot send emergency messages.", Snackbar.LENGTH_LONG).show()
            Log.w("HomeFragment", "SMS permission denied.")
        }

        // Provide feedback for location permission if relevant
        if (settings.locationAccess && !allRequiredLocationGranted) {
            Snackbar.make(requireView(), "Location permissions denied. Cannot include precise location.", Snackbar.LENGTH_LONG).show()
            Log.w("HomeFragment", "Location permissions denied.")
        } else if (settings.locationAccess && allRequiredLocationGranted) {
            Snackbar.make(requireView(), "Location permissions granted!", Snackbar.LENGTH_SHORT).show()
            // If location permissions are just granted, start display updates
            startLocationUpdatesForDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize LocationRequest here as it's a good place for fixed configurations
        sosLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5 seconds interval for SOS
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000) // Minimum 2 seconds interval
            .build()

        displayLocationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000) // 10 seconds interval for display
            .setMinUpdateIntervalMillis(5000) // Minimum 5 seconds interval
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appSettingsManager = AppSettingsManager(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Get the ID of the back camera with a flashlight
            cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            Log.e("HomeFragment", "Cannot access camera for flashlight: ${e.message}")
            Snackbar.make(requireView(), "Flashlight not available.", Snackbar.LENGTH_SHORT).show()
        }
        setupSosButton()
        observeAppSettings()

        // Start location updates for display as soon as the view is created
        startLocationUpdatesForDisplay()
    }

    private fun setupSosButton() {
        binding.sosImage.setOnClickListener {
            Log.d("HomeFragment", "SOS Button Clicked")
            triggerSosAction("Manual button press")
        }
    }

    private fun observeAppSettings() {
        lifecycleScope.launch {
            appSettingsManager.appSettingsFlow.collectLatest { settings ->
                currentAppSettings = settings // Update the current settings
                Log.d("HomeFragment", "AppSettings updated: $settings")
                // Handle shake detection state dynamically based on settings
                if (settings.shakeDetection) {
                    enableShakeDetection()
                } else {
                    disableShakeDetection()
                }
            }
        }
    }

    private fun triggerSosAction(triggerSource: String) {
        val settings = currentAppSettings // Get the latest settings
        if (settings == null) {
            Log.e("HomeFragment", "AppSettings not loaded yet. Cannot trigger SOS.")
            Snackbar.make(requireView(), "Settings not loaded. Please try again.", Snackbar.LENGTH_SHORT).show()
            return
        }

        // 1. Show Dialogue (if enabled)
        if (settings.showDialogue) {
            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm SOS Action")
                .setMessage("You are about to send an emergency message. Continue?")
                .setPositiveButton("Send SOS") { dialog, _ ->
                    initiateSosFlow(settings) // Call the new unified flow
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    Snackbar.make(requireView(), "SOS cancelled.", Snackbar.LENGTH_SHORT).show()
                    dialog.cancel()
                }
            val dialog = builder.create()
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color_primary))

                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color_primary))
            }
            dialog.show()
        } else {
            initiateSosFlow(settings) // Call the new unified flow directly
        }
    }

    private fun initiateSosFlow(settings: AppSettings) {
        Log.d("HomeFragment", "Initiating SOS flow with settings: $settings")

        // Perform immediate non-permission-dependent actions
        if (settings.hepticFeedback) {
            triggerHapticFeedback()
        }
        if (settings.flashTrigger) {
            blinkFlashlight()
        }
        // Determine required permissions based on settings
        val permissionsToRequest = mutableListOf(Manifest.permission.SEND_SMS)
        if (settings.locationAccess) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Request permissions
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // If no special permissions needed (e.g., only basic message, no location), send directly
            val message = settings.emergencyMessage.ifEmpty { "Emergency! I need help!" }
            sendSosMessageInternal(message, settings.silentlySend)
        }
    }

    private fun triggerHapticFeedback() {
        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300) // Vibrate for 300 milliseconds
            }
            Log.d("HomeFragment", "Haptic feedback triggered.")
        } else {
            Log.w("HomeFragment", "Device does not have a vibrator.")
        }
    }

    private fun blinkFlashlight() {
        cameraId?.let { id ->
            lifecycleScope.launch {
                Snackbar.make(requireView(), "Blinking flashlight...", Snackbar.LENGTH_SHORT).show()
                repeat(FLASHLIGHT_BLINK_COUNT) { i ->
                    try {
                        val turnOn = i % 2 == 0 // Turn on for even cycles, off for odd
                        cameraManager.setTorchMode(id, turnOn)
                        Log.d("HomeFragment", "Flashlight ${if (turnOn) "ON" else "OFF"}")
                    } catch (e: CameraAccessException) {
                        Log.e("HomeFragment", "Flashlight control error: ${e.message}")
                        Snackbar.make(requireView(), "Error controlling flashlight.", Snackbar.LENGTH_SHORT).show()
                        return@launch // Exit coroutine if error occurs
                    }
                    delay(FLASHLIGHT_BLINK_INTERVAL)
                }
                // Ensure flashlight is off at the end
                try {
                    cameraManager.setTorchMode(id, false)
                } catch (e: CameraAccessException) {
                    Log.e("HomeFragment", "Failed to turn off flashlight: ${e.message}")
                }
            }
        } ?: run {
            Log.w("HomeFragment", "No flashlight available on this device.")
            Snackbar.make(requireView(), "No flashlight available on this device.", Snackbar.LENGTH_SHORT).show()
        }
    }

    // New function to start location updates for UI display
    @SuppressLint("MissingPermission") // Permissions checked by callers
    private fun startLocationUpdatesForDisplay() {
        // Only start if permissions are granted
        val hasFineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w("HomeFragment", "Location permissions not granted for display updates.")
            setDetails(null) // Clear display if no permissions
            return
        }

        // Remove existing callback to avoid multiple listeners if called again
        displayLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("HomeFragment", "Removed existing display location callback.")
        }

        displayLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("HomeFragment", "Display Location received: ${location.latitude}, ${location.longitude}")
                    setDetails(location) // Update UI with latest location
                } ?: run {
                    Log.w("HomeFragment", "Display locationResult.lastLocation is null.")
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(displayLocationRequest, displayLocationCallback!!, requireContext().mainLooper)
        Log.d("HomeFragment", "Started location updates for UI display.")
    }

    // Function to check location settings and then fetch location for SOS
    private fun checkLocationSettingsAndFetchSosLocation(settings: AppSettings) {
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(sosLocationRequest)
            .setAlwaysShow(true) // Show the dialog even if the settings are adequate
            .build()

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { locationSettingsResponse ->
                // Location settings are satisfied. Proceed to request location updates.
                Log.d("HomeFragment", "Location settings are adequate. Requesting SOS location updates.")
                fetchSosLocation(settings)
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        // Show the dialog by calling startResolutionForResult()
                        // and check the result in onActivityResult().
                        exception.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                        Log.e("HomeFragment", "Error showing location settings dialog: ${sendEx.message}")
                    }
                } else {
                    Log.e("HomeFragment", "Location settings check failed: ${exception.message}")
                    Snackbar.make(requireView(), "Location services are not enabled or correctly configured. Sending SOS without location.", Snackbar.LENGTH_LONG).show()
                    sendSosWithLocation(null, settings) // Send without location on critical failure
                    sendingLocationSosJob?.cancel()
                }
            }
    }


    // Function to actively fetch location for SOS message
    @SuppressLint("MissingPermission") // Suppress lint here as permission is checked by callers
    private fun fetchSosLocation(settings: AppSettings) {
        // Cancel any previous location job to ensure only one is active
        sendingLocationSosJob?.cancel()

        // Create a new job for this location request and its timeout
        sendingLocationSosJob = lifecycleScope.launch {
            Log.d("HomeFragment", "Starting location request for SOS...")
            val snackbar = Snackbar.make(requireView(), "Getting current location...", Snackbar.LENGTH_INDEFINITE)
            snackbar.show()

            var foundLocation: Location? = null
            var locationReceived = false

            // Define the LocationCallback to receive updates
            sosLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        if (!locationReceived) { // Process only the first valid location
                            foundLocation = location
                            locationReceived = true
                            Log.d("HomeFragment", "SOS Location received: ${location.latitude}, ${location.longitude}")
                            // Stop updates as soon as a location is found
                            fusedLocationClient.removeLocationUpdates(this) // 'this' refers to sosLocationCallback
                            snackbar.dismiss()
                            sendSosWithLocation(foundLocation, settings) // Call helper to send SMS
                            // No need to call setDetails here, as displayLocationCallback handles UI updates
                            sendingLocationSosJob?.cancel() // Cancel the job once location is processed
                        }
                    }
                }
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(sosLocationRequest, sosLocationCallback!!, requireContext().mainLooper)

            // Implement a timeout for location acquisition
            val LOCATION_TIMEOUT_MS = 10000L // 10 seconds timeout for getting a location
            delay(LOCATION_TIMEOUT_MS)

            // If we reach here and haven't received a location yet (i.e., timed out)
            if (!locationReceived) {
                Log.w("HomeFragment", "SOS Location request timed out after $LOCATION_TIMEOUT_MS ms. Sending SOS without location.")
                sosLocationCallback?.let {
                    fusedLocationClient.removeLocationUpdates(it) // Remove updates if timed out
                }
                snackbar.dismiss()
                Snackbar.make(requireView(), "Could not get precise location in time.", Snackbar.LENGTH_LONG).show()
                sendSosWithLocation(null, settings) // Send without location
                sendingLocationSosJob?.cancel() // Ensure the job is cancelled
            }
        }
    }

    private fun setDetails(location: Location?) {
        binding.tvCurrentLatitude.text = "Current Latitude: "+ location?.latitude.toString() ?: "N/A"
        binding.tvCurrentLongitude.text = "Current Longitude: "+ location?.longitude.toString() ?: "N/A"
    }

    // Helper function to build the SOS message with location (if available) and send it
    private fun sendSosWithLocation(location: Location?, settings: AppSettings) {
        Log.d("HomeFragment", "sendSosWithLocation: Latitude: ${location?.latitude}, Longitude: ${location?.longitude},")
        val emergencyMessage = settings.emergencyMessage.ifEmpty { "Emergency! I need help!" }
        val fullMessage = if (location != null) {
            "Latitude: ${location.latitude}, Longitude: ${location.longitude}, Accuracy: ${location.accuracy}m. $emergencyMessage"
        } else {
            Log.w("HomeFragment", "Location is null for SOS message construction.")
            "Unable to get current location details. $emergencyMessage"
        }
        Log.d("HomeFragment", "Final SOS message to send: $fullMessage")
        sendSosMessageInternal(fullMessage, settings.silentlySend)
    }

    // Renamed from _sendSmsActual to make it clearer it's the internal sending mechanism
    private fun sendSosMessageInternal(message: String, silentlySend: Boolean) {
        Log.d("TAG", "sendSosMessageInternal: into sms msg functionality")
        viewModel.allContacts.observe(viewLifecycleOwner) { contacts ->
            if (contacts.isEmpty()) {
                Snackbar.make(requireView(), "No emergency contacts set!", Snackbar.LENGTH_LONG).show()
                Log.w("HomeFragment", "No emergency contacts found to send message.")
                return@observe // Return from the lambda, not the function
            }
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requireContext().getSystemService(SmsManager::class.java) as SmsManager
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            for (contact in contacts) {
                try {
                    if (silentlySend) {
                        val parts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                        Log.d("HomeFragment", "SMS sent silently to ${contact.phoneNumber}")
                        Snackbar.make(requireView(), "SOS sent silently to ${contact.phoneNumber}", Snackbar.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${contact.phoneNumber}")
                            putExtra("sms_body", message)
                        }
                        if (intent.resolveActivity(requireContext().packageManager) != null) {
                            startActivity(intent)
                            Log.d("HomeFragment", "Opening SMS app for ${contact.phoneNumber}")
                            Snackbar.make(requireView(), "Opening SMS app to send message to ${contact.phoneNumber}", Snackbar.LENGTH_LONG).show()
                        } else {
                            Log.e("HomeFragment", "No SMS app found to handle intent for ${contact.phoneNumber}.")
                            Snackbar.make(requireView(), "No SMS app found. Cannot send message.", Snackbar.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
                    Snackbar.make(
                        requireView(),
                        "Failed to send SOS message to ${contact.phoneNumber}.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // Only process if shake detection is enabled and it's an accelerometer event
            if (currentAppSettings?.shakeDetection == true && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Calculate g-force (magnitude of acceleration)
                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH
                val gForce = kotlin.math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    val now = System.currentTimeMillis()
                    // Debounce: ignore shakes too close to each other
                    if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                        return
                    }
                    mShakeTimestamp = now
                    Log.d("ShakeDetector", "SHAKE detected! G-force: $gForce")
                    Snackbar.make(requireView(), "Shake detected!", Snackbar.LENGTH_SHORT).show()
                    triggerSosAction("Shake detected")
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not used for shake detection
        }
    }

    private fun enableShakeDetection() {
        if (sensorManager == null) {
            sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        accelerometer?.let {
            sensorManager?.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("HomeFragment", "Shake detection enabled.")
        } ?: run {
            Log.w("HomeFragment", "Accelerometer sensor not found on this device.")
            Snackbar.make(requireView(), "Accelerometer not found for shake detection.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun disableShakeDetection() {
        sensorManager?.unregisterListener(shakeListener)
        Log.d("HomeFragment", "Shake detection disabled.")
    }

    override fun onResume() {
        super.onResume()
        // Re-enable shake detection if it was enabled in settings
        if (currentAppSettings?.shakeDetection == true) {
            enableShakeDetection()
        }
        // Restart display location updates if permissions are still granted
        startLocationUpdatesForDisplay()
    }

    override fun onPause() {
        super.onPause()
        // Always disable shake detection when the fragment is not in the foreground
        // to save battery and avoid accidental triggers.
        disableShakeDetection()

        // IMPORTANT: Remove location updates when fragment is paused to save battery
        sosLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("HomeFragment", "SOS Location updates removed in onPause.")
        }
        displayLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("HomeFragment", "Display Location updates removed in onPause.")
        }
        sendingLocationSosJob?.cancel() // Cancel any pending SOS location job
        Log.d("HomeFragment", "Pending SOS location job cancelled in onPause.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Ensure any active listeners are unregistered
        disableShakeDetection()
        // Ensure location updates are removed if fragment view is destroyed
        sosLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        displayLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        sendingLocationSosJob?.cancel() // Cancel the job on view destruction as well
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 100
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // User enabled location or changed settings as requested
                Log.d("HomeFragment", "User enabled location services for SOS. Retrying location fetch.")
                // Re-initiate the location fetch, perhaps by calling checkLocationSettingsAndFetchSosLocation again
                currentAppSettings?.let { settings ->
                    fetchSosLocation(settings) // Directly call fetchSosLocation as settings are now adequate
                }
            } else {
                // User cancelled or declined to change settings
                Log.w("HomeFragment", "User declined to enable location services for SOS. Sending SOS without location.")
                Snackbar.make(requireView(), "Location services not enabled. Sending SOS without location.", Snackbar.LENGTH_LONG).show()
                currentAppSettings?.let { settings ->
                    sendSosWithLocation(null, settings) // Send without location
                    sendingLocationSosJob?.cancel()
                }
            }
        }
    }
}