package com.takhunter.backend.repository;

import com.takhunter.backend.model.Pendaftaran;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendaftaranRepository extends JpaRepository<Pendaftaran, Long> {
    // Nanti bisa tambah custom query misal mencari pendaftaran berdasarkan ID kegiatan
    List<Pendaftaran> findByKegiatanId(Long kegiatanId);

    List<Pendaftaran> findByKegiatanIdOrderByIdDesc(Long kegiatanId);

    long countByKegiatanId(Long kegiatanId);

    List<Pendaftaran> findByNimIgnoreCaseOrEmailIgnoreCaseOrderByIdDesc(String nim, String email);

    List<Pendaftaran> findByKegiatanIdInAndStatusOrderByIdDesc(List<Long> kegiatanIds, String status);
}
