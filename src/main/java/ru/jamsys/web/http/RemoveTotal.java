package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Total;


@Component
@RequestMapping
public class RemoveTotal implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public RemoveTotal(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 10_000L)
                .thenWithResource("loadTppStatistic", JdbcResource.class, "default", (_, p, jdbcResource)
                        -> {
                    ServletHandler servletHandler = p.getRepositoryMapClass(ServletHandler.class);
                    JdbcRequest jdbcRequest = new JdbcRequest(Total.REMOVE);
                    String date = servletHandler.getRequestReader().getMap().getOrDefault("docDate", "-");
                    jdbcRequest.addArg("date_local", date);
                    jdbcResource.execute(jdbcRequest);
                });
    }

}
