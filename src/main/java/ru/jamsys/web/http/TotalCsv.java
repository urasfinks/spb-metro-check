package ru.jamsys.web.http;

import com.opencsv.CSVWriter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.jt.Total;

import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequestMapping
public class TotalCsv implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public TotalCsv(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {

        return servicePromise.get(index, 60_000L)
                .then("check", (_, _, promise) -> SpbMetroCheckApplication.checkDateRangeRequest(promise))
                .thenWithResource("loadFromDb", JdbcResource.class, "default", (_, _, promise, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(Total.TOTAL)
                            .addArg(promise
                                    .getRepositoryMapClass(ServletHandler.class)
                                    .getRequestReader()
                                    .getMap());
                    promise.setRepositoryMap("result", jdbcResource.execute(jdbcRequest));
                })
                .then("generateCsv", (_, _, promise) -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = promise.getRepositoryMap(List.class, "result");
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);

                    servletHandler.setResponseHeader("Content-Type", "text/csv");
                    servletHandler.setResponseHeader("Content-Disposition", "attachment;filename=" + getUniqueFileName("total"));

                    Writer responseWriter = servletHandler.getResponseWriter();
                    CSVWriter csvWriter = new CSVWriter(responseWriter, ';', '"', '"', "\n");
                    AtomicInteger counter = new AtomicInteger(0);

                    if (!result.isEmpty()) {
                        byte[] bs = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
                        responseWriter.write(new String(bs));

                        String[] fLine = SpbMetroCheckApplication.getLineCorrectionFirstLine(result.getFirst());
                        csvWriter.writeNext(SpbMetroCheckApplication.headerReplace(fLine));
                        result.forEach(stringObjectMap -> csvWriter.writeNext(SpbMetroCheckApplication.getLineCorrection(
                                stringObjectMap,
                                counter,
                                fLine
                        )));
                    }
                    csvWriter.flush();
                    csvWriter.close();
                })
                .onComplete((_, _, promise) -> promise.getRepositoryMapClass(ServletHandler.class).getCompletableFuture().complete(null))
                .extension(SpbMetroCheckApplication::addErrorHandler);
    }

    public String getUniqueFileName(String direction) {
        return URLEncoder.encode(direction + "_" + getDate() + ".csv", StandardCharsets.UTF_8);
    }

    public String getDate() {
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        return dtf.format(new Date());
    }

}
