package smart.finance.ai.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting in-app para los endpoints de autenticacón (signin/signup).
 * Los buckets se agrupan por IP del cliente y se almacenan en memoria.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long CLEANUP_INTERVAL_MS = 10 * 60 * 1000L;

    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.signin.max-requests:5}")
    private int signinMaxRequests;

    @Value("${app.rate-limit.signin.period-secs:60}")
    private long signinPeriodSecs;

    @Value("${app.rate-limit.signup.max-requests:5}")
    private int signupMaxRequests;

    @Value("${app.rate-limit.signup.period-secs:300}")
    private long signupPeriodSecs;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(isPost(request, "/auth/signin") || isPost(request, "/auth/signup"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientKey = clientKey(request);
        boolean isSignin = isPost(request, "/auth/signin");

        int maxRequests = isSignin ? signinMaxRequests : signupMaxRequests;
        long periodSecs = isSignin ? signinPeriodSecs : signupPeriodSecs;

        Bucket bucket = buckets.computeIfAbsent(clientKey, k -> bucketFor(maxRequests, periodSecs));
        lastAccess.put(clientKey, System.currentTimeMillis());
        cleanup();

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(periodSecs));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "message", "Demasiadas peticiones. Intenta nuevamente mas tarde.",
                "path", request.getRequestURI())));
    }

    private Bucket bucketFor(int maxRequests, long periodSecs) {
        Bandwidth limit = Bandwidth.classic(maxRequests, Refill.greedy(maxRequests, Duration.ofSeconds(periodSecs)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isPost(HttpServletRequest request, String path) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().endsWith(path);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        buckets.forEach((key, bucket) -> {
            Long last = lastAccess.get(key);
            if (last != null && now - last > CLEANUP_INTERVAL_MS) {
                buckets.remove(key);
                lastAccess.remove(key);
            }
        });
    }
}