/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.shared

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.siecomp.speaker.ImageLoadListener
import com.forcetower.uefs.widget.CustomSwipeRefreshLayout
import timber.log.Timber

@BindingAdapter("clipToCircle")
fun clipToCircle(view: View, clip: Boolean) {
    view.clipToOutline = clip
    view.outlineProvider = if (clip) CircularOutlineProvider else null
}

@BindingAdapter(value = ["imageUri", "placeholder", "clipCircle", "listener"], requireAll = false)
fun imageUri(imageView: ImageView, imageUri: Uri?, placeholder: Drawable?, clipCircle: Boolean?, listener: ImageLoadListener?) {
    val placeholderDrawable = placeholder ?: AppCompatResources.getDrawable(
            imageView.context, R.mipmap.ic_unes_large_image_512
    )
    val circular = clipCircle ?: false
    var request = when (imageUri) {
        null -> {
            Timber.d("Unsetting image url")
            Glide.with(imageView)
                    .load(placeholderDrawable)
        }
        else -> {
            Glide.with(imageView)
                    .load(imageUri)
                    .apply(RequestOptions().placeholder(placeholderDrawable))
        }
    }
    request = if (circular) {
        request.circleCrop()
    } else {
        request
    }

    if (listener != null) {
        request = request.listener(object : RequestListener<Drawable> {
            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                listener.onImageLoaded()
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
        })
    }
    request.into(imageView)
}

@BindingAdapter(value = ["imageUrl", "placeholder", "clipCircle", "listener"], requireAll = false)
fun imageUrl(imageView: ImageView, imageUrl: String?, placeholder: Drawable?, clipCircle: Boolean?, listener: ImageLoadListener?) {
    imageUri(imageView, imageUrl?.toUri(), placeholder, clipCircle, listener)
}

@BindingAdapter("swipeRefreshColors")
fun setSwipeRefreshColors(swipeRefreshLayout: CustomSwipeRefreshLayout, colorResIds: IntArray) {
    swipeRefreshLayout.setColorSchemeColors(*colorResIds)
}

@BindingAdapter("invisibleUnless")
fun invisibleUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else INVISIBLE
}

@BindingAdapter("goneIf")
fun goneIf(view: View, gone: Boolean) {
    view.visibility = if (gone) GONE else VISIBLE
}

@BindingAdapter("goneUnless")
fun goneUnless(view: View, condition: Boolean) {
    view.visibility = if (condition) VISIBLE else GONE
}

@BindingAdapter("pageMargin")
fun pageMargin(viewPager: ViewPager, pageMargin: Float) {
    viewPager.pageMargin = pageMargin.toInt()
}

@BindingAdapter("refreshing")
fun swipeRefreshing(refreshLayout: CustomSwipeRefreshLayout, refreshing: Boolean) {
    refreshLayout.isRefreshing = refreshing
}

@BindingAdapter("onSwipeRefresh")
fun onSwipeRefresh(view: CustomSwipeRefreshLayout, function: SwipeRefreshLayout.OnRefreshListener) {
    view.setOnRefreshListener(function)
}

@BindingAdapter("swipeEnabled")
fun swipeEnabled(view: CustomSwipeRefreshLayout, enabled: Boolean) {
    view.isEnabled = enabled
}

@BindingAdapter("accountName")
fun accountName(tv: TextView, name: String?) {
    val real = name ?: "Cidadão Anônimo"
    val titled = WordUtils.toTitleCase(real)
    val formatted = tv.context.getString(R.string.evaluation_welcome, titled)
    tv.text = formatted
}

// @BindingAdapter("circleMax")
// fun circleMax(pv: CircleProgressBar, max: Int) {
//
// }