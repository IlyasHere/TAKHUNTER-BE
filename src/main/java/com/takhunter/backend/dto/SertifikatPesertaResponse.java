package com.takhunter.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SertifikatPesertaResponse {

    private Long pendaftaranId;
    private Long mahasiswaId;
    private String namaMahasiswa;
    private String nim;
    private String email;
    private String programStudi;
    private String statusPendaftaran;
    private boolean layakSertifikat;
    private boolean sertifikatTerbit;
}
