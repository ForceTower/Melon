/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.sagres.request;

import com.forcetower.sagres.Constants;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import androidx.annotation.NonNull;
import okhttp3.FormBody;
import okhttp3.RequestBody;

import java.util.HashMap;
import java.util.Map;

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
}
