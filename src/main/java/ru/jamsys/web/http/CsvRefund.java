package ru.jamsys.web.http;

import com.opencsv.CSVWriter;
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
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.jt.Station;
import ru.jamsys.jt.TPP;

import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequestMapping
public class CsvRefund implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CsvRefund(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {

        return servicePromise.get(index, 60_000L)
                .then("check", (_, _, promise) -> SpbMetroCheckApplication.checkDateRangeRequest(promise))
                .thenWithResource(
                        "selectStation",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> {
                            JdbcRequest jdbcRequest = new JdbcRequest(Station.SELECT);
                            List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                            Map<String, String> station = new HashMap<>();
                            execute.forEach(stringObjectMap -> station.put(
                                    (String) stringObjectMap.get("code"),
                                    (String) stringObjectMap.get("place"))
                            );
                            promise.setRepositoryMap("station", station);
                        }
                )
                .thenWithResource(
                        "loadFromDb",
                        JdbcResource.class,
                        "default",
                        (_, _, promise, jdbcResource) -> promise.setRepositoryMap("result", jdbcResource.execute(
                                new JdbcRequest(TPP.PROCESSED)
                                        .addArg(promise
                                                .getRepositoryMapClass(ServletHandler.class)
                                                .getRequestReader()
                                                .getMap())
                                        .addArg("processed", List.of("fn_future")))))
                .then("generateCsv", (_, _, promise) -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = promise.getRepositoryMap(List.class, "result");

                    @SuppressWarnings("unchecked")
                    Map<String, String> station = promise.getRepositoryMap(Map.class, "station");

                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);

                    servletHandler.setResponseHeader("Content-Type", "text/csv");
                    servletHandler.setResponseHeader("Content-Disposition", "attachment;filename=" + getUniqueFileName("refund"));

                    Writer responseWriter = servletHandler.getResponseWriter();
                    CSVWriter csvWriter = new CSVWriter(responseWriter, ';', '"', '"', "\n");
                    AtomicInteger counter = new AtomicInteger(0);

                    if (!result.isEmpty()) {
                        byte[] bs = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
                        responseWriter.write(new String(bs));

                        csvWriter.writeNext(getLineReturnFirstLine());
                        result.forEach(stringObjectMap -> csvWriter.writeNext(getLineReturn(
                                stringObjectMap,
                                counter,
                                station
                        )));
                    }

                    csvWriter.flush();
                    csvWriter.close();
                })
                .onComplete((_, _, promise) -> promise.getRepositoryMapClass(ServletHandler.class).getCompletableFuture().complete(null));
    }

    public String getUniqueFileName(String direction) {
        return URLEncoder.encode(direction + "_" + getDate() + ".csv", StandardCharsets.UTF_8);
    }

    public String getDate() {
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        return dtf.format(new Date());
    }

    public String[] getLineReturnFirstLine() {
        return new String[]{
                "id\nОбязательное поле\nИдентификатор документа\nСтрока от 1 до 64 символов",
                "type\nОбязательное поле\nПризнак расчета, 1054\nЧисло от 1 до 4:\n1. Приход\n2. Возврат прихода\n3. Расход\n4. Возврат расхода",
                "taxationSystem\nОбязательное поле\nСистема налогообложения, 1055\nЧисло от 0 до 5:\n0 – Общая, ОСН\n1 – Упрощенная доход, УСН доход\n2 – Упрощенная доход минус расход, УСН доход - расход\n3 – Единый налог на вмененный доход, ЕНВД\n4 – Единый сельскохозяйственный налог, ЕСН\n5 – Патентная система налогообложения, Патент",
                "customerContact\nТелефон или электронный адрес\nпокупателя, 1008\nСтрока от 1 до 64 символов, формат +{Ц} или {С}@{C}\nПример: +79991112233 или example@example.ru",
                "positions\nОбязательное поле\nСписок предметов расчета, 1059\nМассив структур не более 170 элементов, вида \n[text, quantity, price, tax, paymentMethodType, paymentSubjectType, nomenclatureCode, supplierInfoName, supplierInfoPhoneNumbers, supplierINN], […], …, […] ",
                "paymentType\nОбязательное поле\nТип оплаты\nЧисло от 1 до 16:\n1 – сумма по чеку наличными, 1031\n2 – сумма по чеку электронными, 1081\n14 – сумма по чеку предоплатой (зачетом аванса и (или) предыдущих платежей), 1215\n15 – сумма по чеку постоплатой (в кредит), 1216\n16 – сумма по чеку (БСО) встречным предоставлением, 1217",
                "paymentAmount\nОбязательное поле\nСумма оплаты\nДесятичное число с точностью до 2\nсимволов после точки",
                "agentType\nПризнак агента, 1057.\nЧисло от 1 до 127, где номер бита обозначает,\nчто оказывающий услугу покупателю (клиенту)\nпользователь является:\n0 – банковский платежный агент\n1 – банковский платежный субагент\n2 – платежный агент\n3 – платежный субагент\n4 – поверенный\n5 – комиссионер\n6 – иной агент\nКассовый чек (БСО) может  содержать\nреквизиты «признак агента» (тег 1057),\nтолько если отчет о регистрации и (или)\nтекущий отчет о перерегистрации содержит\nреквизит «признак агента» (тег 1057),\nимеющий значение, идентичное значению реквизита\n«признак агента» (тег 1057) кассового чека. ",
                "paymentTransferOperatorPhoneNumbers\nТелефон оператора перевода, 1075 \nМассив строк длиной от 1 до 19 символов,\nформат +{Ц}, вида […], […], …, […] ",
                "paymentAgentOperation\nОперация платежного агента, 1044 \nСтрока длиной от 1 до 24 символов",
                "paymentAgentPhoneNumbers\nТелефон платежного агента, 1073\nМассив строк длиной от 1 до 19 символов,\nформат +{Ц}, вида […], […], …, […] ",
                "paymentOperatorPhoneNumbers\nТелефон оператора по приему платежей, 1074 \nМассив строк длиной от 1 до 19 символов,\nформат +{Ц}, вида […], […], …, […]",
                "paymentOperatorName\nНаименование оператора перевода, 1026 \nСтрока длиной от 1 до 64 символов",
                "paymentOperatorAddress\nАдрес оператора перевода, 1005 \nСтрока длиной от 1 до 243 символов",
                "paymentOperatorINN\nИНН оператора перевода, 1016 \nСтрока длиной от 10 до 12 символов,\nформат ЦЦЦЦЦЦЦЦЦЦ",
                "supplierPhoneNumbers\nТелефон поставщика, 1171\nМассив строк длиной от 1 до 19 символов,\nформат +{Ц}, вида […], […], …, […] ",
                "automatNumber\nОбязательное поле\nНомер автомата, 1036\nСтрока длиной от 1 до 20 символов",
                "settlementAddress\nОбязательное поле\nАдрес расчетов, 1009 \nСтрока длиной от 1 до 243 символов",
                "settlementPlace\nОбязательное поле\nМесто расчетов, 1187\nСтрока длиной от 1 до 243 символов",
                "additionalAttribute\nДополнительный реквизит чека (БСО), 1192\nСтрока длиной от 1 до 16 символов",
                "additionalUserAttributeName\nНаименование дополнительногоЫ реквизита пользователя, 1085\nСтрока длиной от 1 до 64 символов",
                "additionalUserAttributeValue\nЗначение дополнительного реквизита пользователя, 1086\nСтрока длиной от 1 до 234 символов",
                "customer\nПокупатель (клиент), 1227\nСтрока длиной от 1 до 243 символов",
                "customerINN\nИНН покупателя (клиента), 1228\nСтрока длиной от 10 до 12 символов,\nформат ЦЦЦЦЦЦЦЦЦЦ",
                "cashier\nКассир, 1021\nСтрока от 1 до 64 символов",
                "cashierINN\nИНН кассира, 1203\nСтрока длиной 12 символов,\nформат ЦЦЦЦЦЦЦЦЦЦ"
        };
    }

    public String[] getLineReturn(Map<String, Object> row, AtomicInteger counter, Map<String, String> station) {
        String complexCode = "" + row.get("code") + row.get("gate");
        String summa = String.format("%.2f", row.get("summa")).replace(",", ".").replace(".00", "");
        return new String[]{
                counter.incrementAndGet() + "",
                "2",
                "0",
                "",
                "[Услуга по перевозке, 1, " + summa + ", 6, 4, 4]",
                "2",
                summa,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                complexCode,
                "Россия, город Москва, Алтуфьевское шоссе, д.33Г",
                station.get(row.get("code")) + complexCode,
                row.get("f54") + "",
                "",
                "",
                "",
                "",
                "",
                ""
        };
    }

}
