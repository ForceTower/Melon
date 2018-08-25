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

package com.forcetower.unes.feature.shared

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.View.*
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.forcetower.unes.R
import com.forcetower.unes.widget.CustomSwipeRefreshLayout
import timber.log.Timber

@BindingAdapter("clipToCircle")
fun clipToCircle(view: View, clip: Boolean) {
    view.clipToOutline = clip
    view.outlineProvider = if (clip) CircularOutlineProvider else null
}

@BindingAdapter(value = ["imageUri", "placeholder"], requireAll = false)
fun imageUri(imageView: ImageView, imageUri: Uri?, placeholder: Drawable?) {
    val placeholderDrawable = placeholder ?: AppCompatResources.getDrawable(
            imageView.context, R.mipmap.ic_unes_large_image_512
    )
    when (imageUri) {
        null -> {
            Timber.d("Unsetting image url")
            Glide.with(imageView)
                    .load(placeholderDrawable)
                    .into(imageView)
        }
        else -> {
            Glide.with(imageView)
                    .load(imageUri)
                    .apply(RequestOptions().placeholder(placeholderDrawable))
                    .into(imageView)
        }
    }
}

@BindingAdapter(value = ["imageUrl", "placeholder"], requireAll = false)
fun imageUrl(imageView: ImageView, imageUrl: String?, placeholder: Drawable?) {
    imageUri(imageView, imageUrl?.toUri(), placeholder)
}

@BindingAdapter("swipeRefreshColors")
fun setSwipeRefreshColors(swipeRefreshLayout: CustomSwipeRefreshLayout, colorResIds: IntArray) {
    swipeRefreshLayout.setColorSchemeColors(*colorResIds)
}

@BindingAdapter("invisibleUnless")
fun invisibleUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else INVISIBLE
}

@BindingAdapter("goneUnless")
fun goneUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) VISIBLE else GONE
}

@BindingAdapter("pageMargin")
fun pageMargin(viewPager: ViewPager, pageMargin: Float) {
    viewPager.pageMargin = pageMargin.toInt()
}