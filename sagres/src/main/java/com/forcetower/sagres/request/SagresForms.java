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

package com.forcetower.sagres.request;

import androidx.annotation.NonNull;
import com.forcetower.sagres.Constants;
import com.forcetower.sagres.database.model.SDemandOffer;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SagresForms {

    private static void extractHiddenFields(@NonNull Document document, @NonNull FormBody.Builder formBody) {
        Elements elements = document.select("input[value][type=\"hidden\"]");
        for (Element element : elements) {
            String key = element.attr("id");
            String value = element.attr("value");
            formBody.add(key, value);
        }
    }

    public static RequestBody loginBody(@NonNull String username, @NonNull String password) {
        return new FormBody.Builder()
                .add("ctl00$PageContent$LoginPanel$UserName", username)
                .add("ctl00$PageContent$LoginPanel$Password", password)
                .add("ctl00$PageContent$LoginPanel$LoginButton", "Entrar")
                .add("__EVENTTARGET", "")
                .add("__EVENTARGUMENT", "")
                .add("__VIEWSTATE", Constants.getParameter("LOGIN_VIEW_STATE"))
                .add("__VIEWSTATEGENERATOR", Constants.getParameter("LOGIN_VW_STT_GEN"))
                .add("__EVENTVALIDATION", Constants.getParameter("LOGIN_VIEW_VALID"))
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

    public static FormBody.Builder makeFormBodyForDisciplineMaterials(@NonNull Document document, @NonNull String encoded) {
        FormBody.Builder builderIn = new FormBody.Builder();

        HashMap<String, String> values = new HashMap<>();
        values.put("ctl00$MasterPlaceHolder$RowsPerPage1$ddMostrar", "0");

        Elements elements = document.select("input[value][type=\"hidden\"]");

        for (Element elementIn : elements) {
            String id = elementIn.attr("id");
            String val = elementIn.attr("value");
            values.put(id, val);
        }

        values.put("__aspnetForm_ClientStateInput", encoded);
        values.put("ctl00$smpManager", "ctl00$MasterPlaceHolder$UpdatePanel1|ctl00$MasterPlaceHolder$pvMaterialApoio");
        values.put("_ajax_ctl00_MasterPlaceHolder_dwForm_context", "(objctl00_MasterPlaceHolder_dwForm 0)(21890 )((currentrow 0)(sortString '?'))");
        values.put("_ajax_ctl00_MasterPlaceHolder_dwForm_client", "(scrollbar 0 0)");
        values.put("_ajax_ctl00_MasterPlaceHolder_ucPopupConsultaMaterialApoio_dwForm_context", "(objctl00_MasterPlaceHolder_ucPopupConsultaMaterialApoio_dwForm 0)(22022 )((sortString 'anx_ds_anexo A'))");
        values.put("_ajax_ctl00_MasterPlaceHolder_ucPopupConsultaMaterialApoio_dwForm_client", "(scrollbar 0 0)");
        values.put("__EVENTTARGET", "ctl00$MasterPlaceHolder$pvMaterialApoio");
        values.put("__EVENTARGUMENT", "true");
        values.put("__ctl00_MasterPlaceHolder_pvMaterialApoio_ClientStateInput", "eyJfcmVhbFR5cGUiOnRydWUsInNob3ckX2luc2VydE5ld1JvdyI6ZmFsc2V9");
        values.put("__ctl00_MasterPlaceHolder_ucPopupConsultaMaterialApoio_ClientStateInput", "eyJfcmVhbFR5cGUiOnRydWV9");
        values.put("__ctl00_MasterPlaceHolder_ucPopupConsultaPlanoAula_PopupView1_ClientStateInput", "eyJfcmVhbFR5cGUiOnRydWV9");
        values.put("ctl00$HeaderPlaceHolder$ucCabecalhoClasse$PainelRetratil1_ClientState", "true");
        values.put("__ASYNCPOST", "false");

        for (Map.Entry<String, String> value : values.entrySet()) {
            builderIn.add(value.getKey(), value.getValue());
        }

        return builderIn;
    }

    @NotNull
    public static RequestBody makeFormBodyForDemand(@NotNull List<SDemandOffer> list, @NotNull Document document) {
        FormBody.Builder form = new FormBody.Builder();

        for (SDemandOffer offer : list) {
            form.add(offer.getId(), Boolean.toString(offer.getSelected()));
        }

        Elements elements = document.select("input[value][type=\"hidden\"]");
        for (Element element : elements) {
            String key = element.attr("id");
            String value = element.attr("value");

            if (value.trim().isEmpty() && !key.equalsIgnoreCase("__EVENTTARGET") && !key.equalsIgnoreCase("__EVENTARGUMENT") && !key.equalsIgnoreCase("__VIEWSTATE")) {
                value = "eyJfcmVhbFR5cGUiOnRydWV9";
            }

            if (!key.endsWith("hfChecked")) {
                form.add(key, value);
            }
        }

        form.add("ctl00$smpManager", "ctl00$MasterPlaceHolder$UpdatePanel1|ctl00$MasterPlaceHolder$btnSalvar");
        form.add("__ASYNCPOST", "false");
        form.add("ctl00$MasterPlaceHolder$btnSalvar", "Salvar");
        return form.build();
    }

    @NotNull
    public static RequestBody makeFormBodyForAllDisciplines(@NotNull Document document) {
        FormBody.Builder formBody = new FormBody.Builder();
        extractHiddenFields(document, formBody);

        formBody.add("ctl00$MasterPlaceHolder$ctl00$ddMostrar", "0");
        formBody.add("ctl00$MasterPlaceHolder$FiltroClasses$imRecuperar", "Exibir");
        formBody.add("ctl00$MasterPlaceHolder$FiltroClasses$ddPeriodosLetivos", "");
        formBody.add("ctl00$MasterPlaceHolder$FiltroClasses$txbFiltroNome", "");
        return formBody.build();
    }

    @NotNull
    public static RequestBody goToDisciplineAlternative(@NonNull String position, @NonNull Document document) {
        FormBody.Builder formBody = new FormBody.Builder();
        extractHiddenFields(document, formBody);

        formBody.add("__EVENTTARGET", "Selecionar");
        formBody.add("__EVENTARGUMENT", position);
        formBody.add("ctl00$MasterPlaceHolder$FiltroClasses$txbFiltroNome", "");
        formBody.add("ctl00$MasterPlaceHolder$ctl00$ddMostrar", "0");
        return formBody.build();
    }
}
