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
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Component
@RequestMapping
public class DownloadCorrection implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public DownloadCorrection(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 700_000L)
                .thenWithResource("loadToDb", JdbcResource.class, "default", (_, _, jdbcResource) -> {
//                    JdbcRequest jdbcRequest = new JdbcRequest(DB.CREATE);
//                    try {
//                        jdbcResource.execute(jdbcRequest);
//                    } catch (Throwable th) {
//                        th.printStackTrace();
//                        throw new ForwardException(th);
//                    }
                })
                .then("end", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    HttpServletResponse response = input.getResponse();

                    //response.setContentType("application/x-download;charset=utf-8");
                    response.setContentType("text/csv");
                    SimpleDateFormat dtf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                    String curDateStr = dtf.format(new Date());
                    String fileName = URLEncoder.encode("Привет страна_" + curDateStr + ".csv", StandardCharsets.UTF_8);
                    response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
                    CSVWriter csvWriter = new CSVWriter(response.getWriter());
                    csvWriter.writeNext(getLine(null));
                    csvWriter.flush();
                    csvWriter.close();
                });
    }

    public String[] getLine(Map<String, Object> row) {
        String[] entries = {"book", "coin", "pencil", "cup"};
        return entries;
    }

}
