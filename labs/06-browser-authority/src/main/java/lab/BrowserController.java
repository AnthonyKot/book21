package lab;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class BrowserController {
    @Value("${lab.safe-render}") boolean safeRender;
    @GetMapping(value="/", produces="text/html") String home(CsrfToken csrf) {
        String token = csrf == null ? "" : csrf.getToken();
        return """
            <!doctype html><html lang="en"><meta charset="utf-8"><title>Cedar approval lab</title>
            <h1>Cedar invoice C-1001</h1>
            <form method="post" action="/api/approve">
            <input type="hidden" name="invoice" value="C-1001">
            <input type="hidden" name="_csrf" value="%s">
            <button>Approve invoice</button></form>
            <p id="note"></p><a href="/summary">Open summary practice</a>
            <script src="/render.js"></script></html>
            """.formatted(token);
    }
    @GetMapping("/api/state") Map<String,Object> state(HttpSession session) {
        return Map.of("invoice", "C-1001", "approved", Boolean.TRUE.equals(session.getAttribute("approved")));
    }
    @PostMapping("/api/approve") Map<String,Object> approve(@RequestParam String invoice, HttpSession session) {
        // The only account belongs to Cedar; retain the resource check in all modes.
        if (!"C-1001".equals(invoice)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        session.setAttribute("approved", true);
        return state(session);
    }
    @GetMapping("/csrf") Map<String,String> token(CsrfToken csrf) {
        return csrf == null ? Map.of("header", "X-CSRF-TOKEN", "token", "")
                : Map.of("header", csrf.getHeaderName(), "token", csrf.getToken());
    }
    @GetMapping(value="/render.js", produces="text/javascript") String render() {
        return "const note = new URLSearchParams(location.search).get('note') || '';\n"
            + "document.getElementById('note')." + (safeRender ? "textContent" : "innerHTML") + " = note;";
    }
    @GetMapping(value="/summary", produces="text/html") String summary() {
        return """
            <!doctype html><html lang="en"><meta charset="utf-8"><title>Summary practice</title>
            <h1>Invoice summary</h1><div id="summary"></div><a href="/">Back</a>
            <script src="/summary.js"></script></html>
            """;
    }
}
