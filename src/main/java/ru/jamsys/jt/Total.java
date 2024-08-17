package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum Total implements JdbcTemplate {

    TOTAL("""
            SELECT
            	group_key,
            	group_title,
            	sum(t1.group_count)
            FROM "spb-metro-check".total t1
            WHERE date_local between ${IN.date_start::VARCHAR}::timestamp and ${IN.date_end::VARCHAR}::timestamp
            GROUP BY group_key, group_title
            ORDER BY group_key, group_title
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO "spb-metro-check".total (
                date_local,
                group_key,
                group_title,
                group_count
            ) values (
                ${IN.date_local::VARCHAR}::timestamp,
                ${IN.group_key::VARCHAR},
                ${IN.group_title::VARCHAR},
                ${IN.group_count::NUMBER}
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