package com.takhunter.backend.service;

import com.takhunter.backend.dto.SertifikatKegiatanResponse;
import com.takhunter.backend.dto.SertifikatMahasiswaResponse;
import com.takhunter.backend.dto.SertifikatPesertaResponse;
import com.takhunter.backend.dto.TerbitkanSertifikatPesertaRequest;
import com.takhunter.backend.dto.TerbitkanSertifikatRequest;
import com.takhunter.backend.model.EventOrganizer;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.Mahasiswa;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.model.Sertifikat;
import com.takhunter.backend.model.StatusSertifikat;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.EventOrganizerRepository;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.MahasiswaRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
import com.takhunter.backend.repository.SertifikatRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SertifikatService {

    private static final String ROLE_EVENT_ORGANIZER = "EVENT_ORGANIZER";
    private static final String ROLE_MAHASISWA = "MAHASISWA";
    private static final String STATUS_DITERIMA = "DITERIMA";

    private final SertifikatRepository sertifikatRepository;
    private final KegiatanRepository kegiatanRepository;
    private final PendaftaranRepository pendaftaranRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final EventOrganizerRepository eventOrganizerRepository;

    public SertifikatService(
            SertifikatRepository sertifikatRepository,
            KegiatanRepository kegiatanRepository,
            PendaftaranRepository pendaftaranRepository,
            MahasiswaRepository mahasiswaRepository,
            EventOrganizerRepository eventOrganizerRepository
    ) {
        this.sertifikatRepository = sertifikatRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.pendaftaranRepository = pendaftaranRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
    }

    public List<SertifikatKegiatanResponse> getKegiatanSertifikatForEO() {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();

        return kegiatanRepository.findByEventOrganizerIdOrderByCreatedAtDesc(eventOrganizer.getId())
                .stream()
                .map(this::toKegiatanResponse)
                .toList();
    }

    public List<SertifikatPesertaResponse> getPesertaSertifikat(Long kegiatanId) {
        Kegiatan kegiatan = getOwnedKegiatan(kegiatanId);

        return pendaftaranRepository.findByKegiatanIdOrderByIdDesc(kegiatan.getId())
                .stream()
                .map(this::toPesertaResponse)
                .toList();
    }

    @Transactional
    public Map<String, Object> terbitkanSertifikatMassal(Long kegiatanId, TerbitkanSertifikatRequest request) {
        Kegiatan kegiatan = getOwnedKegiatan(kegiatanId);
        validateTerbitkanRequest(request);

        int jumlahTerbit = 0;
        int jumlahDilewati = 0;

        for (TerbitkanSertifikatPesertaRequest pesertaRequest : request.getPeserta()) {
            if (pesertaRequest == null || !pesertaRequest.isLayak() || pesertaRequest.getMahasiswaId() == null) {
                jumlahDilewati++;
                continue;
            }

            Pendaftaran pendaftaran = findValidPendaftaranForTerbit(kegiatan, pesertaRequest.getPendaftaranId());
            if (pendaftaran == null || !STATUS_DITERIMA.equals(normalizeStatus(pendaftaran.getStatus()))) {
                jumlahDilewati++;
                continue;
            }

            Mahasiswa mahasiswa = findValidMahasiswaForTerbit(pendaftaran, pesertaRequest.getMahasiswaId());
            if (mahasiswa == null) {
                jumlahDilewati++;
                continue;
            }

            if (sertifikatRepository.existsByKegiatanIdAndMahasiswaId(kegiatan.getId(), mahasiswa.getId())) {
                jumlahDilewati++;
                continue;
            }

            Sertifikat sertifikat = Sertifikat.builder()
                    .kegiatan(kegiatan)
                    .pendaftaran(pendaftaran)
                    .mahasiswa(mahasiswa)
                    .eventOrganizer(kegiatan.getEventOrganizer())
                    .driveLink(request.getDriveLink().trim())
                    .poinTak(kegiatan.getPoinTak())
                    .status(StatusSertifikat.TERBIT)
                    .build();
            sertifikatRepository.save(sertifikat);

            int totalPoinTak = mahasiswa.getTotalPoinTak() == null ? 0 : mahasiswa.getTotalPoinTak();
            mahasiswa.setTotalPoinTak(totalPoinTak + kegiatan.getPoinTak());
            mahasiswaRepository.save(mahasiswa);
            jumlahTerbit++;
        }

        return Map.of(
                "message", "Sertifikat berhasil diterbitkan",
                "jumlahTerbit", jumlahTerbit,
                "jumlahDilewati", jumlahDilewati
        );
    }

    public List<SertifikatMahasiswaResponse> getSertifikatMahasiswa() {
        Mahasiswa mahasiswa = getLoggedInMahasiswa();

        return sertifikatRepository.findByMahasiswaIdOrderByIssuedAtDesc(mahasiswa.getId())
                .stream()
                .map(this::toMahasiswaResponse)
                .toList();
    }

    private void validateTerbitkanRequest(TerbitkanSertifikatRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request sertifikat wajib diisi");
        }
        if (isBlank(request.getDriveLink())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link sertifikat wajib diisi");
        }
        if (request.getPeserta() == null || request.getPeserta().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Peserta tidak valid");
        }
    }

    private Pendaftaran findValidPendaftaranForTerbit(Kegiatan kegiatan, Long pendaftaranId) {
        if (pendaftaranId == null) {
            return null;
        }

        Pendaftaran pendaftaran = pendaftaranRepository.findById(pendaftaranId).orElse(null);
        if (pendaftaran == null) {
            return null;
        }

        if (!kegiatan.getId().equals(pendaftaran.getKegiatanId())) {
            return null;
        }

        return pendaftaran;
    }

    private Mahasiswa findValidMahasiswaForTerbit(Pendaftaran pendaftaran, Long mahasiswaId) {
        if (mahasiswaId == null) {
            return null;
        }

        Mahasiswa mahasiswa = mahasiswaRepository.findById(mahasiswaId).orElse(null);
        if (mahasiswa == null) {
            return null;
        }

        boolean nimCocok = !isBlank(pendaftaran.getNim())
                && pendaftaran.getNim().trim().equalsIgnoreCase(mahasiswa.getNim());
        boolean emailCocok = !isBlank(pendaftaran.getEmail())
                && mahasiswa.getUser() != null
                && !isBlank(mahasiswa.getUser().getEmail())
                && pendaftaran.getEmail().trim().equalsIgnoreCase(mahasiswa.getUser().getEmail());

        if (!nimCocok && !emailCocok) {
            return null;
        }

        return mahasiswa;
    }

    private Kegiatan getOwnedKegiatan(Long kegiatanId) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();

        return kegiatanRepository.findByIdAndEventOrganizerId(kegiatanId, eventOrganizer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kegiatan tidak ditemukan"));
    }

    private EventOrganizer getLoggedInEventOrganizer() {
        User user = getLoggedInUser();
        if (!ROLE_EVENT_ORGANIZER.equals(normalizeRole(user.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke kegiatan ini");
        }

        return eventOrganizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Data Event Organizer tidak ditemukan"));
    }

    private Mahasiswa getLoggedInMahasiswa() {
        User user = getLoggedInUser();
        if (!ROLE_MAHASISWA.equals(normalizeRole(user.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Mahasiswa yang boleh akses");
        }

        return mahasiswaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Data mahasiswa tidak ditemukan"));
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wajib dikirim");
        }

        return user;
    }

    private SertifikatKegiatanResponse toKegiatanResponse(Kegiatan kegiatan) {
        return SertifikatKegiatanResponse.builder()
                .id(kegiatan.getId())
                .namaKegiatan(kegiatan.getNamaKegiatan())
                .kategori(kegiatan.getKategori().name())
                .tanggal(kegiatan.getTanggal())
                .waktu(kegiatan.getWaktu())
                .lokasi(kegiatan.getLokasi())
                .poinTak(kegiatan.getPoinTak())
                .statusPublikasi(kegiatan.getStatusPublikasi().name())
                .jumlahPendaftar(pendaftaranRepository.countByKegiatanId(kegiatan.getId()))
                .jumlahSertifikatTerbit(sertifikatRepository.countByKegiatanId(kegiatan.getId()))
                .build();
    }

    private SertifikatPesertaResponse toPesertaResponse(Pendaftaran pendaftaran) {
        Mahasiswa mahasiswa = resolveMahasiswa(pendaftaran);
        boolean sertifikatTerbit = mahasiswa != null
                && sertifikatRepository.existsByKegiatanIdAndMahasiswaId(pendaftaran.getKegiatanId(), mahasiswa.getId());

        return SertifikatPesertaResponse.builder()
                .pendaftaranId(pendaftaran.getId())
                .mahasiswaId(mahasiswa != null ? mahasiswa.getId() : null)
                .namaMahasiswa(pendaftaran.getNamaMahasiswa())
                .nim(pendaftaran.getNim())
                .email(pendaftaran.getEmail())
                .programStudi(pendaftaran.getProgramStudi())
                .statusPendaftaran(pendaftaran.getStatus())
                .layakSertifikat(STATUS_DITERIMA.equals(normalizeStatus(pendaftaran.getStatus())))
                .sertifikatTerbit(sertifikatTerbit)
                .build();
    }

    private SertifikatMahasiswaResponse toMahasiswaResponse(Sertifikat sertifikat) {
        Kegiatan kegiatan = sertifikat.getKegiatan();

        return SertifikatMahasiswaResponse.builder()
                .id(sertifikat.getId())
                .kegiatanId(kegiatan.getId())
                .namaKegiatan(kegiatan.getNamaKegiatan())
                .kategori(kegiatan.getKategori().name())
                .tanggalKegiatan(kegiatan.getTanggal())
                .penyelenggara(sertifikat.getEventOrganizer().getUser().getName())
                .driveLink(sertifikat.getDriveLink())
                .poinTak(sertifikat.getPoinTak())
                .issuedAt(sertifikat.getIssuedAt())
                .status(sertifikat.getStatus().name())
                .build();
    }

    private Mahasiswa resolveMahasiswa(Pendaftaran pendaftaran) {
        if (!isBlank(pendaftaran.getNim())) {
            return mahasiswaRepository.findByNimIgnoreCase(pendaftaran.getNim().trim()).orElse(null);
        }

        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }

        return status.trim().toUpperCase();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        return role.trim()
                .replace("-", "_")
                .toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
