package com.takhunter.backend.service;

import com.takhunter.backend.dto.AuthResponse;
import com.takhunter.backend.dto.LoginRequest;
import com.takhunter.backend.dto.RegisterRequest;
import com.takhunter.backend.dto.UserResponse;
import com.takhunter.backend.model.EventOrganizer;
import com.takhunter.backend.model.Mahasiswa;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.EventOrganizerRepository;
import com.takhunter.backend.repository.MahasiswaRepository;
import com.takhunter.backend.repository.UserRepository;
import com.takhunter.backend.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String ROLE_MAHASISWA = "MAHASISWA";
    private static final String ROLE_EVENT_ORGANIZER = "EVENT_ORGANIZER";

    private final UserRepository userRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            MahasiswaRepository mahasiswaRepository,
            EventOrganizerRepository eventOrganizerRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedRole = normalizeRole(request.getRole());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email sudah terdaftar");
        }
        if (ROLE_MAHASISWA.equals(normalizedRole) && mahasiswaRepository.existsByNim(request.getNim().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NIM sudah terdaftar");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(normalizedRole)
                .build();

        User savedUser = userRepository.save(user);

        if (ROLE_MAHASISWA.equals(normalizedRole)) {
            Mahasiswa mahasiswa = Mahasiswa.builder()
                    .nim(request.getNim().trim())
                    .totalPoinTak(0)
                    .user(savedUser)
                    .build();
            mahasiswaRepository.save(mahasiswa);
        }

        if (ROLE_EVENT_ORGANIZER.equals(normalizedRole)) {
            EventOrganizer eventOrganizer = EventOrganizer.builder()
                    .user(savedUser)
                    .build();
            eventOrganizerRepository.save(eventOrganizer);
        }

        String token = jwtUtil.generateToken(savedUser);
        return buildAuthResponse("Register berhasil", token, savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email dan password wajib diisi");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email tidak ditemukan"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password salah");
        }

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse("Login berhasil", token, user);
    }

    public UserResponse check(String authorizationHeader) {
        String token = getTokenFromHeader(authorizationHeader);
        if (!jwtUtil.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token tidak valid atau sudah expired");
        }

        String email = jwtUtil.getEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User tidak ditemukan"));

        return buildUserResponse(user);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request register wajib diisi");
        }
        if (isBlank(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama wajib diisi");
        }
        if (isBlank(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email wajib diisi");
        }
        if (isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password wajib diisi");
        }
        if (request.getPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password minimal 8 karakter");
        }
        if (isBlank(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role wajib diisi");
        }

        String normalizedRole = normalizeRole(request.getRole());
        if (ROLE_MAHASISWA.equals(normalizedRole) && isBlank(request.getNim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NIM wajib diisi untuk role Mahasiswa");
        }
    }

    private String normalizeRole(String role) {
        String normalizedRole = role.trim()
                .replace("-", "_")
                .toUpperCase();

        if ("MAHASISWA".equals(normalizedRole)) {
            return ROLE_MAHASISWA;
        }
        if ("EVENT_ORGANIZER".equals(normalizedRole)) {
            return ROLE_EVENT_ORGANIZER;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role wajib Mahasiswa atau Event_organizer");
    }

    private AuthResponse buildAuthResponse(String message, String token, User user) {
        return AuthResponse.builder()
                .message(message)
                .token(token)
                .user(buildUserResponse(user))
                .build();
    }

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private String getTokenFromHeader(String authorizationHeader) {
        if (isBlank(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wajib dikirim dengan format Bearer token");
        }

        return authorizationHeader.substring(7);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
