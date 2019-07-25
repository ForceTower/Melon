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

public class DateUtils {
    public static int getDayOfWeek(String text) {
        if (text.equalsIgnoreCase("SEG"))
            return 1;
        else if (text.equalsIgnoreCase("TER"))
            return 2;
        else if (text.equalsIgnoreCase("QUA"))
            return 3;
        else if (text.equalsIgnoreCase("QUI"))
            return 4;
        else if (text.equalsIgnoreCase("SEX"))
            return 5;
        else if (text.equalsIgnoreCase("SAB"))
            return 6;
        else if (text.equalsIgnoreCase("DOM"))
            return 0;

        return 99;
    }

    public static String getDayOfWeek(int i) {
        if (i == 1)
            return "SEG";
        else if (i == 2)
            return "TER";
        else if (i == 3)
            return "QUA";
        else if (i == 4)
            return "QUI";
        else if (i == 5)
            return "SEX";
        else if (i == 6)
            return "SAB";
        else if (i == 7)
            return "DOM";

        return "???";
    }
}
