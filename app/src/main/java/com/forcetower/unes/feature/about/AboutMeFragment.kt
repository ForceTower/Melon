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
