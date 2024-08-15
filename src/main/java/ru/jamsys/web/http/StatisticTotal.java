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


@Component
@RequestMapping
public class StatisticTotal implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public StatisticTotal(ServicePromise servicePromise) {
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
                        -> p.setMapRepository("orange-2", jdbcResource.execute(new JdbcRequest(Orange.STATISTIC_2))))
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setMapRepository("kkt", jdbcResource.execute(new JdbcRequest(KKT.STATISTIC))))
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> {
                    HttpAsyncResponse input = p.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    JdbcRequest jdbcRequest = new JdbcRequest(Total.INSERT);
                    HashMapBuilder<Object, Object> append = new HashMapBuilder<>()
                            .append("tpp", p.getRepositoryMap("tpp", List.class))
                            .append("orange", p.getRepositoryMap("orange", List.class))
                            .append("orange-2", p.getRepositoryMap("orange-2", List.class))
                            .append("kkt", p.getRepositoryMap("kkt", List.class));
                    System.out.println(input.getHttpRequestReader().getMap().getOrDefault("docDate", "-") + "'T'00:00:00");
//                    jdbcRequest.addArg("date_local", input.getHttpRequestReader().getMap().getOrDefault("docDate", "-")+"'T'00:00:00");
//                    jdbcRequest.addArg("data", UtilJson.toStringPretty(append, "{}"));
//                    System.out.println(jdbcRequest.getListArgs());
                    //jdbcResource.execute(jdbcRequest);
                });
    }

}
