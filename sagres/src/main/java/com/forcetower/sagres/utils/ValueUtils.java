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
        return toInteger(string, 0);
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
