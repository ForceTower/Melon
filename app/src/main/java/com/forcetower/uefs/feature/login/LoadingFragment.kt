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

package com.forcetower.uefs.feature.login

import `in`.uncod.android.bypass.Bypass
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AlignmentSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.util.HtmlUtils
import com.forcetower.uefs.databinding.FragmentLoadingBinding
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.fadeIn
import com.forcetower.uefs.feature.shared.fadeOut
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoadingFragment : UFragment() {
    @Inject
    lateinit var analytics: FirebaseAnalytics
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: FragmentLoadingBinding
    private lateinit var markdown: Bypass

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            FragmentLoadingBinding.inflate(inflater, container, false).also {
                binding = it
                binding.btnFirstSteps.setOnClickListener {
                    onMoveToNextScreen()
                }
                markdown = Bypass(requireContext(), Bypass.Options())
                setupTermsText()
            }.root
        } catch (error: Exception) {
            Timber.e(error, "Failed inflating initial layout")
            showInitializationError()
            null
        }
    }

    private fun onMoveToNextScreen() {
        try {
            findNavController().navigate(R.id.action_login_loading_to_login_form)
        } catch (error: IllegalArgumentException) {
            showSnack(getString(R.string.why_are_you_so_clicky))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getAccess().observe(viewLifecycleOwner, { onReceiveToken(it) })
    }

    private fun showInitializationError() {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.start_up_failed)
                .setMessage(R.string.start_up_failed_description)
                .setPositiveButton(R.string.start_up_failed_positive_btn) { dialog, _ ->
                    ContextCompat.getSystemService(requireContext(), ActivityManager::class.java)?.clearApplicationUserData()
                    dialog.dismiss()
                }
                .setCancelable(false)
                .create()
                .show()
        } catch (error: Throwable) {
            try {
                NotificationCreator.showSimpleNotification(requireContext(), getString(R.string.start_up_failed_resumed), getString(R.string.start_up_failed_resumed_desc))
                analytics.logEvent("start_up_failed", bundleOf("error_type" to "text_init_error"))
            } catch (_: Throwable) {
                analytics.logEvent("start_up_ntf_failed", bundleOf("error_type" to "ntf_show_error"))
            }

            Handler(Looper.getMainLooper()).postDelayed(3000) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    ContextCompat.getSystemService(requireContext(), ActivityManager::class.java)?.clearApplicationUserData()
                    // Actually, you may crash :D
                    throw error
                }
            }
        }
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
