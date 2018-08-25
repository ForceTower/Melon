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

package com.forcetower.unes.feature.about

import `in`.uncod.android.bypass.Bypass
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.databinding.FragmentAboutMeBinding
import com.forcetower.unes.feature.shared.UFragment
import android.text.SpannableString
import android.text.Spanned
import android.text.Layout
import android.text.style.AlignmentSpan
import android.text.TextUtils
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forcetower.unes.GlideApp
import com.forcetower.unes.core.util.HtmlUtils


class AboutMeFragment: UFragment(), Injectable {
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
        val about0 = markdown.markdownToSpannable(getString(R.string.about_unes_0), binding.textAboutDescription, null)
        val about1 = SpannableString(getString(R.string.about_unes_1))
        about1.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val about2 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.about_unes_2), binding.textAboutContinuation, null))
        about2.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val about3 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.about_unes_3), binding.textAboutContinuation, null))
        about3.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, about3.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        HtmlUtils.setTextWithNiceLinks(binding.textAboutDescription, about0)
        val sequence = TextUtils.concat(about1, "\n", about2, "\n\n", about3)
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
