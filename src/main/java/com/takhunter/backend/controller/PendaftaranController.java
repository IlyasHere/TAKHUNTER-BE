package com.takhunter.backend.controller;

import com.takhunter.backend.dto.PendaftaranRequest;
import com.takhunter.backend.dto.PendaftaranResponse;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.Mahasiswa;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.model.Sertifikat;
import com.takhunter.backend.model.StatusPublikasi;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.MahasiswaRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
import com.takhunter.backend.repository.SertifikatRepository;
import com.takhunter.backend.service.NotifikasiService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pendaftaran")
@CrossOrigin(origins = "*")
public class PendaftaranController {

    private static final String ROLE_MAHASISWA = "MAHASISWA";

    private final PendaftaranRepository pendaftaranRepository;
    private final KegiatanRepository kegiatanRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final SertifikatRepository sertifikatRepository;
    private final NotifikasiService notifikasiService;

    public PendaftaranController(
            PendaftaranRepository pendaftaranRepository,
            KegiatanRepository kegiatanRepository,
            MahasiswaRepository mahasiswaRepository,
            SertifikatRepository sertifikatRepository,
            NotifikasiService notifikasiService
    ) {
        this.pendaftaranRepository = pendaftaranRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.sertifikatRepository = sertifikatRepository;
        this.notifikasiService = notifikasiService;
    }

    @GetMapping("/saya")
    public List<PendaftaranResponse> getPendaftaranSaya() {
        User user = getLoggedInUser();
        Mahasiswa mahasiswa = mahasiswaRepository.findByUserId(user.getId()).orElse(null);
        if (mahasiswa != null) {
            return pendaftaranRepository.findByMahasiswaIdOrderByIdDesc(mahasiswa.getId())
                    .stream()
                    .map((pendaftaran) -> {
                        Kegiatan kegiatan = kegiatanRepository.findById(pendaftaran.getKegiatanId()).orElse(null);
                        return toResponse(pendaftaran, kegiatan);
                    })
                    .toList();
        }

        String nim = mahasiswa != null ? mahasiswa.getNim() : "";
        String email = user.getEmail() != null ? user.getEmail() : "";

        return pendaftaranRepository.findByNimIgnoreCaseOrEmailIgnoreCaseOrderByIdDesc(nim, email)
                .stream()
                .map((pendaftaran) -> {
                    Kegiatan kegiatan = kegiatanRepository.findById(pendaftaran.getKegiatanId()).orElse(null);
                    return toResponse(pendaftaran, kegiatan);
                })
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> daftarKegiatan(@RequestBody PendaftaranRequest request) {
        if (request == null || request.getKegiatanId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID kegiatan wajib dikirim.");
        }

        Kegiatan kegiatan = kegiatanRepository.findByIdAndStatusPublikasi(request.getKegiatanId(), StatusPublikasi.AKTIF)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kegiatan aktif tidak ditemukan."));

        if (kegiatan.getBatasPendaftaran() != null && kegiatan.getBatasPendaftaran().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batas pendaftaran kegiatan sudah lewat.");
        }

        long jumlahPendaftar = pendaftaranRepository.countByKegiatanId(kegiatan.getId());
        if (kegiatan.getKuotaPeserta() != null && jumlahPendaftar >= kegiatan.getKuotaPeserta()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kuota peserta sudah penuh.");
        }

        User user = getOptionalLoggedInUser();
        Mahasiswa mahasiswa = null;
        if (user != null) {
            if (!ROLE_MAHASISWA.equals(normalizeRole(user.getRole()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya mahasiswa yang bisa mendaftar kegiatan.");
            }
            mahasiswa = mahasiswaRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Data mahasiswa tidak ditemukan."));
        }

        String namaMahasiswa = mahasiswa != null ? user.getName() : request.getNamaMahasiswa();
        String nim = mahasiswa != null ? mahasiswa.getNim() : request.getNim();
        String email = mahasiswa != null ? user.getEmail() : request.getEmail();
        String nomorWhatsApp = request.getNomorWhatsApp();

        if (isBlank(namaMahasiswa)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama mahasiswa wajib diisi.");
        }
        if (isBlank(nim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NIM wajib diisi.");
        }
        if (isBlank(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email wajib diisi.");
        }
        if (isBlank(nomorWhatsApp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nomor WhatsApp wajib diisi.");
        }

        if (mahasiswa != null && pendaftaranRepository.existsByKegiatanIdAndMahasiswaId(kegiatan.getId(), mahasiswa.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kamu sudah mendaftar kegiatan ini.");
        }
        if (pendaftaranRepository.existsByKegiatanIdAndNimIgnoreCase(kegiatan.getId(), nim.trim())
                || pendaftaranRepository.existsByKegiatanIdAndEmailIgnoreCase(kegiatan.getId(), email.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mahasiswa dengan NIM/email ini sudah mendaftar kegiatan ini.");
        }

        Pendaftaran pendaftaran = new Pendaftaran();
        pendaftaran.setKegiatanId(kegiatan.getId());
        pendaftaran.setMahasiswaId(mahasiswa != null ? mahasiswa.getId() : null);
        pendaftaran.setNamaMahasiswa(namaMahasiswa.trim());
        pendaftaran.setNim(nim.trim());
        pendaftaran.setProgramStudi(trimToNull(request.getProgramStudi()));
        pendaftaran.setEmail(email.trim().toLowerCase());
        pendaftaran.setNomorWhatsApp(nomorWhatsApp.trim());
        pendaftaran.setAlasan(trimToNull(request.getAlasan()));

        Pendaftaran savedPendaftaran = pendaftaranRepository.save(pendaftaran);
        notifikasiService.notifyNewRegistrant(kegiatan, savedPendaftaran);
        notifikasiService.notifyQuotaThresholds(kegiatan);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Pendaftaran berhasil disimpan!",
                "data", toResponse(savedPendaftaran, kegiatan)
        ));
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

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wajib dikirim");
        }

        return user;
    }

    private User getOptionalLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }

        return user;
    }

    private PendaftaranResponse toResponse(Pendaftaran pendaftaran, Kegiatan kegiatan) {
        Sertifikat sertifikat = pendaftaran.getMahasiswaId() != null
                ? sertifikatRepository.findByKegiatanIdAndMahasiswaId(pendaftaran.getKegiatanId(), pendaftaran.getMahasiswaId()).orElse(null)
                : null;
        boolean sertifikatTerbit = sertifikat != null;
        String sertifikatLink = sertifikat != null ? sertifikat.getDriveLink() : null;

        return PendaftaranResponse.builder()
                .id(pendaftaran.getId())
                .pendaftaranId(pendaftaran.getId())
                .kegiatanId(pendaftaran.getKegiatanId())
                .mahasiswaId(pendaftaran.getMahasiswaId())
                .namaKegiatan(kegiatan != null ? kegiatan.getNamaKegiatan() : null)
                .kategori(kegiatan != null ? kegiatan.getKategori().name() : null)
                .poinTak(kegiatan != null ? kegiatan.getPoinTak() : null)
                .tanggal(kegiatan != null ? kegiatan.getTanggal() : null)
                .tanggalMulai(kegiatan != null ? kegiatan.getTanggal() : null)
                .tanggalSelesai(kegiatan != null ? kegiatan.getTanggalSelesai() : null)
                .waktu(kegiatan != null ? kegiatan.getWaktu() : null)
                .waktuSelesai(kegiatan != null ? kegiatan.getWaktuSelesai() : null)
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
                .sertifikatLink(sertifikatLink)
                .driveLink(sertifikatLink)
                .certificateUrl(sertifikatLink)
                .build();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }
}
