package com.takhunter.backend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class NotifikasiResponse {

    private Long id;
    private String title;
    private String message;
    private String type;
    private String tone;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long kegiatanId;
}
