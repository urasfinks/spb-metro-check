package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum KKT implements JdbcTemplate {

    STATISTIC_DIFF("""
            WITH orange_agg AS (
            SELECT
                   (o1.code || o1.gate) as complex_code_orange,
                   o1.summa as summa_orange,
                   count(o1.*) as count_agg_orange,
                   sum(o1.summa) as summa_agg_orange
               FROM "spb-metro-check".orange o1
               GROUP BY o1.summa, (o1.code || o1.gate)
            )
            SELECT * FROM (
               SELECT * FROM "spb-metro-check".kkt k1
               LEFT JOIN orange_agg oa1
                   ON oa1.complex_code_orange = (k1.code || k1.gate)
                   AND oa1.summa_orange = k1.summa
            ) as sq1
            WHERE summa_agg_orange <> summa_agg
               OR count_agg_orange <> count_agg
               OR summa_agg IS NULL
               OR count_agg IS NULL
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    STATISTIC("""
            WITH orange_agg AS (
            SELECT
                 (o1.code || o1.gate) as complex_code_orange,
                 o1.summa as summa_orange,
                 count(o1.*) as count_agg_orange,
                 sum(o1.summa) as summa_agg_orange
             FROM "spb-metro-check".orange o1
             GROUP BY o1.summa, (o1.code || o1.gate)
            )
            SELECT count(*), 'diff' as title FROM (
             SELECT * FROM "spb-metro-check".kkt k1
             LEFT JOIN orange_agg oa1
                 ON oa1.complex_code_orange = (k1.code || k1.gate)
                 AND oa1.summa_orange = k1.summa
            ) AS sq1
            WHERE summa_agg_orange <> summa_agg
             OR count_agg_orange <> count_agg
             OR summa_agg IS NULL
             OR count_agg IS NULL
            UNION ALL SELECT count(*), 'count' AS title FROM "spb-metro-check".kkt
            UNION ALL SELECT count(*), 'orange' AS title FROM orange_agg
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    TRUNCATE("""
            TRUNCATE "spb-metro-check".kkt
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO "spb-metro-check".kkt (
                summa,
                code,
                gate,
                count_agg,
                summa_agg
            ) values (
                ${IN.summa::NUMBER},
                ${IN.code::VARCHAR},
                ${IN.gate::VARCHAR},
                ${IN.count_agg::NUMBER},
                ${IN.summa_agg::NUMBER}
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    KKT(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }

}