package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.KKT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@Component
@RequestMapping
public class ParseKKTCsv implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ParseKKTCsv(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    AtomicInteger c = new AtomicInteger(0);

    private void addToRequest(Map<String, Object> json, JdbcRequest jdbcRequest) {
        int i = c.incrementAndGet();
        json.put("f14", SpbMetroCheckApplication.expoReplace((String) json.get("f14")));
        json.put("f8", SpbMetroCheckApplication.arrayReplace((String) json.get("f8")));
        json.put("f9", SpbMetroCheckApplication.arrayReplace((String) json.get("f9")));
        json.put("f26", SpbMetroCheckApplication.arrayReplace((String) json.get("f26")));

        String dateLocalString = (String) json.get("f11");
        long dateLocalMs;
        try {
            dateLocalMs = Util.getTimestamp(dateLocalString, "d.M.y H:m") * 1000;
        } catch (Exception e) {
            System.out.println(i);
            System.out.println(UtilJson.toStringPretty(json, "{-}"));
            throw new RuntimeException(e);
        }
        String complexCode = (String) json.get("f21");

        String summa = (String) json.get("f10");

        jdbcRequest
                .addArg("date_local", dateLocalMs)
                .addArg("id_transaction", json.get("f0"))
                .addArg("summa", summa.replace(",", "."))
                .addArg("code", complexCode.substring(0, 3))
                .addArg("gate", complexCode.substring(3))
                .addArg("f24", json.get("f24"))
                .addArg("data", "{}")
                //.addArg("data", UtilJson.toStringPretty(json, "{}"))
                .nextBatch();
    }

    @ToString
    @Getter
    public static class XX {

        String code;

        List<Map<String, Object>> item = new ArrayList<>();

        public XX(String code) {
            this.code = code.substring(0, 3);
        }

    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 700_000L)
                .thenWithResource("loadToDb", JdbcResource.class, "default", (isThreadRun, promise, jdbcResource) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    SpbMetroCheckApplication.onRead(
                            SpbMetroCheckApplication.getCSVReader(input.getHttpRequestReader().getMultiPartFormData("file"), 3, "Cp1251"),
                            isThreadRun,
                            5000,
                            listJson -> {
                                String s1 = ((String) listJson.getFirst().get("f3")).replace(",", ".");
                                String s2 = ((String) listJson.getFirst().get("f5")).replace(",", ".");

                                List<XX> list = new ArrayList<>();
                                for (Map<String, Object> stringObjectMap : listJson) {
                                    String f0 = (String) stringObjectMap.get("f0");
                                    if (f0.length() == 4 && Util.isNumeric(f0)) {
                                        list.add(new XX(f0));
                                    }
                                    XX last = null;
                                    try {
                                        last = list.getLast();
                                    } catch (Throwable th) {
                                    }
                                    String f2 = (String) stringObjectMap.get("f2");
                                    if (last != null && !f2.isEmpty() && Util.isNumeric(f2)) {
                                        last.getItem().add(stringObjectMap);
                                    }
                                }
                                JdbcRequest jdbcRequest = new JdbcRequest(KKT.INSERT);
                                list.forEach(xx -> xx.getItem().forEach(stringObjectMap -> {
                                    jdbcRequest
                                            .addArg("summa", s1)
                                            .addArg("code", xx.getCode())
                                            .addArg("gate", Util.padLeft((String) stringObjectMap.get("f2"), 3, "0"))
                                            .addArg("count_agg", stringObjectMap.get("f3"))
                                            .addArg("summa_agg", ((String) stringObjectMap.get("f4")).replace(",", "."))
                                            .nextBatch();
                                    jdbcRequest
                                            .addArg("summa", s2)
                                            .addArg("code", xx.getCode())
                                            .addArg("gate", Util.padLeft((String) stringObjectMap.get("f2"), 3, "0"))
                                            .addArg("count_agg", stringObjectMap.get("f5"))
                                            .addArg("summa_agg", ((String) stringObjectMap.get("f6")).replace(",", "."))
                                            .nextBatch();
                                }));
                                jdbcResource.execute(jdbcRequest);
                            }
                    );
                });
    }

}
