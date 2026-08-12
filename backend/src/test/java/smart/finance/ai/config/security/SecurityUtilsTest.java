package smart.finance.ai.config.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import smart.finance.ai.exception.ForbiddenException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsUser(Integer id) {
        AuthenticatedUser principal = new AuthenticatedUser(id, "user" + id + "@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }

    private void loginAsAdmin(Integer id) {
        AuthenticatedUser principal = new AuthenticatedUser(id, "admin@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }

    @Test
    void assertOwnerOrAdminPermitsOwner() {
        loginAsUser(1);
        assertThatCode(() -> SecurityUtils.assertOwnerOrAdmin(1)).doesNotThrowAnyException();
    }

    @Test
    void assertOwnerOrAdminRejectsDifferentUser() {
        loginAsUser(1);
        assertThatThrownBy(() -> SecurityUtils.assertOwnerOrAdmin(2))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assertOwnerOrAdminAllowsAdminForAnyUser() {
        loginAsAdmin(9);
        assertThatCode(() -> SecurityUtils.assertOwnerOrAdmin(1)).doesNotThrowAnyException();
    }

    @Test
    void effectiveUserIdForcesCurrentUserForNonAdmin() {
        loginAsUser(3);
        assertThat(SecurityUtils.effectiveUserId(999)).isEqualTo(3);
    }

    @Test
    void effectiveUserIdKeepsRequestedIdForAdmin() {
        loginAsAdmin(9);
        assertThat(SecurityUtils.effectiveUserId(3)).isEqualTo(3);
    }
}