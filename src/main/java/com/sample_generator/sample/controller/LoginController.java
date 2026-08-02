package com.sample_generator.sample.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @GetMapping("/dashboard")
    public String sayHello() {
        return "dashboard";
    }

    @GetMapping("/login")
    public String showLoginPage(){
        return "login2";
    }


    /*
     * ================================================
     * GET CURRENT USER INFO
     * GET /api/user/me
     *
     * Returns: { username, roles: [...], displayRole }
     * Used by the dashboard to show the correct user
     * info in the sidebar and control admin visibility.
     * ================================================
     */

    @GetMapping("/api/user/me")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication
    ) {

        if (authentication == null || !authentication.isAuthenticated()) {

            return ResponseEntity.status(401).body(
                    Map.of("error", "Not authenticated")
            );

        }


        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());


        /*
         * Build a nice display role name:
         *
         * ROLE_ADMIN              -> Admin
         * ROLE_SALES_TEAM         -> Sales Team
         * ROLE_RESEARCH_TEAM      -> Research Team
         * ROLE_DIGITAL_MARKETING  -> Digital Marketing
         * ROLE_USER               -> User
         */

        String displayRole = roles.stream()
                .findFirst()
                .map(r -> r.replace("ROLE_", "").replace("_", " "))
                .map(r -> {
                    // Capitalize each word
                    String[] words = r.toLowerCase().split(" ");
                    StringBuilder sb = new StringBuilder();
                    for (String w : words) {
                        if (!w.isEmpty()) {
                            sb.append(Character.toUpperCase(w.charAt(0)))
                              .append(w.substring(1))
                              .append(" ");
                        }
                    }
                    return sb.toString().trim();
                })
                .orElse("User");


        boolean isAdmin = roles.contains("ROLE_ADMIN");


        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "roles", roles,
                "displayRole", displayRole,
                "isAdmin", isAdmin
        ));

    }

}
