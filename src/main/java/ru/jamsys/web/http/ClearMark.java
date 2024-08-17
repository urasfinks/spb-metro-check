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
public class ClearMark implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ClearMark(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        Promise promise = servicePromise.get(index, 1_200_000L);
        return promise
                .thenWithResource("tppClearMark", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(TPP.CLEAR_MARK)))
                .thenWithResource("orangeClearMark", JdbcResource.class, "default", (_, _, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(Orange.CLEAR_MARK)));
    }

}
