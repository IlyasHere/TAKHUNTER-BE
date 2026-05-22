package com.takhunter.backend.controller;

import com.takhunter.backend.dto.PendaftaranRequest;
import com.takhunter.backend.dto.PendaftaranResponse;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.Mahasiswa;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.MahasiswaRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
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

    private final PendaftaranRepository pendaftaranRepository;
    private final KegiatanRepository kegiatanRepository;
    private final MahasiswaRepository mahasiswaRepository;

    public PendaftaranController(
            PendaftaranRepository pendaftaranRepository,
            KegiatanRepository kegiatanRepository,
            MahasiswaRepository mahasiswaRepository
    ) {
        this.pendaftaranRepository = pendaftaranRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.mahasiswaRepository = mahasiswaRepository;
    }

    @GetMapping("/saya")
    public List<PendaftaranResponse> getPendaftaranSaya() {
        User user = getLoggedInUser();
        Mahasiswa mahasiswa = mahasiswaRepository.findByUserId(user.getId()).orElse(null);
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
        try {
            if (request.getKegiatanId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID kegiatan wajib dikirim."));
            }

            if (isBlank(request.getNim())) {
                return ResponseEntity.badRequest().body(Map.of("message", "NIM wajib diisi."));
            }

            if (isBlank(request.getNomorWhatsApp())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nomor WhatsApp wajib diisi."));
            }

            Pendaftaran pendaftaran = new Pendaftaran();
            pendaftaran.setKegiatanId(request.getKegiatanId());
            pendaftaran.setNamaMahasiswa(request.getNamaMahasiswa());
            pendaftaran.setNim(request.getNim());
            pendaftaran.setProgramStudi(request.getProgramStudi());
            pendaftaran.setEmail(request.getEmail());
            pendaftaran.setNomorWhatsApp(request.getNomorWhatsApp());
            pendaftaran.setAlasan(request.getAlasan());
            
            // Simpan ke database
            pendaftaranRepository.save(pendaftaran);

            return ResponseEntity.ok(Map.of("message", "Pendaftaran berhasil disimpan!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Gagal mendaftar: " + e.getMessage()));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wajib dikirim");
        }

        return user;
    }

    private PendaftaranResponse toResponse(Pendaftaran pendaftaran, Kegiatan kegiatan) {
        return PendaftaranResponse.builder()
                .id(pendaftaran.getId())
                .kegiatanId(pendaftaran.getKegiatanId())
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
                .status(pendaftaran.getStatus())
                .build();
    }
}
