package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.KKT;

import java.util.List;


@Component
@RequestMapping
public class StatisticKkt implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public StatisticKkt(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 10_000L)
                .then("check", (_, _, promise) -> SpbMetroCheckApplication.checkDateRangeRequest(promise))
                .thenWithResource(
                        "loadTppStatistic",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> promise.setRepositoryMap("kkt", jdbcResource.execute(
                                new JdbcRequest(KKT.STATISTIC)
                                        .addArg(promise
                                                .getRepositoryMapClass(ServletHandler.class)
                                                .getRequestReader()
                                                .getMap())
                        )))
                .onComplete((_, _, p) -> {
                    ServletHandler ar = p.getRepositoryMapClass(ServletHandler.class);
                    ar.setResponseBodyFromMap(new HashMapBuilder<>()
                            .append("kkt", p.getRepositoryMap(List.class, "kkt"))
                    );
                    ar.responseComplete();
                })
                .extension(SpbMetroCheckApplication::addErrorHandler);
    }

}
