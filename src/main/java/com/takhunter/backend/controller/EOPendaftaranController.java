package com.takhunter.backend.controller;

import com.takhunter.backend.dto.PendaftaranResponse;
import com.takhunter.backend.model.EventOrganizer;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.EventOrganizerRepository;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
import com.takhunter.backend.repository.SertifikatRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/eo/pendaftaran")
public class EOPendaftaranController {

    private static final String ROLE_EVENT_ORGANIZER = "EVENT_ORGANIZER";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DITERIMA = "DITERIMA";
    private static final String STATUS_DITOLAK = "DITOLAK";

    private final EventOrganizerRepository eventOrganizerRepository;
    private final KegiatanRepository kegiatanRepository;
    private final PendaftaranRepository pendaftaranRepository;
    private final SertifikatRepository sertifikatRepository;

    public EOPendaftaranController(
            EventOrganizerRepository eventOrganizerRepository,
            KegiatanRepository kegiatanRepository,
            PendaftaranRepository pendaftaranRepository,
            SertifikatRepository sertifikatRepository
    ) {
        this.eventOrganizerRepository = eventOrganizerRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.pendaftaranRepository = pendaftaranRepository;
        this.sertifikatRepository = sertifikatRepository;
    }

    @GetMapping("/kegiatan/{kegiatanId}")
    public List<PendaftaranResponse> getPendaftarByKegiatan(@PathVariable Long kegiatanId) {
        Kegiatan kegiatan = getOwnedKegiatan(kegiatanId);

        return pendaftaranRepository.findByKegiatanIdOrderByIdDesc(kegiatan.getId())
                .stream()
                .map((pendaftaran) -> toResponse(pendaftaran, kegiatan))
                .toList();
    }

    @GetMapping("/riwayat")
    public List<PendaftaranResponse> getRiwayatPesertaDiterima() {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        List<Kegiatan> kegiatanList = kegiatanRepository.findByEventOrganizerIdOrderByCreatedAtDesc(eventOrganizer.getId());
        List<Long> kegiatanIds = kegiatanList.stream().map(Kegiatan::getId).toList();

        if (kegiatanIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Kegiatan> kegiatanById = kegiatanList.stream()
                .collect(Collectors.toMap(Kegiatan::getId, (kegiatan) -> kegiatan));

        return pendaftaranRepository.findByKegiatanIdInAndStatusOrderByIdDesc(kegiatanIds, STATUS_DITERIMA)
                .stream()
                .map((pendaftaran) -> toResponse(pendaftaran, kegiatanById.get(pendaftaran.getKegiatanId())))
                .toList();
    }

    @GetMapping("/statistik")
    public Map<String, Object> getStatistikPendaftaran() {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        List<Kegiatan> kegiatanList = kegiatanRepository.findByEventOrganizerIdOrderByCreatedAtDesc(eventOrganizer.getId());
        List<Long> kegiatanIds = kegiatanList.stream().map(Kegiatan::getId).toList();

        if (kegiatanIds.isEmpty()) {
            return Map.of(
                    "totalKegiatan", 0,
                    "totalPendaftar", 0,
                    "pending", 0,
                    "diterima", 0,
                    "ditolak", 0
            );
        }

        List<Pendaftaran> pendaftaranList = pendaftaranRepository.findByKegiatanIdInOrderByIdDesc(kegiatanIds);

        long pending = pendaftaranList.stream().filter((pendaftaran) -> STATUS_PENDING.equals(normalizeStatusValue(pendaftaran.getStatus()))).count();
        long diterima = pendaftaranList.stream().filter((pendaftaran) -> STATUS_DITERIMA.equals(normalizeStatusValue(pendaftaran.getStatus()))).count();
        long ditolak = pendaftaranList.stream().filter((pendaftaran) -> STATUS_DITOLAK.equals(normalizeStatusValue(pendaftaran.getStatus()))).count();

        return Map.of(
                "totalKegiatan", kegiatanList.size(),
                "totalPendaftar", pendaftaranList.size(),
                "pending", pending,
                "diterima", diterima,
                "ditolak", ditolak
        );
    }

    @PatchMapping("/{pendaftaranId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long pendaftaranId,
            @RequestBody Map<String, String> request
    ) {
        Pendaftaran pendaftaran = pendaftaranRepository.findById(pendaftaranId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pendaftaran tidak ditemukan"));
        Kegiatan kegiatan = getOwnedKegiatan(pendaftaran.getKegiatanId());
        String status = normalizeStatus(request.get("status"));

        pendaftaran.setStatus(status);
        if (STATUS_DITOLAK.equals(status)) {
            String alasanDitolak = request.get("alasanPenolakan") != null
                    ? request.get("alasanPenolakan")
                    : request.get("catatanPenolakan") != null
                            ? request.get("catatanPenolakan")
                            : request.get("catatan");
            pendaftaran.setAlasanDitolak(alasanDitolak);
        } else {
            pendaftaran.setAlasanDitolak(null);
        }
        Pendaftaran savedPendaftaran = pendaftaranRepository.save(pendaftaran);

        return ResponseEntity.ok(toResponse(savedPendaftaran, kegiatan));
    }

    private Kegiatan getOwnedKegiatan(Long kegiatanId) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();

        return kegiatanRepository.findByIdAndEventOrganizerId(kegiatanId, eventOrganizer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kegiatan tidak ditemukan untuk Event Organizer ini"));
    }

    private EventOrganizer getLoggedInEventOrganizer() {
        User user = getLoggedInUser();
        if (!ROLE_EVENT_ORGANIZER.equals(normalizeRole(user.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Event Organizer yang boleh akses");
        }

        return eventOrganizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Data Event Organizer tidak ditemukan"));
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wajib dikirim");
        }

        return user;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status wajib dikirim");
        }

        String normalizedStatus = status.trim().toUpperCase();
        if (STATUS_PENDING.equals(normalizedStatus) || STATUS_DITERIMA.equals(normalizedStatus) || STATUS_DITOLAK.equals(normalizedStatus)) {
            return normalizedStatus;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status wajib PENDING, DITERIMA, atau DITOLAK");
    }

    private String normalizeStatusValue(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private PendaftaranResponse toResponse(Pendaftaran pendaftaran, Kegiatan kegiatan) {
        boolean sertifikatTerbit = pendaftaran.getMahasiswaId() != null
                && sertifikatRepository.existsByKegiatanIdAndMahasiswaId(pendaftaran.getKegiatanId(), pendaftaran.getMahasiswaId());

        return PendaftaranResponse.builder()
                .id(pendaftaran.getId())
                .pendaftaranId(pendaftaran.getId())
                .kegiatanId(pendaftaran.getKegiatanId())
                .mahasiswaId(pendaftaran.getMahasiswaId())
                .namaKegiatan(kegiatan != null ? kegiatan.getNamaKegiatan() : null)
                .kategori(kegiatan != null ? kegiatan.getKategori().name() : null)
                .poinTak(kegiatan != null ? kegiatan.getPoinTak() : null)
                .tanggal(kegiatan != null ? kegiatan.getTanggal() : null)
                .waktu(kegiatan != null ? kegiatan.getWaktu() : null)
                .lokasi(kegiatan != null ? kegiatan.getLokasi() : null)
                .eventOrganizerName(kegiatan != null ? kegiatan.getEventOrganizer().getUser().getName() : null)
                .namaMahasiswa(pendaftaran.getNamaMahasiswa())
                .nim(pendaftaran.getNim())
                .programStudi(pendaftaran.getProgramStudi())
                .email(pendaftaran.getEmail())
                .nomorWhatsApp(pendaftaran.getNomorWhatsApp())
                .alasan(pendaftaran.getAlasan())
                .alasanDitolak(pendaftaran.getAlasanDitolak())
                .status(pendaftaran.getStatus())
                .statusPendaftaran(pendaftaran.getStatus())
                .tanggalDaftar(pendaftaran.getTanggalDaftar())
                .tanggalPendaftaran(pendaftaran.getTanggalDaftar())
                .sertifikatTerbit(sertifikatTerbit)
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
