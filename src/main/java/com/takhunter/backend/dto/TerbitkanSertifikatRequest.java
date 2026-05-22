package com.takhunter.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TerbitkanSertifikatRequest {

    private String driveLink;
    private List<TerbitkanSertifikatPesertaRequest> peserta;
}
