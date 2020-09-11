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

package com.forcetower.uefs.feature.baddevice

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentBadDeviceBinding
import com.forcetower.uefs.feature.settings.SettingsActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BadDeviceFragment : BottomSheetDialogFragment() {
    @Inject lateinit var preferences: SharedPreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        try {
            sheetDialog.setOnShowListener {
                val bottomSheet = sheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } catch (t: Throwable) {
            Timber.d(t, "Hum...")
        }
        return sheetDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentBadDeviceBinding.inflate(inflater, container, false).also {
            it.btnOk.setOnClickListener {
                preferences.edit().putBoolean("saw_bad_device_information_key", true).apply()
                dismiss()
            }
            it.links1.apply {
                val content = getString(R.string.bad_device_p8)
                val spannable = SpannableString(content)
                Linkify.addLinks(spannable, Linkify.WEB_URLS)
                text = spannable
                movementMethod = LinkMovementMethod.getInstance()
                autoLinkMask = autoLinkMask or Linkify.WEB_URLS
            }
            it.links2.apply {
                val content = getString(R.string.bad_device_p9)
                val spannable = SpannableString(content)
                Linkify.addLinks(spannable, Linkify.WEB_URLS)
                text = spannable
                movementMethod = LinkMovementMethod.getInstance()
                autoLinkMask = autoLinkMask or Linkify.WEB_URLS
            }
            it.btnAdvSettings.setOnClickListener {
                preferences.edit().putBoolean("saw_bad_device_information_key", true).apply()
                val intent = SettingsActivity.startIntent(requireContext()).apply {
                    putExtra("move_to_screen", 3)
                }
                startActivity(intent)
                dismiss()
            }
        }.root
    }
}
