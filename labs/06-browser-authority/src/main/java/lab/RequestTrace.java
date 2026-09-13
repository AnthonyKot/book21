package lab;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTrace implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var http = (HttpServletRequest) request;
        // Trace received methods/paths only; never cookies, query strings or tokens.
        System.out.println("HTTP " + http.getMethod() + " " + http.getRequestURI());
        chain.doFilter(request, response);
    }
}
