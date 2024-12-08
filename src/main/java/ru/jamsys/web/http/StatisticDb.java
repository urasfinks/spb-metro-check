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
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;

import java.util.List;
import java.util.Map;


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
                .then("check", (_, _, promise) -> SpbMetroCheckApplication.checkDateRangeRequest(promise))
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, _, promise, jdbcResource) -> {
                            ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                            List<Map<String, Object>> execute = jdbcResource.execute(
                                    new JdbcRequest(TPP.STATISTIC)
                                            .addArg(servletHandler.getRequestReader().getMap())
                            );
                            promise.setRepositoryMap("tpp", execute);
                        }
                )
                .thenWithResource("loadOrangeStatistic", JdbcResource.class, "default", (_, _, promise, jdbcResource) -> {
                            ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                            promise.setRepositoryMap("orange", jdbcResource.execute(
                                    new JdbcRequest(Orange.STATISTIC)
                                            .addArg(servletHandler.getRequestReader().getMap())
                            ));
                        }
                )
                .thenWithResource("loadOrangeStatistic", JdbcResource.class, "default", (_, _, promise, jdbcResource) -> {
                            ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                            promise.setRepositoryMap("orange-agg", jdbcResource.execute(
                                    new JdbcRequest(Orange.STATISTIC_2)
                                            .addArg(servletHandler.getRequestReader().getMap())
                            ));
                        }
                )
                .onComplete((_, _, p) -> {
                    ServletHandler servletHandler = p.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseBodyFromMap(new HashMapBuilder<>()
                            .append("tpp", p.getRepositoryMap(List.class, "tpp"))
                            .append("orange", p.getRepositoryMap(List.class, "orange"))
                            .append("orange-agg", p.getRepositoryMap(List.class, "orange-agg")));
                    servletHandler.responseComplete();
                })
                .extension(SpbMetroCheckApplication::addErrorHandler);
    }

}
