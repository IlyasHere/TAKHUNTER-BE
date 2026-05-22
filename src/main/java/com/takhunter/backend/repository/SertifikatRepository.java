package com.takhunter.backend.repository;

import com.takhunter.backend.model.Sertifikat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SertifikatRepository extends JpaRepository<Sertifikat, Long> {

    List<Sertifikat> findByMahasiswaIdOrderByIssuedAtDesc(Long mahasiswaId);

    List<Sertifikat> findByMahasiswaId(Long mahasiswaId);

    List<Sertifikat> findByKegiatanId(Long kegiatanId);

    boolean existsByKegiatanIdAndMahasiswaId(Long kegiatanId, Long mahasiswaId);

    long countByKegiatanId(Long kegiatanId);

    Optional<Sertifikat> findByKegiatanIdAndMahasiswaId(Long kegiatanId, Long mahasiswaId);
}
