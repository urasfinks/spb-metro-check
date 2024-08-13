package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Station;

import java.util.Map;

@Component
@RequestMapping
public class ParseStationCsv implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ParseStationCsv(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    private void addToRequest(Map<String, Object> json, JdbcRequest jdbcRequest) {
        try {
            jdbcRequest
                    .addArg("code", json.get("f0"))
                    .addArg("place", json.get("f15"))
                    .nextBatch();

        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 700_000L)
                .thenWithResource("loadToDb", JdbcResource.class, "default", (isThreadRun, _, jdbcResource)
                        -> SpbMetroCheckApplication.onRead(
                        SpbMetroCheckApplication.getCSVReader("web/station-2.csv", 2),
                        isThreadRun,
                        5000,
                        listJson -> {
                            JdbcRequest jdbcRequest = new JdbcRequest(Station.INSERT);
                            listJson.forEach(json -> addToRequest(json, jdbcRequest));
                            Util.logConsole("insert");
                            jdbcResource.execute(jdbcRequest);
                        })
                )
                .then("end", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    input.setBody("ParseTppCsv complete");
                });
    }

}
