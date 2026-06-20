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
    private String category;
    private String tingkatKegiatan;
    private Integer poinTak;
    private boolean wajib;
    private String statusKegiatan;
    private LocalDate tanggal;
    private LocalDate tanggalMulai;
    private LocalDate tanggalSelesai;
    private LocalTime waktu;
    private LocalTime waktuMulai;
    private LocalTime waktuSelesai;
    private String modeKegiatan;
    private String lokasi;
    private Integer kuotaPeserta;
    private LocalDate batasPendaftaran;
    private LocalDate pendaftaranMulai;
    private LocalDate pendaftaranSelesai;
    private String statusPublikasi;
    private String status;

    public String getKategori() {
        return kategori != null ? kategori : category;
    }

    public LocalDate getTanggal() {
        return tanggal != null ? tanggal : tanggalMulai;
    }

    public LocalTime getWaktu() {
        return waktu != null ? waktu : waktuMulai;
    }

    public LocalDate getBatasPendaftaran() {
        return batasPendaftaran != null ? batasPendaftaran : pendaftaranSelesai;
    }

    public String getStatusPublikasi() {
        return statusPublikasi != null ? statusPublikasi : status;
    }
}
