package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum KKT implements JdbcRequestRepository {

    STATISTIC_DIFF("""
            WITH orange_agg AS (
            SELECT
                   (o1.code || o1.gate) as complex_code_orange,
                   o1.summa as summa_orange,
                   count(o1.*) as count_agg_orange,
                   sum(o1.summa) as summa_agg_orange
               FROM "spb-metro-check".orange o1
               WHERE date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
               GROUP BY o1.summa, (o1.code || o1.gate)
            )
            SELECT * FROM (
               SELECT * FROM "spb-metro-check".kkt k1
               LEFT JOIN orange_agg oa1
                   ON oa1.complex_code_orange = (k1.code || k1.gate)
                   AND oa1.summa_orange = k1.summa
               WHERE k1.date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
            ) as sq1
            WHERE summa_agg_orange <> summa_agg
               OR count_agg_orange <> count_agg
               OR summa_agg IS NULL
               OR count_agg IS NULL
            LIMIT 5000
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    STATISTIC("""
            WITH orange_agg AS (
            SELECT
                 (o1.code || o1.gate) as complex_code_orange,
                 o1.summa as summa_orange,
                 count(o1.*) as count_agg_orange,
                 sum(o1.summa) as summa_agg_orange
             FROM "spb-metro-check".orange o1
             WHERE o1.date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
             GROUP BY o1.summa, (o1.code || o1.gate)
            )
            SELECT count(*), 'diff' as title FROM (
             SELECT * FROM "spb-metro-check".kkt k1
             LEFT JOIN orange_agg oa1
                 ON oa1.complex_code_orange = (k1.code || k1.gate)
                 AND oa1.summa_orange = k1.summa
             WHERE k1.date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
            ) AS sq1
            WHERE summa_agg_orange <> summa_agg
             OR count_agg_orange <> count_agg
             OR summa_agg IS NULL
             OR count_agg IS NULL
            UNION ALL SELECT count(*), 'count' AS title FROM "spb-metro-check".kkt
            WHERE date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
            UNION ALL SELECT count(*), 'orange' AS title FROM orange_agg
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    DELETE("""
            DELETE FROM "spb-metro-check".kkt
            WHERE date_fof between ${IN.date_start::VARCHAR}::date and ${IN.date_end::VARCHAR}::date
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO "spb-metro-check".kkt (
                date_fof,
                summa,
                code,
                gate,
                count_agg,
                summa_agg
            ) values (
                ${IN.date_fof::VARCHAR}::date,
                ${IN.summa::NUMBER},
                ${IN.code::VARCHAR},
                ${IN.gate::VARCHAR},
                ${IN.count_agg::NUMBER},
                ${IN.summa_agg::NUMBER}
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate template;

    KKT(String sql, StatementType statementType) {
        template = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return template;
    }
}