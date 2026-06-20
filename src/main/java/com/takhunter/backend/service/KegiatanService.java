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
import com.takhunter.backend.repository.PendaftaranRepository;
import com.takhunter.backend.repository.SertifikatRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private static final List<String> VALID_CATEGORIES = List.of("SEMINAR", "BOOTCAMP", "LOMBA", "KEPANITIAAN");
    private static final List<String> VALID_LEVELS = List.of("UNIVERSITAS", "REGIONAL", "NASIONAL", "INTERNASIONAL");
    private static final List<String> VALID_MODES = List.of("OFFLINE", "ONLINE", "HYBRID");
    private static final List<String> VALID_ACTIVITY_STATUSES = List.of("PILIHAN", "WAJIB");

    private final KegiatanRepository kegiatanRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final PendaftaranRepository pendaftaranRepository;
    private final SertifikatRepository sertifikatRepository;
    private final NotifikasiService notifikasiService;

    public KegiatanService(
            KegiatanRepository kegiatanRepository,
            EventOrganizerRepository eventOrganizerRepository,
            PendaftaranRepository pendaftaranRepository,
            SertifikatRepository sertifikatRepository,
            NotifikasiService notifikasiService
    ) {
        this.kegiatanRepository = kegiatanRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
        this.pendaftaranRepository = pendaftaranRepository;
        this.sertifikatRepository = sertifikatRepository;
        this.notifikasiService = notifikasiService;
    }

    public KegiatanResponse create(CreateKegiatanRequest request, MultipartFile banner) {
        validateRequest(request);
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        validateUniqueNameForCreate(request.getNamaKegiatan(), eventOrganizer.getId());

        Kegiatan kegiatan = Kegiatan.builder()
                .namaKegiatan(request.getNamaKegiatan().trim())
                .deskripsi(request.getDeskripsi().trim())
                .kategori(parseKategori(request.getKategori()))
                .poinTak(request.getPoinTak())
                .wajib(request.isWajib())
                .tingkatKegiatan(trimToNull(request.getTingkatKegiatan()))
                .statusKegiatan(resolveStatusKegiatan(request))
                .bannerPath(saveBannerFile(banner))
                .tanggal(request.getTanggal())
                .tanggalSelesai(request.getTanggalSelesai() != null ? request.getTanggalSelesai() : request.getTanggal())
                .waktu(request.getWaktu())
                .waktuSelesai(request.getWaktuSelesai())
                .modeKegiatan(trimToNull(request.getModeKegiatan()))
                .lokasi(request.getLokasi().trim())
                .kuotaPeserta(request.getKuotaPeserta())
                .batasPendaftaran(request.getBatasPendaftaran())
                .pendaftaranMulai(request.getPendaftaranMulai())
                .statusPublikasi(parseStatusPublikasi(request.getStatusPublikasi()))
                .eventOrganizer(eventOrganizer)
                .build();

        Kegiatan savedKegiatan = kegiatanRepository.save(kegiatan);
        notifikasiService.notifyKegiatanCreated(savedKegiatan);

        return toResponse(savedKegiatan);
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
        validateUniqueNameForUpdate(request.getNamaKegiatan(), eventOrganizer.getId(), id);

        kegiatan.setNamaKegiatan(request.getNamaKegiatan().trim());
        kegiatan.setDeskripsi(request.getDeskripsi().trim());
        kegiatan.setKategori(parseKategori(request.getKategori()));
        kegiatan.setPoinTak(request.getPoinTak());
        kegiatan.setWajib(request.isWajib());
        kegiatan.setTingkatKegiatan(trimToNull(request.getTingkatKegiatan()));
        kegiatan.setStatusKegiatan(resolveStatusKegiatan(request));
        String newBannerPath = saveBannerFile(banner);
        if (newBannerPath != null) {
            kegiatan.setBannerPath(newBannerPath);
        }
        kegiatan.setTanggal(request.getTanggal());
        kegiatan.setTanggalSelesai(request.getTanggalSelesai() != null ? request.getTanggalSelesai() : request.getTanggal());
        kegiatan.setWaktu(request.getWaktu());
        kegiatan.setWaktuSelesai(request.getWaktuSelesai());
        kegiatan.setModeKegiatan(trimToNull(request.getModeKegiatan()));
        kegiatan.setLokasi(request.getLokasi().trim());
        kegiatan.setKuotaPeserta(request.getKuotaPeserta());
        kegiatan.setBatasPendaftaran(request.getBatasPendaftaran());
        kegiatan.setPendaftaranMulai(request.getPendaftaranMulai());
        kegiatan.setStatusPublikasi(parseStatusPublikasi(request.getStatusPublikasi()));

        Kegiatan savedKegiatan = kegiatanRepository.save(kegiatan);
        notifikasiService.notifyKegiatanUpdated(savedKegiatan);

        return toResponse(savedKegiatan);
    }

    public void delete(Long id) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        Kegiatan kegiatan = findMineById(id, eventOrganizer.getId());

        notifikasiService.notifyKegiatanDeleted(kegiatan);
        kegiatanRepository.delete(kegiatan);
    }

    public KegiatanResponse updateStatus(Long id, String statusPublikasi) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        Kegiatan kegiatan = findMineById(id, eventOrganizer.getId());
        kegiatan.setStatusPublikasi(parseStatusPublikasi(statusPublikasi));

        return toResponse(kegiatanRepository.save(kegiatan));
    }

    public KegiatanResponse finish(Long id) {
        return updateStatus(id, StatusPublikasi.SELESAI.name());
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
        if (!isBlank(request.getTingkatKegiatan()) && !VALID_LEVELS.contains(request.getTingkatKegiatan().trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tingkat kegiatan tidak valid");
        }
        if (!isBlank(request.getStatusKegiatan()) && !VALID_ACTIVITY_STATUSES.contains(request.getStatusKegiatan().trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status kegiatan tidak valid");
        }
        if (request.getTanggal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal wajib diisi");
        }
        if (request.getTanggalSelesai() != null && request.getTanggalSelesai().isBefore(request.getTanggal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal selesai tidak boleh sebelum tanggal mulai");
        }
        if (request.getWaktu() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waktu wajib diisi");
        }
        if (request.getWaktuSelesai() != null
                && request.getTanggalSelesai() != null
                && request.getTanggalSelesai().equals(request.getTanggal())
                && !request.getWaktuSelesai().isAfter(request.getWaktu())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waktu selesai harus setelah waktu mulai");
        }
        if (!isBlank(request.getModeKegiatan()) && !VALID_MODES.contains(request.getModeKegiatan().trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode kegiatan tidak valid");
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
        if (request.getPendaftaranMulai() != null && request.getPendaftaranMulai().isAfter(request.getBatasPendaftaran())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal pembukaan pendaftaran tidak boleh setelah batas pendaftaran");
        }
        if (request.getBatasPendaftaran().isAfter(request.getTanggal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal pendaftaran ditutup tidak boleh setelah tanggal mulai");
        }

        parseKategori(request.getKategori());
        parseStatusPublikasi(request.getStatusPublikasi());
    }

    private KategoriKegiatan parseKategori(String kategori) {
        String normalizedKategori = kategori == null ? "" : kategori.trim().toUpperCase();
        if (!VALID_CATEGORIES.contains(normalizedKategori)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kategori tidak valid");
        }

        return KategoriKegiatan.valueOf(normalizedKategori);
    }

    private StatusPublikasi parseStatusPublikasi(String statusPublikasi) {
        if (isBlank(statusPublikasi)) {
            return StatusPublikasi.DRAFT;
        }

        try {
            return StatusPublikasi.valueOf(statusPublikasi.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status publikasi wajib DRAFT, AKTIF, atau SELESAI");
        }
    }

    private KegiatanResponse toResponse(Kegiatan kegiatan) {
        long jumlahPendaftar = pendaftaranRepository.countByKegiatanId(kegiatan.getId());
        long jumlahSertifikatTerbit = sertifikatRepository.countByKegiatanId(kegiatan.getId());
        String statusKegiatan = !isBlank(kegiatan.getStatusKegiatan())
                ? kegiatan.getStatusKegiatan()
                : kegiatan.isWajib() ? "WAJIB" : "PILIHAN";
        LocalDate tanggalSelesai = kegiatan.getTanggalSelesai() != null ? kegiatan.getTanggalSelesai() : kegiatan.getTanggal();
        LocalTime waktuSelesai = kegiatan.getWaktuSelesai() != null ? kegiatan.getWaktuSelesai() : kegiatan.getWaktu();
        LocalDate pendaftaranMulai = kegiatan.getPendaftaranMulai() != null ? kegiatan.getPendaftaranMulai() : kegiatan.getBatasPendaftaran();

        return KegiatanResponse.builder()
                .id(kegiatan.getId())
                .namaKegiatan(kegiatan.getNamaKegiatan())
                .name(kegiatan.getNamaKegiatan())
                .title(kegiatan.getNamaKegiatan())
                .deskripsi(kegiatan.getDeskripsi())
                .description(kegiatan.getDeskripsi())
                .kategori(kegiatan.getKategori().name())
                .category(kegiatan.getKategori().name())
                .tingkatKegiatan(kegiatan.getTingkatKegiatan())
                .tingkat(kegiatan.getTingkatKegiatan())
                .level(kegiatan.getTingkatKegiatan())
                .poinTak(kegiatan.getPoinTak())
                .points(kegiatan.getPoinTak())
                .wajib(kegiatan.isWajib())
                .statusKegiatan(statusKegiatan)
                .statusWajib(statusKegiatan)
                .jenisStatus(statusKegiatan)
                .bannerPath(kegiatan.getBannerPath())
                .bannerUrl(kegiatan.getBannerPath())
                .tanggal(kegiatan.getTanggal())
                .date(kegiatan.getTanggal())
                .startDate(kegiatan.getTanggal())
                .tanggalMulai(kegiatan.getTanggal())
                .tanggalSelesai(tanggalSelesai)
                .waktu(kegiatan.getWaktu())
                .time(kegiatan.getWaktu())
                .waktuMulai(kegiatan.getWaktu())
                .waktuSelesai(waktuSelesai)
                .waktuAkhir(waktuSelesai)
                .endTime(waktuSelesai)
                .modeKegiatan(kegiatan.getModeKegiatan())
                .mode(kegiatan.getModeKegiatan())
                .eventMode(kegiatan.getModeKegiatan())
                .lokasi(kegiatan.getLokasi())
                .location(kegiatan.getLokasi())
                .kuotaPeserta(kegiatan.getKuotaPeserta())
                .quota(kegiatan.getKuotaPeserta())
                .batasPendaftaran(kegiatan.getBatasPendaftaran())
                .pendaftaranMulai(pendaftaranMulai)
                .pendaftaranSelesai(kegiatan.getBatasPendaftaran())
                .registrationDeadline(kegiatan.getBatasPendaftaran())
                .statusPublikasi(kegiatan.getStatusPublikasi().name())
                .status(kegiatan.getStatusPublikasi().name())
                .createdAt(kegiatan.getCreatedAt())
                .updatedAt(kegiatan.getUpdatedAt())
                .eventOrganizerId(kegiatan.getEventOrganizer().getId())
                .organizerId(kegiatan.getEventOrganizer().getId())
                .eventOrganizerName(kegiatan.getEventOrganizer().getUser().getName())
                .jumlahPendaftar(jumlahPendaftar)
                .totalPeserta(jumlahPendaftar)
                .peserta(jumlahPendaftar)
                .registrants(jumlahPendaftar)
                .jumlahSertifikatTerbit(jumlahSertifikatTerbit)
                .sertifikatTerbit(jumlahSertifikatTerbit)
                .build();
    }

    private String saveBannerFile(MultipartFile banner) {
        if (banner == null || banner.isEmpty()) {
            return null;
        }
        if (banner.getSize() > MAX_BANNER_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Banner maksimal 2MB");
        }

        String originalFilename = banner.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Banner wajib JPG, PNG, atau WEBP");
        }
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

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    private String resolveStatusKegiatan(CreateKegiatanRequest request) {
        if (!isBlank(request.getStatusKegiatan())) {
            return request.getStatusKegiatan().trim().toUpperCase();
        }

        return request.isWajib() ? "WAJIB" : "PILIHAN";
    }

    private void validateUniqueNameForCreate(String namaKegiatan, Long eventOrganizerId) {
        if (kegiatanRepository.existsByNamaKegiatanIgnoreCaseAndEventOrganizerId(namaKegiatan.trim(), eventOrganizerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama kegiatan sudah digunakan");
        }
    }

    private void validateUniqueNameForUpdate(String namaKegiatan, Long eventOrganizerId, Long kegiatanId) {
        if (kegiatanRepository.existsByNamaKegiatanIgnoreCaseAndEventOrganizerIdAndIdNot(namaKegiatan.trim(), eventOrganizerId, kegiatanId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama kegiatan sudah digunakan");
        }
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
