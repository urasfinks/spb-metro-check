package ru.jamsys;

import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilRisc;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DocTest {

    public static void main(String[] args) throws Exception {
        UtilFile.writeBytes(
                "output.docx",
                generateDocxFileFromTemplate("visa.docx", new HashMapBuilder<String, String>()
                        .append("visa_bank_name", "Виза банк")
                        .append("visa_inn", "12345")
                ),
                FileWriteOptions.CREATE_OR_REPLACE
        );
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
                    .append("a9", "list2_merchant_is")
                    .append("a10", "list2_dpan")
                    .append("a11", "list2_arn")
                    .append("a12", "list2_trans_date")
                    .append("a13", "list2_mcc")
                    .append("a14", "list2_acceptor_name")
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
        documentPart.variableReplace(getMapTemplate(fileTemplate, input));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        wordMLPackage.save(outputStream);
        return outputStream.toByteArray();
    }
}
