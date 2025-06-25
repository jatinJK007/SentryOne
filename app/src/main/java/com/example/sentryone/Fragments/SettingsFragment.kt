package com.example.sentryone.Fragments

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.sentryone.AppSettingsKeys
import com.example.sentryone.AppSettingsManager
import com.example.sentryone.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    private lateinit var appSettingsManager: AppSettingsManager
    private var _binding : FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Request launcher for location permissions
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // Permissions granted, you might want to update UI or take action
            binding.tvErrorMessage.visibility = View.GONE
            binding.switchLocationAccess.isChecked = true
        } else {
            // Permissions denied, update UI or show a message
            binding.tvErrorMessage.text = "Location permission denied. Features may be limited."
            binding.tvErrorMessage.visibility = View.VISIBLE
            binding.switchLocationAccess.isChecked = false
        }
        // Save the updated state to DataStore
        lifecycleScope.launch {
            appSettingsManager.updateSetting(AppSettingsKeys.LOCATION_ACCESS, granted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding= FragmentSettingsBinding.inflate(inflater,container,false)
        val view = binding.root
        appSettingsManager = AppSettingsManager(requireContext())
        setupListeners()
        observeSettings()
        return view
    }

    private fun setupListeners() {
        // Listener for Dark Mode switch
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.DARK_MODE, isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        // Listener for Location Access switch
        binding.switchLocationAccess.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestLocationPermissions()
            } else {
                // User wants to revoke, update DataStore directly
                lifecycleScope.launch {
                    appSettingsManager.updateSetting(AppSettingsKeys.LOCATION_ACCESS, false)
                    binding.tvErrorMessage.visibility = View.GONE // Hide error if user manually unchecks
                }
            }
        }

        // Listener for Triggering Mode switch
//        binding.switchTriggeringMode.setOnCheckedChangeListener { _, isChecked ->
//            lifecycleScope.launch {
//                appSettingsManager.updateSetting(AppSettingsKeys.TRIGGERING_MODE, isChecked)
//            }
//        }

        // Listener for Silently Send switch
        binding.switchSilentlySend.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.SILENTLY_SEND, isChecked)
            }
        }

        // Listener for Show Dialogue switch
        binding.switchDialogue.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.SHOW_DIALOGUE, isChecked)
            }
        }

        // Listener for Shake Detection switch
        binding.switchShakeDetction.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.SHAKE_DETECTION, isChecked)
            }
        }

        // Listener for Flash Trigger switch - ADDED THIS ONE
        binding.switchFlashTrigger.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.FLASH_TRIGGER, isChecked)
            }
        }

        // Listener for Heptic Feedback switch - ADDED THIS ONE
        binding.switchHepticFeedback.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.HEPTIC_FEEDBACK, isChecked)
            }
        }

        // Save button listener
        binding.btnSave.setOnClickListener {
            val emergencyMessage = binding.emergencyMsg.text.toString()
            lifecycleScope.launch {
                appSettingsManager.updateEmergencyMessage(emergencyMessage)
                Snackbar.make(requireView(), "Settings saved!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            appSettingsManager.appSettingsFlow.collectLatest { settings ->
                // Update UI based on the latest settings using binding
                binding.switchDarkMode.isChecked = settings.darkMode
                binding.switchLocationAccess.isChecked = settings.locationAccess
//                binding.switchTriggeringMode.isChecked = settings.triggeringMode
                binding.switchSilentlySend.isChecked = settings.silentlySend
                binding.emergencyMsg.setText(settings.emergencyMessage) // Use binding.emergencyMsg
                binding.switchDialogue.isChecked = settings.showDialogue
                binding.switchShakeDetction.isChecked = settings.shakeDetection
                binding.switchFlashTrigger.isChecked = settings.flashTrigger
                binding.switchHepticFeedback.isChecked = settings.hepticFeedback


                // Handle initial state of location access and error message
                if (settings.locationAccess && !checkLocationPermissions()) {
                    binding.tvErrorMessage.text = "Location permission needed. Please grant it."
                    binding.tvErrorMessage.visibility = View.VISIBLE
                    binding.switchLocationAccess.isChecked = false // Uncheck if permission is not truly granted
                } else {
                    binding.tvErrorMessage.visibility = View.GONE
                }
            }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocationGranted && coarseLocationGranted
    }

    private fun requestLocationPermissions() {
        if (!checkLocationPermissions()) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permissions are already granted, update DataStore
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.LOCATION_ACCESS, true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}