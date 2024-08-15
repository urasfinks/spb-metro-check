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
import ru.jamsys.jt.Station;
import ru.jamsys.jt.TPP;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequestMapping
public class CsvRefund implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CsvRefund(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {

        return servicePromise.get(index, 700_000L)
                .thenWithResource("selectStation", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(Station.SELECT);
                    List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                    Map<String, String> station = new HashMap<>();
                    execute.forEach(stringObjectMap
                            -> station.put((String) stringObjectMap.get("code"), (String) stringObjectMap.get("place")));
                    promise.setMapRepository("station", station);
                })
                .thenWithResource("loadFromDb", JdbcResource.class, "default", (_, p, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.PROCESSED);
                    jdbcRequest.addArg("processed", List.of("fn_future"));
                    p.setMapRepository("result", jdbcResource.execute(jdbcRequest));
                })
                .then("generateCsv", (_, promise) -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = promise.getRepositoryMap("result", List.class);

                    @SuppressWarnings("unchecked")
                    Map<String, String> station = promise.getRepositoryMap("station", Map.class);

                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    HttpServletResponse response = input.getResponse();

                    response.setContentType("text/csv");
                    response.addHeader("Content-Disposition", "attachment;filename=" + getUniqueFileName("refund"));

                    CSVWriter csvWriter = new CSVWriter(response.getWriter());
                    AtomicInteger counter = new AtomicInteger(0);

                    if (!result.isEmpty()) {
                        csvWriter.writeNext(getLineReturnFirstLine());
                        result.forEach(stringObjectMap -> csvWriter.writeNext(getLineReturn(
                                stringObjectMap,
                                counter,
                                station
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

    public String[] getLineReturnFirstLine() {
        return new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""};
    }

    public String[] getLineReturn(Map<String, Object> row, AtomicInteger counter, Map<String, String> station) {
        String complexCode = "" + row.get("code") + row.get("gate");
        return new String[]{
                counter.incrementAndGet() + "",
                "2",
                "0",
                "",
                "[Услуга по перевозке, 1, " + String.format("%.2f", row.get("summa")) + ", 6, 4, 4]",
                "2",
                String.format("%.2f", row.get("summa")),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                complexCode,
                "Россия, город Москва, Алтуфьевское шоссе, д.33Г",
                station.get(row.get("code")) + complexCode,
                row.get("f54") + "",
                "",
                "",
                "",
                "",
                "",
                ""
        };
    }

}
