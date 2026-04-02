package com.smarthire.auth_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ValidateResponse {
    private boolean valid;
    private String userId;
    private String email;
    private String role;
}