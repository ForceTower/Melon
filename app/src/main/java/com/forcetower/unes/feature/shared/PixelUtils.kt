package com.forcetower.unes.feature.shared

import android.content.Context
import android.util.TypedValue

fun getPixelsFromDp(context: Context, dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()