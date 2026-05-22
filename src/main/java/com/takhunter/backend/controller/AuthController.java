package com.takhunter.backend.controller;

import com.takhunter.backend.dto.AuthResponse;
import com.takhunter.backend.dto.LoginRequest;
import com.takhunter.backend.dto.RegisterRequest;
import com.takhunter.backend.dto.UserResponse;
import com.takhunter.backend.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PutMapping("/profile")
    public AuthResponse updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto
    ) {
        return authService.updateProfile(authorizationHeader, name, email, profilePhoto);
    }
}
