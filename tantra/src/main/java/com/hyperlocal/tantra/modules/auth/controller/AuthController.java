package com.hyperlocal.tantra.modules.auth.controller;

import com.hyperlocal.tantra.modules.auth.dto.*;
import com.hyperlocal.tantra.modules.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignUpRequestDTO request) {
        Map<String, String> res = authService.registerUser(request);
        return res.containsKey("error") ? ResponseEntity.badRequest().body(res) : ResponseEntity.ok(res);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SignInRequestDTO request, @RequestParam(defaultValue = "EN") String lang) {
        Map<String, Object> res = authService.loginUser(request, lang);
        return res.containsKey("error") ? ResponseEntity.badRequest().body(res) : ResponseEntity.ok(res);
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<String> requestOtp(@RequestParam String mobileNumber, @RequestParam(defaultValue = "EN") String lang) {
        String res = authService.sendResetOtp(mobileNumber, lang);
        return res.contains("Error") || res.contains("त्रुटि") ? ResponseEntity.badRequest().body(res) : ResponseEntity.ok(res);
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<String> resetPwd(@RequestBody ForgotPasswordDTO request, @RequestParam(defaultValue = "EN") String lang) {
        String res = authService.resetPassword(request, lang);
        return res.contains("Error") || res.contains("त्रुटि") ? ResponseEntity.badRequest().body(res) : ResponseEntity.ok(res);
    }
}