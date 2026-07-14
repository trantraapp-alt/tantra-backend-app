package com.hyperlocal.tantra.modules.auth.service;

import com.hyperlocal.tantra.modules.auth.dto.*;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.security.JwtUtil;
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

    // 1. SIGN UP (With Auto UserID: FIRST4-LAST5MOBILE)
    public Map<String, String> registerUser(SignUpRequestDTO dto) {
        boolean isHindi = "HI".equalsIgnoreCase(dto.getPreferredLanguage());
        Map<String, String> response = new HashMap<>();

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

        // Auto ID Generator
        String fNamePart = dto.getFirstName().replaceAll("[^a-zA-Z]", "").toUpperCase();
        fNamePart = fNamePart.length() < 4 ? String.format("%-4s", fNamePart).replace(' ', 'X') : fNamePart.substring(0, 4);
        String mobileStr = dto.getMobileNumber();
        String mobilePart = mobileStr.substring(Math.max(0, mobileStr.length() - 5));
        String generatedUserId = fNamePart + "-" + mobilePart;
        user.setUserId(generatedUserId);

        userRepository.save(user);

        response.put("message", isHindi ? "पंजीकरण सफल! यूजर आईडी: " + generatedUserId : "Registration successful! UserID: " + generatedUserId);
        response.put("userId", generatedUserId);
        return response;
    }

    // 2. SIGN IN (Generates Long-Term JWT)
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

    // 3. FORGOT PASSWORD - REQUEST OTP (5 Mins Expiry)
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

        // 🟢 भविष्य के लिए: यहाँ आपका SMS API कोड आ जाएगा
        System.out.println("🔥 [TANTRA SMS DUMMY] OTP for " + mobileNumber + " is: " + generatedOtp + " (Valid for 5 mins)");

        return isHindi ? "ओटीपी भेज दिया गया है। (जांचें टर्मिनल)" : "OTP sent successfully. (Check Terminal)";
    }

    // 4. FORGOT PASSWORD - RESET ACTION
    public String resetPassword(ForgotPasswordDTO dto, String lang) {
        boolean isHindi = "HI".equalsIgnoreCase(lang);
        Optional<User> userOpt = userRepository.findByMobileNumber(dto.getMobileNumber());
        if (userOpt.isEmpty()) return isHindi ? "त्रुटि: यूज़र नहीं मिला।" : "Error: User not found.";

        User user = userOpt.get();

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return isHindi ? "त्रुटि: ओटीपी की समय सीमा समाप्त हो गई है!" : "Error: OTP has expired!";
        }

        if (user.getResetOtp() == null || !user.getResetOtp().equals(dto.getOtp())) {
            return isHindi ? "त्रुटि: गलत ओटीपी!" : "Error: Invalid OTP!";
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return isHindi ? "पासवर्ड सफलतापूर्वक बदल गया है!" : "Password reset successful!";
    }
}