package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum Total implements JdbcRequestRepository {

    TOTAL("""
            SELECT
            	group_key,
            	group_title,
            	sum(group_count)
            FROM "spb-metro-check".total
            WHERE date_fof between ${IN.date_start::VARCHAR}::timestamp and ${IN.date_end::VARCHAR}::timestamp
            GROUP BY group_key, group_title
            ORDER BY group_key, group_title
            LIMIT 5000
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    REMOVE("""
            DELETE
            FROM "spb-metro-check".total
            WHERE date_fof between ${IN.date_start::VARCHAR}::timestamp and ${IN.date_end::VARCHAR}::timestamp
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""
            INSERT INTO "spb-metro-check".total (
                date_fof,
                group_key,
                group_title,
                group_count
            ) values (
                ${IN.date_fof::VARCHAR}::timestamp,
                ${IN.group_key::VARCHAR},
                ${IN.group_title::VARCHAR},
                ${IN.group_count::NUMBER}
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate template;

    Total(String sql, StatementType statementType) {
        template = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

}