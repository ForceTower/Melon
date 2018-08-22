package com.forcetower.sagres.operation.start_page;

import com.forcetower.sagres.database.model.Discipline;
import com.forcetower.sagres.database.model.DisciplineClassLocation;
import com.forcetower.sagres.database.model.DisciplineGroup;
import com.forcetower.sagres.database.model.SagresCalendar;
import com.forcetower.sagres.database.model.Semester;
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
        List<SagresCalendar> calendar = SagresCalendarParser.getCalendar(document);
        List<Semester> semesters = SagresSemesterParser.getSemesters(document);
        List<Discipline> disciplines = SagresDisciplineParser.getDisciplines(document);
        List<DisciplineGroup> groups = SagresDcpGroupsParser.getGroups(document);
        List<DisciplineClassLocation> locations = SagresScheduleParser.getSchedule(document);

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
