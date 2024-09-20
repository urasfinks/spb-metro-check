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
* data_mvv l2

* {
  "f0" : "dpan",
  "f1" : "acceptor_name",
  "f2" : "merchant_id",
  "f3" : "inn",
  "f4" : "bin",
  "f5" : "arn",
  "f6" : "trans_date",
  "f7" : "bankname",
  "f8" : "legal"
}
* */

public class Master {

    public static void main(String[] args) throws Throwable {
        AtomicLong counter = new AtomicLong(0);
        Map<String, List<Map<String, Object>>> visa = getVisa();
        visa.forEach((_, list) -> {
            Map<String, Object> l1 = list.getFirst();
            //                      .append("a1", "mc_bank_name")
            //                    .append("a2", "mc_legal")
            //                    .append("a3", "mc_bank_name")
            //                    .append("a4", "mc_mvv")
            //                    .append("a5", "mc_inn")
            Map<String, String> append = new HashMapBuilder<String, String>()
                    .append("mc_bank_name", (String) l1.getOrDefault("f7", ""))
                    .append("mc_legal", (String) l1.getOrDefault("f8", ""))
                    .append("mc_inn", (String) l1.getOrDefault("f3", ""));
            ArrayList<Map<String, String>> tableData = new ArrayList<>();
            list.forEach(stringObjectMap -> {
                //                    .append("a6", "mc_trans_date")
                //                    .append("a7", "mc_dpan")
                //                    .append("a8", "mc_acceptor_name")
                //                    .append("a9", "mc_merchant_id")
                //                    .append("a10", "mc_arn")
                //                    .append("a11", "mc_inn")
                tableData.add(new HashMapBuilder<String, String>()
                        .append("mc_trans_date", (String) stringObjectMap.getOrDefault("f6", ""))
                        .append("mc_dpan", (String) stringObjectMap.getOrDefault("f0", ""))
                        .append("mc_acceptor_name", (String) stringObjectMap.getOrDefault("f1", ""))
                        .append("mc_merchant_id", (String) stringObjectMap.getOrDefault("f2", ""))
                        .append("mc_arn", (String) stringObjectMap.getOrDefault("f5", ""))
                        .append("mc_inn", (String) stringObjectMap.getOrDefault("f3", ""))
                );
            });
            try {
                MasterVisa.compile(
                        "master.docx",
                        "data/master/f" + counter.incrementAndGet() + "-master.docx",
                        UtilFileResource.getAsString("master_row.txt"),
                        append,
                        tableData
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private static Map<String, List<Map<String, Object>>> getVisa() throws Throwable {
        AtomicLong counter = new AtomicLong(0);
        Map<String, List<Map<String, Object>>> base = new HashMap<>();
        SpbMetroCheckApplication.onRead(
                SpbMetroCheckApplication.getCSVReader(new FileInputStream("data/input/data_mvv_l2.csv"), 1, "Cp1251"),
                new AtomicBoolean(true),
                1,
                list -> {
                    list.forEach(map -> {
                        counter.incrementAndGet();
                        String mk = map.get("f3") + (String) map.get("f7") + map.get("f8");
                        List<Map<String, Object>> innerList = base.computeIfAbsent(mk, _ -> new ArrayList<>());
                        innerList.add(map);
                    });
                }
        );
        System.out.println(counter.get());
        return base;
    }

}
