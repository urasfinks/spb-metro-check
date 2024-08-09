package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum Station implements JdbcTemplate {

    SELECT("""
            SELECT * FROM "spb-metro-check".station
            ORDER BY id ASC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""            
            INSERT INTO "spb-metro-check".station (
                code,
                place
            ) values (
                ${IN.code::VARCHAR},
                ${IN.place::VARCHAR}
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    Station(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }
}
