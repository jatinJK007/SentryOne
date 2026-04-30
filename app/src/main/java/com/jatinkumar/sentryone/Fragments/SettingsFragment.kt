package com.jatinkumar.sentryone.Fragments

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
import com.jatinkumar.sentryone.AppSettingsKeys
import com.jatinkumar.sentryone.AppSettingsManager
import com.jatinkumar.sentryone.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    private lateinit var appSettingsManager: AppSettingsManager
    private var _binding : FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            binding.tvErrorMessage.visibility = View.GONE
            binding.switchLocationAccess.isChecked = true
        } else {
            binding.tvErrorMessage.text = "Location permission denied. Features may be limited."
            binding.tvErrorMessage.visibility = View.VISIBLE
            binding.switchLocationAccess.isChecked = false
        }
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
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.DARK_MODE, isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        binding.switchLocationAccess.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestLocationPermissions()
            } else {
                lifecycleScope.launch {
                    appSettingsManager.updateSetting(AppSettingsKeys.LOCATION_ACCESS, false)
                    binding.tvErrorMessage.visibility = View.GONE // Hide error if user manually unchecks
                }
            }
        }
        binding.switchDialogue.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.SHOW_DIALOGUE, isChecked)
            }
        }

        binding.switchShakeDetction.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.SHAKE_DETECTION, isChecked)
            }
        }

        binding.switchFlashTrigger.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.FLASH_TRIGGER, isChecked)
            }
        }

        binding.switchHepticFeedback.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appSettingsManager.updateSetting(AppSettingsKeys.HEPTIC_FEEDBACK, isChecked)
            }
        }

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
                binding.switchDarkMode.isChecked = settings.darkMode
                binding.switchLocationAccess.isChecked = settings.locationAccess
                binding.emergencyMsg.setText(settings.emergencyMessage)
                binding.switchDialogue.isChecked = settings.showDialogue
                binding.switchShakeDetction.isChecked = settings.shakeDetection
                binding.switchFlashTrigger.isChecked = settings.flashTrigger
                binding.switchHepticFeedback.isChecked = settings.hepticFeedback


                if (settings.locationAccess && !checkLocationPermissions()) {
                    binding.tvErrorMessage.text = "Location permission needed. Please grant it."
                    binding.tvErrorMessage.visibility = View.VISIBLE
                    binding.switchLocationAccess.isChecked = false
                } else {
                    binding.tvErrorMessage.visibility = View.GONE
                }
            }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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