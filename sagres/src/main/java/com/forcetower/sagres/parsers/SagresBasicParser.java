package com.forcetower.sagres.parsers;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SagresBasicParser {

    public static boolean needApproval(@NonNull Document document) {
        Element approval = document.selectFirst("div[class=\"acesso-externo-pagina-aLogin\"]");
        if (approval != null) return true;

        approval = document.selectFirst("input[value=\"Acessar o SAGRES Portal\"]");
        return approval != null;

    }

    public static boolean isConnected(@Nullable Document document) {
        if (document == null) return false;

        Element element = document.selectFirst("div[class=\"externo-erro\"]");
        if (element != null) {
            if (element.text().length() != 0) {
                return false;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
