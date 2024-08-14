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
            	LEFT JOIN "spb-metro-check".tpp t1
            	    ON (t1.id_transaction || '-INCOME') = o1.id_transaction
            	WHERE o1.processed IS NULL
            	AND t1.id_transaction IS NULL
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    // После того как пометили кого нет в ТПП - пометим все остальные как успешные
    FILL_CONTINUE("""
            UPDATE "spb-metro-check".orange
            SET processed = 'checked', date_processed = now()::timestamp
            WHERE processed IS NULL
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    TRUNCATE("""
            TRUNCATE "spb-metro-check".orange
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    STATISTIC("""
            SELECT processed as title, count(*) FROM "spb-metro-check".orange
            GROUP BY processed
            """, StatementType.SELECT_WITH_AUTO_COMMIT),
    STATISTIC_2("""
            SELECT f24 as title, count(*) FROM "spb-metro-check".orange
            GROUP BY f24
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    CLEAR_MARK("""
            UPDATE "spb-metro-check".orange
            SET processed = null
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO "spb-metro-check".orange (
                date_local,
                id_transaction,
                summa,
                code,
                gate,
                f24,
                data
            ) values (
                ${IN.date_local::TIMESTAMP},
                ${IN.id_transaction::VARCHAR},
                ${IN.summa::NUMBER},
                ${IN.code::VARCHAR},
                ${IN.gate::VARCHAR},
                ${IN.f24::VARCHAR},
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
