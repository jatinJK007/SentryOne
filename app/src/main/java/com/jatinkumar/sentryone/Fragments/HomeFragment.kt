package com.jatinkumar.sentryone.Fragments

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
import com.jatinkumar.sentryone.AppSettings
import com.jatinkumar.sentryone.AppSettingsManager
import com.jatinkumar.sentryone.ContactsViewModelFactory
import com.jatinkumar.sentryone.R
import com.jatinkumar.sentryone.databinding.FragmentHomeBinding
import com.jatinkumar.sentryone.viewModels.ContactsViewModel
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
import com.jatinkumar.sentryone.Database.SOSHistoryClass
import com.jatinkumar.sentryone.viewModels.SOSHistoryViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactsViewModel by activityViewModels {
        ContactsViewModelFactory(requireActivity().application)
    }
    private val sosViewmodel: SOSHistoryViewModel by activityViewModels()
    private lateinit var appSettingsManager: AppSettingsManager
    private var currentAppSettings: AppSettings? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private val SHAKE_THRESHOLD_GRAVITY = 2.7f
    private val SHAKE_SLOP_TIME_MS = 500
    private var mShakeTimestamp: Long = 0

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private val FLASHLIGHT_BLINK_INTERVAL = 400L // milliseconds
    private val FLASHLIGHT_BLINK_COUNT = 6 // Number of blinks

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sosLocationRequest: LocationRequest
    private lateinit var displayLocationRequest: LocationRequest

    private var sosLocationCallback: LocationCallback? = null
    private var displayLocationCallback: LocationCallback? = null
    private var sendingLocationSosJob: Job? = null

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

        if (smsPermissionGranted) {
            Log.d("HomeFragment", "SMS permission granted.")
            if (settings.locationAccess && allRequiredLocationGranted) {
                checkLocationSettingsAndFetchSosLocation(settings)
            } else {
                val message = settings.emergencyMessage.ifEmpty { "Emergency! I need help!" }
                sendSosMessageInternal(message)
            }
        } else {
            Snackbar.make(requireView(), "SMS permission denied. Cannot send emergency messages.", Snackbar.LENGTH_LONG).show()
            Log.w("HomeFragment", "SMS permission denied.")
        }

        if (settings.locationAccess && !allRequiredLocationGranted) {
            Snackbar.make(requireView(), "Location permissions denied. Cannot include precise location.", Snackbar.LENGTH_LONG).show()
            Log.w("HomeFragment", "Location permissions denied.")
        } else if (settings.locationAccess && allRequiredLocationGranted) {
            Snackbar.make(requireView(), "Location permissions granted!", Snackbar.LENGTH_SHORT).show()
            startLocationUpdatesForDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sosLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5 seconds interval for SOS
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .build()

        displayLocationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000) // 10 seconds interval for display
            .setMinUpdateIntervalMillis(5000)
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
            cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            Log.e("HomeFragment", "Cannot access camera for flashlight: ${e.message}")
            Snackbar.make(requireView(), "Flashlight not available.", Snackbar.LENGTH_SHORT).show()
        }
        setupSosButton()
        observeAppSettings()
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
                currentAppSettings = settings
                Log.d("HomeFragment", "AppSettings updated: $settings")
                if (settings.shakeDetection) {
                    enableShakeDetection()
                } else {
                    disableShakeDetection()
                }
                binding.tvLastAlert.text = "  Last Alert Sent:\n"+"${settings.lastAlertSend}"
            }
        }
    }

    private fun triggerSosAction(triggerSource: String) {
        val settings = currentAppSettings
        if (settings == null) {
            Log.e("HomeFragment", "AppSettings not loaded yet. Cannot trigger SOS.")
            Snackbar.make(requireView(), "Settings not loaded. Please try again.", Snackbar.LENGTH_SHORT).show()
            return
        }

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
            initiateSosFlow(settings)
        }
    }

    private fun initiateSosFlow(settings: AppSettings) {
        Log.d("HomeFragment", "Initiating SOS flow with settings: $settings")

        if (settings.hepticFeedback) {
            triggerHapticFeedback()
        }
        if (settings.flashTrigger) {
            blinkFlashlight()
        }
        val permissionsToRequest = mutableListOf(Manifest.permission.SEND_SMS)
        if (settings.locationAccess) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            val message = settings.emergencyMessage.ifEmpty { "Emergency! I need help!" }
            sendSosMessageInternal(message)
        }
    }

    private fun triggerHapticFeedback() {
        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
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
                        val turnOn = i % 2 == 0
                        cameraManager.setTorchMode(id, turnOn)
                        Log.d("HomeFragment", "Flashlight ${if (turnOn) "ON" else "OFF"}")
                    } catch (e: CameraAccessException) {
                        Log.e("HomeFragment", "Flashlight control error: ${e.message}")
                        Snackbar.make(requireView(), "Error controlling flashlight.", Snackbar.LENGTH_SHORT).show()
                        return@launch
                    }
                    delay(FLASHLIGHT_BLINK_INTERVAL)
                }
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

    @SuppressLint("MissingPermission")
    private fun startLocationUpdatesForDisplay() {
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
            setDetails(null)
            return
        }

        displayLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("HomeFragment", "Removed existing display location callback.")
        }

        displayLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("HomeFragment", "Display Location received: ${location.latitude}, ${location.longitude}")
                    setDetails(location)
                } ?: run {
                    Log.w("HomeFragment", "Display locationResult.lastLocation is null.")
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(displayLocationRequest, displayLocationCallback!!, requireContext().mainLooper)
        Log.d("HomeFragment", "Started location updates for UI display.")
    }

    private fun checkLocationSettingsAndFetchSosLocation(settings: AppSettings) {
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(sosLocationRequest)
            .setAlwaysShow(true)
            .build()

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { locationSettingsResponse ->
                Log.d("HomeFragment", "Location settings are adequate. Requesting SOS location updates.")
                fetchSosLocation(settings)
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {

                        exception.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
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

    @SuppressLint("MissingPermission")
    private fun fetchSosLocation(settings: AppSettings) {
        sendingLocationSosJob?.cancel()

        sendingLocationSosJob = lifecycleScope.launch {
            Log.d("HomeFragment", "Starting location request for SOS...")
            val snackbar = Snackbar.make(requireView(), "Getting current location...", Snackbar.LENGTH_INDEFINITE)
            snackbar.show()

            var foundLocation: Location? = null
            var locationReceived = false

            sosLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        if (!locationReceived) {
                            foundLocation = location
                            locationReceived = true
                            Log.d("HomeFragment", "SOS Location received: ${location.latitude}, ${location.longitude}")
                            fusedLocationClient.removeLocationUpdates(this) // 'this' refers to sosLocationCallback
                            snackbar.dismiss()
                            sendSosWithLocation(foundLocation, settings)
                            sendingLocationSosJob?.cancel()
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(sosLocationRequest, sosLocationCallback!!, requireContext().mainLooper)

            val LOCATION_TIMEOUT_MS = 10000L
            delay(LOCATION_TIMEOUT_MS)

            if (!locationReceived) {
                Log.w("HomeFragment", "SOS Location request timed out after $LOCATION_TIMEOUT_MS ms. Sending SOS without location.")
                sosLocationCallback?.let {
                    fusedLocationClient.removeLocationUpdates(it)
                }
                snackbar.dismiss()
                Snackbar.make(requireView(), "Could not get precise location in time.", Snackbar.LENGTH_LONG).show()
                sendSosWithLocation(null, settings)
                sendingLocationSosJob?.cancel()
            }
        }
    }

    private fun setDetails(location: Location?) {
        binding.tvCurrentLatitude.text = "Current Latitude: ${location?.latitude.toString() ?: "N/A"}"
        binding.tvCurrentLongitude.text = "Current Longitude: ${location?.longitude.toString() ?: "N/A"}"

    }

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
        val time = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val formattedTime = time.format(formatter)
        Log.d("HomeFragment", "Formatted time: $formattedTime")

        val sosHistoryItem = SOSHistoryClass(
            locationLatitude = location?.latitude.toString(),
            locationLongitude = location?.longitude.toString(),
            triggerTime = formattedTime
        )
        sosViewmodel.insert(sosHistoryItem)
        Log.d("TAG", "sendSosWithLocation: db created and updated")

        sendSosMessageInternal(fullMessage)
    }

private fun sendSosMessageInternal(message: String) {
    Log.d("TAG", "sendSosMessageInternal: into sms msg functionality")
    viewModel.allContacts.observe(viewLifecycleOwner) { contacts ->
        if (contacts.isEmpty()) {
            Snackbar.make(requireView(), "No emergency contacts set!", Snackbar.LENGTH_LONG).show()
            Log.w("HomeFragment", "No emergency contacts found to send message.")
            return@observe
        }

        var anySmsSentAttempted = false
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requireContext().getSystemService(SmsManager::class.java) as SmsManager
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        for (contact in contacts) {
            if (contact.phoneNumber.isNotBlank()) {
                try {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                    Log.d("HomeFragment", "SMS sent to ${contact.phoneNumber}")
                    Snackbar.make(requireView(), "SOS sent to ${contact.phoneNumber}", Snackbar.LENGTH_SHORT).show()
                    anySmsSentAttempted = true // Set flag to true if at least one SMS was sent
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
                    Snackbar.make(requireView(), "Failed to send SOS message to ${contact.phoneNumber}.", Snackbar.LENGTH_LONG).show()
                }
            } else {
                Log.w("HomeFragment", "Contact ${contact.name} has no phone number, skipping SMS.")
            }
        }

        if (anySmsSentAttempted) {
            lifecycleScope.launch {
                val currentTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                val formattedTime = currentTime.format(formatter)
                appSettingsManager.updateLastAlertSend(formattedTime)
                Log.d("HomeFragment", "Last alert send time updated: $formattedTime")
            }
        }
        if (!anySmsSentAttempted) {
            Log.w("HomeFragment", "No SMS messages could be sent.")
        }
    }
}

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (currentAppSettings?.shakeDetection == true && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH
                val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    val now = System.currentTimeMillis()
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
        if (currentAppSettings?.shakeDetection == true) {
            enableShakeDetection()
        }
        startLocationUpdatesForDisplay()
    }

    override fun onPause() {
        super.onPause()
        disableShakeDetection()

        sosLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("HomeFragment", "SOS Location updates removed in onPause.")
        }
        displayLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("HomeFragment", "Display Location updates removed in onPause.")
        }
        sendingLocationSosJob?.cancel()
        Log.d("HomeFragment", "Pending SOS location job cancelled in onPause.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        disableShakeDetection()
        sosLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        displayLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        sendingLocationSosJob?.cancel()
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 100
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("HomeFragment", "User enabled location services for SOS. Retrying location fetch.")
                currentAppSettings?.let { settings ->
                    fetchSosLocation(settings)
                }
            } else {
                Log.w("HomeFragment", "User declined to enable location services for SOS. Sending SOS without location.")
                Snackbar.make(requireView(), "Location services not enabled. Sending SOS without location.", Snackbar.LENGTH_LONG).show()
                currentAppSettings?.let { settings ->
                    sendSosWithLocation(null, settings)
                    sendingLocationSosJob?.cancel()
                }
            }
        }
    }
}