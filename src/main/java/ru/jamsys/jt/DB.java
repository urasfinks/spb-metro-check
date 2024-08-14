package ru.jamsys.jt;

import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;
import ru.jamsys.core.flat.util.UtilFileResource;

public enum DB implements JdbcTemplate {

    CREATE("classpath:db.sql", StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    DB(String sql, StatementType statementType) {
        if (sql.startsWith("classpath:")) {
            try {
                template = new TemplateJdbc(UtilFileResource.getAsString(sql.substring(10)), statementType);
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        } else {
            template = new TemplateJdbc(sql, statementType);
        }
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }
}
