package com.takhunter.backend.controller;

import com.takhunter.backend.dto.PendaftaranRequest;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.repository.PendaftaranRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pendaftaran")
@CrossOrigin(origins = "*")
public class PendaftaranController {

    private final PendaftaranRepository pendaftaranRepository;

    public PendaftaranController(PendaftaranRepository pendaftaranRepository) {
        this.pendaftaranRepository = pendaftaranRepository;
    }

    @PostMapping
    public ResponseEntity<?> daftarKegiatan(@RequestBody PendaftaranRequest request) {
        try {
            Pendaftaran pendaftaran = new Pendaftaran();
            pendaftaran.setKegiatanId(request.getKegiatanId());
            pendaftaran.setNamaMahasiswa(request.getNamaMahasiswa());
            pendaftaran.setNim(request.getNim());
            pendaftaran.setProgramStudi(request.getProgramStudi());
            pendaftaran.setEmail(request.getEmail());
            pendaftaran.setNomorWhatsApp(request.getNomorWhatsApp());
            pendaftaran.setAlasan(request.getAlasan());
            
            // Simpan ke database
            pendaftaranRepository.save(pendaftaran);

            return ResponseEntity.ok("{\"message\": \"Pendaftaran berhasil disimpan!\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"message\": \"Gagal mendaftar: " + e.getMessage() + "\"}");
        }
    }
}