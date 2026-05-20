package com.takhunter.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateKegiatanRequest {

    private String namaKegiatan;
    private String deskripsi;
    private String kategori;
    private Integer poinTak;
    private boolean wajib;
    private LocalDate tanggal;
    private LocalTime waktu;
    private String lokasi;
    private Integer kuotaPeserta;
    private LocalDate batasPendaftaran;
    private String statusPublikasi;
}
