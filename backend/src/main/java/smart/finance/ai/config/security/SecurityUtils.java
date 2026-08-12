package smart.finance.ai.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import smart.finance.ai.exception.ForbiddenException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    public static Integer currentUserId() {
        AuthenticatedUser user = currentUser();
        return user == null ? null : user.id();
    }

    public static boolean isAdmin() {
        AuthenticatedUser user = currentUser();
        return user != null && user.isAdmin();
    }

    public static Integer effectiveUserId(Integer requestedId) {
        return isAdmin() ? requestedId : currentUserId();
    }

    public static void assertOwnerOrAdmin(Integer requestedUserId) {
        if (isAdmin()) {
            return;
        }
        Integer current = currentUserId();
        if (current == null || !current.equals(requestedUserId)) {
            throw new ForbiddenException("No tiene permisos para acceder a este recurso");
        }
    }
}