package com.hyperlocal.tantra.modules.auth.service;

import com.hyperlocal.tantra.modules.auth.dto.*;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.security.JwtUtil;
import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    /**
     * 1. SIGN UP (Registers a user with a secure, time-sorted short unique ID)
     */
    public Map<String, String> registerUser(SignUpRequestDTO dto) {
        boolean isHindi = "HI".equalsIgnoreCase(dto.getPreferredLanguage());
        Map<String, String> response = new HashMap<>();

        // Check if mobile number is already taken
        if (userRepository.findByMobileNumber(dto.getMobileNumber()).isPresent()) {
            response.put("error", isHindi ? "त्रुटि: यह मोबाइल नंबर पहले से पंजीकृत है!" : "Error: Mobile number already registered!");
            return response;
        }

        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setMobileNumber(dto.getMobileNumber());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setAppUsageRole("ROLE_" + dto.getAppUsageRole().toUpperCase());
        user.setPreferredLanguage(dto.getPreferredLanguage() != null ? dto.getPreferredLanguage().toUpperCase() : "EN");

        // Execute the new hybrid time-sorted unique ID generation strategy
        String generatedUserId = IdGeneratorUtil.generateShortUniqueId();
        user.setUserId(generatedUserId);

        userRepository.save(user);

        response.put("message", isHindi ? "पंजीकरण सफल! यूजर आईडी: " + generatedUserId : "Registration successful! UserID: " + generatedUserId);
        response.put("userId", generatedUserId);
        return response;
    }

    /**
     * 2. SIGN IN (Authenticates credentials and generates long-term JWT token)
     */
    public Map<String, Object> loginUser(SignInRequestDTO dto, String lang) {
        boolean isHindi = "HI".equalsIgnoreCase(lang);
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userRepository.findByMobileNumber(dto.getMobileNumber());
        if (userOpt.isEmpty()) {
            response.put("error", isHindi ? "त्रुटि: मोबाइल नंबर पंजीकृत नहीं है!" : "Error: Mobile number not registered!");
            return response;
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            response.put("error", isHindi ? "त्रुटि: गलत पासवर्ड!" : "Error: Invalid password!");
            return response;
        }

        String token = jwtUtil.generateToken(user.getMobileNumber(), user.getAppUsageRole());

        response.put("message", isHindi ? "लॉगिन सफल!" : "Login successful!");
        response.put("token", token);
        response.put("role", user.getAppUsageRole());
        response.put("userId", user.getUserId());
        return response;
    }

    /**
     * 3. FORGOT PASSWORD - REQUEST OTP (Generates 6-digit numeric OTP with 5 mins validity)
     */
    public String sendResetOtp(String mobileNumber, String lang) {
        boolean isHindi = "HI".equalsIgnoreCase(lang);
        Optional<User> userOpt = userRepository.findByMobileNumber(mobileNumber);
        if (userOpt.isEmpty()) {
            return isHindi ? "त्रुटि: नंबर रिकॉर्ड में नहीं है।" : "Error: Number not found.";
        }

        User user = userOpt.get();
        String generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        user.setResetOtp(generatedOtp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // integration checkpoint: external SMS API gateway logic will be triggered here
        System.out.println("🔥 [TANTRA SMS DUMMY] OTP for " + mobileNumber + " is: " + generatedOtp + " (Valid for 5 mins)");

        return isHindi ? "ओटीपी भेज दिया गया है। (जांचें टर्मिनल)" : "OTP sent successfully. (Check Terminal)";
    }

    /**
     * 4. FORGOT PASSWORD - RESET ACTION (Validates OTP tokens and updates password credentials)
     */
    public String resetPassword(ForgotPasswordDTO dto, String lang) {
        boolean isHindi = "HI".equalsIgnoreCase(lang);
        Optional<User> userOpt = userRepository.findByMobileNumber(dto.getMobileNumber());
        if (userOpt.isEmpty()) return isHindi ? "त्रुटि: यूज़र नहीं मिला।" : "Error: User not found.";

        User user = userOpt.get();

        // Validate OTP temporal expiration window
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return isHindi ? "त्रुटि: ओटीपी की समय सीमा समाप्त हो गई है!" : "Error: OTP has expired!";
        }

        // Validate cryptographic integrity of the incoming OTP string
        if (user.getResetOtp() == null || !user.getResetOtp().equals(dto.getOtp())) {
            return isHindi ? "त्रुटि: गलत ओटीपी!" : "Error: Invalid OTP!";
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return isHindi ? "पासवर्ड सफलतापूर्वक बदल गया है!" : "Password reset successful!";
    }

    /**
     * 5. GET USER PROFILE (Validates incoming JWT tokens and resolves user session details)
     */
    public Object getUserProfile(String authHeader, String lang) {
        boolean isHindi = "HI".equalsIgnoreCase(lang);

        // 1. Verify Authorization header format constraints
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new AppErrorResponse(
                    LocalDateTime.now(),
                    401,
                    "Unauthorized",
                    MessageConstants.getMessage("AUTH_ERROR_TOKEN", isHindi),
                    "/api/v1/auth/profile"
            );
        }

        String token = authHeader.substring(7); // Parse the raw token string values

        try {
            String mobileNumber = jwtUtil.extractMobileNumber(token);
            Optional<User> userOpt = userRepository.findByMobileNumber(mobileNumber);

            // 2. Validate token details against relational database records
            if (userOpt.isEmpty() || !jwtUtil.validateToken(token, mobileNumber)) {
                return new AppErrorResponse(
                        LocalDateTime.now(),
                        401,
                        "Unauthorized",
                        MessageConstants.getMessage("AUTH_ERROR_TOKEN", isHindi),
                        "/api/v1/auth/profile"
                );
            }

            User user = userOpt.get();

            // Convert expiration timestamp properties to local timezone formats
            java.util.Date expDate = jwtUtil.extractExpiration(token);
            LocalDateTime sessionExpiry = expDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            // 3. Populate and return response data transport structure
            ProfileResponseDTO profile = new ProfileResponseDTO();
            profile.setUserId(user.getUserId());
            profile.setFirstName(user.getFirstName());
            profile.setLastName(user.getLastName());
            profile.setMobileNumber(user.getMobileNumber());
            profile.setAppUsageRole(user.getAppUsageRole());
            profile.setPreferredLanguage(user.getPreferredLanguage());
            profile.setSessionToken(token);
            profile.setSessionExpiry(sessionExpiry);

            return profile;

        } catch (Exception e) {
            // Intercept parsing errors, token expiration exceptions, or signature mismatches safely
            return new AppErrorResponse(
                    LocalDateTime.now(),
                    401,
                    "Unauthorized",
                    MessageConstants.getMessage("AUTH_ERROR_TOKEN", isHindi),
                    "/api/v1/auth/profile"
            );
        }
    }
}