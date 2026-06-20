package com.takhunter.backend.controller;

import com.takhunter.backend.dto.KegiatanResponse;
import com.takhunter.backend.service.KegiatanService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kegiatan")
public class PublicKegiatanController {

    private final KegiatanService kegiatanService;

    public PublicKegiatanController(KegiatanService kegiatanService) {
        this.kegiatanService = kegiatanService;
    }

    @GetMapping
    public List<KegiatanResponse> getAllActive() {
        return kegiatanService.getAllActive();
    }

    @GetMapping("/{id}")
    public KegiatanResponse getActiveDetail(@PathVariable Long id) {
        return kegiatanService.getActiveDetail(id);
    }
}
