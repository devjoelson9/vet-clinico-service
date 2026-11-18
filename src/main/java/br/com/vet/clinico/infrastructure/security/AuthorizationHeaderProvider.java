package br.com.vet.clinico.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Recupera o header Authorization da requisição atual para repassá-lo nas
 * chamadas síncronas entre serviços.
 */
@Component
public class AuthorizationHeaderProvider {

    public String current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("Authorization");
    }
}
