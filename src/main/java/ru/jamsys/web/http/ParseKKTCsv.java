package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.KKT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
        return servicePromise.get(index, 1_200_000L)
                .thenWithResource("loadToDb", JdbcResource.class, "default", (isThreadRun, promise, jdbcResource) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    SpbMetroCheckApplication.onRead(
                            SpbMetroCheckApplication.getCSVReader(servletHandler.getRequestReader().getMultiPartFormData("file"), 3, "Cp1251"),
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
                                    } catch (Throwable _) {
                                    }
                                    String f2 = (String) stringObjectMap.get("f2");
                                    if (last != null && !f2.isEmpty() && Util.isNumeric(f2)) {
                                        last.getItem().add(stringObjectMap);
                                    }
                                }

                                Map<String, String> map = servletHandler.getRequestReader().getMap();

                                JdbcRequest jdbcRequest = new JdbcRequest(KKT.INSERT);
                                list.forEach(xx -> xx.getItem().forEach(stringObjectMap -> {
                                    String su1 = (String) stringObjectMap.get("f3");
                                    if (su1 != null && !su1.isEmpty()) {
                                        jdbcRequest
                                                .addArg("date_fof", map.get("date_start"))
                                                .addArg("summa", s1)
                                                .addArg("code", xx.getCode())
                                                .addArg("gate", Util.padLeft((String) stringObjectMap.get("f2"), 3, "0"))
                                                .addArg("count_agg", stringObjectMap.get("f3"))
                                                .addArg("summa_agg", ((String) stringObjectMap.get("f4")).replace(",", "."))
                                                .nextBatch();
                                    }
                                    String su2 = (String) stringObjectMap.get("f5");
                                    if (su2 != null && !su2.isEmpty()) {
                                        jdbcRequest
                                                .addArg("date_fof", map.get("date_start"))
                                                .addArg("summa", s2)
                                                .addArg("code", xx.getCode())
                                                .addArg("gate", Util.padLeft((String) stringObjectMap.get("f2"), 3, "0"))
                                                .addArg("count_agg", stringObjectMap.get("f5"))
                                                .addArg("summa_agg", ((String) stringObjectMap.get("f6")).replace(",", "."))
                                                .nextBatch();
                                    }
                                }));
                                jdbcResource.execute(jdbcRequest);
                            }
                    );
                });
    }

}
