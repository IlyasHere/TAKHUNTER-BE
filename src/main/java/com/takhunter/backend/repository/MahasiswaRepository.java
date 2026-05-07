package com.takhunter.backend.repository;

import com.takhunter.backend.model.Mahasiswa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MahasiswaRepository extends JpaRepository<Mahasiswa, Long> {

    boolean existsByNim(String nim);
}
