package com.takhunter.backend.config;

import com.takhunter.backend.model.EventOrganizer;
import com.takhunter.backend.model.KategoriKegiatan;
import com.takhunter.backend.model.Kegiatan;
import com.takhunter.backend.model.Mahasiswa;
import com.takhunter.backend.model.Pendaftaran;
import com.takhunter.backend.model.Sertifikat;
import com.takhunter.backend.model.StatusPublikasi;
import com.takhunter.backend.model.StatusSertifikat;
import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.EventOrganizerRepository;
import com.takhunter.backend.repository.KegiatanRepository;
import com.takhunter.backend.repository.MahasiswaRepository;
import com.takhunter.backend.repository.PendaftaranRepository;
import com.takhunter.backend.repository.SertifikatRepository;
import com.takhunter.backend.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final KegiatanRepository kegiatanRepository;
    private final PendaftaranRepository pendaftaranRepository;
    private final SertifikatRepository sertifikatRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            MahasiswaRepository mahasiswaRepository,
            EventOrganizerRepository eventOrganizerRepository,
            KegiatanRepository kegiatanRepository,
            PendaftaranRepository pendaftaranRepository,
            SertifikatRepository sertifikatRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.eventOrganizerRepository = eventOrganizerRepository;
        this.kegiatanRepository = kegiatanRepository;
        this.pendaftaranRepository = pendaftaranRepository;
        this.sertifikatRepository = sertifikatRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User eoUser = userRepository.save(User.builder()
                .name("Tak Hunter EO")
                .email("eo@takhunter.test")
                .password(passwordEncoder.encode("password123"))
                .role("EVENT_ORGANIZER")
                .phone("081200000001")
                .city("Bandung")
                .address("Telkom University")
                .build());
        EventOrganizer eo = eventOrganizerRepository.save(EventOrganizer.builder()
                .user(eoUser)
                .namaOrganisasi("TAK Hunter Organizer")
                .build());

        User mahasiswaUser = userRepository.save(User.builder()
                .name("Alya Maharani")
                .email("alya@student.test")
                .password(passwordEncoder.encode("password123"))
                .role("MAHASISWA")
                .phone("081234560011")
                .city("Bandung")
                .address("Sukapura")
                .build());
        Mahasiswa mahasiswa = mahasiswaRepository.save(Mahasiswa.builder()
                .nim("1301223011")
                .totalPoinTak(15)
                .user(mahasiswaUser)
                .build());

        User mahasiswaUser2 = userRepository.save(User.builder()
                .name("Bima Pratama")
                .email("bima@student.test")
                .password(passwordEncoder.encode("password123"))
                .role("MAHASISWA")
                .phone("081234560022")
                .city("Bandung")
                .address("Dayeuhkolot")
                .build());
        Mahasiswa mahasiswa2 = mahasiswaRepository.save(Mahasiswa.builder()
                .nim("1301223022")
                .totalPoinTak(0)
                .user(mahasiswaUser2)
                .build());

        Kegiatan lomba = saveKegiatan(eo, "Intelecta Cup", "Kompetisi inovasi digital mahasiswa.", KategoriKegiatan.LOMBA, 15, LocalDate.now().plusDays(7), LocalTime.of(9, 0), "Auditorium Telkom University", 100, StatusPublikasi.AKTIF);
        Kegiatan seminar = saveKegiatan(eo, "Seminar Karier", "Sesi persiapan karier bersama praktisi.", KategoriKegiatan.SEMINAR, 10, LocalDate.now().plusDays(10), LocalTime.of(13, 0), "Gedung Damar", 80, StatusPublikasi.AKTIF);
        saveKegiatan(eo, "Bootcamp Web", "Pelatihan web frontend intensif.", KategoriKegiatan.BOOTCAMP, 20, LocalDate.now().plusDays(14), LocalTime.of(10, 0), "Lab Programming FIT", 60, StatusPublikasi.DRAFT);
        saveKegiatan(eo, "Kepanitiaan Expo", "Rekrutmen kepanitiaan acara kampus.", KategoriKegiatan.KEPANITIAAN, 12, LocalDate.now().plusDays(17), LocalTime.of(8, 30), "Student Center", 50, StatusPublikasi.AKTIF);

        Pendaftaran diterima = savePendaftaran(lomba, mahasiswa, "S1 Informatika", "Ingin mengikuti kompetisi dan menambah pengalaman.", "DITERIMA");
        savePendaftaran(lomba, mahasiswa2, "S1 Sistem Informasi", "Tertarik bidang inovasi digital.", "PENDING");
        savePendaftaran(seminar, mahasiswa2, "S1 Sistem Informasi", "Butuh insight magang dan CV.", "DITERIMA");

        sertifikatRepository.save(Sertifikat.builder()
                .kegiatan(lomba)
                .pendaftaran(diterima)
                .mahasiswa(mahasiswa)
                .eventOrganizer(eo)
                .driveLink("https://drive.google.com/drive/folders/demo-sertifikat")
                .poinTak(lomba.getPoinTak())
                .status(StatusSertifikat.TERBIT)
                .build());
    }

    private Kegiatan saveKegiatan(
            EventOrganizer eo,
            String nama,
            String deskripsi,
            KategoriKegiatan kategori,
            Integer poinTak,
            LocalDate tanggal,
            LocalTime waktu,
            String lokasi,
            Integer kuota,
            StatusPublikasi status
    ) {
        return kegiatanRepository.save(Kegiatan.builder()
                .namaKegiatan(nama)
                .deskripsi(deskripsi)
                .kategori(kategori)
                .poinTak(poinTak)
                .wajib(false)
                .tanggal(tanggal)
                .waktu(waktu)
                .lokasi(lokasi)
                .kuotaPeserta(kuota)
                .batasPendaftaran(tanggal.minusDays(1))
                .statusPublikasi(status)
                .eventOrganizer(eo)
                .build());
    }

    private Pendaftaran savePendaftaran(Kegiatan kegiatan, Mahasiswa mahasiswa, String programStudi, String alasan, String status) {
        Pendaftaran pendaftaran = new Pendaftaran();
        pendaftaran.setKegiatanId(kegiatan.getId());
        pendaftaran.setMahasiswaId(mahasiswa.getId());
        pendaftaran.setNamaMahasiswa(mahasiswa.getUser().getName());
        pendaftaran.setNim(mahasiswa.getNim());
        pendaftaran.setProgramStudi(programStudi);
        pendaftaran.setEmail(mahasiswa.getUser().getEmail());
        pendaftaran.setNomorWhatsApp(mahasiswa.getUser().getPhone());
        pendaftaran.setAlasan(alasan);
        pendaftaran.setStatus(status);

        return pendaftaranRepository.save(pendaftaran);
    }
}
