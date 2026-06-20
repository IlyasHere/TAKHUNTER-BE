package com.takhunter.backend.controller;

import com.takhunter.backend.dto.AuthResponse;
import com.takhunter.backend.dto.LoginRequest;
import com.takhunter.backend.dto.RegisterRequest;
import com.takhunter.backend.dto.UserResponse;
import com.takhunter.backend.service.AuthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/check")
    public UserResponse check(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.check(authorizationHeader);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.check(authorizationHeader);
    }

    @GetMapping("/profile")
    public UserResponse profile(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.check(authorizationHeader);
    }

    @PutMapping("/profile")
    public AuthResponse updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "nomorWhatsApp", required = false) String nomorWhatsApp,
            @RequestParam(value = "organization", required = false) String organization,
            @RequestParam(value = "namaOrganisasi", required = false) String namaOrganisasi,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto
    ) {
        return authService.updateProfile(
                authorizationHeader,
                name,
                email,
                phone != null ? phone : nomorWhatsApp,
                organization != null ? organization : namaOrganisasi,
                city,
                address,
                profilePhoto
        );
    }

    @PatchMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody Map<String, String> request
    ) {
        return ResponseEntity.ok(authService.changePassword(authorizationHeader, request));
    }

    @PatchMapping("/email")
    public AuthResponse changeEmail(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody Map<String, String> request
    ) {
        return authService.changeEmail(authorizationHeader, request);
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) Map<String, String> request
    ) {
        return ResponseEntity.ok(authService.deleteAccount(authorizationHeader, request));
    }
}
