package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;

@Component
@RequestMapping
public class ProcessTppTransaction implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ProcessTppTransaction(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        Promise promise = servicePromise.get(index, 700_000L);
        return promise
                .appendWithResource("tppCancel", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.CANCEL)))
                .appendWithResource("tppAccepted", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.ACCEPTED)))
                .appendWithResource("tppNotOrange", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.NOT_ORANGE)))
                .appendWithResource("tppFnFuture", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.FN_FUTURE)))
                .appendWithResource("tppFillContinue", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.FILL_CONTINUE)))
                .appendWithResource("orangeNotTpp", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(Orange.NOT_TPP)))
                .appendWithResource("orangeFillContinue", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(Orange.FILL_CONTINUE)));
    }

}
