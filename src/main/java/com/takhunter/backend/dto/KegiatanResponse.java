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
    private String name;
    private String title;
    private String deskripsi;
    private String description;
    private String kategori;
    private String category;
    private String tingkatKegiatan;
    private String tingkat;
    private String level;
    private Integer poinTak;
    private Integer points;
    private boolean wajib;
    private String statusKegiatan;
    private String statusWajib;
    private String jenisStatus;
    private String bannerPath;
    private String bannerUrl;
    private LocalDate tanggal;
    private LocalDate date;
    private LocalDate startDate;
    private LocalDate tanggalMulai;
    private LocalDate tanggalSelesai;
    private LocalTime waktu;
    private LocalTime time;
    private LocalTime waktuMulai;
    private LocalTime waktuSelesai;
    private LocalTime waktuAkhir;
    private LocalTime endTime;
    private String modeKegiatan;
    private String mode;
    private String eventMode;
    private String lokasi;
    private String location;
    private Integer kuotaPeserta;
    private Integer quota;
    private LocalDate batasPendaftaran;
    private LocalDate pendaftaranMulai;
    private LocalDate pendaftaranSelesai;
    private LocalDate registrationDeadline;
    private String statusPublikasi;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long eventOrganizerId;
    private Long organizerId;
    private String eventOrganizerName;
    private Long jumlahPendaftar;
    private Long totalPeserta;
    private Long peserta;
    private Long registrants;
    private Long jumlahSertifikatTerbit;
    private Long sertifikatTerbit;
}
