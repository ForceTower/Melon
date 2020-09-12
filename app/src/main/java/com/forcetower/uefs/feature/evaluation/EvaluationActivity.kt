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

package com.forcetower.uefs.feature.evaluation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.forcetower.uefs.EvalNavGraphDirections
import com.forcetower.uefs.R
import com.forcetower.uefs.core.vm.UserSessionViewModel
import com.forcetower.uefs.databinding.ActivityEvaluationBinding
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EvaluationActivity : UGameActivity() {
    private val sessionViewModel: UserSessionViewModel by viewModels()
    private lateinit var binding: ActivityEvaluationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_evaluation)
        if (savedInstanceState == null) {
            val teacherName = intent.getStringExtra("teacherName")
            if (teacherName != null) {
                val direction = EvalNavGraphDirections.actionGlobalEvalTeacher(0, teacherName)
                findNavController(R.id.eval_nav_host).navigate(direction)
            }
        }
    }

    override fun navigateUpTo(upIntent: Intent?): Boolean = findNavController(R.id.eval_nav_host).navigateUp()

    override fun showSnack(string: String, duration: Int) {
        getSnackInstance(string, duration).show()
    }

    override fun getSnackInstance(string: String, duration: Int): Snackbar {
        val snack = Snackbar.make(binding.root, string, duration)
        snack.config()
        return snack
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionViewModel.onUserInteraction()
    }

    override fun onPause() {
        super.onPause()
        sessionViewModel.onUserInteraction()
    }

    override fun onResume() {
        super.onResume()
        sessionViewModel.onUserInteraction()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionViewModel.onUserInteraction()
    }

    companion object {
        fun startIntentForTeacher(context: Context, teacherName: String): Intent {
            return Intent(context, EvaluationActivity::class.java).apply {
                putExtra("teacherName", teacherName)
            }
        }
    }
}
