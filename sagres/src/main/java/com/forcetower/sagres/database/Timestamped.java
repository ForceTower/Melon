package com.forcetower.sagres.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public interface Timestamped {

    default long getInMillis(String string) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());
        return formatter.parse(string).getTime();
    }

    default long getInMillis(String string, long def) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());
            return formatter.parse(string).getTime();
        } catch (Exception e) {
            return def;
        }
    }
}