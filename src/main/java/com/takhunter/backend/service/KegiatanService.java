package com.takhunter.backend.service;

import com.takhunter.backend.dto.CreateKegiatanRequest;
import com.takhunter.backend.dto.KegiatanResponse;
import com.takhunter.backend.model.EventOrganizer;
import com.takhunter.backend.model.KategoriKegiatan;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.StatusPublikasi;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.EventOrganizerRepository;
import com.takhunter.backend.repository.KegiatanRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KegiatanService {

    private static final Logger log = LoggerFactory.getLogger(KegiatanService.class);

    private static final String ROLE_EVENT_ORGANIZER = "EVENT_ORGANIZER";
    private static final Path BANNER_UPLOAD_DIRECTORY = Paths.get("uploads", "banners");
    private static final long MAX_BANNER_SIZE = 2 * 1024 * 1024;

    private final KegiatanRepository kegiatanRepository;
    private final EventOrganizerRepository eventOrganizerRepository;

    public KegiatanService(
            KegiatanRepository kegiatanRepository,
            EventOrganizerRepository eventOrganizerRepository
    ) {
        this.kegiatanRepository = kegiatanRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
    }

    public KegiatanResponse create(CreateKegiatanRequest request, MultipartFile banner) {
        validateRequest(request);
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();

        Kegiatan kegiatan = Kegiatan.builder()
                .namaKegiatan(request.getNamaKegiatan().trim())
                .deskripsi(request.getDeskripsi().trim())
                .kategori(parseKategori(request.getKategori()))
                .poinTak(request.getPoinTak())
                .wajib(request.isWajib())
                .bannerPath(saveBannerFile(banner))
                .tanggal(request.getTanggal())
                .waktu(request.getWaktu())
                .lokasi(request.getLokasi().trim())
                .kuotaPeserta(request.getKuotaPeserta())
                .batasPendaftaran(request.getBatasPendaftaran())
                .statusPublikasi(parseStatusPublikasi(request.getStatusPublikasi()))
                .eventOrganizer(eventOrganizer)
                .build();

        return toResponse(kegiatanRepository.save(kegiatan));
    }

    public List<KegiatanResponse> getAllMine() {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();

        return kegiatanRepository.findByEventOrganizerIdOrderByCreatedAtDesc(eventOrganizer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public KegiatanResponse getDetail(Long id) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        Kegiatan kegiatan = findMineById(id, eventOrganizer.getId());

        return toResponse(kegiatan);
    }

    public List<KegiatanResponse> getAllActive() {
        return kegiatanRepository.findByStatusPublikasiOrderByCreatedAtDesc(StatusPublikasi.AKTIF)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public KegiatanResponse getActiveDetail(Long id) {
        Kegiatan kegiatan = kegiatanRepository.findByIdAndStatusPublikasi(id, StatusPublikasi.AKTIF)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kegiatan tidak ditemukan"));

        return toResponse(kegiatan);
    }

    public KegiatanResponse update(Long id, CreateKegiatanRequest request, MultipartFile banner) {
        validateRequest(request);
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        log.info(
                "Updating kegiatan id={} by eventOrganizerId={} hasBanner={}",
                id,
                eventOrganizer.getId(),
                banner != null && !banner.isEmpty()
        );
        Kegiatan kegiatan = findMineById(id, eventOrganizer.getId());

        kegiatan.setNamaKegiatan(request.getNamaKegiatan().trim());
        kegiatan.setDeskripsi(request.getDeskripsi().trim());
        kegiatan.setKategori(parseKategori(request.getKategori()));
        kegiatan.setPoinTak(request.getPoinTak());
        kegiatan.setWajib(request.isWajib());
        String newBannerPath = saveBannerFile(banner);
        if (newBannerPath != null) {
            kegiatan.setBannerPath(newBannerPath);
        }
        kegiatan.setTanggal(request.getTanggal());
        kegiatan.setWaktu(request.getWaktu());
        kegiatan.setLokasi(request.getLokasi().trim());
        kegiatan.setKuotaPeserta(request.getKuotaPeserta());
        kegiatan.setBatasPendaftaran(request.getBatasPendaftaran());
        kegiatan.setStatusPublikasi(parseStatusPublikasi(request.getStatusPublikasi()));

        return toResponse(kegiatanRepository.save(kegiatan));
    }

    public void delete(Long id) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        Kegiatan kegiatan = findMineById(id, eventOrganizer.getId());

        kegiatanRepository.delete(kegiatan);
    }

    private Kegiatan findMineById(Long id, Long eventOrganizerId) {
        return kegiatanRepository.findByIdAndEventOrganizerId(id, eventOrganizerId)
                .orElseThrow(() -> {
                    log.warn("Kegiatan id={} not found for eventOrganizerId={}", id, eventOrganizerId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Kegiatan tidak ditemukan");
                });
    }

    private EventOrganizer getLoggedInEventOrganizer() {
        User user = getLoggedInUser();
        String normalizedRole = normalizeRole(user.getRole());
        if (!ROLE_EVENT_ORGANIZER.equals(normalizedRole)) {
            log.warn("Forbidden kegiatan access by userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
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

    private void validateRequest(CreateKegiatanRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request kegiatan wajib diisi");
        }
        if (isBlank(request.getNamaKegiatan())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama kegiatan wajib diisi");
        }
        if (isBlank(request.getDeskripsi())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deskripsi wajib diisi");
        }
        if (isBlank(request.getKategori())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kategori wajib diisi");
        }
        if (request.getPoinTak() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poin TAK wajib diisi");
        }
        if (request.getPoinTak() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poin TAK minimal 0");
        }
        if (request.getTanggal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal wajib diisi");
        }
        if (request.getWaktu() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waktu wajib diisi");
        }
        if (isBlank(request.getLokasi())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lokasi wajib diisi");
        }
        if (request.getKuotaPeserta() != null && request.getKuotaPeserta() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kuota peserta minimal 1");
        }
        if (request.getBatasPendaftaran() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batas pendaftaran wajib diisi");
        }

        parseKategori(request.getKategori());
        parseStatusPublikasi(request.getStatusPublikasi());
    }

    private KategoriKegiatan parseKategori(String kategori) {
        try {
            return KategoriKegiatan.valueOf(kategori.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kategori wajib seminar, lomba, webinar, atau pelatihan");
        }
    }

    private StatusPublikasi parseStatusPublikasi(String statusPublikasi) {
        if (isBlank(statusPublikasi)) {
            return StatusPublikasi.DRAFT;
        }

        try {
            return StatusPublikasi.valueOf(statusPublikasi.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status publikasi wajib DRAFT atau AKTIF");
        }
    }

    private KegiatanResponse toResponse(Kegiatan kegiatan) {
        return KegiatanResponse.builder()
                .id(kegiatan.getId())
                .namaKegiatan(kegiatan.getNamaKegiatan())
                .deskripsi(kegiatan.getDeskripsi())
                .kategori(kegiatan.getKategori().name())
                .poinTak(kegiatan.getPoinTak())
                .wajib(kegiatan.isWajib())
                .bannerPath(kegiatan.getBannerPath())
                .tanggal(kegiatan.getTanggal())
                .waktu(kegiatan.getWaktu())
                .lokasi(kegiatan.getLokasi())
                .kuotaPeserta(kegiatan.getKuotaPeserta())
                .batasPendaftaran(kegiatan.getBatasPendaftaran())
                .statusPublikasi(kegiatan.getStatusPublikasi().name())
                .createdAt(kegiatan.getCreatedAt())
                .updatedAt(kegiatan.getUpdatedAt())
                .eventOrganizerId(kegiatan.getEventOrganizer().getId())
                .build();
    }

    private String saveBannerFile(MultipartFile banner) {
        if (banner == null || banner.isEmpty()) {
            return null;
        }
        if (banner.getSize() > MAX_BANNER_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukuran banner maksimal 2 MB");
        }

        String originalFilename = banner.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(BANNER_UPLOAD_DIRECTORY);
            Path targetPath = BANNER_UPLOAD_DIRECTORY.resolve(fileName);
            Files.copy(banner.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/banners/" + fileName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal upload banner");
        }
    }

    private String getFileExtension(String fileName) {
        if (isBlank(fileName) || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
