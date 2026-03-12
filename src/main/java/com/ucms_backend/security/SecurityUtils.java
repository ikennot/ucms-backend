package com.ucms_backend.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) authentication.getPrincipal();
    }

    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}
