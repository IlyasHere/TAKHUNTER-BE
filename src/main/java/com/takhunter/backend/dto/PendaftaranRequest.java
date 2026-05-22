package com.takhunter.backend.dto;

public class PendaftaranRequest {
    private Long kegiatanId; // ID kegiatan yang didaftar
    private Long eventId; // Alias dari frontend versi lama
    private String namaMahasiswa;
    private String nim;
    private String programStudi;
    private String email;
    private String nomorWhatsApp;
    private String noWhatsapp; // Alias dari frontend versi lama
    private String alasan;

    // Tambahkan Getter dan Setter (atau gunakan @Data jika pakai Lombok)
    public Long getKegiatanId() {
        return kegiatanId != null ? kegiatanId : eventId;
    }

    public void setKegiatanId(Long kegiatanId) {
        this.kegiatanId = kegiatanId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
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
        return nomorWhatsApp != null ? nomorWhatsApp : noWhatsapp;
    }

    public void setNomorWhatsApp(String nomorWhatsApp) {
        this.nomorWhatsApp = nomorWhatsApp;
    }

    public String getNoWhatsapp() {
        return noWhatsapp;
    }

    public void setNoWhatsapp(String noWhatsapp) {
        this.noWhatsapp = noWhatsapp;
    }

    public String getAlasan() {
        return alasan;
    }

    public void setAlasan(String alasan) {
        this.alasan = alasan;
    }
}
