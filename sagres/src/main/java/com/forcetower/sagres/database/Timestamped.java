/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.sagres.database;

import timber.log.Timber;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public interface Timestamped {

    default long getInMillis(String string) throws ParseException {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX", Locale.getDefault());
            return formatter.parse(string.trim()).getTime();
        } catch (Exception e) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());
                return formatter.parse(string.trim()).getTime();
            } catch (Exception e1) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.getDefault());
                    return formatter.parse(string.trim()).getTime();
                } catch (Exception e2) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                    return formatter.parse(string.trim()).getTime();
                }
            }
        }
    }

    default long getInMillis(String string, long def) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX", Locale.getDefault());
            return formatter.parse(string.trim()).getTime();
        } catch (Exception e) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());
                return formatter.parse(string.trim()).getTime();
            } catch (Exception e1) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.getDefault());
                    return formatter.parse(string.trim()).getTime();
                } catch (Exception e2) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                        return formatter.parse(string.trim()).getTime();
                    } catch (Exception e3) {
                        Timber.e("Error while parsing data! Exception is %s", e.getMessage());
                        return def;
                    }
                }
            }
        }
    }
}