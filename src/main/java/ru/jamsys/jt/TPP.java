package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum TPP implements JdbcTemplate {

    INSERT("""
            INSERT INTO "spb-metro-check".tpp (
                date_local,
                date_fn,
                status,
                id_transaction,
                data
            ) values (
                ${IN.date_local::TIMESTAMP},
                ${IN.date_fn::TIMESTAMP},
                ${IN.status::VARCHAR},
                ${IN.id_transaction::VARCHAR},
                ${IN.data::VARCHAR}::json
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    TPP(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }

}