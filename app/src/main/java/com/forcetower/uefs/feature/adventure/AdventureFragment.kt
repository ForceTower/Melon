/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.feature.adventure

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.GameConnectionStatus
import com.forcetower.uefs.R
import com.forcetower.uefs.RC_LOCATION_PERMISSION
import com.forcetower.uefs.REQUEST_CHECK_SETTINGS
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentAdventureBeginsBinding
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject

class AdventureFragment : UFragment(), Injectable {
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var firebaseStorage: FirebaseStorage
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: AdventureViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private var activity: UGameActivity? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var mLocationRequest: LocationRequest? = null
    private var showedLocationMessage: Boolean = false
    private var requestingLocationUpdates = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        profileViewModel = provideViewModel(factory)
        locationSettings()
        return FragmentAdventureBeginsBinding.inflate(inflater, container, false).apply {
            interactor = viewModel
            profile = profileViewModel
            storage = firebaseStorage
            firebaseUser = this@AdventureFragment.firebaseAuth.currentUser
            setLifecycleOwner(this@AdventureFragment)
            executePendingBindings()
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        profileViewModel.getMeProfile().observe(this, Observer {
            profileViewModel.setProfileId(it.uuid)
        })

        viewModel.run {
            achievements.observe(this@AdventureFragment, EventObserver { activity?.openAchievements() })
            start.observe(this@AdventureFragment, EventObserver { activity?.signIn() })
            locations.observe(this@AdventureFragment, Observer { requestLocations(it) })
            leave.observe(this@AdventureFragment, EventObserver { activity?.signOut() })
        }

        if (activity?.isConnectedToPlayGames() == false && savedInstanceState == null) {
            openStartupDialog()
        }

        activity?.mGamesInstance?.connectionStatus?.observe(this, EventObserver {
            when (it) {
                GameConnectionStatus.DISCONNECTED -> openStartupDialog()
                GameConnectionStatus.CONNECTED -> showSnack(getString(R.string.connected_to_play_games))
                GameConnectionStatus.LOADING -> Unit
            }
        })
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

    @AfterPermissionGranted(RC_LOCATION_PERMISSION)
    private fun startRequesting() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
            startLocationsUpdate()
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.permission_location_adventure), RC_LOCATION_PERMISSION, *perms)
        }
    }

    private fun startLocationsUpdate() {
        mLocationRequest = LocationRequest.create()
        mLocationRequest?.run {
            interval = 7000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest!!)
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
            if (mLocationRequest == null) startLocationsUpdate()
            fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            Timber.d("Method could not be called")
        }
    }

    private fun stopUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun locationSettings() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result ?: return
                result.locations.forEach { location ->
                    if (location.accuracy >= 100) {
                        if (!showedLocationMessage) {
                            showSnack(getString(R.string.adventure_not_accurate))
                            showedLocationMessage = true
                        } else {
                            onReceiveLocation(location)
                        }
                    }
                }
            }
        }
    }

    private fun onReceiveLocation(location: Location) {
        val value = viewModel.onReceiveLocation(location)
        if (value != null) { activity?.unlockAchievement(value) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}