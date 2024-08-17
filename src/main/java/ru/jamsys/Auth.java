package ru.jamsys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.web.http.HttpInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Basic ")) {
                String[] valueExplode = authorization.split("Basic ");
                if (valueExplode.length == 2) {
                    String str = new String(Base64.getDecoder().decode(valueExplode[1]), StandardCharsets.UTF_8);
                    String user = str.substring(0, str.indexOf(":"));
                    String password = str.substring(str.indexOf(":") + 1);
                    String cred = null;
                    try {
                        char[] chars = securityComponent.get("web.password." + user);
                        if (chars != null) {
                            cred = new String(chars);
                        }
                    } catch (Throwable th) {
                        App.error(th);
                    }
                    if (cred != null && cred.equals(password)) {
                        return true;
                    }
                }
            }
            setUnauthorized(response);
        } catch (Throwable th) {
            App.error(th);
        }
        return false;
    }

    public void setUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader("WWW-Authenticate", "Basic realm=\"JamSys\"");
        response.getWriter().print("<html><body><h1>401. Unauthorized</h1></body>");
    }
}
