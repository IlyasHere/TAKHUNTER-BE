package com.takhunter.backend.controller;

import com.takhunter.backend.dto.CreateKegiatanRequest;
import com.takhunter.backend.dto.KegiatanResponse;
import com.takhunter.backend.dto.PendaftaranResponse;
import com.takhunter.backend.service.KegiatanService;
import com.takhunter.backend.service.PendaftaranService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final PendaftaranService pendaftaranService;

    public KegiatanController(KegiatanService kegiatanService, PendaftaranService pendaftaranService) {
        this.kegiatanService = kegiatanService;
        this.pendaftaranService = pendaftaranService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KegiatanResponse create(
            @ModelAttribute CreateKegiatanRequest request,
            @RequestParam(value = "banner", required = false) MultipartFile banner
    ) {
        return kegiatanService.create(request, banner);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KegiatanResponse createJson(@RequestBody CreateKegiatanRequest request) {
        return kegiatanService.create(request, null);
    }

    @GetMapping
    public List<KegiatanResponse> getAllMine() {
        return kegiatanService.getAllMine();
    }

    @GetMapping("/{id}")
    public KegiatanResponse getDetail(@PathVariable Long id) {
        return kegiatanService.getDetail(id);
    }

    @GetMapping("/{id}/pendaftaran")
    public List<PendaftaranResponse> getPendaftar(@PathVariable Long id) {
        return pendaftaranService.getPendaftarForMyKegiatan(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KegiatanResponse update(
            @PathVariable Long id,
            @ModelAttribute CreateKegiatanRequest request,
            @RequestParam(value = "banner", required = false) MultipartFile banner
    ) {
        return kegiatanService.update(id, request, banner);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public KegiatanResponse updateJson(
            @PathVariable Long id,
            @RequestBody CreateKegiatanRequest request
    ) {
        return kegiatanService.update(id, request, null);
    }

    @PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KegiatanResponse updateWithPost(
            @PathVariable Long id,
            @ModelAttribute CreateKegiatanRequest request,
            @RequestParam(value = "banner", required = false) MultipartFile banner
    ) {
        return kegiatanService.update(id, request, banner);
    }

    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public KegiatanResponse updateJsonWithPost(
            @PathVariable Long id,
            @RequestBody CreateKegiatanRequest request
    ) {
        return kegiatanService.update(id, request, null);
    }

    @PatchMapping("/{id}/status")
    public KegiatanResponse updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return kegiatanService.updateStatus(id, request.get("statusPublikasi") != null ? request.get("statusPublikasi") : request.get("status"));
    }

    @PostMapping("/{id}/finish")
    public KegiatanResponse finish(@PathVariable Long id) {
        return kegiatanService.finish(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        kegiatanService.delete(id);
    }
}
