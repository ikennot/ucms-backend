package com.ucms_backend.security;

import com.ucms_backend.exception.AppException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(401, "UNAUTHORIZED", "No authenticated user");
        }
        return (UUID) authentication.getPrincipal();
    }

    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(401, "UNAUTHORIZED", "No authenticated user");
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElseThrow(() -> new AppException(401, "UNAUTHORIZED", "No role assigned to authenticated user"));
    }
}
