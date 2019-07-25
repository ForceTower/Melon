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

package com.forcetower.uefs.feature.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityProfileBinding
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class ProfileActivity : UActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 0)
                add(R.id.fragment_container, ProfileFragment.newInstance(profileId))
            }
        }
    }

    override fun supportFragmentInjector() = fragmentInjector

    companion object {
        const val EXTRA_PROFILE_ID = "student_id"
        fun startIntent(context: Context, profileId: Long): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
        }
    }
}
