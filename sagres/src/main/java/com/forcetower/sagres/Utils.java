package com.forcetower.sagres;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.Charset;

import androidx.annotation.NonNull;

public class Utils {
    public static Document createDocument(@NonNull String string) {
        Document document = Jsoup.parse(string);
        document.charset(Charset.forName("ISO-8859-1"));
        return document;
    }
}
