package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Station;
import ru.jamsys.jt.TPP;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
{
  "f0" : "Локальная дата поездки",
  "f1" : "Локальное время поездки",
  "f2" : "Дата первой авторизации в банке",
  "f3" : "Время первой авторизации в банке",
  "f4" : "Дата последней авторизации в банке",
  "f5" : "Время последней авторизации в банке",
  "f6" : "Дата успешной авторизации в банке",
  "f7" : "Время успешной авторизации в банке",
  "f8" : "Наименование перевозчика",
  "f9" : "ИНН перевозчика",
  "f10" : "Системный тип билета",
  "f11" : "Тип билета",
  "f12" : "Вид билета",
  "f13" : "Номер билета",
  "f14" : "Регион (муниципалитет)",
  "f15" : "Вид перевозок",
  "f16" : "Вид транспорта",
  "f17" : "Номер маршрута",
  "f18" : "Маршрут",
  "f19" : "Сумма на терминале, ?",
  "f20" : "Базовый тариф, ?",
  "f21" : "Скидка, ?",
  "f22" : "Сумма, ?",
  "f23" : "Способ оплаты",
  "f24" : "HASH карты",
  "f25" : "BIN карты",
  "f26" : "Маска карты",
  "f27" : "PAR",
  "f28" : "Платёжная система",
  "f29" : "Статус оплаты",
  "f30" : "RRN",
  "f31" : "Результат авторизации",
  "f32" : "Код авторизации",
  "f33" : "Дата билета в ТПП",
  "f34" : "Время билета в ТПП",
  "f35" : "Остановка входа",
  "f36" : "Остановка выхода",
  "f37" : "Идентификатор транспортного средства",
  "f38" : "Уникальный идентификатор терминала",
  "f39" : "Серийный номер",
  "f40" : "Номер терминала",
  "f41" : "Кондуктор",
  "f42" : "Водитель",
  "f43" : "Дата открытия смены",
  "f44" : "Время открытия смены",
  "f45" : "Смена",
  "f46" : "Номер рейса",
  "f47" : "Направление движения",
  "f48" : "Уникальный идентификатор операции",
  "f49" : "Идентификатор фискального документа",
  "f50" : "Заводской номер устройства пробившего чек",
  "f51" : "Регистрационный номер устройства пробившего чек",
  "f52" : "Номер фискального накопителя",
  "f53" : "Номер фискального документа",
  "f54" : "Фискальный признак документа",
  "f55" : "Дата регистрации фискального документа в ФН",
  "f56" : "Время регистрации фискального документа в ФН",
  "f57" : "Сумма чека, ?",
  "f58" : "Наименование ОФД",
  "f59" : "Банк Получателя",
  "f60" : "Идентификатор Банка Получателя",
  "f61" : "Наименование ЮЛ",
  "f62" : "Идентификатор ЮЛ",
  "f63" : "Наименование торговой точки",
  "f64" : "Наименование РОС",
  "f65" : "Идентификатор ТСП",
  "f66" : "Идентификатор Операции СБП",
  "f67" : "Идентификатор Транзакции СБП",
  "f68" : "Часовой пояс терминала"
}
* */

@Component
@RequestMapping
public class ParseTppCsv implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ParseTppCsv(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    private void addToRequest(Map<String, Object> json, JdbcRequest jdbcRequest, Map<String, String> station) {
        try {
            String dateLocalString = json.get("f0") + " " + json.get("f1");
            Long dateLocalMs = Util.getTimestamp(dateLocalString, "d.M.y H:m:s") * 1000;

            Long dateFnMs = null;
            String dateFnString = json.get("f55") + " " + json.get("f56");
            if (!dateFnString.trim().isEmpty()) {
                dateFnMs = Util.getTimestamp(dateFnString, "d.M.y H:m:s") * 1000;
            }

            String o = (String) json.get("f17");
            for (String key : station.keySet()) {
                if (station.get(key).contains(o)) {
                    json.put("code", key);
                    break;
                }
            }

            //System.out.println(UtilJson.toStringPretty(json, "{}"));
            jdbcRequest
                    .addArg("date_local", dateLocalMs)
                    .addArg("date_fn", dateFnMs)
                    .addArg("status", json.get("f29"))
                    .addArg("id_transaction", json.get("f48"))
                    .addArg("id_transaction_orange", json.get("f49"))
                    .addArg("summa", json.get("f22"))
                    .addArg("code", json.get("code"))
                    .addArg("gate", Util.padLeft((String) json.get("f35"), 3, "0"))
                    .addArg("f54", json.get("f54"))
                    .nextBatch();

        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1_200_000L)
                .thenWithResource("selectStation", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(Station.SELECT);
                    List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                    Map<String, String> station = new HashMap<>();
                    execute.forEach(stringObjectMap
                            -> station.put((String) stringObjectMap.get("code"), (String) stringObjectMap.get("place")));
                    promise.setRepositoryMap("station", station);

                })
                .thenWithResource("loadToDb", JdbcResource.class, "default", (isThreadRun, promise, jdbcResource) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> station = promise.getRepositoryMap("station", Map.class);
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
                                        jdbcResource,
                                        station,
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
                                jdbcResource,
                                station,
                                servletHandler.getRequestReader().getMultiPartFormData("file")
                        );
                    }
                });
    }

    public void doAction(
            AtomicBoolean isThreadRun,
            JdbcResource jdbcResource,
            Map<String, String> station,
            InputStream is
    ) throws Throwable {
        SpbMetroCheckApplication.onRead(
                SpbMetroCheckApplication.getCSVReader(is, 1),
                isThreadRun, 5000, listJson -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.INSERT);
                    listJson.forEach(json -> addToRequest(json, jdbcRequest, station));
                    Util.logConsole("insert");
                    jdbcResource.execute(jdbcRequest);
                });
    }

}
