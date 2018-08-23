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
