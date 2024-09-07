package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum Station implements JdbcRequestRepository {

    SELECT("""
            SELECT * FROM "spb-metro-check".station
            ORDER BY id ASC
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("""            
            INSERT INTO "spb-metro-check".station (
                code,
                place
            ) values (
                ${IN.code::VARCHAR},
                ${IN.place::VARCHAR}
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate template;

    Station(String sql, StatementType statementType) {
        template = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

}
