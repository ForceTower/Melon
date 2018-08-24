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

package com.forcetower.sagres.utils;

import androidx.annotation.NonNull;

/**
 * Created by João Paulo on 06/03/2018.
 */

public class ValueUtils {
    public static int toInteger(@NonNull String string, int def) {
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            return def;
        }
    }

    public static int toInteger(@NonNull String string) {
        return toInteger(string, -1);
    }

    public static double toDouble(@NonNull String string) {
        return toDouble(string, -1);
    }

    public static double toDoubleMod(@NonNull String string) {
        string = string.replace(",", ".");
        return toDouble(string, -1);
    }

    public static double toDouble(@NonNull String string, double def) {
        try {
            return Double.parseDouble(string);
        } catch (Exception e) {
            return def;
        }
    }
}
