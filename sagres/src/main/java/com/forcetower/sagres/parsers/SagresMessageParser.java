/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.parsers;

import android.os.SystemClock;

import com.forcetower.sagres.database.model.Message;

import org.jsoup.nodes.Document;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by João Paulo on 06/03/2018.
 */

public class SagresMessageParser {
    private static final String MESSAGE_CLASS_RECEIVED = "class=\"recado-escopo\">";
    private static final String MESSAGE_DATE_RECEIVED = "class=\"recado-data\">";
    private static final String MESSAGE_MESSAGE_RECEIVED = "class=\"recado-texto\">";
    private static final String MESSAGE_FROM_RECEIVED = "class=\"recado-remetente\">";

    public static List<Message> getMessages(Document document) {
        String html = document.html();
        List<Message> messages = new ArrayList<>();

        int position = 0;
        boolean found = html.indexOf("<article id", position) != -1;

        while (found) {
            int start = html.indexOf("<article id", position);
            int end = html.indexOf("</article>", start);

            if (start == -1)
                return messages;

            String article = html.substring(start, end);

            Message message = extractInfoArticle(article);
            messages.add(message);
            position = end;
        }

        return messages;
    }

    private static Message extractInfoArticle(String article) {
        String clazz = extractArticleForm1(MESSAGE_CLASS_RECEIVED, article);
        String date = extractArticleForm1(MESSAGE_DATE_RECEIVED, article);
        String message = extractArticleForm2(MESSAGE_MESSAGE_RECEIVED, article);
        String from = extractArticleForm2(MESSAGE_FROM_RECEIVED, article);
        //TODO Figure this out
        return null;
    }

    private static String extractArticleForm2(String regex, String article) {
        int startRRE = article.indexOf(regex);
        if (startRRE != -1) {
            int endRRE = article.indexOf("</span>", startRRE);
            String message = article.substring(startRRE, endRRE).trim();
            message = message.substring(regex.length() + 1);

            message = message.substring(message.indexOf(">") + 1).trim();
            return message;
        }

        return null;
    }

    private static String extractArticleForm1(String regex, String article) {
        int startCRE = article.indexOf(regex);
        if (startCRE != -1) {
            int endCRE = article.indexOf("</span>", startCRE);
            String extracted = article.substring(startCRE, endCRE);

            //CLASS NAME
            extracted = extracted.substring(extracted.indexOf(">") + 1).trim();
            return extracted;
        }

        return null;
    }
}
