package com.hyperlocal.tantra.modules.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class SessionController {

    /**
     * Verifies if the user session/token is active and valid.
     */
    @GetMapping("/verify-session")
    public ResponseEntity<Map<String, Object>> verifySession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal().toString())) {

            response.put("authenticated", true);
            response.put("message", "UserLoggedInAlready");
            response.put("mobileNumber", authentication.getName());
            return ResponseEntity.ok(response);
        }

        response.put("authenticated", false);
        response.put("message", "User is not logged in");
        return ResponseEntity.status(401).body(response);
    }
}