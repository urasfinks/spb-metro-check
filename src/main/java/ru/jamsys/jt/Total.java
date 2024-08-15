package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum Total implements JdbcTemplate {

    INSERT("""
            INSERT INTO "spb-metro-check".kkt (
                date_local,
                data
            ) values (
                ${IN.date_local::NUMBER},
                ${IN.data::VARCHAR}::json
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    Total(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }

}