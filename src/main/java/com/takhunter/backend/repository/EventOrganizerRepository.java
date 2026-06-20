package com.takhunter.backend.repository;

import com.takhunter.backend.model.EventOrganizer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOrganizerRepository extends JpaRepository<EventOrganizer, Long> {

    Optional<EventOrganizer> findByUserId(Long userId);
}
