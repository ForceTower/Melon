package com.forcetower.unes.core.parsers;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class SagresBasicParser {
    public static boolean needApproval(@NonNull Document document) {
        Element approval = document.selectFirst("div[class=\"acesso-externo-pagina-login\"]");
        if (approval != null) return true;

        approval = document.selectFirst("input[value=\"Acessar o SAGRES Portal\"]");
        return approval != null;

    }

    public static boolean isConnected(@Nullable Document document) {
        if (document == null) return false;

        Element element = document.selectFirst("div[class=\"externo-erro\"]");
        if (element != null) {
            if (element.text().length() != 0) {
                Timber.d("Login failed - Invalid Credentials");
                return false;
            } else {
                Timber.d("Login failed - Length is different now");
                return false;
            }
        } else {
            Timber.d("Correct Login");
            return true;
        }
    }
}
