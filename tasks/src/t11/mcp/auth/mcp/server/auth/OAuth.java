package t11.mcp.auth.mcp.server.auth;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@Profile("oauth-auth")
public class OAuth {

    // ==================== CONFIGURATION ====================

    private static final String KEYCLOAK_URL = System.getenv("KEYCLOAK_URL") != null
            ? System.getenv("KEYCLOAK_URL") : "http://localhost:8089";
    private static final String KEYCLOAK_REALM = System.getenv("KEYCLOAK_REALM") != null
            ? System.getenv("KEYCLOAK_REALM") : "mcp-realm";
    private static final String REQUIRED_ROLE = System.getenv("MCP_REQUIRED_ROLE") != null
            ? System.getenv("MCP_REQUIRED_ROLE") : "mcp-tools-access";

    private static final String ISSUER_URI = KEYCLOAK_URL + "/realms/" + KEYCLOAK_REALM;
    private static final String JWKS_URI = ISSUER_URI + "/protocol/openid-connect/certs";

    // ==================== SECURITY FILTER CHAIN ====================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Build decoder with JWKS fetched lazily (no Keycloak needed at startup)
        // and with issuer validation mirroring the Python jwt.decode(issuer=ISSUER)
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER_URI));

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(decoder))
            )
            // Role check runs after BearerTokenAuthenticationFilter sets the JwtAuthenticationToken
            .addFilterAfter(new RoleCheckFilter(), BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // SSE async dispatches run on a new thread without a SecurityContext — permit them
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // ==================== ROLE CHECK FILTER ====================

    private static class RoleCheckFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                // Keycloak embeds realm roles in: claims["realm_access"]["roles"]
                @SuppressWarnings("unchecked")
                Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaim("realm_access");
                @SuppressWarnings("unchecked")
                List<String> roles = realmAccess != null
                        ? (List<String>) realmAccess.get("roles")
                        : List.of();

                if (!roles.contains(REQUIRED_ROLE)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                        {"error":"Forbidden","detail":"Role '%s' is required. User has roles: %s"}
                        """.formatted(REQUIRED_ROLE, roles));
                    return;
                }

                logger.info("Authenticated: " + jwt.getClaim("preferred_username") + " | roles: " + roles);
            }

            chain.doFilter(request, response);
        }
    }
}
