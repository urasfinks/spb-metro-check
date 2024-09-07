package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Orange;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
{
  "f0" : "Идентификатор",
  "f1" : "№",
  "f2" : "Смена",
  "f3" : "Признак расчета",
  "f4" : "ФД",
  "f5" : "ФП",
  "f6" : "СНО",
  "f7" : "Контакт клиента",
  "f8" : "Список предметов расчета",
  "f9" : "Оплата",
  "f10" : "Сумма",
  "f11" : "Дата",
  "f12" : "Идентификатор в пакете",
  "f13" : "Статус",
  "f14" : "Номер ККТ",
  "f15" : "Группа кассы",
  "f16" : "Наименование доп. реквизита пользователя",
  "f17" : "Значение доп. реквизита пользователя",
  "f18" : "Доп реквизит чека(БСО)",
  "f19" : "Покупатель (клиент)",
  "f20" : "ИНН покупателя (клиента)",
  "f21" : "Номер ТС",
  "f22" : "Адрес расчетов",
  "f23" : "Место расчетов",
  "f24" : "Тип операции",
  "f25" : "Тип чека",
  "f26" : "Способ оплаты",
  "f27" : "ИНН поставщика",
  "f28" : "Признак агента позиций",
  "f29" : "Версия ФФД"
}
* */

@Component
@RequestMapping
public class ParseOrangeCsv implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ParseOrangeCsv(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    AtomicInteger c = new AtomicInteger(0);

    private void addToRequest(Promise promise, Map<String, Object> json, JdbcRequest jdbcRequest) {
        int i = c.incrementAndGet();
        json.put("f14", SpbMetroCheckApplication.expoReplace((String) json.get("f14")));
        json.put("f8", SpbMetroCheckApplication.arrayReplace((String) json.get("f8")));
        json.put("f9", SpbMetroCheckApplication.arrayReplace((String) json.get("f9")));
        json.put("f26", SpbMetroCheckApplication.arrayReplace((String) json.get("f26")));

        String dateLocalString = (String) json.get("f11");
        long dateLocalMs;
        try {
            dateLocalMs = UtilDate.getTimestamp(dateLocalString, "d.M.y H:m") * 1000;
        } catch (Exception e) {
            System.out.println(i);
            System.out.println(UtilJson.toStringPretty(json, "{-}"));
            throw new RuntimeException(e);
        }
        String complexCode = (String) json.get("f21");

        String summa = (String) json.get("f10");

        ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
        Map<String, String> map = servletHandler.getRequestReader().getMap();
        jdbcRequest
                .addArg("date_fof", map.get("date_start"))
                .addArg("date_local", dateLocalMs)
                .addArg("id_transaction", json.get("f0"))
                .addArg("summa", summa.replace(",", "."))
                .addArg("code", complexCode.substring(0,3))
                .addArg("gate", complexCode.substring(3))
                .addArg("f24", json.get("f24"))
                .nextBatch();
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1200000L)
                .then("check", (_, promise) -> SpbMetroCheckApplication.checkStartDate(promise))
                .thenWithResource("loadToDb", JdbcResource.class, "default", (isThreadRun, promise, jdbcResource) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    Map<String, String> name = servletHandler.getRequestReader().getMultiPartFormSubmittedFileName();
                    if (name.get("file").endsWith(".zip")) {
                        ZipInputStream zis = new ZipInputStream(
                                servletHandler.getRequestReader().getMultiPartFormData("file"),
                                Charset.forName("Cp1251")
                        );
                        ZipEntry zipEntry;
                        while ((zipEntry = zis.getNextEntry()) != null) {
                            if (zipEntry.getName().endsWith(".csv")) {
                                doAction(
                                        isThreadRun,
                                        promise,
                                        jdbcResource,
                                        zis
                                );
                                zis.closeEntry();
                                break;
                            }
                            zis.closeEntry();
                        }
                        zis.close();
                    } else {
                        doAction(
                                isThreadRun,
                                promise,
                                jdbcResource,
                                servletHandler.getRequestReader().getMultiPartFormData("file")
                        );
                    }
                });
    }

    public void doAction(
            AtomicBoolean isThreadRun,
            Promise promise,
            JdbcResource jdbcResource,
            InputStream is
    ) throws Throwable {
        SpbMetroCheckApplication.onRead(
                SpbMetroCheckApplication.getCSVReader(is, 1),
                isThreadRun,
                5000,
                listJson -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(Orange.INSERT);
                    listJson.forEach(json -> addToRequest(promise, json, jdbcRequest));
                    Util.logConsole("insert");
                    jdbcResource.execute(jdbcRequest);
                }
        );
    }

}
