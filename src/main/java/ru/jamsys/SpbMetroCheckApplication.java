package ru.jamsys;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.functional.ConsumerThrowing;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

}
