/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.feature.login

import `in`.uncod.android.bypass.Bypass
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.model.Access
import com.forcetower.unes.core.util.HtmlUtils
import com.forcetower.unes.core.vm.LoginViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentLoadingBinding
import com.forcetower.unes.feature.home.HomeActivity
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.fadeIn
import com.forcetower.unes.feature.shared.fadeOut
import com.forcetower.unes.feature.shared.provideViewModel
import timber.log.Timber
import javax.inject.Inject

class LoadingFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: FragmentLoadingBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var markdown: Bypass

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLoadingBinding.inflate(inflater, container, false).also {
            binding = it
            binding.btnFirstSteps.setOnClickListener { v ->
                v.findNavController().navigate(R.id.action_login_loading_to_login_form)
            }
            markdown = Bypass(requireContext(), Bypass.Options())
            setupTermsText()
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = provideViewModel(factory)
        viewModel.getAccess().observe(this, Observer { onReceiveToken(it) })
    }

    private fun onReceiveToken(access: Access?) {
        if (access == null) {
            binding.btnFirstSteps.fadeIn()
            binding.contentLoading.fadeOut()
        } else {
            Timber.d("Connected already")
            if (viewModel.isConnected()) return

            viewModel.setConnected()
            val intent = Intent(context, HomeActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun setupTermsText() {
        val sequence = markdown.markdownToSpannable(resources.getString(R.string.label_terms_and_conditions), binding.textTermsAndPrivacy, null)
        HtmlUtils.setTextWithNiceLinks(binding.textTermsAndPrivacy, sequence)
    }
}