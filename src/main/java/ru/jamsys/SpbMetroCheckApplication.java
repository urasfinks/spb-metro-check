package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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

}
