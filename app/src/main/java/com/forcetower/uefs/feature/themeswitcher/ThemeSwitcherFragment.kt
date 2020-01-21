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

package com.forcetower.uefs.feature.themeswitcher

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.widget.CompoundButtonCompat
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentThemeSwitcherBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import javax.inject.Inject

class ThemeSwitcherFragment : BottomSheetDialogFragment(), Injectable {
    @Inject
    lateinit var resourceProvider: ThemeSwitcherResourceProvider
    private lateinit var binding: FragmentThemeSwitcherBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentThemeSwitcherBinding.inflate(inflater, container, false)

        initThemingValues(
            binding.primaryColors,
            resourceProvider.primaryColors,
            resourceProvider.primaryColorsContentDescription,
            resourceProvider.primaryThemeOverlayAttrs,
            R.id.theme_feature_primary_color
        )

        initThemingValues(
            binding.secondaryColors,
            resourceProvider.secondaryColors,
            resourceProvider.secondaryColorsContentDescription,
            resourceProvider.secondaryThemeOverlayAttrs,
            R.id.theme_feature_secondary_color
        )

        binding.applyButton.setOnClickListener {
            applyThemeOverlays()
        }

        return binding.root
    }

    private fun initThemingValues(
        group: RadioGroup,
        overlays: Int,
        contentDescriptions: Int,
        themeOverlayAttrs: IntArray,
        overlayId: Int
    ) {
        val context = requireContext()
        val themeValues = resources.obtainTypedArray(overlays)
        val contentDescriptionArray = resources.obtainTypedArray(contentDescriptions)

        if (themeValues.length() != contentDescriptionArray.length())
            throw IllegalStateException("Values and contents must be same length")

        for (i in 0 until themeValues.length()) {
            @StyleRes val valueThemeOverlay = themeValues.getResourceId(i, 0)

            val themeAttr = ThemeAttributeValues.ColorPalette(valueThemeOverlay, themeOverlayAttrs, context)

            val button = createCompatRadioButton(group, contentDescriptionArray.getString(i) ?: "")
            button.tag = themeAttr
            themeAttr.customizeRadioButton(button)

            val currentThemeOverlay = ThemeOverlayUtils.getThemeOverlay(overlayId)
            if (themeAttr.themeOverlay == currentThemeOverlay) {
                group.check(button.id)
            }
        }

        themeValues.recycle()
        contentDescriptionArray.recycle()
    }

    private fun createCompatRadioButton(group: RadioGroup, description: String): AppCompatRadioButton {
        val button = AppCompatRadioButton(context)
        button.contentDescription = description
        group.addView(button)
        return button
    }

    private fun applyThemeOverlays() {
        val themesMap = arrayOf(
            intArrayOf(R.id.theme_feature_primary_color, getThemeOverlayResId(binding.primaryColors)),
            intArrayOf(R.id.theme_feature_secondary_color, getThemeOverlayResId(binding.secondaryColors))
        )
        for (i in themesMap.indices) {
            ThemeOverlayUtils.setThemeOverlay(themesMap[i][0], themesMap[i][1])
        }

        requireActivity().recreate()
    }

    private fun getThemeOverlayResId(radioGroup: RadioGroup): Int {
        if (radioGroup.checkedRadioButtonId == View.NO_ID) {
            return 0
        }
        val overlayFeature = binding.root.findViewById<View>(radioGroup.checkedRadioButtonId).tag as ThemeAttributeValues
        return overlayFeature.themeOverlay
    }

    private sealed class ThemeAttributeValues(@StyleRes val themeOverlay: Int) {
        open fun customizeRadioButton(button: AppCompatRadioButton) {}

        class ColorPalette(@StyleRes themeOverlay: Int, @StyleableRes themeOverlayAttrs: IntArray, context: Context) : ThemeAttributeValues(themeOverlay) {
            private val main: Int
            init {
                val array = context.obtainStyledAttributes(themeOverlay, themeOverlayAttrs)
                main = array.getColor(0, Color.TRANSPARENT)
                array.recycle()
            }

            override fun customizeRadioButton(button: AppCompatRadioButton) {
                CompoundButtonCompat.setButtonTintList(button, ColorStateList.valueOf(convertToDisplay(main)))
            }

            @ColorInt
            private fun convertToDisplay(@ColorInt color: Int): Int {
                return if (color == Color.WHITE) Color.BLACK else color
            }
        }
    }
}