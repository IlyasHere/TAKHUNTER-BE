package com.takhunter.backend.controller;

import com.takhunter.backend.dto.CreateKegiatanRequest;
import com.takhunter.backend.dto.KegiatanResponse;
import com.takhunter.backend.service.KegiatanService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/eo/kegiatan")
public class KegiatanController {

    private final KegiatanService kegiatanService;

    public KegiatanController(KegiatanService kegiatanService) {
        this.kegiatanService = kegiatanService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KegiatanResponse create(
            @ModelAttribute CreateKegiatanRequest request,
            @RequestParam(value = "banner", required = false) MultipartFile banner
    ) {
        return kegiatanService.create(request, banner);
    }

    @GetMapping
    public List<KegiatanResponse> getAllMine() {
        return kegiatanService.getAllMine();
    }

    @GetMapping("/{id}")
    public KegiatanResponse getDetail(@PathVariable Long id) {
        return kegiatanService.getDetail(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KegiatanResponse update(
            @PathVariable Long id,
            @ModelAttribute CreateKegiatanRequest request,
            @RequestParam(value = "banner", required = false) MultipartFile banner
    ) {
        return kegiatanService.update(id, request, banner);
    }

    @PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KegiatanResponse updateWithPost(
            @PathVariable Long id,
            @ModelAttribute CreateKegiatanRequest request,
            @RequestParam(value = "banner", required = false) MultipartFile banner
    ) {
        return kegiatanService.update(id, request, banner);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        kegiatanService.delete(id);
    }
}
