package com.takhunter.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SertifikatMahasiswaResponse {

    private Long id;
    private Long kegiatanId;
    private String namaKegiatan;
    private String kategori;
    private LocalDate tanggalKegiatan;
    private String penyelenggara;
    private String driveLink;
    private Integer poinTak;
    private LocalDateTime issuedAt;
    private String status;
}
