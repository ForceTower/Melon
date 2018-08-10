package com.forcetower.unes.feature.shared

import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils

fun View.fadeIn() {
    if (visibility == VISIBLE) return
    val fade: Animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
    visibility = VISIBLE
    startAnimation(fade)
    requestLayout()
}

fun View.fadeOut() {
    if (visibility == INVISIBLE) return
    val fade: Animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
    visibility = INVISIBLE
    startAnimation(fade)
    requestLayout()
}