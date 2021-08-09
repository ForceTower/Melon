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

package com.forcetower.uefs.feature.home

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SagresCredential
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.REQUEST_IN_APP_UPDATE
import com.forcetower.uefs.architecture.service.bigtray.BigTrayService
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.databinding.ActivityHomeBinding
import com.forcetower.uefs.feature.adventure.AdventureViewModel
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.login.LoginActivity
import com.forcetower.uefs.feature.messages.MessagesDFMViewModel
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.isNougatMR1
import com.forcetower.uefs.feature.shared.extensions.toShortcut
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
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : UGameActivity() {
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var analytics: FirebaseAnalytics
    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject lateinit var executors: AppExecutors
    private lateinit var reviewManager: ReviewManager

    private val updateListener = InstallStateUpdatedListener { state -> onStateUpdateChanged(state) }
    private val viewModel: HomeViewModel by viewModels()
    private val adventureViewModel: AdventureViewModel by viewModels()
    private val dynamicDFMViewModel: MessagesDFMViewModel by viewModels()
    private val disciplineViewModel: DisciplineViewModel by viewModels()

    private lateinit var binding: ActivityHomeBinding
    private lateinit var updateManager: AppUpdateManager
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        setupBottomNav()
        setupUserData()

        updateManager = AppUpdateManagerFactory.create(this)
        reviewManager = ReviewManagerFactory.create(this)

        if (savedInstanceState == null) {
            onActivityStart()
            subscribeToTopics()
        }
    }

    private fun subscribeToTopics() {
        viewModel.subscribeToTopics("events", "messages", "general")
    }

    private fun onActivityStart() {
        try {
            initShortcuts()
            verifyUpdates()
            getReviews()
            viewModel.onSessionStarted()
            viewModel.account.observe(this, { })
            checkServerAchievements()
            viewModel.getAffinityQuestions()
//            if (preferences.isStudentFromUEFS()) {
//                val intent = Intent(this, SyncService::class.java)
//                startService(intent)
//            }
        } catch (t: Throwable) {}
        moveToTask()
    }

    private fun getReviews() {
        if (
            remoteConfig.getBoolean("feature_flag_in_app_review") &&
            preferences.isStudentFromUEFS() &&
            !preferences.getBoolean("__user_in_app_review_once__", false)
        ) {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    lifecycleScope.launchWhenCreated {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            try {
                                val request = reviewManager.requestReview()
                                reviewManager.launchReview(this@HomeActivity, request)
                            } catch (error: Throwable) {
                                Timber.e(error, "on request review")
                            }
                            preferences.edit().putBoolean("__user_in_app_review_once__", true).apply()
                        }
                    }
                },
                2000
            )
        }
    }

    private fun verifyUpdates() {
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
        viewModel.updateType = type
        updateManager.registerListener(updateListener)
        updateManager.startUpdateFlowForResult(info, type, this, REQUEST_IN_APP_UPDATE)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.onUserInteraction()
    }

    override fun onStart() {
        super.onStart()
        lightWeightCalcScore()
    }

    private fun lightWeightCalcScore() {
        viewModel.lightWeightCalcScore()
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
        binding.root.doOnLayout {
            findNavController(R.id.home_nav_host).navigate(direction, intent.extras)
        }
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
        binding.root.doOnLayout {
            NavigationUI.setupWithNavController(binding.bottomNavigation, findNavController(R.id.home_nav_host))
        }
    }

    private fun setupUserData() {
        viewModel.access.observe(this, { onAccessUpdate(it) })
        viewModel.snackbarMessage.observe(this, EventObserver { showSnack(it) })
        dynamicDFMViewModel.snackbarMessage.observe(this, EventObserver { showSnack(it) })
        viewModel.sendToken().observe(this, {})
        if (preferences.isStudentFromUEFS()) {
            // Update and unlock achievements for participating in a class with the creator
            viewModel.connectToServiceIfNeeded()
//            viewModel.goodCookies()
            disciplineViewModel.prepareAndSendStats()
            viewModel.getMeProfile()
        }
        viewModel.scheduleHideCount.observe(
            this,
            {
                Timber.d("Schedule hidden stuff: $it")
                analytics.setUserProperty("using_schedule_hide", "${it > 0}")
                analytics.setUserProperty("using_schedule_hide_cnt", "$it")
            }
        )
        viewModel.onMoveToSchedule.observe(
            this,
            EventObserver {
                binding.bottomNavigation.selectedItemId = R.id.schedule
            }
        )
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

            analytics.setUserId(access.username)
            analytics.setUserProperty("institution", SagresNavigator.instance.getSelectedInstitution())
            analytics.setUserProperty("access_valid", "${access.valid}")
            SagresNavigator.instance.putCredentials(SagresCredential(access.username, access.password, SagresNavigator.instance.getSelectedInstitution()))

            if (!access.valid) {
                val snack = Snackbar.make(binding.root, R.string.invalid_access_snack, Snackbar.LENGTH_INDEFINITE)
                snack.setAction(R.string.invalid_access_snack_solve) {
                    showInvalidAccessDialog()
                    snack.dismiss()
                }
                snack.anchorView = binding.bottomNavigation
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

    override fun showSnack(string: String, duration: Int) {
        val snack = getSnackInstance(string, duration)
        snack.show()
    }

    override fun getSnackInstance(string: String, duration: Int): Snackbar {
        val snack = Snackbar.make(binding.root, string, duration)
        snack.config()
        snack.anchorView = binding.bottomNavigation
        return snack
    }

    override fun checkAchievements(email: String?) {
        adventureViewModel.checkAchievements().observe(this) {
            it.entries.forEach { achievement ->
                if (achievement.value == -1)
                    unlockAchievement(achievement.key)
                else
                    updateAchievementProgress(achievement.key, achievement.value)
            }
        }
        checkServerAchievements()
    }

    override fun checkNotConnectedAchievements() {
        adventureViewModel.checkNotConnectedAchievements().observe(this, {})
    }

    private fun checkServerAchievements() {
        adventureViewModel.checkServerAchievements().observe(this) { achievements ->
            achievements.forEach { achievement ->
                try {
                    if (achievement.progress != null) {
                        updateAchievementProgress(achievement.identifier, achievement.progress)
                    } else {
                        unlockAchievement(achievement.identifier)
                    }
                } catch (error: Throwable) { Timber.e(error, "Failed to unlock achievement ${achievement.identifier}") }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onUserInteraction()
        updateManager.appUpdateInfo.addOnSuccessListener {
            if (viewModel.updateType == AppUpdateType.IMMEDIATE && it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                updateManager.startUpdateFlowForResult(it, AppUpdateType.IMMEDIATE, this, REQUEST_IN_APP_UPDATE)
            } else if (viewModel.updateType != AppUpdateType.IMMEDIATE && it.installStatus() == InstallStatus.DOWNLOADED) {
                showSnackbarForRestartRequired()
            }
        }
    }

    private fun onStateUpdateChanged(state: InstallState) {
        viewModel.setCurrentUpdateState(state.installStatus())
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                updateManager.unregisterListener(updateListener)
                showSnackbarForRestartRequired()
            }
            InstallStatus.FAILED -> showSnack(getString(R.string.in_app_update_request_failed_or_canceled))
            InstallStatus.CANCELED -> showSnack(getString(R.string.in_app_update_request_failed_or_canceled))
            else -> Unit
        }
    }

    private fun showSnackbarForRestartRequired() {
        val message = getString(R.string.in_app_updates_update_ready)
        val restart = getString(R.string.in_app_updates_restart_app)
        val snack = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(restart) { updateManager.completeUpdate() }
        }
        snack.config()
        snack.anchorView = binding.bottomNavigation
        snack.show()
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
                    showSnack(message, Snackbar.LENGTH_LONG)
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

    companion object {
        const val EXTRA_FRAGMENT_DIRECTIONS = "extra_directions"
        const val EXTRA_MESSAGES_SAGRES_DIRECTION = "messages.sagres"
        const val EXTRA_BIGTRAY_DIRECTION = "home.bigtray"
        const val EXTRA_GRADES_DIRECTION = "grades"
        const val EXTRA_DEMAND_DIRECTION = "demand"
        const val EXTRA_REQUEST_SERVICE_DIRECTION = "request_service"
    }
}
