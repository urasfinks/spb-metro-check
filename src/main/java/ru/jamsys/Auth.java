package ru.jamsys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.extension.http.ServletRequestReader;
import ru.jamsys.core.web.http.HttpInterceptor;

@Component
@Lazy
public class Auth implements HttpInterceptor {

    private final SecurityComponent securityComponent;

    public Auth(SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;
    }

    @Override
    public boolean handle(HttpServletRequest request, HttpServletResponse response) {
        try {
            ServletRequestReader.basicAuthHandler(request.getHeader("Authorization"), (user, password) -> {
                String cred = new String(securityComponent.get("web.password." + user));
                if (!cred.equals(password)) {
                    throw new RuntimeException("Incorrect password");
                }
            });
        } catch (Throwable th) {
            App.error(th);
            ServletHandler.setResponseUnauthorized(response);
            return false;
        }
        return true;
    }

}
