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

package com.forcetower.uefs.feature.home

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.R
import com.forcetower.uefs.architecture.service.bigtray.BigTrayService
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityHomeBinding
import com.forcetower.uefs.feature.adventure.AdventureViewModel
import com.forcetower.uefs.feature.login.LoginActivity
import com.forcetower.uefs.feature.profile.ProfileActivity
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.isNougatMR1
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.shared.extensions.toShortcut
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

class HomeActivity : UGameActivity(), HasSupportFragmentInjector {
    companion object {
        const val EXTRA_FRAGMENT_DIRECTIONS = "extra_directions"
        const val EXTRA_MESSAGES_SAGRES_DIRECTION = "messages.sagres"
        const val EXTRA_BIGTRAY_DIRECTION = "home.bigtray"
        const val EXTRA_GRADES_DIRECTION = "grades"
        const val EXTRA_DEMAND_DIRECTION = "demand"
    }

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var vmFactory: UViewModelFactory
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var viewModel: HomeViewModel
    private lateinit var adventureViewModel: AdventureViewModel
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        setupViewModel()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        setupBottomNav()
        setupUserData()

        if (savedInstanceState == null) {
            onActivityStart()
            subscribeToTopics()
        }
    }

    private fun subscribeToTopics() {
        viewModel.subscribeToTopics("events", "messages", "general")
    }

    private fun onActivityStart() {
        initShortcuts()
        moveToTask()
    }

    private fun moveToTask() {
        val directions = intent.getStringExtra(EXTRA_FRAGMENT_DIRECTIONS)
        val direction = when (directions) {
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

        manager.addDynamicShortcuts(listOf(gradesShort, messagesShort))
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
        viewModel.openProfileCase.observe(this, EventObserver { openProfile(it) })
    }

    private fun openProfile(profileId: String) {
        startActivity(ProfileActivity.startIntent(this, profileId))
    }

    private fun onAccessUpdate(access: Access?) {
        if (access == null) {
            Timber.d("Access Invalidated")
            firebaseAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            mGamesInstance.changePlayerName(access.username)
            Crashlytics.setUserIdentifier(access.username)
            Crashlytics.setUserName(firebaseAuth.currentUser?.email)
        }
    }

    override fun onSupportNavigateUp(): Boolean = findNavController(R.id.home_nav_host).navigateUp()

    override fun showSnack(string: String, long: Boolean) {
        val snack = Snackbar.make(binding.snack, string, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
    }

    override fun checkAchievements(email: String?) {
        adventureViewModel.checkAchievements(email).observe(this, Observer {
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
}