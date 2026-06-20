package com.takhunter.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotifikasiListResponse {

    private List<NotifikasiResponse> data;
}
