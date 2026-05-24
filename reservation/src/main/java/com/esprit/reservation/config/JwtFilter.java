package com.esprit.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@Order(1)
public class JwtFilter implements Filter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
                var claims = MAPPER.readTree(payload);

                httpRequest.setAttribute("userId", claims.get("sub").asText());

                var realmAccess = claims.get("realm_access");
                if (realmAccess != null && realmAccess.has("roles")) {
                    List<String> roles = new ArrayList<>();
                    realmAccess.get("roles").forEach(r -> roles.add(r.asText()));
                    httpRequest.setAttribute("userRoles", roles);
                }
            } catch (Exception ignored) {
            }
        }

        chain.doFilter(request, response);
    }
}
