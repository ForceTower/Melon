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

package com.forcetower.core.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.forcetower.core.R;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

/**
 * A extension of ForegroundImageView that has a fixes aspect ratio.
 */
public class FixedDimensionImageView extends ForegroundImageView {
    private float heightDimension;
    private float widthDimension;

    public FixedDimensionImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.FixedDimensionImageView);
        if (a.hasValue(R.styleable.FixedDimensionImageView_heightDimension)) {
            heightDimension = a.getFloat(R.styleable.FixedDimensionImageView_heightDimension, 4);
        }

        if (a.hasValue(R.styleable.FixedDimensionImageView_widthDimension)) {
            widthDimension = a.getFloat(R.styleable.FixedDimensionImageView_widthDimension, 3);
        }
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int fourThreeHeight = makeMeasureSpec((int)(getSize(widthSpec) * heightDimension / widthDimension), EXACTLY);
        super.onMeasure(widthSpec, fourThreeHeight);
    }
}