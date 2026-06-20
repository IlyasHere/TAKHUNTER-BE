package com.takhunter.backend.repository;

import com.takhunter.backend.model.Notifikasi;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifikasiRepository extends JpaRepository<Notifikasi, Long> {

    List<Notifikasi> findByEventOrganizerIdOrderByCreatedAtDesc(Long eventOrganizerId);

    Optional<Notifikasi> findByIdAndEventOrganizerId(Long id, Long eventOrganizerId);

    boolean existsByDedupeKey(String dedupeKey);

    List<Notifikasi> findByEventOrganizerIdAndIsReadFalse(Long eventOrganizerId);

    void deleteByEventOrganizerId(Long eventOrganizerId);
}
