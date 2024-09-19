package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
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
public class TotalRemove implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public TotalRemove(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 10_000L)
                .then("check", (_, _, promise) -> SpbMetroCheckApplication.checkDateRangeRequest(promise))
                .thenWithResource(
                        "remove",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> jdbcResource.execute(new JdbcRequest(Total.REMOVE).addArg(promise
                                        .getRepositoryMapClass(ServletHandler.class)
                                        .getRequestReader()
                                        .getMap()
                                )
                        )
                )
                .extension(SpbMetroCheckApplication::addErrorHandler);
    }

}
