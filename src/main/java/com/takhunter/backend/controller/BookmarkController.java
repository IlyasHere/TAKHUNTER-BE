package com.takhunter.backend.controller;

import com.takhunter.backend.dto.KegiatanResponse;
import com.takhunter.backend.model.BookmarkKegiatan;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.Mahasiswa;
import com.takhunter.backend.model.StatusPublikasi;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.BookmarkKegiatanRepository;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.MahasiswaRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookmark")
public class BookmarkController {

    private final BookmarkKegiatanRepository bookmarkKegiatanRepository;
    private final KegiatanRepository kegiatanRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final PendaftaranRepository pendaftaranRepository;

    public BookmarkController(
            BookmarkKegiatanRepository bookmarkKegiatanRepository,
            KegiatanRepository kegiatanRepository,
            MahasiswaRepository mahasiswaRepository,
            PendaftaranRepository pendaftaranRepository
    ) {
        this.bookmarkKegiatanRepository = bookmarkKegiatanRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.pendaftaranRepository = pendaftaranRepository;
    }

    @GetMapping
    public List<KegiatanResponse> getBookmarks() {
        Mahasiswa mahasiswa = getLoggedInMahasiswa();

        return bookmarkKegiatanRepository.findByMahasiswaIdOrderByCreatedAtDesc(mahasiswa.getId())
                .stream()
                .map(BookmarkKegiatan::getKegiatan)
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/ids")
    public List<Long> getBookmarkIds() {
        Mahasiswa mahasiswa = getLoggedInMahasiswa();

        return bookmarkKegiatanRepository.findByMahasiswaIdOrderByCreatedAtDesc(mahasiswa.getId())
                .stream()
                .map((bookmark) -> bookmark.getKegiatan().getId())
                .toList();
    }

    @PostMapping("/{kegiatanId}")
    public ResponseEntity<?> addBookmark(@PathVariable Long kegiatanId) {
        Mahasiswa mahasiswa = getLoggedInMahasiswa();
        Kegiatan kegiatan = getActiveKegiatan(kegiatanId);

        if (!bookmarkKegiatanRepository.existsByMahasiswaIdAndKegiatanId(mahasiswa.getId(), kegiatan.getId())) {
            BookmarkKegiatan bookmark = BookmarkKegiatan.builder()
                    .mahasiswa(mahasiswa)
                    .kegiatan(kegiatan)
                    .build();
            bookmarkKegiatanRepository.save(bookmark);
        }

        return ResponseEntity.ok(Map.of("message", "Kegiatan berhasil disimpan."));
    }

    @DeleteMapping("/{kegiatanId}")
    public ResponseEntity<?> removeBookmark(@PathVariable Long kegiatanId) {
        Mahasiswa mahasiswa = getLoggedInMahasiswa();
        BookmarkKegiatan bookmark = bookmarkKegiatanRepository.findByMahasiswaIdAndKegiatanId(mahasiswa.getId(), kegiatanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bookmark tidak ditemukan"));

        bookmarkKegiatanRepository.delete(bookmark);

        return ResponseEntity.ok(Map.of("message", "Bookmark berhasil dihapus."));
    }

    private Kegiatan getActiveKegiatan(Long kegiatanId) {
        return kegiatanRepository.findByIdAndStatusPublikasi(kegiatanId, StatusPublikasi.AKTIF)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kegiatan aktif tidak ditemukan"));
    }

    private Mahasiswa getLoggedInMahasiswa() {
        User user = getLoggedInUser();

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

    private KegiatanResponse toResponse(Kegiatan kegiatan) {
        long jumlahPendaftar = pendaftaranRepository.countByKegiatanId(kegiatan.getId());

        return KegiatanResponse.builder()
                .id(kegiatan.getId())
                .namaKegiatan(kegiatan.getNamaKegiatan())
                .name(kegiatan.getNamaKegiatan())
                .title(kegiatan.getNamaKegiatan())
                .deskripsi(kegiatan.getDeskripsi())
                .description(kegiatan.getDeskripsi())
                .kategori(kegiatan.getKategori().name())
                .category(kegiatan.getKategori().name())
                .poinTak(kegiatan.getPoinTak())
                .points(kegiatan.getPoinTak())
                .wajib(kegiatan.isWajib())
                .bannerPath(kegiatan.getBannerPath())
                .bannerUrl(kegiatan.getBannerPath())
                .tanggal(kegiatan.getTanggal())
                .date(kegiatan.getTanggal())
                .startDate(kegiatan.getTanggal())
                .tanggalMulai(kegiatan.getTanggal())
                .tanggalSelesai(kegiatan.getTanggal())
                .waktu(kegiatan.getWaktu())
                .time(kegiatan.getWaktu())
                .waktuMulai(kegiatan.getWaktu())
                .lokasi(kegiatan.getLokasi())
                .location(kegiatan.getLokasi())
                .kuotaPeserta(kegiatan.getKuotaPeserta())
                .quota(kegiatan.getKuotaPeserta())
                .batasPendaftaran(kegiatan.getBatasPendaftaran())
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
                .build();
    }
}
