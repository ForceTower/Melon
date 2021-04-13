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

import android.app.Activity
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.palette.graphics.Palette
import com.forcetower.core.adapters.ImageLoadListener
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.core.injection.dependencies.EventModuleDependencies
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.getBitmap
import com.forcetower.uefs.feature.shared.extensions.postponeEnterTransition
import com.forcetower.uefs.feature.web.CustomTabActivityHelper
import com.forcetower.uefs.widget.ElasticDragDismissFrameLayout
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.event.R
import dev.forcetower.event.core.injection.DaggerEventComponent
import dev.forcetower.event.databinding.ActivityEventDetailsBinding
import javax.inject.Inject

class EventDetailsActivity : UActivity() {
    @Inject lateinit var factory: ViewModelProvider.Factory
    private lateinit var chromeFader: ElasticDragDismissFrameLayout.SystemChromeFader
    lateinit var binding: ActivityEventDetailsBinding

    private val viewModel by viewModels<EventDetailsViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        SplitCompat.installActivity(this)
        DaggerEventComponent.builder()
            .context(this)
            .dependencies(
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    EventModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)

        super.onCreate(savedInstanceState)
        postponeEnterTransition(500L)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_event_details)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val id = intent.getLongExtra("eventId", 0L)
        if (id == 0L) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        binding.actions = viewModel

        viewModel.loadModel(id).observe(
            this,
            Observer {
                binding.event = it
            }
        )

        viewModel.onEventCreationSent.observe(
            this,
            EventObserver {
                setResult(Activity.RESULT_OK)
                finishAfterTransition()
            }
        )

        viewModel.onEventMoveToPage.observe(
            this,
            EventObserver {
                it.registerPage ?: return@EventObserver
                CustomTabActivityHelper.openCustomTab(
                    this,
                    CustomTabsIntent.Builder()
                        .setDefaultColorSchemeParams(
                            CustomTabColorSchemeParams
                                .Builder()
                                .setToolbarColor(ViewUtils.attributeColorUtils(this, com.forcetower.uefs.R.attr.colorPrimary))
                                .build()
                        )
                        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                        .build(),
                    Uri.parse(it.registerPage)
                )
            }
        )

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
            palette,
            0.25f,
            0.5f,
            ContextCompat.getColor(this, com.forcetower.uefs.R.color.mid_grey),
            true
        )
        // slightly more opaque ripple on the pinned image to compensate for the scrim
        binding.image.foreground = ViewUtils.createRipple(
            palette,
            0.3f,
            0.6f,
            ContextCompat.getColor(this, com.forcetower.uefs.R.color.mid_grey),
            true
        )
    }

    companion object {
        private const val SCRIM_ADJUSTMENT = 0.075f
    }
}
