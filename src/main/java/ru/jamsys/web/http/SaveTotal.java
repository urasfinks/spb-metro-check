package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.KKT;
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;
import ru.jamsys.jt.Total;

import java.util.List;
import java.util.Map;


@Component
@RequestMapping
public class SaveTotal implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public SaveTotal(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 10_000L)
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setMapRepository("tpp", jdbcResource.execute(new JdbcRequest(TPP.STATISTIC))))
                .thenWithResource("loadOrangeStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setMapRepository("orange", jdbcResource.execute(new JdbcRequest(Orange.STATISTIC))))
                .thenWithResource("loadOrangeStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setMapRepository("orange-agg", jdbcResource.execute(new JdbcRequest(Orange.STATISTIC_2))))
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setMapRepository("kkt", jdbcResource.execute(new JdbcRequest(KKT.STATISTIC))))
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> {
                    HttpAsyncResponse input = p.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    JdbcRequest jdbcRequest = new JdbcRequest(Total.INSERT);
                    HashMapBuilder<Object, Object> append = new HashMapBuilder<>()
                            .append("tpp", p.getRepositoryMap("tpp", List.class))
                            .append("orange", p.getRepositoryMap("orange", List.class))
                            .append("orange-agg", p.getRepositoryMap("orange-agg", List.class))
                            .append("kkt", p.getRepositoryMap("kkt", List.class));

                    String date = input.getHttpRequestReader().getMap().getOrDefault("docDate", "-");
                    append.forEach((key, value) -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) value;
                        list.forEach(stringObjectMap -> jdbcRequest
                                .addArg("date_local", date)
                                .addArg("group_key", key)
                                .addArg("group_title", stringObjectMap.get("title"))
                                .addArg("group_count", stringObjectMap.get("count"))
                                .nextBatch());
                    });
                    jdbcResource.execute(jdbcRequest);
                });
    }

}
