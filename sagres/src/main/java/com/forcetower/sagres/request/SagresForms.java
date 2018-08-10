package com.forcetower.sagres.request;

import com.forcetower.sagres.Constants;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import androidx.annotation.NonNull;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class SagresForms {

    public static RequestBody loginBody(@NonNull String username, @NonNull String password) {
        return new FormBody.Builder()
                .add("ctl00$PageContent$LoginPanel$UserName", username)
                .add("ctl00$PageContent$LoginPanel$Password", password)
                .add("ctl00$PageContent$LoginPanel$LoginButton", "Entrar")
                .add("__EVENTTARGET", "")
                .add("__EVENTARGUMENT", "")
                .add("__VIEWSTATE", Constants.LOGIN_VIEW_STATE)
                .add("__VIEWSTATEGENERATOR", Constants.LOGIN_VW_STT_GEN)
                .add("__EVENTVALIDATION", Constants.LOGIN_VIEW_VALID)
                .build();
    }

    public static RequestBody loginApprovalBody(@NonNull Document document) {
        FormBody.Builder formBody = new FormBody.Builder();
        Elements elements = document.select("input[value][type=\"hidden\"]");
        for (Element element : elements) {
            String key = element.attr("id");
            String value = element.attr("value");
            formBody.add(key, value);
        }

        formBody.add("ctl00$btnLogin", "Acessar o SAGRES Portal");
        return formBody.build();
    }
}
