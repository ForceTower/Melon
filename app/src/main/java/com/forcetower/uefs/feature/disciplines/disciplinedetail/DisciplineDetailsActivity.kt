/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.disciplines.disciplinedetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityDisciplineDetailsBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class DisciplineDetailsActivity : UGameActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: ActivityDisciplineDetailsBinding
    private lateinit var viewModel: DisciplineViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_discipline_details)
        viewModel = provideViewModel(factory)

        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val classGroupId = intent.getLongExtra(CLASS_GROUP_ID, 1)
                val classId = intent.getLongExtra(CLASS_ID, 1)
                add(R.id.fragment_container, DisciplineDetailsFragment.newInstance(classId, classGroupId))
            }
        }

        viewModel.clazz.observe(this, Observer {
            if (it != null) {
                val teacher = Constants.HARD_DISCIPLINES[it.clazz.discipline().code]
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
        })
    }

    override fun showSnack(string: String, long: Boolean) {
        val snack = Snackbar.make(binding.root, string, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
    }

    override fun supportFragmentInjector() = fragmentInjector

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
