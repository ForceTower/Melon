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

package com.forcetower.uefs.feature.disciplines.disciplinedetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.vm.UserSessionViewModel
import com.forcetower.uefs.databinding.ActivityDisciplineDetailsBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisciplineDetailsActivity : UGameActivity() {
    private val sessionViewModel: UserSessionViewModel by viewModels()
    private val viewModel: DisciplineViewModel by viewModels()
    private lateinit var binding: ActivityDisciplineDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_discipline_details)

        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val classGroupId = intent.getLongExtra(CLASS_GROUP_ID, 1)
                val classId = intent.getLongExtra(CLASS_ID, 1)
                add(R.id.fragment_container, DisciplineDetailsFragment.newInstance(classId, classGroupId))
            }
        }

        viewModel.clazz.observe(
            this,
            Observer {
                if (it != null) {
                    val teacher = Constants.HARD_DISCIPLINES[it.discipline.code]
                    if (teacher != null) {
                        if (teacher == "__ANY__") {
                            unlockAchievement(R.string.achievement_vale_das_sombras)
                        } else {
                            it.groups.forEach { group ->
                                if (group.teacher != null && group.teacher == teacher) {
                                    unlockAchievement(R.string.achievement_vale_das_sombras)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

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
        const val CLASS_GROUP_ID = "class_group_id"
        const val CLASS_ID = "class_id"
        fun startIntent(context: Context, classId: Long, classGroupId: Long): Intent {
            return Intent(context, DisciplineDetailsActivity::class.java).apply {
                putExtra(CLASS_GROUP_ID, classGroupId)
                putExtra(CLASS_ID, classId)
            }
        }
    }
}
