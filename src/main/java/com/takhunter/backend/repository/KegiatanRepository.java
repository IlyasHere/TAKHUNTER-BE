package com.takhunter.backend.repository;

import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.StatusPublikasi;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KegiatanRepository extends JpaRepository<Kegiatan, Long> {

    List<Kegiatan> findByEventOrganizerIdOrderByCreatedAtDesc(Long eventOrganizerId);

    Optional<Kegiatan> findByIdAndEventOrganizerId(Long id, Long eventOrganizerId);

    List<Kegiatan> findByStatusPublikasiOrderByCreatedAtDesc(StatusPublikasi statusPublikasi);

    Optional<Kegiatan> findByIdAndStatusPublikasi(Long id, StatusPublikasi statusPublikasi);
}
