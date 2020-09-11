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

package com.forcetower.uefs.feature.about

import `in`.uncod.android.bypass.Bypass
import android.os.Bundle
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AlignmentSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.HtmlUtils
import com.forcetower.uefs.databinding.FragmentAboutMeBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutMeFragment : UFragment() {
    private lateinit var binding: FragmentAboutMeBinding
    private val markdown: Bypass by lazy { Bypass(requireContext(), Bypass.Options()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentAboutMeBinding.inflate(inflater, container, false).also {
            binding = it
        }

        setupInterface()
        return binding.root
    }

    private fun setupInterface() {
        val about0 = SpannableString(markdown.markdownToSpannable(getString(R.string.about_unes_0), binding.textAboutDescription, null))
        about0.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about0.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val about1 = SpannableString(getString(R.string.about_unes_1))
        about1.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val about2 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.about_unes_2), binding.textAboutContinuation, null))
        about2.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val about3 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.about_unes_3, BuildConfig.VERSION_NAME), binding.textAboutContinuation, null))
        about3.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about3.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val about4 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.about_unes_4, BuildConfig.VERSION_CODE), binding.textAboutContinuation, null))
        about4.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about4.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        HtmlUtils.setTextWithNiceLinks(binding.textAboutDescription, about0)
        val sequence = TextUtils.concat(about1, "\n", about2, "\n\n", about3, "\n", about4)
        HtmlUtils.setTextWithNiceLinks(binding.textAboutContinuation, sequence)

        GlideApp.with(this)
            .load("https://avatars.githubusercontent.com/ForceTower")
            .fallback(R.mipmap.ic_unes_large_image_512)
            .placeholder(R.mipmap.ic_unes_large_image_512)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .into(binding.imageCreatorPicture)
    }
}
