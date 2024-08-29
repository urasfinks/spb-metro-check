package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.KKT;
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;
import ru.jamsys.jt.Total;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


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
                        -> p.setRepositoryMap("tpp", jdbcResource.execute(new JdbcRequest(TPP.STATISTIC))))
                .thenWithResource("loadOrangeStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setRepositoryMap("orange", jdbcResource.execute(new JdbcRequest(Orange.STATISTIC))))
                .thenWithResource("loadOrangeStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setRepositoryMap("orange-agg", jdbcResource.execute(new JdbcRequest(Orange.STATISTIC_2))))
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> p.setRepositoryMap("kkt", jdbcResource.execute(new JdbcRequest(KKT.STATISTIC))))
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> {
                    ServletHandler servletHandler = p.getRepositoryMapClass(ServletHandler.class);
                    JdbcRequest jdbcRequest = new JdbcRequest(Total.INSERT);
                    HashMapBuilder<Object, Object> append = new HashMapBuilder<>()
                            .append("tpp", p.getRepositoryMap("tpp", List.class))
                            .append("orange", p.getRepositoryMap("orange", List.class))
                            .append("orange-agg", p.getRepositoryMap("orange-agg", List.class))
                            .append("kkt", p.getRepositoryMap("kkt", List.class));

                    String date = servletHandler.getRequestReader().getMap().getOrDefault("docDate", "-");
                    AtomicLong money = new AtomicLong();
                    append.forEach((key, value) -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) value;
                        list.forEach(stringObjectMap -> {
                            jdbcRequest
                                    .addArg("date_local", date)
                                    .addArg("group_key", key)
                                    .addArg("group_title", stringObjectMap.get("title"))
                                    .addArg("group_count", stringObjectMap.get("count"))
                                    .nextBatch();
                            if (stringObjectMap.get("title").equals("Приход")) {
                                money.set(Long.parseLong(stringObjectMap.get("count") + ""));
                            }
                        });
                    });

                    jdbcRequest
                            .addArg("date_local", date)
                            .addArg("group_key", "orange")
                            .addArg("group_title", "money")
                            .addArg("group_count", new BigDecimal(money.get()).multiply(new BigDecimal("0.1")))
                            .nextBatch();
                    jdbcResource.execute(jdbcRequest);
                });
    }

}
