package com.takhunter.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TerbitkanSertifikatPesertaRequest {

    private Long pendaftaranId;
    private Long mahasiswaId;
    private boolean layak;
}
