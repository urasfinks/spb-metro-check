package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Orange;
import ru.jamsys.jt.TPP;

@Component
@RequestMapping("/processTppTransaction")
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
//        PromiseTask promiseTask = new PromiseTask("eachTransaction", promise, PromiseTaskExecuteType.ASYNC_NO_WAIT_IO, (_, _) -> {
//            System.out.println("ASYNC");
//        });
        return promise
                .appendWithResource("tppCancel", JdbcResource.class, "logger", (_, _, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.CANCEL);
                    try {
                        jdbcResource.execute(jdbcRequest);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                })
                .appendWithResource("tppAccepted", JdbcResource.class, "logger", (_, _, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.ACCEPTED);
                    try {
                        jdbcResource.execute(jdbcRequest);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                })
                .appendWithResource("tppNotOrange", JdbcResource.class, "logger", (_, _, jdbcResource) -> {
//                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.NOT_ORANGE);
//                    try {
//                        jdbcResource.execute(jdbcRequest);
//                    } catch (Throwable th) {
//                        throw new ForwardException(th);
//                    }
                })
                .appendWithResource("tppFnFuture", JdbcResource.class, "logger", (_, _, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.FN_FUTURE);
                    try {
                        jdbcResource.execute(jdbcRequest);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                })
                .appendWithResource("tppFillContinue", JdbcResource.class, "logger", (_, _, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.FILL_CONTINUE);
                    try {
                        jdbcResource.execute(jdbcRequest);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                })
                .appendWithResource("orangeNotTpp", JdbcResource.class, "logger", (_, _, jdbcResource) -> {
//                    JdbcRequest jdbcRequest = new JdbcRequest(Orange.NOT_TPP);
//                    try {
//                        jdbcResource.execute(jdbcRequest);
//                    } catch (Throwable th) {
//                        throw new ForwardException(th);
//                    }
                })
                .appendWithResource("orangeFillContinue", JdbcResource.class, "logger", (_, _, jdbcResource) -> {
//                    JdbcRequest jdbcRequest = new JdbcRequest(Orange.FILL_CONTINUE);
//                    try {
//                        jdbcResource.execute(jdbcRequest);
//                    } catch (Throwable th) {
//                        throw new ForwardException(th);
//                    }
                })
                //.append(promiseTask)
                ;
    }

}
