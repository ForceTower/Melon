/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.adventure

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.GameConnectionStatus
import com.forcetower.uefs.R
import com.forcetower.uefs.REQUEST_CHECK_SETTINGS
import com.forcetower.uefs.core.model.service.AchDistance
import com.forcetower.uefs.databinding.FragmentAdventureBeginsBinding
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AdventureFragment : UFragment() {
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var firebaseStorage: FirebaseStorage
    @Inject lateinit var preferences: SharedPreferences

    private val viewModel: AdventureViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private var activity: UGameActivity? = null
    private lateinit var binding: FragmentAdventureBeginsBinding

    private val distanceAdapter by lazy { DistanceAdapter() }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var mLocationRequest: LocationRequest
    private var showedLocationMessage: Boolean = false
    private var requestingLocationUpdates = false

    private var currentList: List<AchDistance>? = null

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val allGranted = result.entries.all { it.value }
        if (allGranted) startRequesting()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        locationSettings()
        return FragmentAdventureBeginsBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            interactor = viewModel
            profile = profileViewModel
            lifecycleOwner = this@AdventureFragment
            executePendingBindings()
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.adventureAchievements.distanceRecycler.run {
            adapter = distanceAdapter
        }

        profileViewModel.getMeProfile().observe(
            viewLifecycleOwner,
            {
                if (it != null) {
                    profileViewModel.setProfileId(it.data?.id)
                }
            }
        )

        viewModel.run {
            achievements.observe(viewLifecycleOwner, EventObserver { activity?.openAchievements() })
            start.observe(viewLifecycleOwner, EventObserver { activity?.signIn() })
            locations.observe(viewLifecycleOwner, { requestLocations(it) })
            leave.observe(viewLifecycleOwner, EventObserver { activity?.signOut() })
        }

        if (activity?.isConnectedToPlayGames() == false && savedInstanceState == null) {
            openStartupDialog()
        }

        activity?.mGamesInstance?.connectionStatus?.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    GameConnectionStatus.DISCONNECTED -> openStartupDialog()
                    GameConnectionStatus.CONNECTED -> {
                        val fragment = childFragmentManager.findFragmentByTag("adventure_sign_in")
                        (fragment as? DialogFragment)?.dismiss()
                        showSnack(getString(R.string.connected_to_play_games))
                    }
                    GameConnectionStatus.LOADING -> Unit
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        stopUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startUpdates()
    }

    private fun openStartupDialog() {
        val dialog = AdventureSignInDialog()
        dialog.show(childFragmentManager, "adventure_sign_in")
    }

    private fun requestLocations(request: Boolean) {
        if (request) {
            startRequesting()
        } else {
            stopUpdates()
        }
    }

    private fun startRequesting() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
            startLocationsUpdate()
        } else {
            requestPermissions.launch(perms)
        }
    }

    private fun startLocationsUpdate() {
        mLocationRequest = LocationRequest.create()
        mLocationRequest.run {
            interval = 7000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (currentList == null) {
            onReceiveLocation(null)
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(requireContext())
        val task = client.checkLocationSettings(builder.build())

        task.addOnCompleteListener(requireActivity()) {
            Timber.d("Can make location request")

            try {
                startUpdates()
            } catch (e: SecurityException) {
                Timber.e("What??? How did this happen?")
            }
        }.addOnFailureListener(requireActivity()) { fail ->
            if (fail is ResolvableApiException) {
                try {
                    fail.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                } catch (e: Exception) {
                    Timber.d("Ignored exception")
                    e.printStackTrace()
                    showSnack(getString(R.string.cant_receive_location))
                }
            } else {
                Timber.d("Unresolvable Exception")
                fail.printStackTrace()
                showSnack(getString(R.string.cant_receive_location))
            }
        }
    }

    private fun startUpdates() {
        try {
            if (!::mLocationRequest.isInitialized) startLocationsUpdate()
            fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper())
            binding.adventureAchievements.distanceRecycler.visibility = VISIBLE
        } catch (e: SecurityException) {
            Timber.d("Method could not be called")
        }
    }

    private fun stopUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        binding.adventureAchievements.distanceRecycler.visibility = GONE
    }

    private fun locationSettings() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result ?: return
                result.locations.forEach { location ->
                    if (location.accuracy >= 100) {
                        if (!showedLocationMessage) {
                            if (context != null) {
                                showSnack(getString(R.string.adventure_not_accurate))
                                showedLocationMessage = true
                            }
                        }
                    } else {
                        onReceiveLocation(location)
                    }
                }
            }
        }
    }

    private fun onReceiveLocation(location: Location?) {
        val value = viewModel.onReceiveLocation(location)
        currentList = value
        distanceAdapter.submitList(value)
        value.mapNotNull { it.id }.forEach {
            activity?.unlockAchievement(it)
        }
    }
}
