package ru.jamsys.web.http;

import com.opencsv.CSVWriter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.KKT;

import java.io.Writer;
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
public class CsvDiffKkt implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CsvDiffKkt(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {

        return servicePromise.get(index, 60_000L)
                .thenWithResource("loadFromDb", JdbcResource.class, "default", (_, p, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(KKT.STATISTIC_DIFF);
                    p.setRepositoryMap("result", jdbcResource.execute(jdbcRequest));
                })
                .then("generateCsv", (_, promise) -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = promise.getRepositoryMap("result", List.class);

                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);

                    servletHandler.setResponseHeader("Content-Type", "text/csv");
                    servletHandler.setResponseHeader("Content-Disposition", "attachment;filename=" + getUniqueFileName("diff_kkt"));

                    Writer responseWriter = servletHandler.getResponseWriter();
                    CSVWriter csvWriter = new CSVWriter(responseWriter, ';', '"', '"', "\n");
                    AtomicInteger counter = new AtomicInteger(0);

                    if (!result.isEmpty()) {
                        byte[] bs = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
                        responseWriter.write(new String(bs));

                        String[] firstLineField = getLineCorrectionFirstLine(result.getFirst());
                        csvWriter.writeNext(firstLineField);
                        result.forEach(stringObjectMap -> csvWriter.writeNext(getLineCorrection(
                                stringObjectMap,
                                counter,
                                firstLineField
                        )));
                    }
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
