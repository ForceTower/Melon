package com.forcetower.sagres.utils;

import android.util.Patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by João Paulo on 06/03/2018.
 */

public class WordUtils {
    public static String toTitleCase(String givenString) {
        if (givenString == null)
            return null;

        String[] arr = givenString.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String anArr : arr) {
            if (anArr.length() < 4 && !anArr.endsWith(".")) {
                sb.append(anArr).append(" ");
                continue;
            }

            sb.append(Character.toUpperCase(anArr.charAt(0)))
                    .append(anArr.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public static boolean validString(String string) {
        return string != null && !string.trim().isEmpty();
    }


}
