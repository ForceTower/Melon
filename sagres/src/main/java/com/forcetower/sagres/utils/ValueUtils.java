/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
