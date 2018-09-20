package com.forcetower.uefs.feature.profile

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R

@BindingAdapter("profileImage")
fun profileImage(iv: ImageView, url: String) {
    GlideApp.with(iv.context)
            .load(url)
            .fallback(R.mipmap.ic_unes_large_image_512)
            .placeholder(R.mipmap.ic_unes_large_image_512)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(iv)
}