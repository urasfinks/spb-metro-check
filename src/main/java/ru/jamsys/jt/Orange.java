package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum Orange implements JdbcRequestRepository {

    NOT_TPP("""
            UPDATE "spb-metro-check".orange
            SET processed = 'not_tpp', date_processed = now()::timestamp
            WHERE id IN (
            	SELECT o1.id FROM "spb-metro-check".orange o1
            	LEFT JOIN "spb-metro-check".tpp t1
            	    ON t1.id_transaction_orange = o1.id_transaction
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

    DELETE("""
            DELETE FROM "spb-metro-check".orange
            WHERE date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    STATISTIC("""
            SELECT processed as title, count(*) FROM "spb-metro-check".orange
            WHERE date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
            GROUP BY processed
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    STATISTIC_2("""
            SELECT f24 as title, count(*) FROM "spb-metro-check".orange
            WHERE date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
            GROUP BY f24
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    PROCESSED("""
            SELECT * FROM "spb-metro-check".orange
            WHERE processed IN (${IN.processed::IN_ENUM_VARCHAR})
            ORDER BY date_local
            LIMIT 5000
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    CLEAR_MARK("""
            UPDATE "spb-metro-check".orange
            SET processed = null
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO "spb-metro-check".orange (
                date_fof,
                date_local,
                id_transaction,
                summa,
                code,
                gate,
                f24
            ) values (
                ${IN.date_fof::VARCHAR}::date,
                ${IN.date_local::TIMESTAMP},
                ${IN.id_transaction::VARCHAR},
                ${IN.summa::NUMBER},
                ${IN.code::VARCHAR},
                ${IN.gate::VARCHAR},
                ${IN.f24::VARCHAR}
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate template;

    Orange(String sql, StatementType statementType) {
        template = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

}
