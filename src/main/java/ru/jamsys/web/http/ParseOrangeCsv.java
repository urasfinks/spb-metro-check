package ru.jamsys.web.http;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.SpbMetroCheckApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.jt.Orange;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
@RequestMapping("/parseOrangeCsv")
public class ParseOrangeCsv implements PromiseGenerator {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public ParseOrangeCsv(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .appendWithResource("loadToDb", JdbcResource.class, "logger", (isThreadRun, promise, jdbcResource) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    try {
                        onRead(isThreadRun, json -> {
                            json.put("f14", SpbMetroCheckApplication.expoReplace((String) json.get("f14")));
                            json.put("f8", SpbMetroCheckApplication.arrayReplace((String) json.get("f8")));
                            json.put("f9", SpbMetroCheckApplication.arrayReplace((String) json.get("f9")));
                            json.put("f26", SpbMetroCheckApplication.arrayReplace((String) json.get("f26")));


                            String dateLocalString = (String) json.get("f11");
                            Long dateLocalMs;
                            try {
                                dateLocalMs = Util.getTimestamp(dateLocalString, "d.M.y H:m") * 1000;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }


                            JdbcRequest jdbcRequest = new JdbcRequest(Orange.INSERT);
                            jdbcRequest
                                    .addArg("date_local", dateLocalMs)
                                    .addArg("id_transaction", json.get("f0"))
                                    .addArg("data", UtilJson.toStringPretty(json, "{}"));

                            try {
                                jdbcResource.execute(jdbcRequest);
                            } catch (Throwable th) {
                                th.printStackTrace();
                                throw new ForwardException(th);
                            }
                        });
                        input.setBody("ParseOrangeCsv complete");
                    } catch (Throwable th) {
                        th.printStackTrace();
                        throw new ForwardException(th);
                    }
                });
    }

    private CSVReader getCSVReader() throws IOException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .withIgnoreQuotations(true)
                .build();
        return new CSVReaderBuilder(new FileReader("web/rrr/orange.csv", Charset.forName("Cp1251")))
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();
    }

    private void onRead(AtomicBoolean isThreadRun, Consumer<Map<String, Object>> onRead) throws CsvValidationException, IOException {
        String[] nextLine;
        CSVReader csvReader = getCSVReader();
        while ((nextLine = csvReader.readNext()) != null && isThreadRun.get()) {
            Map<String, Object> json = new LinkedHashMap<>();
            for (int i = 0; i < nextLine.length; i++) {
                json.put("f" + i, nextLine[i]);
            }
            onRead.accept(json);
        }
    }

}
