/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.home

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.REQUEST_IN_APP_UPDATE
import com.forcetower.uefs.architecture.service.bigtray.BigTrayService
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityHomeBinding
import com.forcetower.uefs.feature.adventure.AdventureViewModel
import com.forcetower.uefs.feature.forms.FormActivity
import com.forcetower.uefs.feature.login.LoginActivity
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.isNougatMR1
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.shared.extensions.toShortcut
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class HomeActivity : UGameActivity(), HasSupportFragmentInjector {
    companion object {
        const val EXTRA_FRAGMENT_DIRECTIONS = "extra_directions"
        const val EXTRA_MESSAGES_SAGRES_DIRECTION = "messages.sagres"
        const val EXTRA_BIGTRAY_DIRECTION = "home.bigtray"
        const val EXTRA_GRADES_DIRECTION = "grades"
        const val EXTRA_DEMAND_DIRECTION = "demand"
        const val EXTRA_REQUEST_SERVICE_DIRECTION = "request_service"
    }

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var vmFactory: UViewModelFactory
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var analytics: FirebaseAnalytics
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject
    lateinit var executors: AppExecutors

    private val updateListener = InstallStateUpdatedListener { state -> onStateUpdateChanged(state) }
    private lateinit var viewModel: HomeViewModel
    private lateinit var adventureViewModel: AdventureViewModel
    private lateinit var binding: ActivityHomeBinding
    private lateinit var updateManager: AppUpdateManager
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewModel()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        setupBottomNav()
        setupUserData()

        updateManager = AppUpdateManagerFactory.create(this)

        val admobEnabled = remoteConfig.getBoolean("admob_enabled")
        setupAds(admobEnabled)

        if (savedInstanceState == null) {
            onActivityStart()
            subscribeToTopics()
        }
    }

    private fun setupAds(willShowAds: Boolean = true) {
        MobileAds.initialize(this)
        prepareAdsForPublic(willShowAds)
    }

    private fun subscribeToTopics() {
        viewModel.subscribeToTopics("events", "messages", "general")
    }

    private fun onActivityStart() {
        try {
            initShortcuts()
            verifyUpdates()
            viewModel.onSessionStarted()
            viewModel.account.observe(this, Observer { Unit })
        } catch (t: Throwable) {}
        moveToTask()
        satisfactionSurvey()
    }

    private fun satisfactionSurvey() {
        val answered = preferences.getBoolean("answered_forms_satisfaction_pre", false)
        if (answered || !preferences.isStudentFromUEFS()) return

        val installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
        val checkDate = Calendar.getInstance().apply {
            set(2019, 9, 21, 0, 0, 0)
        }

        val checkTime = checkDate.timeInMillis

        if (installTime <= checkTime) {
            startActivity(Intent(this, FormActivity::class.java))
        } else {
            Timber.d("Verification of date failed... User will not share opinion")
            Timber.d("Current check date: ${checkDate.time}")
            Timber.d("Install Time $installTime")
        }
    }

    private fun verifyUpdates() {
        val check = preferences.getInt("daily_check_updates", -1)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= check + 6) {
            Timber.d("Update check postponed")
            return
        }

        preferences.edit().putInt("daily_check_updates", hour).apply()

        val updateTask = updateManager.appUpdateInfo
        val required = remoteConfig.getLong("version_disable")
        updateTask.addOnSuccessListener {
            if (it.installStatus() == InstallStatus.DOWNLOADED) {
                showSnackbarForRestartRequired()
            } else if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (BuildConfig.VERSION_CODE < required && it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    requestUpdate(AppUpdateType.IMMEDIATE, it)
                } else if (it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    requestUpdate(AppUpdateType.FLEXIBLE, it)
                } else {
                    val message = getString(R.string.in_app_update_no_update_type)
                    showSnack(message)
                }
            }
        }
    }

    private fun requestUpdate(@AppUpdateType type: Int, info: AppUpdateInfo) {
        updateManager.startUpdateFlowForResult(info, type, this, REQUEST_IN_APP_UPDATE)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.onUserInteraction()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onUserInteraction()
        updateManager.appUpdateInfo.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                updateManager.startUpdateFlowForResult(it, AppUpdateType.IMMEDIATE, this, REQUEST_IN_APP_UPDATE)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        verifyDarkTheme()
        lightWeightCalcScore()
    }

    private fun lightWeightCalcScore() {
        viewModel.lightWeightCalcScore()
    }

    private fun verifyDarkTheme() {
        viewModel.verifyDarkTheme().observe(this, Observer { Unit })
    }

    private fun moveToTask() {
        val direction = when (intent.getStringExtra(EXTRA_FRAGMENT_DIRECTIONS)) {
            EXTRA_MESSAGES_SAGRES_DIRECTION -> R.id.messages
            EXTRA_GRADES_DIRECTION -> R.id.grades_disciplines
            EXTRA_BIGTRAY_DIRECTION -> {
                val intent = Intent(this, BigTrayService::class.java).apply {
                    action = BigTrayService.STOP_SERVICE_ACTION
                }
                startService(intent)
                R.id.big_tray
            }
            EXTRA_DEMAND_DIRECTION -> R.id.demand
            EXTRA_REQUEST_SERVICE_DIRECTION -> R.id.request_services
            else -> null
        }

        direction ?: return
        findNavController(R.id.home_nav_host).navigate(direction, intent.extras)
    }

    private fun initShortcuts() {
        if (!isNougatMR1()) return

        val manager = getSystemService(ShortcutManager::class.java)

        val messages = Intent(this, HomeActivity::class.java).apply {
            putExtra(EXTRA_FRAGMENT_DIRECTIONS, EXTRA_MESSAGES_SAGRES_DIRECTION)
            action = "android.intent.action.VIEW"
            addFlags(FLAG_ACTIVITY_CLEAR_TASK)
        }

        val grades = Intent(this, HomeActivity::class.java).apply {
            putExtra(EXTRA_FRAGMENT_DIRECTIONS, EXTRA_GRADES_DIRECTION)
            action = "android.intent.action.VIEW"
            addFlags(FLAG_ACTIVITY_CLEAR_TASK)
        }

        val messagesShort = messages.toShortcut(this, "messages_sagres", R.drawable.ic_shortcut_message, getString(R.string.label_messages))
        val gradesShort = grades.toShortcut(this, "grades", R.drawable.ic_shortcut_school, getString(R.string.label_grades_disciplines))

        manager?.addDynamicShortcuts(listOf(gradesShort, messagesShort))
    }

    private fun setupBottomNav() {
        NavigationUI.setupWithNavController(binding.bottomNavigation, findNavController(R.id.home_nav_host))
    }

    private fun setupViewModel() {
        viewModel = provideViewModel(vmFactory)
        adventureViewModel = provideViewModel(vmFactory)
    }

    private fun setupUserData() {
        viewModel.access.observe(this, Observer { onAccessUpdate(it) })
        viewModel.snackbarMessage.observe(this, EventObserver { showSnack(it) })
        viewModel.sendToken().observe(this, Observer { Unit })
        if (preferences.isStudentFromUEFS()) {
            viewModel.connectToServiceIfNeeded()
            viewModel.onSyncSessions()
        }
    }

    private fun onAccessUpdate(access: Access?) {
        if (access == null) {
            Timber.d("Access Invalidated")
            firebaseAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            username = access.username
            mGamesInstance.changePlayerName(access.username)
            Crashlytics.setUserIdentifier(access.username)
            Crashlytics.setUserName(firebaseAuth.currentUser?.email)

            if (!access.valid) {
                val snack = Snackbar.make(binding.snack, R.string.invalid_access_snack, Snackbar.LENGTH_INDEFINITE)
                snack.setAction(R.string.invalid_access_snack_solve) {
                    showInvalidAccessDialog()
                    snack.dismiss()
                }
                snack.config()
                snack.show()
            }
        }
    }

    private fun showInvalidAccessDialog() {
        val dialog = InvalidAccessDialog()
        dialog.show(supportFragmentManager, "invalid_access")
    }

    override fun onSupportNavigateUp(): Boolean = findNavController(R.id.home_nav_host).navigateUp()

    override fun showSnack(string: String, long: Boolean) {
        val snack = getSnackInstance(string, long)
        snack.show()
    }

    override fun getSnackInstance(string: String, long: Boolean): Snackbar {
        val snack = Snackbar.make(binding.snack, string, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
        snack.config()
        return snack
    }

    override fun checkAchievements(email: String?) {
        adventureViewModel.checkAchievements().observe(this, Observer {
            it.entries.forEach { achievement ->
                if (achievement.value == -1)
                    unlockAchievement(achievement.key)
                else
                    updateAchievementProgress(achievement.key, achievement.value)
            }
        })
    }

    override fun checkNotConnectedAchievements() {
        adventureViewModel.checkNotConnectedAchievements().observe(this, Observer { Unit })
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    private fun onStateUpdateChanged(state: InstallState) {
        when {
            state.installStatus() == InstallStatus.DOWNLOADED -> {
                updateManager.unregisterListener(updateListener)
                showSnackbarForRestartRequired()
            }
            state.installStatus() == InstallStatus.FAILED -> showSnack(getString(R.string.in_app_update_request_failed_or_canceled))
            state.installStatus() == InstallStatus.CANCELED -> showSnack(getString(R.string.in_app_update_request_failed_or_canceled))
        }
    }

    private fun showSnackbarForRestartRequired() {
        val message = getString(R.string.in_app_updates_update_ready)
        val restart = getString(R.string.in_app_updates_restart_app)
        val snack = Snackbar.make(binding.snack, message, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(restart) { updateManager.completeUpdate() }
        }
        snack.config()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IN_APP_UPDATE) {
            when (resultCode) {
                RESULT_CANCELED -> {
                    val message = getString(R.string.in_app_update_request_canceled)
                    showSnack(message)
                }
                RESULT_IN_APP_UPDATE_FAILED -> {
                    val message = getString(R.string.in_app_update_request_failed)
                    showSnack(message, true)
                }
                else -> {
                    updateManager.registerListener(updateListener)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onUserInteraction()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onUserInteraction()
    }

    private fun prepareAdsForPublic(willShowAd: Boolean = true) {
        if (!willShowAd) return

        executors.others().execute {
            val account = viewModel.getAccountSync()
            onSelectAdType(account, willShowAd)
        }
    }

    private fun onSelectAdType(account: Account?, willShowAd: Boolean) {
        if (!willShowAd || account == null) return

        val code = when (account.grouping) {
            null, 0 -> R.string.admob_common

            1 -> R.string.admob_frequency_low
            2 -> R.string.admob_frequency_medium
            3 -> R.string.admob_frequency_high

            4 -> R.string.admob_size_small
            5 -> R.string.admob_size_medium
            6 -> R.string.admob_size_big

            7 -> R.string.admob_content_rich_text
            8 -> R.string.admob_content_everything

            else -> null
        }

        code ?: return
        executors.mainThread().execute { onAdTypeSelected(code, account.grouping ?: 0) }
    }

    private fun onAdTypeSelected(@StringRes code: Int, @IntRange(from = 0, to = 8) grouping: Int) {
        val unitId = getString(code)
        when (grouping) {
            4 -> setupSmallAd(unitId)
            5 -> setupMediumAd(unitId)
            else -> setupInterstitial(unitId)
        }
    }

    private fun setupInterstitial(unitId: String) {
        val interstitial = InterstitialAd(this)
        val request = AdRequest.Builder().build()
        interstitial.adUnitId = unitId
        interstitial.loadAd(request)
        interstitial.adListener = object : AdListener() {
            override fun onAdLoaded() {
                interstitial.show()
            }

            override fun onAdClicked() {
                viewModel.onUserClickedAd()
            }

            override fun onAdImpression() {
                viewModel.onUserAdImpression()
            }
        }
    }

    private fun setupMediumAd(unitId: String) {
        val adView = AdView(this)
        adView.id = R.id.adViewConnect
        adView.adSize = AdSize.LARGE_BANNER
        adView.adUnitId = unitId
        placeAdViewOnLayout(adView)
    }

    private fun setupSmallAd(unitId: String) {
        val adView = AdView(this)
        adView.id = R.id.adViewConnect
        adView.adSize = AdSize.SMART_BANNER
        adView.adUnitId = unitId
        placeAdViewOnLayout(adView)
    }

    private fun placeAdViewOnLayout(adView: AdView) {
        adView.adListener = object : AdListener() {
            override fun onAdClicked() { viewModel.onUserClickedAd() }
            override fun onAdImpression() { viewModel.onUserAdImpression() }
        }

        val set = ConstraintSet()
        val constraintLayout = binding.internalContent

        constraintLayout.addView(
            adView, ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        )
        set.clone(constraintLayout)
        set.connect(
            R.id.adViewConnect,
            ConstraintSet.BOTTOM,
            constraintLayout.id,
            ConstraintSet.BOTTOM,
            0
        )
        set.connect(
            R.id.adViewConnect,
            ConstraintSet.START,
            constraintLayout.id,
            ConstraintSet.START,
            0
        )
        set.connect(
            R.id.adViewConnect,
            ConstraintSet.END,
            constraintLayout.id,
            ConstraintSet.END,
            0
        )
        set.applyTo(constraintLayout)

        val contentSet = ConstraintSet()
        contentSet.clone(constraintLayout)
        contentSet.connect(
            R.id.home_nav_host,
            ConstraintSet.BOTTOM,
            R.id.adViewConnect,
            ConstraintSet.TOP,
            0
        )
        contentSet.applyTo(constraintLayout)

        val request = AdRequest.Builder().build()
        adView.loadAd(request)
    }
}
