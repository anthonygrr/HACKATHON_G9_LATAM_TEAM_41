package smart.finance.ai.config.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public record AuthenticatedUser(Integer id, String email, Collection<GrantedAuthority> authorities) {

    public boolean isAdmin() {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}