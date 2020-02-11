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

package dev.forcetower.event.feature.details

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.palette.graphics.Palette
import com.forcetower.core.adapters.ImageLoadListener
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.getBitmap
import com.forcetower.uefs.feature.shared.extensions.postponeEnterTransition
import com.forcetower.uefs.widget.ElasticDragDismissFrameLayout
import com.google.android.play.core.splitcompat.SplitCompat
import dev.forcetower.event.R
import dev.forcetower.event.databinding.ActivityEventDetailsBinding
import org.threeten.bp.ZonedDateTime

class EventDetailsActivity : UActivity() {
    private lateinit var chromeFader: ElasticDragDismissFrameLayout.SystemChromeFader
    lateinit var binding: ActivityEventDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        SplitCompat.installActivity(this)
        super.onCreate(savedInstanceState)
        postponeEnterTransition(500L)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_event_details)
        binding.root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        binding.event = event

        val headLoadListener = object : ImageLoadListener {
            override fun onImageLoaded(drawable: Drawable) {
                val bitmap = drawable.getBitmap() ?: return

                Palette.from(bitmap)
                    .clearFilters()
                    .generate { palette -> applyFullImagePalette(palette) }

                binding.image.background = null
                startPostponedEnterTransition()
            }
            override fun onImageLoadFailed() {
                binding.image.background = null
                startPostponedEnterTransition()
            }
        }

        binding.imageListener = headLoadListener

        chromeFader = object : ElasticDragDismissFrameLayout.SystemChromeFader(this) {
            override fun onDragDismissed() {
                finishAfterTransition()
            }
        }

        binding.apply {
            bodyScroll.setOnScrollChangeListener { _: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->
                image.offset = -scrollY
            }
            back.setOnClickListener { finishAfterTransition() }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.draggableFrame.addListener(chromeFader)
    }

    override fun onPause() {
        binding.draggableFrame.removeListener(chromeFader)
        super.onPause()
    }

    override fun onBackPressed() {
        finishAfterTransition()
    }

    fun applyFullImagePalette(palette: Palette?) {
        // color the ripple on the image spacer (default is grey)
        binding.imageSpacer.background = ViewUtils.createRipple(
            palette, 0.25f, 0.5f,
            ContextCompat.getColor(this, com.forcetower.uefs.R.color.mid_grey), true
        )
        // slightly more opaque ripple on the pinned image to compensate for the scrim
        binding.image.foreground = ViewUtils.createRipple(
            palette, 0.3f, 0.6f,
            ContextCompat.getColor(this, com.forcetower.uefs.R.color.mid_grey), true
        )
    }

    companion object {
        private const val SCRIM_ADJUSTMENT = 0.075f

        val event = Event(
            1,
            "XXIII SIECOMP",
            "Muita coisa engraçada e gente legal",
            "https://images.even3.com.br/UPJVSvZBwbrcakjVHQLyiz90jHU=/1300x536/smart/even3.blob.core.windows.net/banner/BannerXXIISIECOMP.e34ffcd81b1d4a019044.jpg",
            "João Paulo",
            1,
            "Ele mesmo",
            ZonedDateTime.now().plusDays(3),
            ZonedDateTime.now().plusDays(3).plusHours(3),
            "Na sua casa",
            9.99,
            20,
            null,
            true,
            ZonedDateTime.now(),
            true,
            canModify = true,
            participating = false
        )
    }
}