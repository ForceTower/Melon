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

package com.forcetower.uefs.feature.siecomp.speaker

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.forcetower.core.adapters.ImageLoadListener
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.siecomp.Speaker

/**
 * Loads a [Speaker]'s photo or picks a default avatar if no photo is specified.
 */
@SuppressLint("CheckResult")
@BindingAdapter(value = ["speakerImage", "listener"], requireAll = false)
fun speakerImage(
    imageView: ImageView,
    speaker: Speaker?,
    listener: ImageLoadListener?
) {
    speaker ?: return

    val placeholderId = when (speaker.name[0].toLowerCase()) {
        in 'a'..'i' -> R.drawable.ic_default_avatar_1
        in 'j'..'r' -> R.drawable.ic_default_avatar_2
        else -> R.drawable.ic_default_avatar_3
    }

    if (speaker.image.isNullOrBlank()) {
        imageView.setImageResource(placeholderId)
    } else {
        val imageLoad = Glide.with(imageView)
            .load(speaker.image)
            .apply(
                RequestOptions()
                    .placeholder(placeholderId)
                    .circleCrop()
            )

        if (listener != null) {
            imageLoad.listener(
                object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onImageLoaded(resource)
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onImageLoadFailed()
                        return false
                    }
                }
            )
        }

        imageLoad.into(imageView)
    }
}

@BindingAdapter(value = ["facebookUrl", "twitterUrl", "githubUrl", "linkedInUrl"], requireAll = true)
fun createSpeakerLinksView(
    textView: TextView,
    facebookUrl: String?,
    twitterUrl: String?,
    githubUrl: String?,
    linkedInUrl: String?
) {
    val links =
        mapOf(
            R.string.speaker_link_facebook to facebookUrl,
            R.string.speaker_link_twitter to twitterUrl,
            R.string.speaker_link_github to githubUrl,
            R.string.speaker_link_linkedin to linkedInUrl
        )
            .filterValues { !it.isNullOrEmpty() }
            .map { (labelRes, url) ->
                val span = SpannableString(textView.context.getString(labelRes))
                span.setSpan(URLSpan(url), 0, span.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                span
            }
            .joinTo(
                SpannableStringBuilder(),
                separator = textView.context.getString(R.string.speaker_link_separator)
            )
    if (links.isNotBlank()) {
        textView.apply {
            visibility = VISIBLE
            text = links
            // Make links clickable
            movementMethod = LinkMovementMethod.getInstance()
            isFocusable = false
            isClickable = false
        }
    } else {
        textView.visibility = GONE
    }
}
