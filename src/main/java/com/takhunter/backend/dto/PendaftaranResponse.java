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
public class PendaftaranResponse {

    private Long id;
    private Long pendaftaranId;
    private Long kegiatanId;
    private Long mahasiswaId;
    private String namaKegiatan;
    private String kategori;
    private Integer poinTak;
    private LocalDate tanggal;
    private LocalDate tanggalMulai;
    private LocalDate tanggalSelesai;
    private LocalTime waktu;
    private LocalTime waktuSelesai;
    private String lokasi;
    private String eventOrganizerName;
    private String namaMahasiswa;
    private String nim;
    private String programStudi;
    private String email;
    private String nomorWhatsApp;
    private String alasan;
    private String alasanDitolak;
    private String status;
    private String statusPendaftaran;
    private LocalDateTime tanggalDaftar;
    private LocalDateTime tanggalPendaftaran;
    private boolean sertifikatTerbit;
    private String sertifikatLink;
    private String driveLink;
    private String certificateUrl;
}
