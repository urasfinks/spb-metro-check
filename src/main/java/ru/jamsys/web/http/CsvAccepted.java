package ru.jamsys.web.http;

import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.TPP;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
@RequestMapping
public class CsvAccepted implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CsvAccepted(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {

        return servicePromise.get(index, 700_000L)
                .thenWithResource("loadFromDb", JdbcResource.class, "default", (_, p, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.PROCESSED);
                    jdbcRequest.addArg("processed", List.of("accepted_tpp"));
                    p.setMapRepository("result", jdbcResource.execute(jdbcRequest));
                })
                .then("generateCsv", (_, promise) -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = promise.getRepositoryMap("result", List.class);

                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    HttpServletResponse response = input.getResponse();

                    response.setContentType("text/csv");
                    response.addHeader("Content-Disposition", "attachment;filename=" + getUniqueFileName("accepted_tpp"));

                    CSVWriter csvWriter = new CSVWriter(response.getWriter());
                    AtomicInteger counter = new AtomicInteger(0);
                    String[] fLine = getLineCorrectionFirstLine(result.getFirst());
                    csvWriter.writeNext(fLine);
                    result.forEach(stringObjectMap -> csvWriter.writeNext(getLineCorrection(
                            stringObjectMap,
                            counter,
                            fLine
                    )));
                    csvWriter.flush();
                    csvWriter.close();
                });
    }

    public String getUniqueFileName(String direction) {
        return URLEncoder.encode(direction + "_" + getDate() + ".csv", StandardCharsets.UTF_8);
    }

    public String getDate() {
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        return dtf.format(new Date());
    }

    public String[] getLineCorrectionFirstLine(Map<String, Object> row) {
        String[] array = row.keySet().toArray(new String[0]);
        return Stream.concat(Arrays.stream(new String[]{""}), Arrays.stream(array)).toArray(String[]::new);
    }

    public String[] getLineCorrection(Map<String, Object> row, AtomicInteger counter, String[] fields) {
        String[] result = new String[row.size() + 1];
        for (int i = 0; i < fields.length; i++) {
            if (i == 0) {
                result[i] = counter.incrementAndGet() + "";
            } else {
                result[i] = String.valueOf(row.get(fields[i]));
            }
        }
        return result;
    }

}
