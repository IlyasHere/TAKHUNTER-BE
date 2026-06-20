package com.takhunter.backend.controller;

import com.takhunter.backend.dto.NotifikasiListResponse;
import com.takhunter.backend.dto.NotifikasiResponse;
import com.takhunter.backend.service.NotifikasiService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eo/notifikasi")
public class EONotifikasiController {

    private final NotifikasiService notifikasiService;

    public EONotifikasiController(NotifikasiService notifikasiService) {
        this.notifikasiService = notifikasiService;
    }

    @GetMapping
    public NotifikasiListResponse getNotifikasi() {
        return notifikasiService.getNotifikasiForEO();
    }

    @PatchMapping("/{id}/read")
    public NotifikasiResponse markRead(@PathVariable Long id) {
        return notifikasiService.markRead(id);
    }

    @PatchMapping("/read-all")
    public Map<String, String> markAllRead() {
        return notifikasiService.markAllRead();
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteNotification(@PathVariable Long id) {
        return notifikasiService.deleteNotification(id);
    }

    @DeleteMapping
    public Map<String, String> deleteAllNotifications() {
        return notifikasiService.deleteAllNotifications();
    }
}
