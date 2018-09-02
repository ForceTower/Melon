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

package com.forcetower.unes.feature.login

import `in`.uncod.android.bypass.Bypass
import android.content.Intent
import android.os.Bundle
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AlignmentSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.model.unes.Access
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
        val sequence1 = SpannableString(resources.getString(R.string.label_terms_and_conditions_p1))
        sequence1.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, sequence1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val sequence2 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.label_terms_and_conditions_p2), binding.textTermsAndPrivacy, null))
        sequence2.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, sequence2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val sequence = TextUtils.concat(sequence1, "\n", sequence2)
        HtmlUtils.setTextWithNiceLinks(binding.textTermsAndPrivacy, sequence)
    }
}