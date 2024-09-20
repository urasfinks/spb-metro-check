package ru.jamsys;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilFileResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/*
* data_mvv l1
{
  "f0" : "dpan",
  "f1" : "acceptor_name",
  "f2" : "merchant_id",
  "f3" : "inn",
  "f4" : "bin",
  "f5" : "arn",
  "f6" : "trans_date",
  "f7" : "mvv",
  "f8" : "bankname",
  "f9" : "legal"
}
* */

public class Visa {

    public static void main(String[] args) throws Throwable {
        AtomicLong counter = new AtomicLong(0);
        Map<String, List<Map<String, Object>>> visa = getVisa();
        visa.forEach((_, list) -> {
            Map<String, Object> l1 = list.getFirst();
            Map<String, String> append = new HashMapBuilder<String, String>()
                    .append("visa_bank_name", (String) l1.getOrDefault("f8", ""))
                    .append("visa_mvv", (String) l1.getOrDefault("f7", ""))
                    .append("visa_legal", (String) l1.getOrDefault("f9", ""))
                    .append("visa_inn", (String) l1.getOrDefault("f3", ""));
            ArrayList<Map<String, String>> tableData = new ArrayList<>();
            list.forEach(stringObjectMap -> {
                tableData.add(new HashMapBuilder<String, String>()
                        .append("visa_trans_date", (String) stringObjectMap.getOrDefault("f6", ""))
                        .append("visa_dpan", (String) stringObjectMap.getOrDefault("f0", ""))
                        .append("visa_acceptor_name", (String) stringObjectMap.getOrDefault("f1", ""))
                        .append("visa_merchant_id", (String) stringObjectMap.getOrDefault("f2", ""))
                        .append("visa_mvv", (String) stringObjectMap.getOrDefault("f7", ""))
                        .append("visa_arn", (String) stringObjectMap.getOrDefault("f5", ""))
                );
            });
            try {
                MasterVisa.compile(
                        "visa.docx",
                        "data/visa/f" + counter.incrementAndGet() + "-visa.docx",
                        UtilFileResource.getAsString("visa_row.txt"),
                        append,
                        tableData
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private static Map<String, List<Map<String, Object>>> getVisa() throws Throwable {

        Map<String, List<Map<String, Object>>> base = new HashMap<>();
        SpbMetroCheckApplication.onRead(
                SpbMetroCheckApplication.getCSVReader(new FileInputStream("data/input/data_mvv_l1.csv"), 1, "Cp1251"),
                new AtomicBoolean(true),
                1,
                list -> {
                    list.forEach(map -> {
                        String mk = map.get("f3") + (String) map.get("f8") + map.get("f9");
                        List<Map<String, Object>> innerList = base.computeIfAbsent(mk, _ -> new ArrayList<>());
                        innerList.add(map);
                    });
                }
        );
        return base;
    }


}
