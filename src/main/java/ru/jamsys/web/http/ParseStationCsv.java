package ru.jamsys.web.http;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/*

* */

@Component
@RequestMapping("/parseStationCsv")
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
            th.printStackTrace();
            throw new ForwardException(th);
        }
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 700_000L)
                .thenWithResource("loadToDb", JdbcResource.class, "default", (isThreadRun, promise, jdbcResource) -> {
                    try {
                        onRead(isThreadRun, 5000, listJson -> {
                            JdbcRequest jdbcRequest = new JdbcRequest(Station.INSERT);
                            listJson.forEach(json -> addToRequest(json, jdbcRequest));
                            try {
                                Util.logConsole("insert");
                                jdbcResource.execute(jdbcRequest);
                            } catch (Throwable th) {
                                th.printStackTrace();
                                System.out.println(jdbcRequest.getListArgs());
                                throw new ForwardException(th);
                            }
                        });
                    } catch (Throwable th) {
                        th.printStackTrace();
                        throw new ForwardException(th);
                    }
                })
                .then("end", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    input.setBody("ParseTppCsv complete");
                });
    }

    private CSVReader getCSVReader() throws IOException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .withIgnoreQuotations(false)
                .build();
        return new CSVReaderBuilder(new FileReader("web/station-2.csv", Charset.forName("UTF-8")))
                .withSkipLines(2)
                .withCSVParser(parser)
                .build();
    }

    private void onRead(AtomicBoolean isThreadRun, int sizeBatch, Consumer<List<Map<String, Object>>> onRead) throws CsvValidationException, IOException {
        String[] nextLine;
        CSVReader csvReader = getCSVReader();
        int curSizeBatch = 0;
        List<Map<String, Object>> batch = new ArrayList<>();
        while ((nextLine = csvReader.readNext()) != null && isThreadRun.get()) {
            Map<String, Object> json = new LinkedHashMap<>();
            for (int i = 0; i < nextLine.length; i++) {
                json.put("f" + i, nextLine[i]);
            }
            batch.add(json);
            curSizeBatch++;
            if (curSizeBatch > sizeBatch) {
                onRead.accept(batch);
                batch = new ArrayList<>();
                curSizeBatch = 0;
            }
            //break;
        }
        if (!batch.isEmpty()) {
            onRead.accept(batch);
        }
    }

}
