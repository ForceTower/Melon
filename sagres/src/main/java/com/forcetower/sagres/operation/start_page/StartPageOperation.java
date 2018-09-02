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

package com.forcetower.sagres.operation.start_page;

import com.forcetower.sagres.database.model.SDiscipline;
import com.forcetower.sagres.database.model.SDisciplineClassLocation;
import com.forcetower.sagres.database.model.SDisciplineGroup;
import com.forcetower.sagres.database.model.SCalendar;
import com.forcetower.sagres.database.model.SSemester;
import com.forcetower.sagres.operation.Operation;
import com.forcetower.sagres.operation.Status;
import com.forcetower.sagres.parsers.SagresCalendarParser;
import com.forcetower.sagres.parsers.SagresDcpGroupsParser;
import com.forcetower.sagres.parsers.SagresDisciplineParser;
import com.forcetower.sagres.parsers.SagresScheduleParser;
import com.forcetower.sagres.parsers.SagresSemesterParser;
import com.forcetower.sagres.request.SagresCalls;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Response;

import static com.forcetower.sagres.Utils.createDocument;

public class StartPageOperation extends Operation<StartPageCallback> {
    public StartPageOperation(@Nullable Executor executor) {
        super(executor);
        this.perform();
    }

    @Override
    protected void execute() {
        result.postValue(new StartPageCallback(Status.STARTED));
        Call call = SagresCalls.getStartPage();
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                Document document = createDocument(body);
                successMeasures(document);
            } else {
                result.postValue(new StartPageCallback(Status.RESPONSE_FAILED).message(response.message()).code(response.code()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.postValue(new StartPageCallback(Status.NETWORK_ERROR).throwable(e));
        }
    }

    private void successMeasures(Document document) {
        List<SCalendar> calendar = SagresCalendarParser.getCalendar(document);
        List<SSemester> semesters = SagresSemesterParser.getSemesters(document);
        List<SDiscipline> disciplines = SagresDisciplineParser.getDisciplines(document);
        List<SDisciplineGroup> groups = SagresDcpGroupsParser.getGroups(document);
        List<SDisciplineClassLocation> locations = SagresScheduleParser.getSchedule(document);

        StartPageCallback callback = new StartPageCallback(Status.SUCCESS)
                .document(document)
                .calendar(calendar)
                .semesters(semesters)
                .disciplines(disciplines)
                .groups(groups)
                .locations(locations);

        finished = callback;
        success = true;

        result.postValue(callback);
    }
}
