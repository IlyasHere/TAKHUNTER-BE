package com.takhunter.backend.repository;

import com.takhunter.backend.model.BookmarkKegiatan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkKegiatanRepository extends JpaRepository<BookmarkKegiatan, Long> {

    List<BookmarkKegiatan> findByMahasiswaIdOrderByCreatedAtDesc(Long mahasiswaId);

    Optional<BookmarkKegiatan> findByMahasiswaIdAndKegiatanId(Long mahasiswaId, Long kegiatanId);

    boolean existsByMahasiswaIdAndKegiatanId(Long mahasiswaId, Long kegiatanId);
}
