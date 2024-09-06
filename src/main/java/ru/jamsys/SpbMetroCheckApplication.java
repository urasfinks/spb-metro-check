package ru.jamsys;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.functional.ConsumerThrowing;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@SpringBootApplication
public class SpbMetroCheckApplication {

    public static void main(String[] args) {
        App.springSource = SpbMetroCheckApplication.class;
        App.main(args);
    }

    public static String expoReplace(String str) {
        if (str != null && !str.isEmpty()) {
            str = str.replace(",", ".");
            BigInteger bigInteger = new BigDecimal(str).toBigInteger();
            return bigInteger.toString();
        }
        return str;
    }

    public static List<String> arrayReplace(String str) {
        List<String> result = new ArrayList<>();
        if (str != null && str.startsWith("[") && str.endsWith("]")) {
            String[] split = str.substring(1, str.length() - 1).split(",");
            for (String item : split) {
                result.add(item.trim());
            }
        }
        return result;
    }

    public static void onRead(
            CSVReader csvReader,
            AtomicBoolean isThreadRun,
            int sizeBatch,
            ConsumerThrowing<List<Map<String, Object>>> onRead
    ) throws Throwable {
        String[] nextLine;
        int curSizeBatch = 0;
        List<Map<String, Object>> batch = new ArrayList<>();
        while ((nextLine = csvReader.readNext()) != null && isThreadRun.get()) {
            Map<String, Object> json = new LinkedHashMap<>();
            for (int i = 0; i < nextLine.length; i++) {
                json.put("f" + i, nextLine[i]);
            }
            batch.add(json);
            curSizeBatch++;
            if (curSizeBatch > sizeBatch) {
                onRead.accept(batch);
                batch = new ArrayList<>();
                curSizeBatch = 0;
            }
            //break;
        }
        if (!batch.isEmpty()) {
            onRead.accept(batch);
        }
    }

    public static CSVReader getCSVReader(String path, int offset) throws IOException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .withIgnoreQuotations(false)
                .build();
        return new CSVReaderBuilder(new FileReader(path, StandardCharsets.UTF_8)) //Charset.forName("UTF-8")
                .withSkipLines(offset)
                .withCSVParser(parser)
                .build();
    }

    public static CSVReader getCSVReader(InputStream is, int offset, String charset) {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .withIgnoreQuotations(false)
                .build();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charset)));
        return new CSVReaderBuilder(br)
                .withSkipLines(offset)
                .withCSVParser(parser)
                .build();
    }

    public static CSVReader getCSVReader(InputStream is, int offset) {
        return getCSVReader(is, offset, "UTF-8");
    }

    public static String[] getLineCorrectionFirstLine(Map<String, Object> row) {
        String[] array = row.keySet().toArray(new String[0]);
        return Stream.concat(Arrays.stream(new String[]{""}), Arrays.stream(array)).toArray(String[]::new);
    }

    public static String[] getLineCorrection(Map<String, Object> row, AtomicInteger counter, String[] fields) {
        String[] result = new String[row.size() + 1];
        for (int i = 0; i < fields.length; i++) {
            if (i == 0) {
                result[i] = counter.incrementAndGet() + "";
            } else {
                result[i] = String.valueOf(row.get(fields[i]));
            }
        }
        return result;
    }

    public static String[] headerReplace(String[] fLine) {
        ArrayList<String> newList = new ArrayList<>();
        Map<String, String> map = new HashMapBuilder<String, String>()
                .append("id", "Порядковый номер")
                .append("summa", "Сумма")
                .append("code", "Код станции")
                .append("gate", "Проходка")
                .append("count_agg", "Кол-во транзакций в ККТ")
                .append("summa_agg", "Сумма в ККТ")
                .append("date_local", "Локальная дата")
                .append("id_transaction", "Идентификатор транзакции")
                .append("f24", "Тип операции")
                .append("processed", "Производный статус")
                .append("date_processed", "Дата установки производного статуса")
                .append("place", "Полное наименование станции")
                .append("group_key", "Наименование группы")
                .append("group_title", "Заголовок группы")
                .append("group_count", "Кол-во в группе")
                .append("date_fn", "Дата фискализации")
                .append("status", "Статус")
                .append("id_transaction_orange", "Идентификатор транзакции в orange")
                .append("f54", "Фискальный признак документа")
                .append("complex_code_orange", "Код проходки в Orange")
                .append("count_agg_orange", "Кол-во транзакций в Orange")
                .append("summa_orange", "Сумма в Orange")
                .append("summa_agg_orange", "Сумма в Orange")
                .append("date_add", "Дата добавления записи");
        for(String key: fLine){
            newList.add(map.getOrDefault(key, key));
        }
        return newList.toArray(new String[0]);
    }

}
