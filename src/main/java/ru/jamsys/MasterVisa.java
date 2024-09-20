package ru.jamsys;

import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilRisc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/*
* data_mcc l1
* {
  "f0" : "num",
  "f1" : "inn",
  "f2" : "bankname",
  "f3" : "name",
  "f4" : "acqbin_visa",
  "f5" : "acqbin_mc",
  "f6" : "mcc_cur",
  "f7" : "legal",
  "f8" : "mcc_req",
  "f9" : "qty",
  "f10" : "vol",
  "f11" : "irf",
  "f12" : "Больше 1 млн на ИНН",
  "f13" : "ling_gis",
  "f14" : "link_site"
}
*
* data_mcc l2
* {
  "f0" : "num",
  "f1" : "inn",
  "f2" : "bin",
  "f3" : "ps",
  "f4" : "dpan",
  "f5" : "acceptor_name",
  "f6" : "merchant_id",
  "f7" : "mcc",
  "f8" : "arn",
  "f9" : "trans_date",
  "f10" : "",
  "f11" : ""
}
* */

public class MasterVisa {

    public static void main(String[] args) throws Throwable {
        List<Map<String, Object>> mccL1 = getMccL1();
        List<Map<String, Object>> mccL2 = getMccL2();
        mccL1.forEach(stringObjectMap -> {
            try {
                merge(stringObjectMap, mccL2);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void unzipZip() throws IOException {
        //unzip(new FileInputStream("data/template/master-visa.docx"), new File("data/template/master-visa").toPath());
        pack("data/template/master-visa", "data/template/master-visa.docx");
    }

    private static void merge(Map<String, Object> l1, List<Map<String, Object>> mccL2) throws IOException {
        ArrayList<Map<String, Object>> visa = new ArrayList<>();
        ArrayList<Map<String, Object>> master = new ArrayList<>();
        l1.put("visa", visa);
        l1.put("master", master);
        mccL2.forEach(stringObjectMap -> {
            if (stringObjectMap.get("f1").equals(l1.get("f1"))) {
                if (stringObjectMap.get("f3").equals("Виза")) {
                    visa.add(stringObjectMap);
                }
                if (stringObjectMap.get("f3").equals("МС")) {
                    master.add(stringObjectMap);
                }
            }
        });
        Map<String, String> append = new HashMapBuilder<String, String>()
                .append("list1_bank_name", (String) l1.getOrDefault("f2", ""))
                .append("list1_legal", (String) l1.getOrDefault("f7", ""))
                .append("list1_inn", (String) l1.getOrDefault("f1", ""))
                .append("list1_mcc_cur", (String) l1.getOrDefault("f6", ""))
                .append("list1_link_site", (String) l1.getOrDefault("f14", ""))
                .append("list1_link_gis", (String) l1.getOrDefault("f13", ""));

        if (visa.isEmpty() && master.isEmpty()) {
            ArrayList<Map<String, String>> tableData = new ArrayList<>();
            compile(
                    "master-visa.docx",
                    "data/ex/f" + l1.get("f0") + "-empty.docx",
                    UtilFileResource.getAsString("master-visa_row.txt"),
                    new HashMapBuilder<>(append).append("list2_ps", ""),
                    tableData
            );
        }

        if (!visa.isEmpty()) {
            ArrayList<Map<String, String>> tableData = new ArrayList<>();
            visa.forEach(stringObjectMap -> {
                tableData.add(new HashMapBuilder<String, String>()
                        .append("list2_merchant_id", (String) stringObjectMap.getOrDefault("f6", ""))
                        .append("list2_dpan", (String) stringObjectMap.getOrDefault("f4", ""))
                        .append("list2_arn", (String) stringObjectMap.getOrDefault("f8", ""))
                        .append("list2_trans_date", (String) stringObjectMap.getOrDefault("f9", ""))
                        .append("list2_mcc", (String) stringObjectMap.getOrDefault("f7", ""))
                        .append("list2_acceptor_name", (String) stringObjectMap.getOrDefault("f5", ""))
                );
            });
            compile(
                    "master-visa.docx",
                    "data/ex/f" + l1.get("f0") + "-visa.docx",
                    UtilFileResource.getAsString("master-visa_row.txt"),
                    new HashMapBuilder<>(append).append("list2_ps", "Виза"),
                    tableData
            );
        }

        if (!master.isEmpty()) {
            ArrayList<Map<String, String>> tableData = new ArrayList<>();
            master.forEach(stringObjectMap -> {
                tableData.add(new HashMapBuilder<String, String>()
                        .append("list2_merchant_id", (String) stringObjectMap.getOrDefault("f6", ""))
                        .append("list2_dpan", (String) stringObjectMap.getOrDefault("f4", ""))
                        .append("list2_arn", (String) stringObjectMap.getOrDefault("f8", ""))
                        .append("list2_trans_date", (String) stringObjectMap.getOrDefault("f9", ""))
                        .append("list2_mcc", (String) stringObjectMap.getOrDefault("f7", ""))
                        .append("list2_acceptor_name", (String) stringObjectMap.getOrDefault("f5", ""))
                );
            });
            compile(
                    "master-visa.docx",
                    "data/ex/f" + l1.get("f0") + "-mc.docx",
                    UtilFileResource.getAsString("master-visa_row.txt"),
                    new HashMapBuilder<>(append).append("list2_ps", "MC"),
                    tableData
            );
        }


    }

    private static List<Map<String, Object>> getMccL1() throws Throwable {
        List<Map<String, Object>> result = new ArrayList<>();
        SpbMetroCheckApplication.onRead(
                SpbMetroCheckApplication.getCSVReader(new FileInputStream("data/visa-master/data_mcc_l1.csv"), 1, "Cp1251"),
                new AtomicBoolean(true),
                1,
                result::addAll
        );
        result.forEach(stringObjectMap -> {
            stringObjectMap.put("f1", SpbMetroCheckApplication.expoReplace((String) stringObjectMap.get("f1")));
        });
        return result;
    }

    private static List<Map<String, Object>> getMccL2() throws Throwable {
        List<Map<String, Object>> result = new ArrayList<>();
        SpbMetroCheckApplication.onRead(
                SpbMetroCheckApplication.getCSVReader(new FileInputStream("data/visa-master/data_mcc_l2.csv"), 1, "Cp1251"),
                new AtomicBoolean(true),
                1,
                result::addAll
        );
        result.forEach(stringObjectMap -> {
            stringObjectMap.put("f1", SpbMetroCheckApplication.expoReplace((String) stringObjectMap.get("f1")));
        });
        return result;
    }

    private static void example() throws IOException {
        ArrayList<Map<String, String>> tableData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tableData.add(new HashMapBuilder<String, String>()
                    .append("a6", "6")
                    .append("a7", "6")
                    .append("a8", "6")
                    .append("a9", "6")
                    .append("a10", "6")
                    .append("a11", "6")
            );
        }
        compile(
                "master_dt.docx",
                "xe.docx",
                UtilFileResource.getAsString("master_dt_row.txt"),
                new HashMapBuilder<String, String>()
                        .append("visa_bank_name", "Виза банк")
                        .append("visa_inn", "12345"),
                tableData
        );
    }

    public static void compile(
            String fileTemplate,
            String destDocx,
            String templateRow,
            Map<String, String> global,
            List<Map<String, String>> listRow
    ) throws IOException {
        UtilFile.removeIfExist(destDocx);
        Map<String, String> arg = getMapTemplate(fileTemplate, global);
        StringBuilder anyTr = new StringBuilder();
        listRow.forEach(map -> anyTr.append(TemplateTwix.template(templateRow, getMapTemplate(fileTemplate, map))));
        arg.put("any_tr", anyTr.toString());
        String folder = java.util.UUID.randomUUID().toString();
        File any = new File(folder);
        if (!any.exists()) {
            any.mkdirs();
        }
        unzip(UtilFileResource.get(fileTemplate), any.toPath());
        byte[] bytes = UtilFile.readBytes(folder + "/word/document.xml");
        String template = TemplateTwix.template(new String(bytes), arg);
        UtilFile.writeBytes(
                folder + "/word/document.xml",
                template.getBytes(StandardCharsets.UTF_8),
                FileWriteOptions.CREATE_OR_REPLACE)
        ;
        pack(folder, destDocx);
        UtilFile.removeDir(any);
    }

    public static void pack(String sourceDirPath, String zipFilePath) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }
    }

    public static void unzip(InputStream is, Path targetDir) throws IOException {
        targetDir = targetDir.toAbsolutePath();
        try (ZipInputStream zipIn = new ZipInputStream(is)) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
                Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new RuntimeException("Entry with an illegal path: " + ze.getName());
                }
                if (ze.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipIn, resolvedPath);
                }
            }
        }
    }

    private static final HashMapBuilder<String, Map<String, String>> map = new HashMapBuilder<String, Map<String, String>>()
            .append("visa.docx", new HashMapBuilder<String, String>()
                    .append("a1", "visa_bank_name")
                    .append("a2", "visa_mvv")
                    .append("a3", "visa_legal")
                    .append("a4", "visa_bank_name")
                    .append("a5", "visa_legal")
                    .append("a6", "visa_inn")
                    .append("a7", "visa_mvv")
                    .append("a8", "visa_trans_date")
                    .append("a9", "visa_dpan")
                    .append("a10", "visa_acceptor_name")
                    .append("a11", "visa_merchant_id")
                    .append("a12", "visa_mvv")
                    .append("a13", "visa_arn")
                    .append("any_tr", "any_tr")
            )
            .append("master.docx", new HashMapBuilder<String, String>()
                    .append("a1", "mc_bank_name")
                    .append("a2", "mc_legal")
                    .append("a3", "mc_bank_name")
                    .append("a4", "mc_mvv")
                    .append("a5", "mc_inn")
                    .append("a6", "mc_trans_date")
                    .append("a7", "mc_dpan")
                    .append("a8", "mc_acceptor_name")
                    .append("a9", "mc_merchant_id")
                    .append("a10", "mc_arn")
                    .append("a11", "mc_inn")
                    .append("any_tr", "any_tr")
            )
            .append("master-visa.docx", new HashMapBuilder<String, String>()
                    .append("a1", "list1_bank_name")
                    .append("a2", "list1_bank_name")
                    .append("a3", "list2_ps")
                    .append("a4", "list1_legal")
                    .append("a5", "list1_inn")
                    .append("a6", "list1_mcc_cur")
                    .append("a7", "list1_link_site")
                    .append("a8", "list1_link_gis")
                    .append("a9", "list2_merchant_id")
                    .append("a10", "list2_dpan")
                    .append("a11", "list2_arn")
                    .append("a12", "list2_trans_date")
                    .append("a13", "list2_mcc")
                    .append("a14", "list2_acceptor_name")
                    .append("any_tr", "any_tr")
            );

    public static Map<String, String> getMapTemplate(String fileTemplate, Map<String, String> input) {
        HashMap<String, String> copy = new HashMap<>(map.get(fileTemplate));
        UtilRisc.forEach(null, copy, (key, value) -> {
            if (input.containsKey(value)) {
                copy.put(key, input.get(value));
            }
        });
        return copy;
    }

    public static byte[] generateDocxFileFromTemplate(String fileTemplate, Map<String, String> input) throws Exception {
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(UtilFileResource.get(fileTemplate));
        MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
        VariablePrepare.prepare(wordMLPackage);
        Map<String, String> mapTemplate = getMapTemplate(fileTemplate, input);
        //documentPart.
        documentPart.variableReplace(mapTemplate);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        wordMLPackage.save(outputStream);
        return outputStream.toByteArray();
    }
}
