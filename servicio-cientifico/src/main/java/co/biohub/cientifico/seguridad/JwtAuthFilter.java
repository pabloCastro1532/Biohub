package co.biohub.cientifico.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lee "Authorization: Bearer ...", valida el token y deja el usuario con su rol en el contexto. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final SesionesRevocadas revocadas;

    public JwtAuthFilter(JwtService jwt, SesionesRevocadas revocadas) {
        this.jwt = jwt;
        this.revocadas = revocadas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.validar(cabecera.substring(7));
                String usuario = claims.getSubject();
                if (!revocadas.estaRevocada(claims.getId(), usuario)) {
                    String rol = claims.get("rol", String.class);
                    var auth = new UsernamePasswordAuthenticationToken(
                            usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();   // sin autenticación: el entry point responde 401
            }
        }
        chain.doFilter(request, response);
    }
}
