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

package com.forcetower.uefs.feature.evaluation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.forcetower.uefs.EvalNavGraphDirections
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityEvaluationBinding
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.google.android.material.snackbar.Snackbar
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class EvaluationActivity : UGameActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

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

    override fun showSnack(string: String, long: Boolean) {
        val snack = Snackbar.make(binding.root, string, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
    }

    override fun supportFragmentInjector() = fragmentInjector

    companion object {
        fun startIntentForTeacher(context: Context, teacherName: String): Intent {
            return Intent(context, EvaluationActivity::class.java).apply {
                putExtra("teacherName", teacherName)
            }
        }
    }
}
