package com.takhunter.backend.controller;

import com.takhunter.backend.dto.SertifikatKegiatanResponse;
import com.takhunter.backend.dto.SertifikatMahasiswaResponse;
import com.takhunter.backend.dto.SertifikatPesertaResponse;
import com.takhunter.backend.dto.TerbitkanSertifikatRequest;
import com.takhunter.backend.service.SertifikatService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SertifikatController {

    private final SertifikatService sertifikatService;

    public SertifikatController(SertifikatService sertifikatService) {
        this.sertifikatService = sertifikatService;
    }

    @GetMapping("/api/eo/sertifikat/kegiatan")
    public List<SertifikatKegiatanResponse> getKegiatanSertifikatForEO() {
        return sertifikatService.getKegiatanSertifikatForEO();
    }

    @GetMapping("/api/eo/sertifikat/kegiatan/{kegiatanId}/peserta")
    public List<SertifikatPesertaResponse> getPesertaSertifikat(@PathVariable Long kegiatanId) {
        return sertifikatService.getPesertaSertifikat(kegiatanId);
    }

    @PostMapping("/api/eo/sertifikat/kegiatan/{kegiatanId}/terbitkan")
    public ResponseEntity<Map<String, Object>> terbitkanSertifikatMassal(
            @PathVariable Long kegiatanId,
            @RequestBody TerbitkanSertifikatRequest request
    ) {
        return ResponseEntity.ok(sertifikatService.terbitkanSertifikatMassal(kegiatanId, request));
    }

    @GetMapping("/api/mahasiswa/sertifikat")
    public List<SertifikatMahasiswaResponse> getSertifikatMahasiswa() {
        return sertifikatService.getSertifikatMahasiswa();
    }
}
