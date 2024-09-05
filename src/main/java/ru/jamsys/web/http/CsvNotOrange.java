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
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.TPP;

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
public class CsvNotOrange implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CsvNotOrange(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {

        return servicePromise.get(index, 60_000L)
                .thenWithResource("loadFromDb", JdbcResource.class, "default", (_, p, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.PROCESSED);
                    jdbcRequest.addArg("processed", List.of("not_orange1"));
                    p.setRepositoryMap("result", jdbcResource.execute(jdbcRequest));
                })
                .then("generateCsv", (_, promise) -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = promise.getRepositoryMap("result", List.class);

                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);

                    servletHandler.setResponseHeader("Content-Type", "text/csv");
                    servletHandler.setResponseHeader("Content-Disposition", "attachment;filename=" + getUniqueFileName("not_orange"));

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
                .onComplete((_, promise) -> promise.getRepositoryMapClass(ServletHandler.class).getCompletableFuture().complete(null));
    }

    public String getUniqueFileName(String direction) {
        return URLEncoder.encode(direction + "_" + getDate() + ".csv", StandardCharsets.UTF_8);
    }

    public String getDate() {
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        return dtf.format(new Date());
    }

}
