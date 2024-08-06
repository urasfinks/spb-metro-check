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
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.jt.Data;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
@RequestMapping("/parseOrangeCsv")
public class ParseOrangeCsv implements PromiseGenerator {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ParseOrangeCsv(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .appendWithResource("loadToDb", JdbcResource.class, "logger", (isThreadRun, promise, jdbcResource) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    try {
                        onRead(isThreadRun, json -> {
                            json.put("f14", SpbMetroCheckApplication.expoReplace((String) json.get("f14")));
                            json.put("f8", SpbMetroCheckApplication.arrayReplace((String) json.get("f8")));
                            json.put("f9", SpbMetroCheckApplication.arrayReplace((String) json.get("f9")));
                            json.put("f26", SpbMetroCheckApplication.arrayReplace((String) json.get("f26")));

                            JdbcRequest jdbcRequest = new JdbcRequest(Data.INSERT);
                            jdbcRequest
                                    .addArg("date_data", System.currentTimeMillis())
                                    .addArg("type_data", "orange")
                                    .addArg("key_data", json.get("f0"))
                                    .addArg("data_data", UtilJson.toStringPretty(json, "{}"));

                            try {
                                jdbcResource.execute(jdbcRequest);
                            } catch (Throwable th) {
                                throw new ForwardException(th);
                            }
                        });
                        input.setBody("ParseOrangeCsv complete");
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                });
    }

    private CSVReader getCSVReader() throws IOException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .withIgnoreQuotations(true)
                .build();
        return new CSVReaderBuilder(new FileReader("web/rrr/orange.csv", Charset.forName("Cp1251")))
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();
    }

    private void onRead(AtomicBoolean isThreadRun, Consumer<Map<String, Object>> onRead) throws CsvValidationException, IOException {
        String[] nextLine;
        while ((nextLine = getCSVReader().readNext()) != null && isThreadRun.get()) {
            Map<String, Object> json = new LinkedHashMap<>();
            for (int i = 0; i < nextLine.length; i++) {
                json.put("f" + i, nextLine[i]);
            }
            onRead.accept(json);
        }
    }

}
