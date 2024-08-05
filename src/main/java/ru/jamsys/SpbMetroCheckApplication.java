package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

@SpringBootApplication
public class SpbMetroCheckApplication {

    public static void main(String[] args) {
        App.springSource = SpbMetroCheckApplication.class;
        App.main(args);
    }

}
