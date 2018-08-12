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
