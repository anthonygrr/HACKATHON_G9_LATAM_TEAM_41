package smart.finance.ai.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter newFilter() {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());
        ReflectionTestUtils.setField(filter, "signinMaxRequests", 5);
        ReflectionTestUtils.setField(filter, "signinPeriodSecs", 60L);
        ReflectionTestUtils.setField(filter, "signupMaxRequests", 5);
        ReflectionTestUtils.setField(filter, "signupPeriodSecs", 300L);
        return filter;
    }

    @Test
    void allowsRequestsUpToLimit() throws Exception {
        RateLimitFilter filter = newFilter();

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/signin");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertThat(chain.getRequest())
                    .as("request %d should reach the chain", i + 1)
                    .isNotNull();
        }
    }

    @Test
    void blocksRequestOverLimitWith429() throws Exception {
        RateLimitFilter filter = newFilter();

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/signin");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/signin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void doesNotInterceptOtherEndpoints() throws Exception {
        RateLimitFilter filter = newFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analisis-financiero");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}