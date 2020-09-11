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

package com.forcetower.core.adapters

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.forcetower.core.R

@BindingAdapter(
    value = [
        "imageUri",
        "imageUrl",
        "placeholder",
        "clipCircle",
        "listener",
        "crossFade",
        "overrideImageWidth",
        "overrideImageHeight",
        "dontTransform"
    ],
    requireAll = false
)
fun imageUri(
    imageView: ImageView,
    imageUri: Uri? = null,
    imageUrl: String? = null,
    placeholder: Drawable? = null,
    clipCircle: Boolean? = false,
    listener: ImageLoadListener? = null,
    crossFade: Boolean? = false,
    overrideWidth: Int? = null,
    overrideHeight: Int? = null,
    dontTransform: Boolean? = false
) {
    val url = imageUrl ?: imageUri
    val placeholderDrawable = placeholder ?: AppCompatResources.getDrawable(
        imageView.context,
        R.mipmap.ic_unes_large_image_512
    )

    val circular = clipCircle ?: false
    var request = when (url) {
        null -> {
            Glide.with(imageView)
                .load(placeholderDrawable)
        }
        else -> {
            Glide.with(imageView)
                .load(url)
                .apply(RequestOptions().placeholder(placeholderDrawable))
        }
    }

    if (circular) request = request.circleCrop()
    if (crossFade == true) request = request.transition(DrawableTransitionOptions.withCrossFade())
    if (overrideWidth != null && overrideHeight != null) {
        request = request.override(overrideWidth, overrideHeight)
    }
    if (dontTransform == true) request = request.dontTransform()

    if (listener != null) {
        request = request.listener(
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
    request.into(imageView)
}

/**
 * An interface for responding to image loading completion.
 */
interface ImageLoadListener {
    fun onImageLoaded(drawable: Drawable)
    fun onImageLoadFailed()
}
