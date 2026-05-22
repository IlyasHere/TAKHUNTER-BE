package com.takhunter.backend.service;

import com.takhunter.backend.dto.PendaftaranResponse;
import com.takhunter.backend.model.EventOrganizer;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.EventOrganizerRepository;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PendaftaranService {

    private static final Logger log = LoggerFactory.getLogger(PendaftaranService.class);

    private static final String ROLE_EVENT_ORGANIZER = "EVENT_ORGANIZER";

    private final PendaftaranRepository pendaftaranRepository;
    private final KegiatanRepository kegiatanRepository;
    private final EventOrganizerRepository eventOrganizerRepository;

    public PendaftaranService(
            PendaftaranRepository pendaftaranRepository,
            KegiatanRepository kegiatanRepository,
            EventOrganizerRepository eventOrganizerRepository
    ) {
        this.pendaftaranRepository = pendaftaranRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
    }

    public List<PendaftaranResponse> getPendaftarForMyKegiatan(Long kegiatanId) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();

        boolean kegiatanMilikEo = kegiatanRepository.existsByIdAndEventOrganizerId(kegiatanId, eventOrganizer.getId());
        if (!kegiatanMilikEo) {
            log.warn("Kegiatan id={} not found for eventOrganizerId={}", kegiatanId, eventOrganizer.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kegiatan tidak ditemukan");
        }

        return pendaftaranRepository.findByKegiatanIdOrderByIdDesc(kegiatanId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EventOrganizer getLoggedInEventOrganizer() {
        User user = getLoggedInUser();
        String normalizedRole = normalizeRole(user.getRole());
        if (!ROLE_EVENT_ORGANIZER.equals(normalizedRole)) {
            log.warn("Forbidden pendaftaran access by userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Event Organizer yang boleh akses");
        }

        return eventOrganizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.warn("EventOrganizer profile not found for userId={} email={}", user.getId(), user.getEmail());
                    return new ResponseStatusException(HttpStatus.FORBIDDEN, "Data Event Organizer tidak ditemukan");
                });
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wajib dikirim");
        }

        return user;
    }

    private PendaftaranResponse toResponse(Pendaftaran pendaftaran) {
        return PendaftaranResponse.builder()
                .id(pendaftaran.getId())
                .kegiatanId(pendaftaran.getKegiatanId())
                .namaMahasiswa(pendaftaran.getNamaMahasiswa())
                .nim(pendaftaran.getNim())
                .programStudi(pendaftaran.getProgramStudi())
                .email(pendaftaran.getEmail())
                .nomorWhatsApp(pendaftaran.getNomorWhatsApp())
                .alasan(pendaftaran.getAlasan())
                .status(pendaftaran.getStatus())
                .build();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        return role.trim()
                .replace("-", "_")
                .toUpperCase();
    }
}
