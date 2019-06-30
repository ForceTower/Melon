/*
 * Copyright (c) 2019.
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

/**
 * Created by João Paulo on 06/03/2018.
 */

public class WordUtils {
    public static String toTitleCase(String givenString) {
        if (givenString == null)
            return null;

        givenString = givenString.toLowerCase();

        String[] arr = givenString.split(" ");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++) {
            String anArr = arr[i];

            if (i == arr.length - 1 && anArr.length() <= 2) {
                sb.append(anArr.toUpperCase());
                continue;
            }

            if (isGreekOneToTen(anArr)) {
                sb.append(anArr.toUpperCase()).append(" ");
                continue;
            }

            // Special case only for "MI's". PBL!!!!
            if (anArr.equalsIgnoreCase("MI")) {
                sb.append(anArr.toUpperCase()).append(" ");
                continue;
            }

            if ((anArr.length() < 3 && !anArr.endsWith(".")) || (anArr.length() == 3 && anArr.endsWith("s"))) {
                sb.append(anArr).append(" ");
                continue;
            }

            sb.append(Character.toUpperCase(anArr.charAt(0)))
                    .append(anArr.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static boolean isGreekOneToTen(String str) {
        return str.equalsIgnoreCase("i") ||
                str.equalsIgnoreCase("ii") ||
                str.equalsIgnoreCase("iii") ||
                str.equalsIgnoreCase("iv") ||
                str.equalsIgnoreCase("v") ||
                str.equalsIgnoreCase("vi") ||
                str.equalsIgnoreCase("vii") ||
                str.equalsIgnoreCase("viii") ||
                str.equalsIgnoreCase("ix") ||
                str.equalsIgnoreCase("x");
    }

    public static String capitalize(String givenString) {
        if (givenString == null)
            return null;

        givenString = givenString.toLowerCase();

        String[] arr = givenString.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String anArr : arr) {
            if (anArr.isEmpty()) continue;
            if (anArr.length() < 2) {
                sb.append(anArr).append(" ");
            } else {
                sb.append(Character.toUpperCase(anArr.charAt(0)))
                        .append(anArr.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static boolean validString(String string) {
        return string != null && !string.trim().isEmpty();
    }


}
