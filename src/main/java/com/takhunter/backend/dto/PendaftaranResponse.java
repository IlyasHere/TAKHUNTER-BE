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
public class PendaftaranResponse {

    private Long id;
    private Long kegiatanId;
    private String namaKegiatan;
    private String kategori;
    private Integer poinTak;
    private LocalDate tanggal;
    private LocalTime waktu;
    private String lokasi;
    private String eventOrganizerName;
    private String namaMahasiswa;
    private String nim;
    private String programStudi;
    private String email;
    private String nomorWhatsApp;
    private String alasan;
    private String status;
}