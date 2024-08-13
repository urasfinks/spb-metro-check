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
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;

import java.util.List;


@Component
@RequestMapping
public class StatisticDb implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public StatisticDb(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 10_000L)
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setMapRepository("tpp", jdbcResource.execute(new JdbcRequest(TPP.STATISTIC))))
                .thenWithResource("loadOrangeStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setMapRepository("orange", jdbcResource.execute(new JdbcRequest(Orange.STATISTIC))))
                .onComplete((_, p) -> {
                    HttpAsyncResponse ar = p.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    ar.setBodyFromMap(new HashMapBuilder<>()
                            .append("tpp", p.getRepositoryMap("tpp", List.class))
                            .append("orange", p.getRepositoryMap("orange", List.class))
                    );
                    ar.complete();
                });
    }

}
