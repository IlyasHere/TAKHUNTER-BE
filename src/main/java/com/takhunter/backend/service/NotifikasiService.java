package com.takhunter.backend.service;

import com.takhunter.backend.dto.NotifikasiListResponse;
import com.takhunter.backend.dto.NotifikasiResponse;
import com.takhunter.backend.model.EventOrganizer;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.Notifikasi;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.model.StatusPublikasi;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.EventOrganizerRepository;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.NotifikasiRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotifikasiService {

    private static final String ROLE_EVENT_ORGANIZER = "EVENT_ORGANIZER";

    private final NotifikasiRepository notifikasiRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final KegiatanRepository kegiatanRepository;
    private final PendaftaranRepository pendaftaranRepository;

    public NotifikasiService(
            NotifikasiRepository notifikasiRepository,
            EventOrganizerRepository eventOrganizerRepository,
            KegiatanRepository kegiatanRepository,
            PendaftaranRepository pendaftaranRepository
    ) {
        this.notifikasiRepository = notifikasiRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.pendaftaranRepository = pendaftaranRepository;
    }

    @Transactional
    public NotifikasiListResponse getNotifikasiForEO() {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        generateAutomaticReminders(eventOrganizer);

        List<NotifikasiResponse> data = notifikasiRepository.findByEventOrganizerIdOrderByCreatedAtDesc(eventOrganizer.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return new NotifikasiListResponse(data);
    }

    @Transactional
    public NotifikasiResponse markRead(Long id) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        Notifikasi notifikasi = notifikasiRepository.findByIdAndEventOrganizerId(id, eventOrganizer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notifikasi tidak ditemukan"));

        notifikasi.setIsRead(true);
        return toResponse(notifikasiRepository.save(notifikasi));
    }

    @Transactional
    public Map<String, String> markAllRead() {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        List<Notifikasi> unreadNotifications = notifikasiRepository.findByEventOrganizerIdAndIsReadFalse(eventOrganizer.getId());
        unreadNotifications.forEach((notifikasi) -> notifikasi.setIsRead(true));
        notifikasiRepository.saveAll(unreadNotifications);

        return Map.of("message", "Semua notifikasi ditandai sudah dibaca");
    }

    @Transactional
    public Map<String, String> deleteNotification(Long id) {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        Notifikasi notifikasi = notifikasiRepository.findByIdAndEventOrganizerId(id, eventOrganizer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notifikasi tidak ditemukan"));

        notifikasiRepository.delete(notifikasi);

        return Map.of("message", "Notifikasi berhasil dihapus");
    }

    @Transactional
    public Map<String, String> deleteAllNotifications() {
        EventOrganizer eventOrganizer = getLoggedInEventOrganizer();
        notifikasiRepository.deleteByEventOrganizerId(eventOrganizer.getId());

        return Map.of("message", "Semua notifikasi berhasil dihapus");
    }

    @Transactional
    public void notifyKegiatanCreated(Kegiatan kegiatan) {
        createNotification(
                kegiatan.getEventOrganizer(),
                "Kegiatan berhasil ditambahkan",
                kegiatan.getNamaKegiatan() + " berhasil ditambahkan.",
                "KEGIATAN_CREATED",
                "green",
                kegiatan.getId(),
                dedupeKey("KEGIATAN_CREATED", kegiatan.getEventOrganizer().getId(), kegiatan.getId(), null)
        );
    }

    @Transactional
    public void notifyKegiatanUpdated(Kegiatan kegiatan) {
        createNotification(
                kegiatan.getEventOrganizer(),
                "Kegiatan berhasil diedit",
                kegiatan.getNamaKegiatan() + " berhasil diperbarui.",
                "KEGIATAN_UPDATED",
                "green",
                kegiatan.getId(),
                dedupeKey("KEGIATAN_UPDATED", kegiatan.getEventOrganizer().getId(), kegiatan.getId(), null)
        );
    }

    @Transactional
    public void notifyKegiatanDeleted(Kegiatan kegiatan) {
        createNotification(
                kegiatan.getEventOrganizer(),
                "Kegiatan berhasil dihapus",
                kegiatan.getNamaKegiatan() + " berhasil dihapus.",
                "KEGIATAN_DELETED",
                "red",
                kegiatan.getId(),
                dedupeKey("KEGIATAN_DELETED", kegiatan.getEventOrganizer().getId(), kegiatan.getId(), null)
        );
    }

    @Transactional
    public void notifyProfileUpdated(User user) {
        findEventOrganizer(user).ifPresent((eventOrganizer) -> createNotification(
                eventOrganizer,
                "Profil EO berhasil diedit",
                "Profil Event Organizer berhasil diperbarui.",
                "PROFIL_DIEDIT",
                "green",
                null,
                uniqueKey("PROFIL_DIEDIT", eventOrganizer.getId(), null)
        ));
    }

    @Transactional
    public void notifyEmailChanged(User user) {
        findEventOrganizer(user).ifPresent((eventOrganizer) -> createNotification(
                eventOrganizer,
                "Email berhasil diganti",
                "Email akun Event Organizer berhasil diganti.",
                "EMAIL_DIGANTI",
                "green",
                null,
                uniqueKey("EMAIL_DIGANTI", eventOrganizer.getId(), null)
        ));
    }

    @Transactional
    public void notifyPasswordChanged(User user) {
        findEventOrganizer(user).ifPresent((eventOrganizer) -> createNotification(
                eventOrganizer,
                "Password berhasil diganti",
                "Password akun Event Organizer berhasil diganti.",
                "PASSWORD_DIGANTI",
                "green",
                null,
                uniqueKey("PASSWORD_DIGANTI", eventOrganizer.getId(), null)
        ));
    }

    @Transactional
    public void notifyNewRegistrant(Kegiatan kegiatan, Pendaftaran pendaftaran) {
        createNotification(
                kegiatan.getEventOrganizer(),
                "Pendaftar baru masuk",
                pendaftaran.getNamaMahasiswa() + " baru saja mendaftar ke " + kegiatan.getNamaKegiatan() + ".",
                "PENDAFTAR_BARU",
                "blue",
                kegiatan.getId(),
                dedupeKey("PENDAFTAR_BARU", kegiatan.getEventOrganizer().getId(), kegiatan.getId(), pendaftaran.getId())
        );
    }

    @Transactional
    public void notifyQuotaThresholds(Kegiatan kegiatan) {
        if (kegiatan.getKuotaPeserta() == null || kegiatan.getKuotaPeserta() <= 0) {
            return;
        }

        long totalPendaftar = pendaftaranRepository.countByKegiatanId(kegiatan.getId());
        long halfQuota = Math.max(1, (long) Math.ceil(kegiatan.getKuotaPeserta() / 2.0));

        if (totalPendaftar >= halfQuota) {
            createNotification(
                    kegiatan.getEventOrganizer(),
                    "Peserta mencapai 50% kuota",
                    "Pendaftar " + kegiatan.getNamaKegiatan() + " sudah mencapai 50% dari kuota.",
                    "KUOTA_50",
                    "amber",
                    kegiatan.getId(),
                    dedupeKey("KUOTA_50", kegiatan.getEventOrganizer().getId(), kegiatan.getId(), null)
            );
        }

        if (totalPendaftar >= kegiatan.getKuotaPeserta()) {
            createNotification(
                    kegiatan.getEventOrganizer(),
                    "Kuota peserta penuh",
                    "Kuota peserta " + kegiatan.getNamaKegiatan() + " sudah penuh.",
                    "KUOTA_PENUH",
                    "red",
                    kegiatan.getId(),
                    dedupeKey("KUOTA_PENUH", kegiatan.getEventOrganizer().getId(), kegiatan.getId(), null)
            );
        }
    }

    private void generateAutomaticReminders(EventOrganizer eventOrganizer) {
        LocalDate today = LocalDate.now();
        List<Kegiatan> kegiatanList = kegiatanRepository.findByEventOrganizerIdOrderByCreatedAtDesc(eventOrganizer.getId());

        for (Kegiatan kegiatan : kegiatanList) {
            if (StatusPublikasi.SELESAI.equals(kegiatan.getStatusPublikasi())) {
                continue;
            }

            createRegistrationReminderIfDue(eventOrganizer, kegiatan, today, 3);
            createRegistrationReminderIfDue(eventOrganizer, kegiatan, today, 1);
            createStartReminderIfDue(eventOrganizer, kegiatan, today, 3);
            createStartReminderIfDue(eventOrganizer, kegiatan, today, 2);
            createStartReminderIfDue(eventOrganizer, kegiatan, today, 1);
            notifyQuotaThresholds(kegiatan);
        }
    }

    private void createRegistrationReminderIfDue(EventOrganizer eventOrganizer, Kegiatan kegiatan, LocalDate today, int daysBefore) {
        if (kegiatan.getBatasPendaftaran() == null) {
            return;
        }

        long days = ChronoUnit.DAYS.between(today, kegiatan.getBatasPendaftaran());
        if (days != daysBefore) {
            return;
        }

        createNotification(
                eventOrganizer,
                "Pendaftaran ditutup dalam " + daysBefore + " hari",
                "Pendaftaran " + kegiatan.getNamaKegiatan() + " akan ditutup dalam " + daysBefore + " hari.",
                "PENDAFTARAN_H_" + daysBefore,
                "amber",
                kegiatan.getId(),
                dedupeKey("PENDAFTARAN_H_" + daysBefore, eventOrganizer.getId(), kegiatan.getId(), null)
        );
    }

    private void createStartReminderIfDue(EventOrganizer eventOrganizer, Kegiatan kegiatan, LocalDate today, int daysBefore) {
        if (kegiatan.getTanggal() == null) {
            return;
        }

        long days = ChronoUnit.DAYS.between(today, kegiatan.getTanggal());
        if (days != daysBefore) {
            return;
        }

        createNotification(
                eventOrganizer,
                "Kegiatan mulai dalam " + daysBefore + " hari",
                kegiatan.getNamaKegiatan() + " akan mulai dalam " + daysBefore + " hari.",
                "KEGIATAN_H_" + daysBefore,
                "blue",
                kegiatan.getId(),
                dedupeKey("KEGIATAN_H_" + daysBefore, eventOrganizer.getId(), kegiatan.getId(), null)
        );
    }

    private void createNotification(
            EventOrganizer eventOrganizer,
            String title,
            String message,
            String type,
            String tone,
            Long kegiatanId,
            String dedupeKey
    ) {
        if (eventOrganizer == null || notifikasiRepository.existsByDedupeKey(dedupeKey)) {
            return;
        }

        Notifikasi notifikasi = Notifikasi.builder()
                .eventOrganizer(eventOrganizer)
                .title(title)
                .message(message)
                .type(type)
                .tone(tone)
                .isRead(false)
                .kegiatanId(kegiatanId)
                .dedupeKey(dedupeKey)
                .build();

        notifikasiRepository.save(notifikasi);
    }

    private java.util.Optional<EventOrganizer> findEventOrganizer(User user) {
        if (user == null || !ROLE_EVENT_ORGANIZER.equals(normalizeRole(user.getRole()))) {
            return java.util.Optional.empty();
        }

        return eventOrganizerRepository.findByUserId(user.getId());
    }

    private EventOrganizer getLoggedInEventOrganizer() {
        User user = getLoggedInUser();
        if (!ROLE_EVENT_ORGANIZER.equals(normalizeRole(user.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Event Organizer yang boleh akses notifikasi");
        }

        return eventOrganizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Data Event Organizer tidak ditemukan"));
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wajib dikirim");
        }

        return user;
    }

    private NotifikasiResponse toResponse(Notifikasi notifikasi) {
        return NotifikasiResponse.builder()
                .id(notifikasi.getId())
                .title(notifikasi.getTitle())
                .message(notifikasi.getMessage())
                .type(notifikasi.getType())
                .tone(notifikasi.getTone())
                .isRead(notifikasi.getIsRead())
                .createdAt(notifikasi.getCreatedAt())
                .kegiatanId(notifikasi.getKegiatanId())
                .build();
    }

    private String uniqueKey(String type, Long eventOrganizerId, Long kegiatanId) {
        return type + ":" + eventOrganizerId + ":" + (kegiatanId == null ? UUID.randomUUID() : kegiatanId) + ":" + UUID.randomUUID();
    }

    private String dedupeKey(String type, Long eventOrganizerId, Long kegiatanId, Long entityId) {
        return type + ":" + eventOrganizerId + ":" + (kegiatanId == null ? "-" : kegiatanId) + ":" + (entityId == null ? "-" : entityId);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        return role.trim()
                .replace("-", "_")
                .toUpperCase();
    }
}
