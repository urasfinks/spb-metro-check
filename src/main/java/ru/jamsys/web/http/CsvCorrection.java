package ru.jamsys.web.http;

import com.opencsv.CSVWriter;
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
public class CsvCorrection implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CsvCorrection(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {

        return servicePromise.get(index, 60_000L)
                .thenWithResource("selectStation", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(Station.SELECT);
                    List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                    Map<String, String> station = new HashMap<>();
                    execute.forEach(stringObjectMap
                            -> station.put((String) stringObjectMap.get("code"), (String) stringObjectMap.get("place")));
                    promise.setRepositoryMap("station", station);
                })
                .thenWithResource("loadFromDb", JdbcResource.class, "default", (_, p, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TPP.PROCESSED);
                    jdbcRequest.addArg("processed", List.of("fn_future", "not_orange"));
                    p.setRepositoryMap("result", jdbcResource.execute(jdbcRequest));
                })
                .then("generateCsv", (_, promise) -> {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = promise.getRepositoryMap("result", List.class);

                    @SuppressWarnings("unchecked")
                    Map<String, String> station = promise.getRepositoryMap("station", Map.class);

                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);

                    servletHandler.setResponseHeader("Content-Type", "text/csv");
                    servletHandler.setResponseHeader("Content-Disposition", "attachment;filename=" + getUniqueFileName("correction"));

                    Writer responseWriter = servletHandler.getResponseWriter();
                    CSVWriter csvWriter = new CSVWriter(responseWriter, ';', '"', '"', "\n");
                    AtomicInteger counter = new AtomicInteger(0);

                    if (!result.isEmpty()) {
                        byte[] bs = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
                        responseWriter.write(new String(bs));

                        csvWriter.writeNext(getLineFirstLine());
                        String docNumber = servletHandler.getRequestReader().getMap().getOrDefault("docNum", "0");
                        result.forEach(stringObjectMap -> csvWriter.writeNext(getLine(
                                stringObjectMap,
                                counter,
                                servletHandler.getRequestReader().getMap().getOrDefault("docDate", "-") + "T00:00:00",
                                Integer.parseInt(docNumber),
                                station
                        )));
                    }

                    csvWriter.flush();
                    csvWriter.close();
                })
                .onComplete((_, promise) -> promise.getRepositoryMapClass(ServletHandler.class).getCompletableFuture().complete(null));
    }

    public String getUniqueFileName(String direction) {
        return URLEncoder.encode(direction + "_" + getDate() + ".csv", StandardCharsets.UTF_8);
    }

    public String getDate() {
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        return dtf.format(new Date());
    }

    public String[] getLineFirstLine() {
        return new String[]{
                "id\nОбязательное поле\nИдентификатор документа\nСтрока от 1 до 64 символов",
                "type\nОбязательное поле\nПризнак расчета, 1054: \n1. Приход \n3. Расход ",
                "correctionType\nОбязательное поле\nТип коррекции 1173:\n0. Самостоятельно \n1. По предписанию",
                "causeDocumentDate\nОбязательное поле\nВремя в виде строки в формате ISO8601\nДата документа основания для коррекции 1178. \nВ данном реквизите время всегда указывать, как 00:00:00",
                "causeDocumentNumber\nОбязательное поле\nСтрока от 1 до 32 символов\nНомер документа основания для коррекции, 1179",
                "totalSum\nОбязательное поле\nСумма расчета, указанного в чеке (БСО), 1020\nДесятичное число с точностью до 2\nсимволов после точки",
                "cashSum\nСумма по чеку (БСО) наличными, 1031\nДесятичное число с точностью до 2\nсимволов после точки",
                "eCashSum\nСумма по чеку (БСО) безналичными, 1081\nДесятичное число с точностью до 2\nсимволов после точки",
                "prepaymentSum\nСумма по чеку (БСО) предоплатой (зачетом аванса и (или) предыдущих платежей), 1215\nДесятичное число с точностью до 2\nсимволов после точки",
                "postpaymentSum\nСумма по чеку (БСО) постоплатой (в кредит), 1216\nДесятичное число с точностью до 2\nсимволов после точки",
                "otherPaymentTypeSum\nСумма по чеку (БСО) встречным предоставлением, 1217\nДесятичное число с точностью до 2\nсимволов после точки",
                "tax1Sum\nСумма НДС чека по ставке 20%, 1102\nДесятичное число с точностью до 2\nсимволов после точки",
                "tax2Sum\nСумма НДС чека по ставке 10%, 1103\nДесятичное число с точностью до 2\nсимволов после точки",
                "tax3Sum\nСумма расчета по чеку с НДС по ставке 0%, 1104\nДесятичное число с точностью до 2\nсимволов после точки",
                "tax4Sum\nСумма расчета по чеку без НДС, 1105\nДесятичное число с точностью до 2\nсимволов после точки",
                "tax5Sum\nСумма НДС чека по расч. ставке 20/120, 1106\nДесятичное число с точностью до 2\nсимволов после точки",
                "tax6Sum\nСумма НДС чека по расч. ставке 10/110, 1107\nДесятичное число с точностью до 2\nсимволов после точки",
                "taxationSystem\nПрименяемая система налогообложения, 1055: \n0. Общая \n1. Упрощенная доход \n2. Упрощенная доход минус расход \n3. Единый налог на вмененный доход \n4. Единый сельскохозяйственный налог \n5. Патентная система налогообложения",
                "automatNumber\nОбязательное поле\nНомер автомата, 1036\nСтрока длиной от 1 до 20 символов",
                "settlementAddress\nОбязательное поле\nАдрес расчетов, 1009 \nСтрока длиной от 1 до 243 символов",
                "settlementPlace\nОбязательное поле\nМесто расчетов, 1187\nСтрока длиной от 1 до 243 символов"
        };
    }

    public String[] getLine(Map<String, Object> row, AtomicInteger counter, String dateCorrection, int numDoc, Map<String, String> station) {
        String complexCode = "" + row.get("code") + row.get("gate");
        String summa = String.format("%.2f", row.get("summa")).replace(",", ".").replace(".00", "");
        return new String[]{
                counter.incrementAndGet() + "",
                "1",
                "0",
                dateCorrection,
                numDoc + "",
                summa,
                "",
                summa,
                "",
                "",
                "",
                "",
                "",
                "",
                summa,
                "",
                "",
                "0",
                complexCode,
                "Россия, город Москва, Алтуфьевское шоссе, д.33Г",
                station.get(row.get("code")) + complexCode
        };
    }



}
