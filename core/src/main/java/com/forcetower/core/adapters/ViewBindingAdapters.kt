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
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowInsets
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
import com.forcetower.core.R
import com.forcetower.core.widget.CircularOutlineProvider
import com.forcetower.core.widget.CustomSwipeRefreshLayout
import com.forcetower.sagres.utils.WordUtils
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@BindingAdapter("dateFromLong")
fun dateFromLong(view: TextView, value: Long?) {
    value ?: return
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val str = format.format(Date(value))
    view.text = str
}

/**
 * An interface for responding to image loading completion.
 */
interface ImageLoadListener {
    fun onImageLoaded()
    fun onImageLoadFailed()
}

data class ViewPaddingState(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val start: Int,
    val end: Int
)

@BindingAdapter(
    "paddingStartSystemWindowInsets",
    "paddingTopSystemWindowInsets",
    "paddingEndSystemWindowInsets",
    "paddingBottomSystemWindowInsets",
    requireAll = false
)
fun applySystemWindows(
    view: View,
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    view.doOnApplyWindowInsets { _, insets, padding ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        view.setPadding(
            padding.left + left,
            padding.top + top,
            padding.right + right,
            padding.bottom + bottom
        )
    }
}

data class InitialPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
        view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun View.doOnApplyWindowInsets(f: (View, WindowInsets, InitialPadding) -> Unit) {
    val initialPadding = recordInitialPaddingForView(this)
    setOnApplyWindowInsetsListener { v, insets ->
        f(v, insets, initialPadding)
        insets
    }
    requestApplyInsetsWhenAttached()
}
