package com.takhunter.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class KegiatanResponse {

    private Long id;
    private String namaKegiatan;
    private String deskripsi;
    private String kategori;
    private Integer poinTak;
    private boolean wajib;
    private String bannerPath;
    private LocalDate tanggal;
    private LocalTime waktu;
    private String lokasi;
    private Integer kuotaPeserta;
    private LocalDate batasPendaftaran;
    private String statusPublikasi;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long eventOrganizerId;
}
