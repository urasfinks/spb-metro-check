package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum Orange implements JdbcTemplate {

    INSERT("""
            INSERT INTO "spb-metro-check".orange (
                date_local,
                id_transaction,
                data
            ) values (
                ${IN.date_local::TIMESTAMP},
                ${IN.id_transaction::VARCHAR},
                ${IN.data::VARCHAR}::json
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    Orange(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }

}
