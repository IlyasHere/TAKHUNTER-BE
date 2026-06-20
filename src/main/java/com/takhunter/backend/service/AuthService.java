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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String ROLE_MAHASISWA = "MAHASISWA";
    private static final String ROLE_EVENT_ORGANIZER = "EVENT_ORGANIZER";
    private static final Path PROFILE_UPLOAD_DIRECTORY = Paths.get("uploads", "profiles");
    private static final long MAX_PROFILE_PHOTO_SIZE = 2 * 1024 * 1024;

    private final UserRepository userRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotifikasiService notifikasiService;

    public AuthService(
            UserRepository userRepository,
            MahasiswaRepository mahasiswaRepository,
            EventOrganizerRepository eventOrganizerRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            NotifikasiService notifikasiService
    ) {
        this.userRepository = userRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.notifikasiService = notifikasiService;
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
        User user = getUserFromAuthorizationHeader(authorizationHeader);
        return buildUserResponse(user);
    }

    @Transactional
    public AuthResponse updateProfile(
            String authorizationHeader,
            String name,
            String email,
            String phone,
            String organization,
            String city,
            String address,
            MultipartFile profilePhoto
    ) {
        User user = getUserFromAuthorizationHeader(authorizationHeader);

        if (isBlank(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama wajib diisi");
        }
        if (isBlank(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email wajib diisi");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email sudah digunakan user lain");
        }

        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(trimToNull(phone));
        user.setCity(trimToNull(city));
        user.setAddress(trimToNull(address));

        String profilePhotoPath = saveProfilePhoto(profilePhoto);
        if (profilePhotoPath != null) {
            user.setProfilePhotoPath(profilePhotoPath);
        }

        if (ROLE_EVENT_ORGANIZER.equals(normalizeRole(user.getRole()))) {
            EventOrganizer eventOrganizer = eventOrganizerRepository.findByUserId(user.getId()).orElse(null);
            if (eventOrganizer != null) {
                eventOrganizer.setNamaOrganisasi(trimToNull(organization));
                eventOrganizerRepository.save(eventOrganizer);
            }
        }

        User savedUser = userRepository.save(user);
        notifikasiService.notifyProfileUpdated(savedUser);
        String token = jwtUtil.generateToken(savedUser);

        return buildAuthResponse("Profil berhasil diperbarui", token, savedUser);
    }

    @Transactional
    public Map<String, String> changePassword(String authorizationHeader, Map<String, String> request) {
        User user = getUserFromAuthorizationHeader(authorizationHeader);
        String currentPassword = request == null ? null : request.get("currentPassword");
        String newPassword = request == null ? null : request.get("newPassword");
        String confirmPassword = request == null ? null : request.get("confirmPassword");

        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password lama, password baru, dan konfirmasi wajib diisi");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password lama salah");
        }
        if (newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password baru minimal 8 karakter");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Konfirmasi password baru belum sama");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);
        notifikasiService.notifyPasswordChanged(savedUser);

        return Map.of("message", "Password berhasil diperbarui");
    }

    @Transactional
    public AuthResponse changeEmail(String authorizationHeader, Map<String, String> request) {
        User user = getUserFromAuthorizationHeader(authorizationHeader);
        String password = request == null ? null : request.get("password");
        String oldEmail = request == null ? null : request.get("oldEmail");
        String newEmail = request == null ? null : request.get("newEmail");

        if (isBlank(password) || isBlank(oldEmail) || isBlank(newEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password, email lama, dan email baru wajib diisi");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password salah");
        }
        if (!user.getEmail().equalsIgnoreCase(oldEmail.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email lama tidak sesuai");
        }

        String normalizedNewEmail = newEmail.trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedNewEmail, user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email baru sudah digunakan user lain");
        }

        user.setEmail(normalizedNewEmail);
        User savedUser = userRepository.save(user);
        notifikasiService.notifyEmailChanged(savedUser);
        String token = jwtUtil.generateToken(savedUser);

        return buildAuthResponse("Email berhasil diperbarui", token, savedUser);
    }

    @Transactional
    public Map<String, String> deleteAccount(String authorizationHeader, Map<String, String> request) {
        User user = getUserFromAuthorizationHeader(authorizationHeader);
        String password = request == null ? null : request.get("password");

        if (!isBlank(password) && !passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password salah");
        }

        return Map.of("message", "Permintaan hapus akun berhasil diproses");
    }

    private User getUserFromAuthorizationHeader(String authorizationHeader) {
        String token = getTokenFromHeader(authorizationHeader);
        if (!jwtUtil.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token tidak valid atau sudah expired");
        }

        String email = jwtUtil.getEmailFromToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User tidak ditemukan"));
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
        Mahasiswa mahasiswa = ROLE_MAHASISWA.equals(normalizeRole(user.getRole()))
                ? mahasiswaRepository.findByUserId(user.getId()).orElse(null)
                : null;
        EventOrganizer eventOrganizer = ROLE_EVENT_ORGANIZER.equals(normalizeRole(user.getRole()))
                ? eventOrganizerRepository.findByUserId(user.getId()).orElse(null)
                : null;

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profilePhotoPath(user.getProfilePhotoPath())
                .profilePhotoUrl(user.getProfilePhotoPath())
                .nim(mahasiswa != null ? mahasiswa.getNim() : null)
                .totalPoinTak(mahasiswa != null ? mahasiswa.getTotalPoinTak() : null)
                .phone(user.getPhone())
                .nomorWhatsApp(user.getPhone())
                .organization(eventOrganizer != null ? eventOrganizer.getNamaOrganisasi() : null)
                .namaOrganisasi(eventOrganizer != null ? eventOrganizer.getNamaOrganisasi() : null)
                .city(user.getCity())
                .address(user.getAddress())
                .build();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    private String saveProfilePhoto(MultipartFile profilePhoto) {
        if (profilePhoto == null || profilePhoto.isEmpty()) {
            return null;
        }
        if (profilePhoto.getSize() > MAX_PROFILE_PHOTO_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukuran foto profil maksimal 2 MB");
        }

        String extension = getFileExtension(profilePhoto.getOriginalFilename()).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto profil wajib JPG, PNG, atau WEBP");
        }

        String fileName = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(PROFILE_UPLOAD_DIRECTORY);
            Path targetPath = PROFILE_UPLOAD_DIRECTORY.resolve(fileName);
            Files.copy(profilePhoto.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/profiles/" + fileName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal upload foto profil");
        }
    }

    private String getFileExtension(String fileName) {
        if (isBlank(fileName) || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
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
