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

package com.forcetower.uefs.easter.darktheme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.databinding.FragmentInviteDarkThemeBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InviteDarkThemeFragment : UFragment() {
    private val viewModel: DarkThemeViewModel by activityViewModels()
    private lateinit var binding: FragmentInviteDarkThemeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentInviteDarkThemeBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            btnSend.setOnClickListener { onSend() }
            btnSendRandom.setOnClickListener { onSendRandom() }
        }

        viewModel.currentCall.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it.status) {
                    Status.SUCCESS -> {
                        binding.loadingPb.visibility = GONE
                        showSnack(getString(R.string.dark_theme_invite_sent))
                    }
                    Status.ERROR -> {
                        binding.loadingPb.visibility = GONE
                        val messageRes = when (it.code) {
                            404 -> R.string.dark_theme_user_not_found
                            403 -> R.string.dark_theme_no_more_invites
                            402 -> R.string.user_already_unlocked_dark_theme
                            401 -> R.string.you_are_not_connected_to_the_unesverso
                            else -> R.string.what_firebase_says_the_app_commits
                        }
                        showSnack(getString(messageRes), Snackbar.LENGTH_LONG)
                    }
                    Status.LOADING -> Unit
                }
            }
        )

        viewModel.profile.observe(
            viewLifecycleOwner,
            {
                if (it == null) {
                    binding.textInvitesLeft.text = "0"
                } else {
                    val invites = it.darkThemeInvites
                    binding.textInvitesLeft.text = "$invites"
                }
            }
        )
    }

    private fun onSendRandom() {
        val text = binding.textInvitesLeft.text.toString().toIntOrNull() ?: 0
        binding.textInvitesLeft.text = "${if (text == 0) 0 else text - 1}"
        binding.loadingPb.visibility = VISIBLE
        viewModel.sendDarkThemeTo(null)
    }

    private fun onSend() {
        val username = binding.textSendTo.text?.toString()
        if (username.isNullOrBlank()) {
            binding.textSendTo.error = getString(R.string.dark_theme_username_is_empty)
            return
        }
        binding.loadingPb.visibility = VISIBLE
        viewModel.sendDarkThemeTo(username)
    }
}
