package vn.edu.phuxuan.elib.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

public final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Object csrfUnder = request.getAttribute("_csrf");
        if (csrfUnder instanceof CsrfToken token) {
            token.getToken();
        }
        if (csrfUnder instanceof Supplier<?> supplier) {
            Object resolved = supplier.get();
            if (resolved instanceof CsrfToken token) {
                token.getToken();
            }
        }

        Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
        if (csrfAttr instanceof CsrfToken token) {
            token.getToken();
        }
        if (csrfAttr instanceof Supplier<?> supplier) {
            Object resolved = supplier.get();
            if (resolved instanceof CsrfToken token) {
                token.getToken();
            }
        }

        filterChain.doFilter(request, response);
    }
}
