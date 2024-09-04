package ru.jamsys.jt;

import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.util.UtilFileResource;

public enum DB implements JdbcRequestRepository {

    CREATE("db.sql", StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate template;

    DB(String path, StatementType statementType) {
        try {
            template = new JdbcTemplate(UtilFileResource.getAsString(path), statementType);
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

}
