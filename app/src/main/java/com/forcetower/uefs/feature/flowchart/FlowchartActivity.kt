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

package com.forcetower.uefs.feature.flowchart

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityFlowchartBinding
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowchartActivity : UActivity() {
    private lateinit var binding: ActivityFlowchartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_flowchart)
    }

    override fun showSnack(string: String, duration: Int) {
        getSnackInstance(string, duration).show()
    }

    override fun getSnackInstance(string: String, duration: Int): Snackbar {
        val snack = Snackbar.make(binding.root, string, duration)
        snack.config()
        return snack
    }

    override fun navigateUpTo(upIntent: Intent?): Boolean = findNavController(R.id.flowchart_nav_host).navigateUp()
}
