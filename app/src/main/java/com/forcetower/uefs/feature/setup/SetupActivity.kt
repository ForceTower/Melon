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

package com.forcetower.uefs.feature.setup

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.vm.CourseViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivitySetupBinding
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.config
import com.forcetower.uefs.feature.shared.provideViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

class SetupActivity : UActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: ActivitySetupBinding
    private lateinit var coursesViewModel: CourseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setup)
        coursesViewModel = provideViewModel(factory)
        coursesViewModel.courses.observe(this, Observer {
            Timber.d("Courses Status: ${it.status}")
        })
    }

    override fun navigateUpTo(upIntent: Intent?): Boolean = findNavController(R.id.setup_nav_host).navigateUp()

    override fun showSnack(string: String) {
        val snack = Snackbar.make(binding.root, string, Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
    }

    override fun supportFragmentInjector() = fragmentInjector
}
