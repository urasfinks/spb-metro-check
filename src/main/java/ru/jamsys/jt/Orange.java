package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum Orange implements JdbcTemplate {

    NOT_TPP("""
            UPDATE "spb-metro-check".orange
            SET processed = 'not_tpp', date_processed = now()::timestamp
            WHERE id IN (
            	SELECT o1.id FROM "spb-metro-check".orange o1
            	LEFT JOIN "spb-metro-check".tpp t1 ON t1.id_transaction  = o1.id_transaction
            	WHERE o1.processed IS NULL
            	AND t1.id_transaction IS NULL
            )
            """, StatementType.CALL_WITH_AUTO_COMMIT),

    // После того как пометили кого нет в ТПП - пометим все остальные как успешные
    FILL_CONTINUE("""
            UPDATE "spb-metro-check".orange
            SET processed = 'checked', date_processed = now()::timestamp
            WHERE processed IS NULL
            """, StatementType.CALL_WITH_AUTO_COMMIT),

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
