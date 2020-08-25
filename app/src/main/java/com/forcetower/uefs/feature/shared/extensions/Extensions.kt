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

package com.forcetower.uefs.feature.shared.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.view.postDelayed
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import java.io.File
import java.io.FileOutputStream

fun <X, Y> LiveData<X>.map(body: (X) -> Y): LiveData<Y> {
    return Transformations.map(this, body)
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun <T> MutableLiveData<T>.setValueIfNew(newValue: T) {
    if (this.value != newValue) value = newValue
}

fun Activity.postponeEnterTransition(timeout: Long) {
    postponeEnterTransition()
    window.decorView.postDelayed(timeout) {
        startPostponedEnterTransition()
    }
}

val LayerDrawable.layers: List<Drawable>
    get() = (0 until numberOfLayers).map { getDrawable(it) }

fun Drawable.getBitmap(): Bitmap? {
    if (this is TransitionDrawable) {
        layers.forEach {
            val bmp = it.getBitmap()
            if (bmp != null) return bmp
        }
    }
    if (this is BitmapDrawable) {
        return bitmap
    } else if (this is GifDrawable) {
        return firstFrame
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.N_MR1)
fun Intent.toShortcut(ctx: Context, id: String, @DrawableRes icon: Int, name: String): ShortcutInfo {
    return ShortcutInfo.Builder(ctx, id)
        .setShortLabel(name)
        .setIcon(Icon.createWithResource(ctx, icon))
        .setIntent(this)
        .build()
}

fun Bitmap.unesLogo(context: Context, pos: Int): Bitmap {
    val result = createBitmap(width, height)
    val canvas = Canvas(result)
    canvas.drawBitmap(this, 0f, 0f, null)

    val px16dp = getPixelsFromDp(context, 12)
    val px42dp = getPixelsFromDp(context, 42).toInt()
    val logo = context.getDrawable(R.mipmap.im_logo)!!.toBitmap().scale(px42dp, px42dp)

    val left = if (pos == 0) px16dp else width - logo.width - px16dp
    val top = if (pos == 0) height - logo.height - px16dp else 42f
    canvas.drawBitmap(logo, left, top, null)
    canvas.save()
    return result
}

fun Bitmap.toFile(context: Context): File {
    val parent = File(context.getExternalFilesDir(null), "messages")
    parent.deleteRecursively()
    parent.mkdirs()
    val file = File(parent, "message_share.jpg")
    val fos = FileOutputStream(file)
    compress(Bitmap.CompressFormat.JPEG, 100, fos)
    fos.flush()
    fos.close()
    return file
}

fun Boolean.asInt(): Int {
    return if (this) 1 else 0
}

fun Int.asBoolean(): Boolean {
    return this >= 1
}

fun View.isRtl() = layoutDirection == View.LAYOUT_DIRECTION_RTL
