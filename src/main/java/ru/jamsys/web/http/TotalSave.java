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
import ru.jamsys.jt.KKT;
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;
import ru.jamsys.jt.Total;

import java.util.List;
import java.util.Map;


@Component
@RequestMapping
public class TotalSave implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public TotalSave(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 10_000L)
                .then("check", (_, _, promise) -> {
                    SpbMetroCheckApplication.checkStartDate(promise);
                    /*
                     * Сохранение сводной работает только по одной дате, но SQL статистики написаны на between
                     * поэтому просто устанавливаем date_end равный date_start
                     * */
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    Map<String, String> map = servletHandler.getRequestReader().getMap();
                    map.put("date_end", map.get("date_start"));
                })
                .thenWithResource(
                        "loadTppStatistic",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> promise.setRepositoryMap("tpp", jdbcResource.execute(
                                        new JdbcRequest(TPP.STATISTIC).addArg(promise
                                                .getRepositoryMapClass(ServletHandler.class)
                                                .getRequestReader()
                                                .getMap()
                                        )
                                )
                        )
                )
                .thenWithResource(
                        "loadOrangeStatistic",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> promise.setRepositoryMap(
                                "orange",
                                jdbcResource.execute(new JdbcRequest(Orange.STATISTIC).addArg(promise
                                                .getRepositoryMapClass(ServletHandler.class)
                                                .getRequestReader()
                                                .getMap()
                                        )
                                )
                        )
                )
                .thenWithResource(
                        "loadOrangeStatistic",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> promise.setRepositoryMap("orange-statistic", jdbcResource.execute(
                                        new JdbcRequest(Orange.STATISTIC_2).addArg(promise
                                                .getRepositoryMapClass(ServletHandler.class)
                                                .getRequestReader()
                                                .getMap()
                                        )
                                )
                        )
                )
                .thenWithResource(
                        "loadTppStatistic",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> promise.setRepositoryMap("kkt", jdbcResource.execute(
                                        new JdbcRequest(KKT.STATISTIC).addArg(promise
                                                .getRepositoryMapClass(ServletHandler.class)
                                                .getRequestReader()
                                                .getMap()
                                        )
                                )
                        )
                )
                .thenWithResource(
                        "loadTppStatistic",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> {
                            ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                            JdbcRequest jdbcRequest = new JdbcRequest(Total.INSERT);
                            HashMapBuilder<Object, Object> append = new HashMapBuilder<>()
                                    .append("tpp", promise.getRepositoryMap(List.class, "tpp"))
                                    .append("orange", promise.getRepositoryMap(List.class, "orange"))
                                    .append("orange-statistic", promise.getRepositoryMap(List.class, "orange-statistic"))
                                    .append("kkt", promise.getRepositoryMap(List.class, "kkt"));

                            String date = servletHandler.getRequestReader().getMap().getOrDefault("date_start", "-");
                            append.forEach((key, value) -> {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> list = (List<Map<String, Object>>) value;
                                list.forEach(stringObjectMap -> {
                                    String title = (String) stringObjectMap.get("title");
                                    if (title == null) {
                                        title = "Статус транзакции не определён";
                                    }
                                    jdbcRequest
                                            .addArg("date_fof", date)
                                            .addArg("group_key", key)
                                            .addArg("group_title", title)
                                            .addArg("group_count", stringObjectMap.get("count"))
                                            .nextBatch();
                                });
                            });
                            jdbcResource.execute(jdbcRequest);
                        })
                .extension(SpbMetroCheckApplication::addErrorHandler);
    }

}
