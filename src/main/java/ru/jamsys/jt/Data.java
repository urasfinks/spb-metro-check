package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum Data implements JdbcTemplate {

    INSERT("""
            INSERT INTO "sbt-metro-check".data (
                date_data,
                type_data,
                key_data,
                data_data
            ) values (
                ${IN.date_data::TIMESTAMP},
                ${IN.type_data::VARCHAR},
                ${IN.key_data::VARCHAR},
                ${IN.data_data::VARCHAR}::json
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    Data(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }

}