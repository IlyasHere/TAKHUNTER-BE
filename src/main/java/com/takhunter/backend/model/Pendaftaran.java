package com.takhunter.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pendaftaran")
public class Pendaftaran {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // ID Pendaftaran menggunakan kolom 'id' biasa
    private Long id;

    @Column(name = "event_id", nullable = false) // Memetakan ke kolom 'event_id' di PostgreSQL kamu
    private Long kegiatanId;

    @Column(name = "nama_mahasiswa")
    private String namaMahasiswa;

    @Column(name = "nim", nullable = false) // Tambahkan ini karena DB menuntut NIM tidak boleh null
    private String nim;

    @Column(name = "program_studi") // Memperbaiki penamaan default 'program_sudi' akibat translasi Hibernate
    private String programStudi;

    private String email;

    @Column(name = "no_whatsapp", nullable = false) 
    private String nomorWhatsApp;

    @Column(columnDefinition = "TEXT")
    private String alasan;

    private String status = "PENDING"; // Default status menunggu konfirmasi EO

    // ==================== GETTER AND SETTER ====================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKegiatanId() {
        return kegiatanId;
    }

    public void setKegiatanId(Long kegiatanId) {
        this.kegiatanId = kegiatanId;
    }

    public String getNamaMahasiswa() {
        return namaMahasiswa;
    }

    public void setNamaMahasiswa(String namaMahasiswa) {
        this.namaMahasiswa = namaMahasiswa;
    }

    public String getNim() { 
        return nim; 
    }
    
    public void setNim(String nim) { 
        this.nim = nim; 
    }

    public String getProgramStudi() {
        return programStudi;
    }

    public void setProgramStudi(String programStudi) {
        this.programStudi = programStudi;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNomorWhatsApp() {
        return nomorWhatsApp;
    }

    public void setNomorWhatsApp(String nomorWhatsApp) {
        this.nomorWhatsApp = nomorWhatsApp;
    }

    public String getAlasan() {
        return alasan;
    }

    public void setAlasan(String alasan) {
        this.alasan = alasan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}