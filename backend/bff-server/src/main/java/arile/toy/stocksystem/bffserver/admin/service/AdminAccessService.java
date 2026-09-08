package arile.toy.stocksystem.bffserver.admin.service;

import arile.toy.stocksystem.bffserver.exception.admin.AdminAccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AdminAccessService {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    public boolean isAdmin(UserDetails user) {
        return user != null && user.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }

    /**
     * 조회 대상 유저명을 결정
     * - requestedUsername이 없거나 본인이면 본인 유저명을 반환
     * - requestedUsername이 본인이 아니면, 관리자에게만 허용하고 그 값을 반환
     * - 관리자가 아닌데 다른 유저를 요청하면 예외를 던짐
     */
    public String resolveTargetUsername(UserDetails principal, String requestedUsername) {
        if (requestedUsername == null || requestedUsername.isBlank()
                || requestedUsername.equals(principal.getUsername())) {
            return principal.getUsername();
        }

        if (!isAdmin(principal)) {
            throw new AdminAccessDeniedException();
        }

        return requestedUsername;
    }
}
