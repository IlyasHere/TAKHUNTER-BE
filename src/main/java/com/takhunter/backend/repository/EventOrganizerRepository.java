package com.takhunter.backend.repository;

import com.takhunter.backend.model.EventOrganizer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOrganizerRepository extends JpaRepository<EventOrganizer, Long> {
}
