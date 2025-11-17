package com.socialmediatraining.authenticationcommons;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RoleUtils {

    public enum role {
        USER,ADMIN
    }

    public static List<String> GetUserRoleNamesAsList(){
        return List.of(role.USER.name(), role.ADMIN.name());
    }

    public static List<String> GetAdminRoleNamesAsList(){
        return List.of(role.ADMIN.name());
    }

    public boolean hasAnyUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        GetUserRoleNamesAsList().contains(authority.getAuthority().replace("ROLE_", ""))
                );
    }

    public boolean hasAnyAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        GetAdminRoleNamesAsList().contains(authority.getAuthority().replace("ROLE_", ""))
                );
    }
}
