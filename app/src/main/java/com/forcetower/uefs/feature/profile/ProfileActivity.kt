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

package com.forcetower.uefs.feature.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityProfileBinding
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : UActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val profileId = intent.getLongExtra(EXTRA_STUDENT_ID, 0)
                val userId = intent.getLongExtra(EXTRA_USER_ID, 0)
                add(R.id.fragment_container, ProfileFragment.newInstance(profileId, userId))
            }
        }
    }

    override fun showSnack(string: String, duration: Int) {
        val snack = getSnackInstance(string, duration)
        snack.show()
    }

    override fun getSnackInstance(string: String, duration: Int): Snackbar {
        val snack = Snackbar.make(binding.rootContainer, string, duration)
        snack.config()
        return snack
    }

    companion object {
        const val EXTRA_STUDENT_ID = "student_id"
        const val EXTRA_USER_ID = "user_id"
        fun startIntent(context: Context, profileId: Long, userId: Long): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                putExtra(EXTRA_STUDENT_ID, profileId)
                putExtra(EXTRA_USER_ID, userId)
            }
        }
    }
}
