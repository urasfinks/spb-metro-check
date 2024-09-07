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
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;

@Component
@RequestMapping
public class MarkingTransaction implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public MarkingTransaction(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1_200_000L)
                .thenWithResource("tppCancel", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.CANCEL)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ))
                .thenWithResource("tppAccepted", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.ACCEPTED_0)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ))
                .thenWithResource("tppAccepted", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.ACCEPTED)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ))
                .thenWithResource("tppNotOrange", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.NOT_ORANGE)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ))
                .thenWithResource("tppFnFuture", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.FN_FUTURE)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ))
                .thenWithResource("tppFillContinue", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.FILL_CONTINUE)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ))
                .thenWithResource("orangeNotTpp", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(Orange.NOT_TPP)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ))
                .thenWithResource("orangeFillContinue", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(Orange.FILL_CONTINUE)
                        .addArg(promise.getRepositoryMapClass(ServletHandler.class).getRequestReader().getMap())
                ));
    }

}
