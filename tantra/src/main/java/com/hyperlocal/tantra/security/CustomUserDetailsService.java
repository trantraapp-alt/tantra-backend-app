package com.hyperlocal.tantra.security;

import com.hyperlocal.tantra.modules.auth.entity.User; // Your exact entity path
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Standard constructor dependency injection
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user authentication data directly using your exact entity fields.
     */
    @Override
    public UserDetails loadUserByUsername(String mobileNumber) throws UsernameNotFoundException {

        // 1. Query the database using your custom repository finder method
        User tantraUser = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new UsernameNotFoundException("No user found in Tantra ecosystem with mobile: " + mobileNumber));

        // 2. Safely capture the role from your exact field name: appUsageRole
        String roleStr = tantraUser.getAppUsageRole() != null ? tantraUser.getAppUsageRole().toUpperCase() : "USER";
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleStr);

        // 3. Return Spring Security's built-in User wrapper using explicit full-path mapping
        // This avoids conflicts between your entity class name and Spring's internal security user class.
        return new org.springframework.security.core.userdetails.User(
                tantraUser.getMobileNumber(),
                tantraUser.getPassword(),
                Collections.singletonList(authority)
        );
    }
}