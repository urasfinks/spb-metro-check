package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum TPP implements JdbcTemplate {

    CANCEL("""
            UPDATE "spb-metro-check".tpp
            SET processed = 'cancel', date_processed = now()::timestamp
            WHERE processed is NULL
            AND status = 'Не подлежит оплате'
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    ACCEPTED("""
            UPDATE "spb-metro-check".tpp
            SET processed = 'accepted_tpp', date_processed = now()::timestamp
            WHERE processed is NULL
            AND status = 'Принято в ТПП'
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    NOT_ORANGE("""
            UPDATE "spb-metro-check".tpp
            SET processed = 'not_orange', date_processed = now()::timestamp
            WHERE id IN (
            	SELECT t1.id FROM "spb-metro-check".tpp t1
            	LEFT JOIN "spb-metro-check".orange o1
            	    ON (t1.id_transaction || '-INCOME') = o1.id_transaction
            	WHERE t1.processed is NULL
            	AND o1.id_transaction IS NULL
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    FN_FUTURE("""
            UPDATE "spb-metro-check".tpp
            SET processed = 'fn_future', date_processed = now()::timestamp
            WHERE id IN (
            	SELECT q1.id from (
            		SELECT
            			t1.id,
            			CASE WHEN (t1.date_local::date || ' 03:00:00')::timestamp < t1.date_local
            			THEN (t1.date_local::date || ' 03:00:00')::timestamp + interval '1 day'
            			ELSE (t1.date_local::date || ' 03:00:00')::timestamp END AS ts,
            			t1.date_local,
            			t1.date_fn
            			FROM "spb-metro-check".tpp t1
            		ORDER BY id ASC
            	) AS q1
            	WHERE q1.date_fn > q1.ts
            )
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    PROCESSED("""
            SELECT * FROM "spb-metro-check".tpp
            WHERE processed IN (${IN.processed::IN_ENUM_VARCHAR})
            ORDER BY date_local
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    FILL_CONTINUE("""
            UPDATE "spb-metro-check".tpp
            SET processed = 'checked', date_processed = now()::timestamp
            WHERE processed IS NULL
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    TRUNCATE("""
            TRUNCATE "spb-metro-check".tpp
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    STATISTIC("""
            SELECT processed as title, count(*) FROM "spb-metro-check".tpp
            GROUP BY processed
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    CLEAR_MARK("""
            UPDATE "spb-metro-check".tpp
            SET processed = null
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO "spb-metro-check".tpp (
                date_local,
                date_fn,
                status,
                id_transaction,
                summa,
                code,
                gate,
                f54,
                data
            ) values (
                ${IN.date_local::TIMESTAMP},
                ${IN.date_fn::TIMESTAMP},
                ${IN.status::VARCHAR},
                ${IN.id_transaction::VARCHAR},
                ${IN.summa::NUMBER},
                ${IN.code::VARCHAR},
                ${IN.gate::VARCHAR},
                ${IN.f54::VARCHAR},
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