package com.takhunter.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SertifikatKegiatanResponse {

    private Long id;
    private String namaKegiatan;
    private String kategori;
    private LocalDate tanggal;
    private LocalTime waktu;
    private String lokasi;
    private Integer poinTak;
    private String statusPublikasi;
    private Long jumlahPendaftar;
    private Long jumlahSertifikatTerbit;
}
