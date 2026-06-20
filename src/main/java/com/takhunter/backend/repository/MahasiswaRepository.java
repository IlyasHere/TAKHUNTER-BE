package com.takhunter.backend.repository;

import com.takhunter.backend.model.Mahasiswa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MahasiswaRepository extends JpaRepository<Mahasiswa, Long> {

    boolean existsByNim(String nim);

    Optional<Mahasiswa> findByUserId(Long userId);

    Optional<Mahasiswa> findByNimIgnoreCase(String nim);

    Optional<Mahasiswa> findByUser_EmailIgnoreCase(String email);
}
