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
